package com.example.pdf

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.model.AnnotationEntity

enum class AnnotationTool {
    NONE,
    PEN,
    HIGHLIGHTER,
    NOTE,
    ERASER
}

data class NormalizedPoint(val x: Float, val y: Float)

data class UiAnnotation(
    val id: Long = 0,
    val pageNumber: Int,
    val tool: AnnotationTool,
    val color: Color,
    val strokeWidth: Float,
    val points: List<NormalizedPoint>,
    val textNote: String? = null
)

object AnnotationParser {
    fun serializePoints(points: List<NormalizedPoint>): String {
        return points.joinToString(";") { "${it.x},${it.y}" }
    }

    fun deserializePoints(pointsStr: String): List<NormalizedPoint> {
        if (pointsStr.isBlank()) return emptyList()
        return pointsStr.split(";").mapNotNull { pair ->
            val parts = pair.split(",")
            if (parts.size == 2) {
                val x = parts[0].toFloatOrNull()
                val y = parts[1].toFloatOrNull()
                if (x != null && y != null) NormalizedPoint(x, y) else null
            } else null
        }
    }

    fun toUiAnnotation(entity: AnnotationEntity): UiAnnotation {
        val tool = when (entity.type) {
            "HIGHLIGHTER" -> AnnotationTool.HIGHLIGHTER
            "NOTE" -> AnnotationTool.NOTE
            else -> AnnotationTool.PEN
        }
        val colorInt = runCatching { android.graphics.Color.parseColor(entity.colorHex) }.getOrDefault(android.graphics.Color.BLUE)
        return UiAnnotation(
            id = entity.id,
            pageNumber = entity.pageNumber,
            tool = tool,
            color = Color(colorInt),
            strokeWidth = entity.strokeWidth,
            points = deserializePoints(entity.pointsJson),
            textNote = entity.textNote
        )
    }

    fun toEntity(ui: UiAnnotation, documentUri: String): AnnotationEntity {
        val hex = String.format("#%06X", 0xFFFFFF and ui.color.hashCode())
        val typeStr = when (ui.tool) {
            AnnotationTool.HIGHLIGHTER -> "HIGHLIGHTER"
            AnnotationTool.NOTE -> "NOTE"
            else -> "PEN"
        }
        return AnnotationEntity(
            id = ui.id,
            documentUri = documentUri,
            pageNumber = ui.pageNumber,
            type = typeStr,
            colorHex = hex,
            strokeWidth = ui.strokeWidth,
            pointsJson = serializePoints(ui.points),
            textNote = ui.textNote
        )
    }
}
