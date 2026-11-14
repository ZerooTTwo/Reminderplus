@file:OptIn(ExperimentalMaterial3Api::class)

package com.ybhgl.reminder.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.ui.common.AppViewModelProvider

/**
 * 便签编辑页。
 *
 * 返回（含系统返回键）时会先落库再出栈，避免 ViewModel 随 entry 销毁导致写入被取消。
 */
@Composable
fun NoteEditScreen(
    noteId: Int,
    onNavigateBack: () -> Unit,
    viewModel: NotesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val categories by viewModel.categories.collectAsState()

    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(0) }
    var createdAt by rememberSaveable { mutableStateOf(0L) }
    var loaded by rememberSaveable { mutableStateOf(noteId <= 0) }
    var saving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val note = viewModel.loadNote(noteId)
            if (note != null) {
                title = note.title
                content = note.content
                categoryId = note.categoryId
                createdAt = note.createdAt
            }
            loaded = true
        }
    }

    fun doSave(then: () -> Unit) {
        if (saving) return
        saving = true
        viewModel.saveNote(noteId, title, content, categoryId, createdAt) {
            saving = false
            then()
        }
    }

    BackHandler {
        doSave { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId > 0) "编辑便签" else "新建便签",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { doSave { onNavigateBack() } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回并保存")
                    }
                },
                actions = {
                    if (noteId > 0) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除便签")
                        }
                    }
                    IconButton(
                        onClick = { doSave { onNavigateBack() } },
                        enabled = loaded && !saving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    NoteFilterChip(
                        label = "未分类",
                        selected = categoryId == 0,
                        onClick = { categoryId = 0 }
                    )
                }
                items(categories, key = { it.id }) { category ->
                    NoteFilterChip(
                        label = category.name,
                        selected = categoryId == category.id,
                        color = noteHexColor(category.color),
                        onClick = { categoryId = category.id }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("标题") },
                placeholder = { Text("可不填，会自动用正文首行作标题") },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("内容") },
                shape = RoundedCornerShape(16.dp),
                minLines = 8
            )

            if (createdAt > 0L) {
                Text(
                    text = "创建于 ${formatNoteTime(createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除便签") },
            text = { Text("删除后无法恢复，确定删除这条便签吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteNoteById(noteId) { onNavigateBack() }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}
