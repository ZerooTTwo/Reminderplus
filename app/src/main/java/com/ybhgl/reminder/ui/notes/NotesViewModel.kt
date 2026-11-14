package com.ybhgl.reminder.ui.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.BackupData
import com.ybhgl.reminder.data.BackupPreferences
import com.ybhgl.reminder.data.ManualWebDavBackup
import com.ybhgl.reminder.data.NoteCategoryItem
import com.ybhgl.reminder.data.NoteItem
import com.ybhgl.reminder.data.NoteRepository
import com.ybhgl.reminder.data.NotesUiPreferences
import com.ybhgl.reminder.util.BackupArchiveManager
import com.ybhgl.reminder.util.WebDavClient
import com.ybhgl.reminder.util.WebDavDownloadBytesResult
import com.ybhgl.reminder.util.WebDavListResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 便签列表 / 分类 / WebDAV 同步的 ViewModel。
 *
 * 同步策略：
 * - 上传：直接复用应用既有的统一备份链路（BackupData 现已包含 notes 字段），
 *   因此加密、压缩、WebDAV 目录轮转、自动备份计数全部沿用原有实现；
 * - 恢复：只从云端最新的自动备份里取回便签做「合并」，不会覆盖本机提醒数据。
 */
class NotesViewModel(
    private val noteRepository: NoteRepository,
    private val appContext: Context
) : ViewModel() {

    companion object {
        const val FILTER_ALL = 0
        const val FILTER_UNCATEGORIZED = -1

        /** 分类可选颜色（与 Material 调色板保持一致） */
        val CATEGORY_COLORS = listOf(
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#FFC107", "#FF9800", "#FF5722", "#F44336",
            "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#607D8B"
        )
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filterCategoryId = MutableStateFlow(FILTER_ALL)
    val filterCategoryId: StateFlow<Int> = _filterCategoryId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val categories: StateFlow<List<NoteCategoryItem>> =
        noteRepository.getAllCategoriesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 「全部」标签是否被用户手动隐藏（只隐藏标签，不影响内容） */
    val hideAllChip: StateFlow<Boolean> =
        NotesUiPreferences.hideAllChipFlow(appContext)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setHideAllChip(hide: Boolean) {
        viewModelScope.launch { NotesUiPreferences.saveHideAllChip(appContext, hide) }
    }

    /** 未筛选的便签全集：用于判断「未分类」标签是否还有内容 */
    val allNotes: StateFlow<List<NoteItem>> =
        noteRepository.getAllNotesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<NoteItem>> = combine(
        noteRepository.getAllNotesFlow(),
        _query,
        _filterCategoryId,
        categories
    ) { list, keyword, filterId, categoryList ->
        val trimmed = keyword.trim()
        // 隐藏分类 = 连同该分类下的便签一起隐藏
        val hiddenCategoryIds = categoryList.filter { it.isHidden }.map { it.id }.toSet()
        list.filter { note ->
            val notInHiddenCategory = note.categoryId == 0 || note.categoryId !in hiddenCategoryIds
            val matchCategory = when {
                filterId == FILTER_ALL -> true
                filterId == FILTER_UNCATEGORIZED -> note.categoryId == 0
                else -> note.categoryId == filterId
            }
            val matchKeyword = trimmed.isEmpty() ||
                note.title.contains(trimmed, ignoreCase = true) ||
                note.content.contains(trimmed, ignoreCase = true)
            notInHiddenCategory && matchCategory && matchKeyword
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- 列表筛选 ----------

    fun setQuery(value: String) { _query.value = value }

    fun setFilterCategory(categoryId: Int) { _filterCategoryId.value = categoryId }

    // ---------- 便签读写 ----------

    fun saveNote(
        id: Int,
        title: String,
        content: String,
        categoryId: Int,
        createdAt: Long,
        onDone: () -> Unit = {}
    ) {
        if (title.isBlank() && content.isBlank()) {
            // 空白便签不落库，但流程要正常返回，否则返回键会被卡住
            onDone()
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (id > 0) {
                val existing = noteRepository.getNoteById(id)
                if (existing != null) {
                    noteRepository.updateNote(
                        existing.copy(
                            title = title,
                            content = content,
                            categoryId = categoryId,
                            updatedAt = now
                        )
                    )
                }
            } else {
                noteRepository.insertNote(
                    NoteItem(
                        title = title,
                        content = content,
                        categoryId = categoryId,
                        createdAt = if (createdAt > 0L) createdAt else now,
                        updatedAt = now
                    )
                )
            }
            onDone()
        }
    }

    /** 编辑页载入单条便签（IO 线程读库） */
    suspend fun loadNote(id: Int): NoteItem? = withContext(Dispatchers.IO) {
        noteRepository.getNoteById(id)
    }

    fun togglePin(note: NoteItem) {
        viewModelScope.launch { noteRepository.setPinned(note.id, !note.isPinned) }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch { noteRepository.deleteNote(note) }
    }

    /** 编辑页删除：写完库再回调，避免页面出栈导致协程被取消 */
    fun deleteNoteById(id: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            noteRepository.deleteNoteById(id)
            onDone()
        }
    }

    // ---------- 分类维护 ----------

    fun addCategory(name: String, color: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (noteRepository.getAllCategories().any { it.name == trimmed }) {
                _syncMessage.value = "已存在同名的便签分类"
                return@launch
            }
            val sortOrder = noteRepository.getAllCategories().size
            noteRepository.insertCategory(NoteCategoryItem(name = trimmed, color = color, sortOrder = sortOrder))
        }
    }

    fun updateCategory(category: NoteCategoryItem) {
        val trimmed = category.name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (noteRepository.getAllCategories().any { it.id != category.id && it.name == trimmed }) {
                _syncMessage.value = "已存在同名的便签分类"
                return@launch
            }
            noteRepository.updateCategory(category.copy(name = trimmed))
        }
    }

    fun deleteCategory(category: NoteCategoryItem) {
        viewModelScope.launch {
            noteRepository.deleteCategoryAndDetachNotes(category)
            if (_filterCategoryId.value == category.id) _filterCategoryId.value = FILTER_ALL
        }
    }

    /** 切换分类的隐藏状态：隐藏后不出现在筛选栏与分类管理中，数据保留 */
    fun setCategoryHidden(category: NoteCategoryItem, hidden: Boolean) {
        viewModelScope.launch {
            noteRepository.setCategoryHidden(category.id, hidden)
            // 正在按该分类筛选时，隐藏后自动回落到「全部」，避免列表被一个看不见的分类过滤
            if (hidden && _filterCategoryId.value == category.id) {
                _filterCategoryId.value = FILTER_ALL
            }
        }
    }

    // ---------- WebDAV ----------

    fun clearSyncMessage() { _syncMessage.value = null }

    /** 上传：走「手动备份」链路（不依赖自动备份开关），备份数据里已包含便签 */
    fun uploadToWebDav() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            val result = runCatching { ManualWebDavBackup.upload(appContext) }.getOrNull()
            _syncMessage.value = result?.message
                ?: "上传失败：发生未知异常，请确认 WebDAV 地址与网络后重试"
            _isSyncing.value = false
        }
    }

    /**
     * 恢复：取云端最新的备份，仅把其中的便签合并回本机。
     *
     * 同时扫描根目录（手动备份：reminder-backup-*.zip）与 Auto 目录
     * （自动备份：reminder-autobackup-*.zip），按文件名里的时间戳取最新的一份。
     */
    fun restoreNotesFromWebDav() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                runCatching {
                    val server = BackupPreferences.webDavServerFlow(appContext).first()
                    val user = BackupPreferences.webDavUsernameFlow(appContext).first()
                    val password = BackupPreferences.webDavPasswordFlow(appContext).first()
                    val path = BackupPreferences.webDavPathFlow(appContext).first()
                    if (server.isBlank() || user.isBlank() || password.isBlank()) {
                        return@runCatching "请先在「设置 → 备份与恢复」中填写并保存 WebDAV 服务器、账号和密码"
                    }

                    val autoPath = if (path.endsWith("/")) "${path}Auto" else "$path/Auto"

                    fun listBackups(dir: String): List<Pair<String, String>> {
                        val listing = WebDavClient.listFilesActual(server, user, password, dir)
                        if (listing !is WebDavListResult.Success) return emptyList()
                        return listing.files
                            .filter { it.name.startsWith("reminder-") && it.name.endsWith(".zip") }
                            .map { dir to it.name }
                    }

                    val candidates = (listBackups(path) + listBackups(autoPath))
                        .sortedByDescending { (_, name) -> timestampOf(name) }

                    if (candidates.isEmpty()) {
                        return@runCatching "云端没有找到备份文件（已扫描 $path 与 $autoPath）"
                    }

                    // 逐个候选尝试：跳过无法下载/无法解密/不含便签的文件，直到找到可用的一份
                    val tried = mutableListOf<String>()
                    for ((dir, fileName) in candidates) {
                        tried += fileName
                        val downloaded = WebDavClient.downloadFileBytes(server, user, password, dir, fileName)
                        if (downloaded !is WebDavDownloadBytesResult.Success) continue
                        val archive = BackupArchiveManager.decode(downloaded.content) ?: continue
                        val backupData = runCatching {
                            Json { ignoreUnknownKeys = true }
                                .decodeFromString<BackupData>(archive.metadataJson)
                        }.getOrNull() ?: continue
                        val notes = backupData.notes.orEmpty()
                        if (notes.isEmpty()) continue

                        val (inserted, updated) = noteRepository.restoreMerge(notes, backupData.noteCategories)
                        return@runCatching if (inserted == 0 && updated == 0) {
                            "云端便签与本机一致，无需恢复（$fileName）"
                        } else {
                            "已从 $fileName 恢复便签：新增 $inserted 条、更新 $updated 条"
                        }
                    }
                    return@runCatching "云端共 ${candidates.size} 个备份（${tried.joinToString("、")}），但都无法下载、解密或不含便签"
                }.getOrElse { "恢复失败：${it.localizedMessage ?: "未知错误"}" }
            }
            _syncMessage.value = message
            _isSyncing.value = false
        }
    }

    /** 从备份文件名中取出 yyyyMMdd-HHmmss 时间戳用于排序，取不到返回空串 */
    private fun timestampOf(fileName: String): String =
        Regex("(\\d{8}-\\d{6})").find(fileName)?.value.orEmpty()
}
