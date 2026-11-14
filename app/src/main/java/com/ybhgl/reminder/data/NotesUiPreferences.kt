package com.ybhgl.reminder.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 便签界面偏好（与提醒的设置相互独立） */
private val Context.notesUiDataStore by preferencesDataStore(name = "notes_ui_preferences")

object NotesUiPreferences {

    /** 是否隐藏便签筛选栏里的「全部」标签（只隐藏标签本身，不影响便签内容） */
    private val HIDE_ALL_CHIP_KEY = booleanPreferencesKey("hide_all_category_chip")

    fun hideAllChipFlow(context: Context): Flow<Boolean> =
        context.notesUiDataStore.data.map { it[HIDE_ALL_CHIP_KEY] ?: false }

    suspend fun saveHideAllChip(context: Context, hide: Boolean) {
        context.notesUiDataStore.edit { it[HIDE_ALL_CHIP_KEY] = hide }
    }
}
