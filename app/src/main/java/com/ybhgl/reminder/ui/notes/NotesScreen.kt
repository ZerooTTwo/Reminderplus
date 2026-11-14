@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.ybhgl.reminder.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.data.NoteCategoryItem
import com.ybhgl.reminder.data.NoteItem
import com.ybhgl.reminder.data.displayTitle
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.CustomToast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** hex 字符串转 Compose 颜色，非法值退化为灰色 */
internal fun noteHexColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF9E9E9E))

private val noteTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.getDefault())

internal fun formatNoteTime(millis: Long): String {
    if (millis <= 0L) return ""
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(noteTimeFormatter)
}

@Composable
fun NotesScreen(
    onNavigateBack: () -> Unit,
    onOpenNote: (Int) -> Unit,
    viewModel: NotesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val query by viewModel.query.collectAsState()
    val filterCategoryId by viewModel.filterCategoryId.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var showCategoryDialog by remember { mutableStateOf(false) }

    // 同步 / 校验结果的轻提示
    LaunchedEffect(syncMessage) {
        val message = syncMessage ?: return@LaunchedEffect
        CustomToast.show(context, message, CustomToast.Type.NORMAL)
        viewModel.clearSyncMessage()
    }

    val categoryNameById = remember(categories) { categories.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("便签", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.uploadToWebDav() }, enabled = !isSyncing) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "上传备份到 WebDAV（含便签）")
                    }
                    IconButton(onClick = { viewModel.restoreNotesFromWebDav() }, enabled = !isSyncing) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "从 WebDAV 恢复便签")
                    }
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Default.Category, contentDescription = "便签分类")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenNote(NEW_NOTE_ID) }) {
                Icon(Icons.Default.Add, contentDescription = "新建便签")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("搜索便签") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清空搜索")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    NoteFilterChip(
                        label = "全部",
                        selected = filterCategoryId == NotesViewModel.FILTER_ALL,
                        onClick = { viewModel.setFilterCategory(NotesViewModel.FILTER_ALL) }
                    )
                }
                item {
                    NoteFilterChip(
                        label = "未分类",
                        selected = filterCategoryId == NotesViewModel.FILTER_UNCATEGORIZED,
                        onClick = { viewModel.setFilterCategory(NotesViewModel.FILTER_UNCATEGORIZED) }
                    )
                }
                items(categories, key = { it.id }) { category ->
                    NoteFilterChip(
                        label = category.name,
                        selected = filterCategoryId == category.id,
                        color = noteHexColor(category.color),
                        onClick = { viewModel.setFilterCategory(category.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isSyncing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在与 WebDAV 同步…", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isBlank() && filterCategoryId == NotesViewModel.FILTER_ALL) {
                            "还没有便签，点右下角新建"
                        } else {
                            "没有匹配的便签"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            category = categoryNameById[note.categoryId],
                            onClick = { onOpenNote(note.id) },
                            onTogglePin = { viewModel.togglePin(note) },
                            onDelete = { viewModel.deleteNote(note) }
                        )
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        NoteCategoryDialog(
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onAdd = { name, color -> viewModel.addCategory(name, color) },
            onUpdate = { viewModel.updateCategory(it) },
            onDelete = { viewModel.deleteCategory(it) }
        )
    }
}

@Composable
internal fun NoteFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color? = null
) {
    val leadingIcon: (@Composable () -> Unit)? = if (color != null) {
        {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        }
    } else {
        null
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun NoteCard(
    note: NoteItem,
    category: NoteCategoryItem?,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "已置顶",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = note.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onTogglePin, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = if (note.isPinned) "取消置顶" else "置顶",
                        tint = if (note.isPinned) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除便签",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (category != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(noteHexColor(category.color), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = formatNoteTime(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除便签") },
            text = { Text("删除后无法恢复，确定删除「${note.displayTitle}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}

// ---------- 分类管理 ----------

@Composable
internal fun NoteCategoryDialog(
    categories: List<NoteCategoryItem>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onUpdate: (NoteCategoryItem) -> Unit,
    onDelete: (NoteCategoryItem) -> Unit
) {
    var editing by remember { mutableStateOf<NoteCategoryItem?>(null) }
    var adding by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("便签分类") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (categories.isEmpty()) {
                    Text(
                        text = "还没有分类，新建后便签就能分类整理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = category }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(noteHexColor(category.color), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { onDelete(category) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除分类",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { adding = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新建分类")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )

    val editingCategory = editing
    if (editingCategory != null) {
        CategoryEditDialog(
            initial = editingCategory,
            onDismiss = { editing = null },
            onConfirm = { name, color ->
                onUpdate(editingCategory.copy(name = name, color = color))
                editing = null
            }
        )
    }
    if (adding) {
        CategoryEditDialog(
            initial = null,
            onDismiss = { adding = false },
            onConfirm = { name, color ->
                onAdd(name, color)
                adding = false
            }
        )
    }
}

@Composable
private fun CategoryEditDialog(
    initial: NoteCategoryItem?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var color by remember(initial?.id) {
        mutableStateOf(initial?.color ?: NotesViewModel.CATEGORY_COLORS.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建分类" else "编辑分类") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("颜色", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NotesViewModel.CATEGORY_COLORS) { hex ->
                        val selected = hex.equals(color, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(if (selected) 30.dp else 24.dp)
                                .background(noteHexColor(hex), CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), color) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 新建便签时使用的哨兵 id */
const val NEW_NOTE_ID = -1
