package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentUri: String,
    val pageNumber: Int,
    val type: String, // "PEN", "HIGHLIGHTER", "NOTE"
    val colorHex: String,
    val strokeWidth: Float,
    val pointsJson: String, // Array of normalized points "x1,y1;x2,y2..."
    val textNote: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
