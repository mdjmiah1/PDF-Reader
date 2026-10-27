package com.example.data.sample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SamplePdfGenerator {

    suspend fun generateSamplePdfsIfMissing(context: Context): List<DocumentEntity> = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "samples").apply { if (!exists()) mkdirs() }
        val generatedList = mutableListOf<DocumentEntity>()

        // Document 1: UI Design Guide 2024 (12 pages)
        val file1 = File(sampleDir, "UI_Design_Guide_2024.pdf")
        if (!file1.exists() || file1.length() == 0L) {
            generateUiDesignGuidePdf(file1)
        }
        generatedList.add(
            DocumentEntity(
                uriString = file1.absolutePath,
                title = "UI_Design_Guide_2024.pdf",
                pageCount = 12,
                fileSizeBytes = file1.length(),
                lastOpenedTimestamp = System.currentTimeMillis() - 1000 * 60 * 30, // 30 mins ago
                lastReadPage = 1,
                isFavorite = true,
                isSample = true
            )
        )

        // Document 2: AI Trends 2024 Report (8 pages)
        val file2 = File(sampleDir, "AI_Trends_2024.pdf")
        if (!file2.exists() || file2.length() == 0L) {
            generateAiTrendsPdf(file2)
        }
        generatedList.add(
            DocumentEntity(
                uriString = file2.absolutePath,
                title = "AI_Trends_2024.pdf",
                pageCount = 8,
                fileSizeBytes = file2.length(),
                lastOpenedTimestamp = System.currentTimeMillis() - 1000 * 60 * 120, // 2 hours ago
                lastReadPage = 1,
                isFavorite = false,
                isSample = true
            )
        )

        // Document 3: Design Systems Handbook (16 pages)
        val file3 = File(sampleDir, "Design_Systems_Handbook.pdf")
        if (!file3.exists() || file3.length() == 0L) {
            generateDesignSystemsPdf(file3)
        }
        generatedList.add(
            DocumentEntity(
                uriString = file3.absolutePath,
                title = "Design_Systems_Handbook.pdf",
                pageCount = 16,
                fileSizeBytes = file3.length(),
                lastOpenedTimestamp = System.currentTimeMillis() - 1000 * 60 * 360, // 6 hours ago
                lastReadPage = 1,
                isFavorite = true,
                isSample = true
            )
        )

        generatedList
    }

    private fun generateUiDesignGuidePdf(outputFile: File) {
        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 points (72 dpi approx)
        val pageHeight = 842
        val totalPages = 12

        val titlePaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(79, 70, 229) // Indigo
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(55, 65, 81)
            textSize = 11.5f
            isAntiAlias = true
        }

        val secondaryPaint = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 10f
            isAntiAlias = true
        }

        val boxPaint = Paint().apply {
            color = Color.rgb(229, 231, 235)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        val dashedPaint = Paint().apply {
            color = Color.rgb(156, 163, 175)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            isAntiAlias = true
        }

        val cardFillPaint = Paint().apply {
            color = Color.rgb(243, 244, 246)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val pageTopics = listOf(
            "UI Design Evolution & Modern Frameworks",
            "Foundations of Spatial Composition",
            "Color Harmony & Accessibility Contrast",
            "Typography Hierarchies in Digital Media",
            "Wireframing to High-Fidelity Prototypes",
            "Micro-interactions & State Feedback",
            "Designing for Touch Targets & Ergonomics",
            "Responsive Breakpoints across Devices",
            "Component Libraries & Atomic Tokens",
            "Motion Design & Spatial Transitions",
            "Dark Mode Theming Best Practices",
            "User Testing & Iterative Design Metrics"
        )

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Page Background
            canvas.drawColor(Color.WHITE)

            // Header Banner Bar
            val accentPaint = Paint().apply {
                color = if (pageIndex % 2 == 0) Color.rgb(79, 70, 229) else Color.rgb(124, 58, 237)
            }
            canvas.drawRect(40f, 35f, pageWidth - 40f, 38f, accentPaint)

            // Header Text
            canvas.drawText("UI DESIGN GUIDE 2024", 40f, 55f, subtitlePaint)
            val pageLabel = "Page $pageIndex of $totalPages"
            canvas.drawText(pageLabel, pageWidth - 40f - secondaryPaint.measureText(pageLabel), 55f, secondaryPaint)

            // Topic Heading
            val topic = pageTopics[pageIndex - 1]
            canvas.drawText("Chapter $pageIndex: $topic", 40f, 100f, titlePaint)

            // Section 1: Overview
            canvas.drawText("1. Core Principles & Philosophy", 40f, 135f, subtitlePaint)
            val p1 = "Modern user interface design requires a strict balance between aesthetic distinction and functional clarity. Interfaces are not merely decorative surfaces; they are visual feedback systems facilitating human-computer interaction with minimal cognitive load."
            drawParagraph(canvas, p1, 40f, 155f, pageWidth - 80f, bodyPaint)

            // Wireframe / Architecture Box Diagram
            val diagramTop = 230f
            val cardRect = RectF(40f, diagramTop, pageWidth - 40f, diagramTop + 180f)
            canvas.drawRoundRect(cardRect, 10f, 10f, cardFillPaint)
            canvas.drawRoundRect(cardRect, 10f, 10f, boxPaint)

            // Inside the wireframe box: simulate screens or layout trees
            val box1 = RectF(60f, diagramTop + 20f, 200f, diagramTop + 160f)
            canvas.drawRect(box1, boxPaint)
            canvas.drawLine(60f, diagramTop + 45f, 200f, diagramTop + 45f, boxPaint)
            canvas.drawText("Mobile Layout", 70f, diagramTop + 38f, secondaryPaint)
            canvas.drawLine(70f, diagramTop + 70f, 190f, diagramTop + 70f, dashedPaint)
            canvas.drawLine(70f, diagramTop + 95f, 170f, diagramTop + 95f, dashedPaint)
            canvas.drawLine(70f, diagramTop + 120f, 150f, diagramTop + 120f, dashedPaint)

            val box2 = RectF(220f, diagramTop + 20f, pageWidth - 60f, diagramTop + 160f)
            canvas.drawRect(box2, boxPaint)
            canvas.drawLine(220f, diagramTop + 45f, pageWidth - 60f, diagramTop + 45f, boxPaint)
            canvas.drawText("Responsive Tablet / Desktop Multi-Column Pane", 230f, diagramTop + 38f, secondaryPaint)
            canvas.drawRect(235f, diagramTop + 60f, 350f, diagramTop + 145f, boxPaint)
            canvas.drawRect(365f, diagramTop + 60f, pageWidth - 75f, diagramTop + 145f, boxPaint)

            // Section 2: Implementation Guidelines
            val sec2Top = 440f
            canvas.drawText("2. Architectural Patterns & Token Design", 40f, sec2Top, subtitlePaint)
            val p2 = "Tokens establish a single source of truth for color values, spacing scales, and type definitions across platforms. Using structured design tokens enables immediate global theming, dark mode transitions, and consistent accessibility compliance."
            drawParagraph(canvas, p2, 40f, sec2Top + 20f, pageWidth - 80f, bodyPaint)

            val p3 = "Key Metrics to track in design reviews:\n• Contrast Ratio: Minimum 4.5:1 for standard body text\n• Minimum Interactive Target: 48dp x 48dp touch region\n• Visual Hierarchy: High-contrast primary anchors and muted secondary metadata"
            drawParagraph(canvas, p3, 40f, sec2Top + 90f, pageWidth - 80f, bodyPaint)

            // Section 3: Summary Card
            val summaryTop = 640f
            val summaryRect = RectF(40f, summaryTop, pageWidth - 40f, summaryTop + 120f)
            val summaryFill = Paint().apply { color = Color.rgb(238, 242, 255) } // Light indigo tint
            canvas.drawRoundRect(summaryRect, 12f, 12f, summaryFill)
            canvas.drawRoundRect(summaryRect, 12f, 12f, boxPaint)

            canvas.drawText("Key Takeaways for Chapter $pageIndex", 60f, summaryTop + 30f, subtitlePaint)
            val summaryText = "Always test your designs under varying light conditions and device widths. Prioritize content readability over decorative clutter. Verify all interactive controls have clear active and pressed states."
            drawParagraph(canvas, summaryText, 60f, summaryTop + 55f, pageWidth - 120f, bodyPaint)

            // Footer
            canvas.drawLine(40f, pageHeight - 40f, pageWidth - 40f, pageHeight - 40f, boxPaint)
            canvas.drawText("PDF Reader • Android Architecture & Design Sample Document", 40f, pageHeight - 25f, secondaryPaint)
            canvas.drawText("$pageIndex", pageWidth - 55f, pageHeight - 25f, subtitlePaint)

            document.finishPage(page)
        }

        FileOutputStream(outputFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
    }

    private fun generateAiTrendsPdf(outputFile: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val totalPages = 8

        val titlePaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(124, 58, 237) // Violet
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(55, 65, 81)
            textSize = 11.5f
            isAntiAlias = true
        }

        val secondaryPaint = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 10f
            isAntiAlias = true
        }

        val topics = listOf(
            "State of Machine Learning & Edge Inference",
            "On-Device Multimodal Processing",
            "Transformer Optimizations & Memory Footprint",
            "Retrieval-Augmented Generation at Scale",
            "Ethical AI, Safety, & Privacy Sandboxes",
            "Neural Audio & Real-Time Speech Synthesis",
            "Autonomous Agents in Mobile Ecosystems",
            "Future Roadmap & Hardware Accelerators"
        )

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            // Header line
            val accentPaint = Paint().apply { color = Color.rgb(16, 185, 129) } // Emerald
            canvas.drawRect(40f, 35f, pageWidth - 40f, 38f, accentPaint)
            canvas.drawText("GLOBAL AI TRENDS 2024", 40f, 55f, subtitlePaint)
            canvas.drawText("Page $pageIndex of $totalPages", pageWidth - 120f, 55f, secondaryPaint)

            canvas.drawText(topics[pageIndex - 1], 40f, 105f, titlePaint)

            val p1 = "Artificial intelligence infrastructure is shifting towards on-device efficiency, minimizing latency while maintaining user data privacy. Specialized neural processing units enable high-throughput token generation and complex embedding search directly on mobile devices."
            drawParagraph(canvas, p1, 40f, 145f, pageWidth - 80f, bodyPaint)

            // Bar Chart Visualization
            val chartTop = 240f
            val chartPaint = Paint().apply { isAntiAlias = true }
            val borderPaint = Paint().apply {
                color = Color.rgb(229, 231, 235)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            canvas.drawRect(40f, chartTop, pageWidth - 40f, chartTop + 190f, borderPaint)
            canvas.drawText("Performance Growth Benchmark (Q1 - Q4)", 60f, chartTop + 30f, subtitlePaint)

            val barHeights = listOf(45f, 75f, 110f, 140f)
            val quarters = listOf("Q1 Baseline", "Q2 Quantized", "Q3 Speculative", "Q4 Hardware NPU")
            val barWidth = 60f

            for (i in quarters.indices) {
                val x = 80f + i * 115f
                val h = barHeights[i]
                val y = chartTop + 160f - h

                chartPaint.color = when (i) {
                    0 -> Color.rgb(209, 213, 219)
                    1 -> Color.rgb(165, 180, 252)
                    2 -> Color.rgb(99, 102, 241)
                    else -> Color.rgb(79, 70, 229)
                }
                canvas.drawRect(x, y, x + barWidth, chartTop + 160f, chartPaint)
                canvas.drawText("${(h * 1.5).toInt()}%", x + 12f, y - 8f, secondaryPaint)
                canvas.drawText(quarters[i], x - 10f, chartTop + 178f, secondaryPaint)
            }

            val p2 = "Efficiency Benchmarking Findings:\n1. Quantization to 4-bit weights reduces memory bandwidth requirements by 65% with less than 1% perplexity deviation.\n2. Speculative decoding increases generation speed by 2.4x across standard conversational workloads.\n3. Local vector indexing achieves sub-10ms similarity queries across 50,000 document chunks."
            drawParagraph(canvas, p2, 40f, 470f, pageWidth - 80f, bodyPaint)

            // Footer
            canvas.drawText("PDF Reader • AI Research Technical Whitepaper", 40f, pageHeight - 25f, secondaryPaint)
            canvas.drawText("$pageIndex", pageWidth - 55f, pageHeight - 25f, subtitlePaint)

            document.finishPage(page)
        }

        FileOutputStream(outputFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
    }

    private fun generateDesignSystemsPdf(outputFile: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val totalPages = 16

        val titlePaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(79, 70, 229)
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(55, 65, 81)
            textSize = 11.5f
            isAntiAlias = true
        }

        val secondaryPaint = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 10f
            isAntiAlias = true
        }

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            // Header line
            val accentPaint = Paint().apply { color = Color.rgb(236, 72, 153) } // Pink accent
            canvas.drawRect(40f, 35f, pageWidth - 40f, 38f, accentPaint)
            canvas.drawText("DESIGN SYSTEMS HANDBOOK", 40f, 55f, subtitlePaint)
            canvas.drawText("Page $pageIndex of $totalPages", pageWidth - 120f, 55f, secondaryPaint)

            canvas.drawText("Module $pageIndex: Architecture & Patterns", 40f, 105f, titlePaint)

            val p1 = "A comprehensive design system bridges product management, visual design, and software engineering. It provides standardized tokens, reusable components, and documented guidelines to maintain consistency and velocity at scale."
            drawParagraph(canvas, p1, 40f, 145f, pageWidth - 80f, bodyPaint)

            // Draw color palette swatches
            val swatchTop = 230f
            val swatches = listOf(
                Pair("Indigo Primary", Color.rgb(79, 70, 229)),
                Pair("Violet Secondary", Color.rgb(124, 58, 237)),
                Pair("Emerald Accent", Color.rgb(16, 185, 129)),
                Pair("Amber Warning", Color.rgb(245, 158, 11))
            )

            for (i in swatches.indices) {
                val swatch = swatches[i]
                val left = 40f + i * 130f
                val paint = Paint().apply { color = swatch.second }
                canvas.drawRoundRect(RectF(left, swatchTop, left + 115f, swatchTop + 70f), 8f, 8f, paint)
                canvas.drawText(swatch.first, left + 4f, swatchTop + 90f, secondaryPaint)
            }

            val p2 = "Standard Spacing & Radius Tokens:\n• Space-XS: 4dp (Tight grouping, badge padding)\n• Space-S: 8dp (Standard icon-to-label separation)\n• Space-M: 16dp (Container padding, card margins)\n• Space-L: 24dp (Section dividers, major view breaks)\n• Space-XL: 32dp (Hero headers, screen bounds)"
            drawParagraph(canvas, p2, 40f, 360f, pageWidth - 80f, bodyPaint)

            val p3 = "Accessibility Compliance:\nAll color pairs must pass WCAG 2.1 AA specifications with a minimum 4.5:1 luminosity contrast ratio. Avoid transmitting critical status via color alone; always pair iconography and explicit textual descriptions."
            drawParagraph(canvas, p3, 40f, 520f, pageWidth - 80f, bodyPaint)

            // Footer
            canvas.drawText("PDF Reader • Design Systems Standards Handbook", 40f, pageHeight - 25f, secondaryPaint)
            canvas.drawText("$pageIndex", pageWidth - 55f, pageHeight - 25f, subtitlePaint)

            document.finishPage(page)
        }

        FileOutputStream(outputFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
    }

    private fun drawParagraph(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        val lines = text.split("\n")
        var currentY = y

        for (rawLine in lines) {
            val words = rawLine.split(" ")
            var lineBuilder = StringBuilder()

            for (word in words) {
                val candidate = if (lineBuilder.isEmpty()) word else "$lineBuilder $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    lineBuilder = StringBuilder(candidate)
                } else {
                    canvas.drawText(lineBuilder.toString(), x, currentY, paint)
                    currentY += paint.textSize * 1.45f
                    lineBuilder = StringBuilder(word)
                }
            }
            if (lineBuilder.isNotEmpty()) {
                canvas.drawText(lineBuilder.toString(), x, currentY, paint)
                currentY += paint.textSize * 1.45f
            }
        }
    }
}
