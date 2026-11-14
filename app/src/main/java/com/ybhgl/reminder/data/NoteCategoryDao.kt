package com.ybhgl.reminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteCategoryDao {

    @Query("SELECT * FROM note_categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategoriesFlow(): Flow<List<NoteCategoryItem>>

    @Query("SELECT * FROM note_categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllCategories(): List<NoteCategoryItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: NoteCategoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<NoteCategoryItem>)

    @Update
    suspend fun update(category: NoteCategoryItem)

    @Delete
    suspend fun delete(category: NoteCategoryItem)

    @Query("DELETE FROM note_categories")
    suspend fun deleteAll()

    @Query("UPDATE note_categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Int, sortOrder: Int)

    @Query("UPDATE note_categories SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Int, hidden: Boolean)

    @Transaction
    suspend fun updateCategorySortOrders(categories: List<NoteCategoryItem>) {
        categories.forEachIndexed { index, category -> updateSortOrder(category.id, index) }
    }
}
