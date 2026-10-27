package com.example.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.pdf.AnnotationTool
import com.example.pdf.NormalizedPoint
import com.example.pdf.UiAnnotation

@Composable
fun AnnotationCanvas(
    annotations: List<UiAnnotation>,
    activeTool: AnnotationTool,
    activeColor: Color,
    strokeWidth: Float,
    onStrokeFinished: (List<NormalizedPoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPoints = remember { mutableStateListOf<NormalizedPoint>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeTool) {
                if (activeTool == AnnotationTool.NONE) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        currentPoints.clear()
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normY = (offset.y / size.height).coerceIn(0f, 1f)
                        currentPoints.add(NormalizedPoint(normX, normY))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                        val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                        currentPoints.add(NormalizedPoint(normX, normY))
                    },
                    onDragEnd = {
                        if (currentPoints.isNotEmpty()) {
                            onStrokeFinished(currentPoints.toList())
                            currentPoints.clear()
                        }
                    },
                    onDragCancel = {
                        currentPoints.clear()
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Draw saved annotations
        for (annotation in annotations) {
            if (annotation.points.isEmpty()) continue

            when (annotation.tool) {
                AnnotationTool.HIGHLIGHTER -> {
                    val path = Path()
                    val first = annotation.points.first()
                    path.moveTo(first.x * canvasWidth, first.y * canvasHeight)
                    for (i in 1 until annotation.points.size) {
                        val pt = annotation.points[i]
                        path.lineTo(pt.x * canvasWidth, pt.y * canvasHeight)
                    }
                    drawPath(
                        path = path,
                        color = annotation.color.copy(alpha = 0.38f),
                        style = Stroke(
                            width = annotation.strokeWidth * (canvasWidth / 400f).coerceAtLeast(1f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                AnnotationTool.PEN -> {
                    val path = Path()
                    val first = annotation.points.first()
                    path.moveTo(first.x * canvasWidth, first.y * canvasHeight)
                    for (i in 1 until annotation.points.size) {
                        val pt = annotation.points[i]
                        path.lineTo(pt.x * canvasWidth, pt.y * canvasHeight)
                    }
                    drawPath(
                        path = path,
                        color = annotation.color,
                        style = Stroke(
                            width = annotation.strokeWidth * (canvasWidth / 600f).coerceAtLeast(1f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                AnnotationTool.NOTE -> {
                    // Draw a note circle marker
                    val pt = annotation.points.first()
                    drawCircle(
                        color = annotation.color,
                        radius = 12f,
                        center = Offset(pt.x * canvasWidth, pt.y * canvasHeight)
                    )
                }
                else -> {}
            }
        }

        // 2. Draw active in-progress stroke
        if (currentPoints.isNotEmpty() && activeTool != AnnotationTool.NONE) {
            val path = Path()
            val first = currentPoints.first()
            path.moveTo(first.x * canvasWidth, first.y * canvasHeight)
            for (i in 1 until currentPoints.size) {
                val pt = currentPoints[i]
                path.lineTo(pt.x * canvasWidth, pt.y * canvasHeight)
            }

            val strokeAlpha = if (activeTool == AnnotationTool.HIGHLIGHTER) 0.38f else 1.0f
            drawPath(
                path = path,
                color = activeColor.copy(alpha = strokeAlpha),
                style = Stroke(
                    width = strokeWidth * (canvasWidth / 600f).coerceAtLeast(1f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
