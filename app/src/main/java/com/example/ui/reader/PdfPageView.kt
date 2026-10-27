package com.example.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.pdf.AnnotationTool
import com.example.pdf.NormalizedPoint
import com.example.pdf.PdfRendererEngine
import com.example.pdf.UiAnnotation
import kotlin.math.roundToInt

@Composable
fun PdfPageView(
    rendererEngine: PdfRendererEngine,
    pageNumber: Int, // 1-indexed
    zoomLevel: Float,
    rotationDegrees: Int,
    activeTool: AnnotationTool,
    activeColor: Color,
    strokeWidth: Float,
    annotations: List<UiAnnotation>,
    onToggleFullscreen: () -> Unit,
    onStrokeFinished: (List<NormalizedPoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageIndex = pageNumber - 1
    var pageBitmap by remember(pageIndex, rotationDegrees) { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember(pageIndex, rotationDegrees) { mutableStateOf(true) }

    // Pan offset for zoomed viewing
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }.roundToInt()
        val containerHeightPx = with(density) { maxHeight.toPx() }.roundToInt()

        val pageSize = rendererEngine.getPageSize(pageIndex)
        val aspect = pageSize.width.toFloat() / pageSize.height.toFloat()

        // Calculate fitted dimensions preserving page aspect ratio
        val fitWidth = (containerHeightPx * aspect).coerceAtMost(containerWidthPx.toFloat() * 0.95f)
        val fitHeight = fitWidth / aspect

        val targetWidth = (fitWidth * zoomLevel).roundToInt().coerceAtLeast(300)
        val targetHeight = (fitHeight * zoomLevel).roundToInt().coerceAtLeast(400)

        LaunchedEffect(pageIndex, zoomLevel, rotationDegrees, targetWidth, targetHeight) {
            isRendering = true
            val bmp = rendererEngine.renderPage(
                pageIndex = pageIndex,
                destWidth = targetWidth,
                destHeight = targetHeight,
                rotationDegrees = rotationDegrees
            )
            pageBitmap = bmp
            isRendering = false
        }

        val pageDpWidth = with(density) { (targetWidth / density.density).dp }
        val pageDpHeight = with(density) { (targetHeight / density.density).dp }

        val interactionSource = remember { MutableInteractionSource() }

        Surface(
            modifier = Modifier
                .offset { IntOffset(panOffset.x.roundToInt(), panOffset.y.roundToInt()) }
                .size(pageDpWidth, pageDpHeight)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (activeTool == AnnotationTool.NONE) {
                        onToggleFullscreen()
                    }
                }
                .testTag("pdf_rendered_page_$pageNumber"),
            color = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val bitmap = pageBitmap
                if (bitmap != null && !bitmap.isRecycled) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page $pageNumber content",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Annotation drawing overlay
                AnnotationCanvas(
                    annotations = annotations,
                    activeTool = activeTool,
                    activeColor = activeColor,
                    strokeWidth = strokeWidth,
                    onStrokeFinished = onStrokeFinished,
                    modifier = Modifier.fillMaxSize()
                )

                if (isRendering && pageBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
