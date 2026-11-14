package com.ybhgl.reminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC, id DESC")
    fun getAllNotesFlow(): Flow<List<NoteItem>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC, id DESC")
    suspend fun getAllNotes(): List<NoteItem>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteItem>)

    @Update
    suspend fun update(note: NoteItem)

    @Delete
    suspend fun delete(note: NoteItem)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Int, pinned: Boolean)

    /** 分类被删除时，把该分类下的便签移入「未分类」 */
    @Query("UPDATE notes SET categoryId = 0 WHERE categoryId = :categoryId")
    suspend fun detachCategory(categoryId: Int)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int
}
