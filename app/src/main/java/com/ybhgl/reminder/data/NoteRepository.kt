package com.ybhgl.reminder.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * 便签数据仓库。
 *
 * 与 [TagRepository] 保持同一套「改动即触发自动备份」的约定：
 * 便签数据同样纳入统一备份（BackupData.notes / BackupData.noteCategories）。
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val noteCategoryDao: NoteCategoryDao,
    private val context: Context
) {

    // ---------- 便签 ----------

    fun getAllNotesFlow(): Flow<List<NoteItem>> = noteDao.getAllNotesFlow()

    suspend fun getAllNotes(): List<NoteItem> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Int): NoteItem? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteItem): Long {
        val id = noteDao.insert(note)
        notifyDataChanged()
        return id
    }

    suspend fun updateNote(note: NoteItem) {
        noteDao.update(note)
        notifyDataChanged()
    }

    suspend fun deleteNote(note: NoteItem) {
        noteDao.delete(note)
        notifyDataChanged()
    }

    suspend fun deleteNoteById(id: Int) {
        noteDao.deleteById(id)
        notifyDataChanged()
    }

    suspend fun setPinned(id: Int, pinned: Boolean) {
        noteDao.setPinned(id, pinned)
        notifyDataChanged()
    }

    suspend fun deleteAllNotes() {
        noteDao.deleteAll()
        notifyDataChanged()
    }

    suspend fun countNotes(): Int = noteDao.count()

    // ---------- 分类 ----------

    fun getAllCategoriesFlow(): Flow<List<NoteCategoryItem>> = noteCategoryDao.getAllCategoriesFlow()

    suspend fun getAllCategories(): List<NoteCategoryItem> = noteCategoryDao.getAllCategories()

    suspend fun insertCategory(category: NoteCategoryItem): Long {
        val id = noteCategoryDao.insert(category)
        notifyDataChanged()
        return id
    }

    suspend fun updateCategory(category: NoteCategoryItem) {
        noteCategoryDao.update(category)
        notifyDataChanged()
    }

    /** 设置分类是否隐藏（仅影响筛选展示，不删除分类与便签） */
    suspend fun setCategoryHidden(id: Int, hidden: Boolean) {
        noteCategoryDao.setHidden(id, hidden)
        notifyDataChanged()
    }

    /** 删除分类，该分类下的便签回到「未分类」，不丢数据 */
    suspend fun deleteCategoryAndDetachNotes(category: NoteCategoryItem) {
        noteCategoryDao.delete(category)
        noteDao.detachCategory(category.id)
        notifyDataChanged()
    }

    suspend fun updateCategorySortOrders(categories: List<NoteCategoryItem>) {
        noteCategoryDao.updateCategorySortOrders(categories)
        notifyDataChanged()
    }

    suspend fun deleteAllCategories() {
        noteCategoryDao.deleteAll()
        notifyDataChanged()
    }

    // ---------- 备份恢复 ----------

    /**
     * 整体覆盖恢复：清空后按备份内容重建，并重建分类 id 映射。
     * @return 恢复的便签条数
     */
    suspend fun restoreReplace(
        notes: List<NoteItem>,
        categories: List<NoteCategoryItem>?
    ): Int {
        noteDao.deleteAll()
        noteCategoryDao.deleteAll()

        val idMapping = mutableMapOf<Int, Int>()
        categories?.sortedBy { it.sortOrder }?.forEachIndexed { index, category ->
            val newId = noteCategoryDao.insert(category.copy(id = 0, sortOrder = index))
            if (newId > 0) idMapping[category.id] = newId.toInt()
        }

        notes.forEach { note ->
            noteDao.insert(
                note.copy(
                    id = 0,
                    categoryId = idMapping[note.categoryId] ?: 0,
                    updatedAt = if (note.updatedAt > 0L) note.updatedAt else System.currentTimeMillis()
                )
            )
        }
        notifyDataChanged()
        return notes.size
    }

    /**
     * 智能合并：分类按名称复用，便签按「创建时间 + 标题」判重，只补新增项。
     * @return Pair(新增条数, 更新条数)
     */
    suspend fun restoreMerge(
        notes: List<NoteItem>,
        categories: List<NoteCategoryItem>?
    ): Pair<Int, Int> {
        val categoryIdMapping = mutableMapOf<Int, Int>()
        val existingCategories = noteCategoryDao.getAllCategories().toMutableList()

        categories?.sortedBy { it.sortOrder }?.forEach { category ->
            val matched = existingCategories.find { it.name == category.name }
            if (matched != null) {
                categoryIdMapping[category.id] = matched.id
            } else {
                val newId = noteCategoryDao.insert(
                    category.copy(id = 0, sortOrder = existingCategories.size)
                )
                if (newId > 0) {
                    val created = category.copy(id = newId.toInt(), sortOrder = existingCategories.size)
                    existingCategories.add(created)
                    categoryIdMapping[category.id] = created.id
                }
            }
        }

        val existingNotes = noteDao.getAllNotes()
        var inserted = 0
        var updated = 0

        notes.forEach { note ->
            val targetCategoryId = categoryIdMapping[note.categoryId] ?: 0
            val matched = existingNotes.find {
                it.createdAt == note.createdAt && it.title == note.title
            }
            if (matched == null) {
                noteDao.insert(
                    note.copy(
                        id = 0,
                        categoryId = targetCategoryId,
                        updatedAt = if (note.updatedAt > 0L) note.updatedAt else System.currentTimeMillis()
                    )
                )
                inserted++
            } else if (matched.content != note.content || matched.categoryId != targetCategoryId) {
                if (note.updatedAt >= matched.updatedAt) {
                    noteDao.update(
                        matched.copy(
                            content = note.content,
                            categoryId = targetCategoryId,
                            updatedAt = note.updatedAt
                        )
                    )
                    updated++
                }
            }
        }
        if (inserted > 0 || updated > 0) notifyDataChanged()
        return inserted to updated
    }

    private suspend fun notifyDataChanged() {
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context)
    }
}
