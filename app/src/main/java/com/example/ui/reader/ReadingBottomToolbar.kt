package com.example.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.AnnotationTool
import com.example.ui.theme.AnnotationIndigo
import com.example.ui.theme.AnnotationYellow
import kotlin.math.roundToInt

@Composable
fun ReadingBottomToolbar(
    activeTool: AnnotationTool,
    activeColor: Color,
    zoomLevel: Float,
    onToolSelected: (AnnotationTool) -> Unit,
    onColorSelected: (Color) -> Unit,
    onUndoAnnotation: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitWidth: () -> Unit,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .testTag("reading_bottom_toolbar"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Highlighter Tool
            ToolButton(
                icon = Icons.Default.BorderColor,
                contentDescription = "Highlighter",
                isSelected = activeTool == AnnotationTool.HIGHLIGHTER,
                selectedBg = MaterialTheme.colorScheme.primary,
                iconTint = if (activeTool == AnnotationTool.HIGHLIGHTER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                testTag = "tool_highlighter",
                onClick = { onToolSelected(AnnotationTool.HIGHLIGHTER) }
            )

            // Pen Tool
            ToolButton(
                icon = Icons.Default.Create,
                contentDescription = "Pen",
                isSelected = activeTool == AnnotationTool.PEN,
                selectedBg = MaterialTheme.colorScheme.primary,
                iconTint = if (activeTool == AnnotationTool.PEN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                testTag = "tool_pen",
                onClick = { onToolSelected(AnnotationTool.PEN) }
            )

            // Sticky Note Tool
            ToolButton(
                icon = Icons.Default.NoteAlt,
                contentDescription = "Sticky Note",
                isSelected = activeTool == AnnotationTool.NOTE,
                selectedBg = MaterialTheme.colorScheme.primary,
                iconTint = if (activeTool == AnnotationTool.NOTE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                testTag = "tool_note",
                onClick = { onToolSelected(AnnotationTool.NOTE) }
            )

            // Undo Markup
            IconButton(
                onClick = onUndoAnnotation,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("tool_undo")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(18.dp)
                    .padding(horizontal = 3.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            // Zoom Out
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom out",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Zoom Text
            Text(
                text = "${(zoomLevel * 100).roundToInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 3.dp)
            )

            // Zoom In
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom in",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(18.dp)
                    .padding(horizontal = 3.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            // Fit Screen / Width
            IconButton(
                onClick = onFitWidth,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("fit_screen_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FitScreen,
                    contentDescription = "Fit to screen",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Rotate
            IconButton(
                onClick = onRotate,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("rotate_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Rotate",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    selectedBg: Color,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) selectedBg else Color.Transparent)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
    }
}
