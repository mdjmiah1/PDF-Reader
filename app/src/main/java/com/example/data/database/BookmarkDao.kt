package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE documentUri = :documentUri ORDER BY pageNumber ASC")
    fun getBookmarksForDocument(documentUri: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE documentUri = :documentUri AND pageNumber = :pageNumber LIMIT 1")
    fun getBookmarkForPage(documentUri: String, pageNumber: Int): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmarks WHERE documentUri = :documentUri AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getBookmarkForPageSync(documentUri: String, pageNumber: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE documentUri = :documentUri AND pageNumber = :pageNumber")
    suspend fun deleteBookmark(documentUri: String, pageNumber: Int)

    @Query("DELETE FROM bookmarks WHERE documentUri = :documentUri")
    suspend fun deleteBookmarksForDocument(documentUri: String)
}
