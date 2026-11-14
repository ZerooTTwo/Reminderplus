package com.ybhgl.reminder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 便签。
 *
 * 时间字段统一使用 epoch 毫秒，避免为备份 JSON 额外引入 LocalDate 序列化器。
 */
@Serializable
@Entity(
    tableName = "notes",
    indices = [Index(value = ["categoryId"]), Index(value = ["updatedAt"])]
)
data class NoteItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    /** 标题；允许为空（纯正文便签） */
    val title: String = "",
    val content: String = "",
    /** 所属分类 id；0 表示「未分类」 */
    val categoryId: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * 列表页展示用的标题：优先标题，标题为空时取正文首个非空行。
 * 放在实体外部，避免 Room 把它当成数据库列。
 */
val NoteItem.displayTitle: String
    get() = title.ifBlank {
        content.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }.ifBlank { "无标题便签" }
