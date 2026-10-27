package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val uriString: String,
    val title: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val lastOpenedTimestamp: Long,
    val lastReadPage: Int = 1,
    val isFavorite: Boolean = false,
    val thumbnailPath: String? = null,
    val isSample: Boolean = false
)
