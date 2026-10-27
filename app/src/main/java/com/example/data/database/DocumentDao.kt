package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastOpenedTimestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY lastOpenedTimestamp DESC LIMIT 10")
    fun getRecentDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY lastOpenedTimestamp DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isSample = 1 ORDER BY title ASC")
    fun getSampleDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE uriString = :uri LIMIT 1")
    fun getDocumentByUri(uri: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE uriString = :uri LIMIT 1")
    suspend fun getDocumentSync(uri: String): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY lastOpenedTimestamp DESC LIMIT 1")
    fun getLastOpenedDocument(): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET lastReadPage = :lastReadPage, lastOpenedTimestamp = :timestamp WHERE uriString = :uri")
    suspend fun updateReadingProgress(uri: String, lastReadPage: Int, timestamp: Long)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE uriString = :uri")
    suspend fun setFavorite(uri: String, isFavorite: Boolean)

    @Query("DELETE FROM documents WHERE uriString = :uri")
    suspend fun deleteDocumentByUri(uri: String)

    @Query("DELETE FROM documents")
    suspend fun clearAll()
}
