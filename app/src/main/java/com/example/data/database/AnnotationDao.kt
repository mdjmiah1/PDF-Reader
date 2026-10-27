package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE documentUri = :documentUri ORDER BY createdAt ASC")
    fun getAnnotationsForDocument(documentUri: String): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE documentUri = :documentUri AND pageNumber = :pageNumber ORDER BY createdAt ASC")
    fun getAnnotationsForPage(documentUri: String, pageNumber: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE documentUri = :documentUri AND pageNumber = :pageNumber ORDER BY createdAt ASC")
    suspend fun getAnnotationsForPageSync(documentUri: String, pageNumber: Int): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: Long)

    @Query("DELETE FROM annotations WHERE id = (SELECT id FROM annotations WHERE documentUri = :documentUri AND pageNumber = :pageNumber ORDER BY createdAt DESC LIMIT 1)")
    suspend fun deleteLastAnnotationForPage(documentUri: String, pageNumber: Int)

    @Query("DELETE FROM annotations WHERE documentUri = :documentUri AND pageNumber = :pageNumber")
    suspend fun clearAnnotationsForPage(documentUri: String, pageNumber: Int)

    @Query("DELETE FROM annotations WHERE documentUri = :documentUri")
    suspend fun clearAnnotationsForDocument(documentUri: String)
}
