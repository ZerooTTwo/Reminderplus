package com.ybhgl.reminder.data

import android.content.Context

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val reminderRepository: ReminderRepository
    val tagRepository: TagRepository
    val noteRepository: NoteRepository
}

/**
 * [AppContainer] implementation that provides instance of [ReminderRepository]
 */
class DefaultAppContainer(private val context: Context) : AppContainer {
    override val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(ReminderDatabase.getDatabase(context).reminderDao(), context)
    }

    override val tagRepository: TagRepository by lazy {
        val database = ReminderDatabase.getDatabase(context)
        TagRepository(database.tagDao(), database.reminderDao(), context)
    }

    override val noteRepository: NoteRepository by lazy {
        val database = ReminderDatabase.getDatabase(context)
        NoteRepository(database.noteDao(), database.noteCategoryDao(), context)
    }
}
