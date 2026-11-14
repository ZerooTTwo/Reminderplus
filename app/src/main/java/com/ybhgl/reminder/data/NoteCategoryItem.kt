package com.ybhgl.reminder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 便签分类。
 *
 * 与提醒使用的 [TagItem] 完全独立：提醒标签服务于「纪念日/倒数日」，
 * 便签分类只服务于便签模块，两者互不干扰。
 */
@Serializable
@Entity(
    tableName = "note_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class NoteCategoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    /** Hex 颜色，例如 #2196F3 */
    val color: String = "#2196F3",
    val sortOrder: Int = 0,
    /** 是否在首页筛选与管理列表中隐藏 */
    val isHidden: Boolean = false
)
