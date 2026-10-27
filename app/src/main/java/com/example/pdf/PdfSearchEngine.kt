package com.example.pdf

data class SearchMatch(
    val id: Int,
    val pageNumber: Int,
    val snippet: String,
    val query: String
)

class PdfSearchEngine {

    // Index of text content by page for sample & loaded documents
    private val pageTextContent = mutableMapOf<Int, String>()

    fun indexSampleDocument(title: String, pageCount: Int) {
        pageTextContent.clear()
        when {
            title.contains("UI_Design", ignoreCase = true) -> {
                val topics = listOf(
                    "UI Design Evolution & Modern Frameworks. Modern user interface design requires strict balance between aesthetic distinction and functional clarity. Interfaces facilitate human-computer interaction with minimal cognitive load.",
                    "Foundations of Spatial Composition. Spatial rhythm, 8dp grid systems, padding hierarchies, and clean layout boundaries make content readable and scanning intuitive.",
                    "Color Harmony & Accessibility Contrast. WCAG 2.1 AA specifications require 4.5:1 minimum contrast ratio. Palette includes Indigo Primary #4F46E5, Violet Secondary #7C3AED, and Neutral background #EEF0F5.",
                    "Typography Hierarchies in Digital Media. Font weights, line heights, dynamic scaling with sp units, and clear distinction between display headings and body copy.",
                    "Wireframing to High-Fidelity Prototypes. Responsive layouts, mobile viewport vs tablet multi-column panes, component states, and user testing flow.",
                    "Micro-interactions & State Feedback. Ripple effects, button press elevations, subtle spring transitions, and real-time state changes.",
                    "Designing for Touch Targets & Ergonomics. Minimum interactive component size 48dp x 48dp, thumb zone reachability, and comfortable spacing.",
                    "Responsive Breakpoints across Devices. Fluid widthIn containers, adaptive navigation rail for tablets, and vertical mobile-first columns.",
                    "Component Libraries & Atomic Tokens. Standardized design tokens for colors, radii, shadows, typography, and elevations.",
                    "Motion Design & Spatial Transitions. Shared axis animations, staggered visibility, enter and exit choreography under 300ms.",
                    "Dark Mode Theming Best Practices. Deep slate backgrounds #101114, elevated surface cards #242630, and high-contrast text #F9FAFB.",
                    "User Testing & Iterative Design Metrics. Usability benchmarking, task completion rates, error recovery flows, and continuous polish."
                )
                for (i in 0 until pageCount) {
                    pageTextContent[i + 1] = topics.getOrElse(i) { "UI Design Chapter ${i + 1} Principles and architecture." }
                }
            }
            title.contains("AI_Trends", ignoreCase = true) -> {
                val topics = listOf(
                    "State of Machine Learning & Edge Inference. On-device neural processing units enable low-latency inference, zero data leaks, and high throughput.",
                    "On-Device Multimodal Processing. Vision models, camera stream analysis, audio transcription, and vector embedding generation.",
                    "Transformer Optimizations & Memory Footprint. 4-bit quantization reduces memory bandwidth requirements by 65% with minimal perplexity shift.",
                    "Retrieval-Augmented Generation at Scale. Local vector databases, sub-10ms similarity queries across 50,000 document chunks.",
                    "Ethical AI, Safety, & Privacy Sandboxes. Device-bound permissions, differential privacy, and explainable model reasoning.",
                    "Neural Audio & Real-Time Speech Synthesis. Expressive prosody, low-bitrate streaming codecs, and noise suppression.",
                    "Autonomous Agents in Mobile Ecosystems. Intent parsing, structured tool calling, background task scheduling, and error recovery.",
                    "Future Roadmap & Hardware Accelerators. Dedicated tensor cores, energy-efficient heterogeneous compute, and edge hardware."
                )
                for (i in 0 until pageCount) {
                    pageTextContent[i + 1] = topics.getOrElse(i) { "AI Research Section ${i + 1} Metrics and findings." }
                }
            }
            else -> {
                for (i in 0 until pageCount) {
                    pageTextContent[i + 1] = "Page ${i + 1} document text content. Architectural design, tokens, performance metrics, and guidelines."
                }
            }
        }
    }

    fun search(query: String): List<SearchMatch> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val results = mutableListOf<SearchMatch>()
        var matchId = 1

        for ((pageNum, text) in pageTextContent.toSortedMap()) {
            var startIndex = 0
            while (startIndex < text.length) {
                val foundIndex = text.indexOf(trimmed, startIndex, ignoreCase = true)
                if (foundIndex == -1) break

                val snippetStart = (foundIndex - 30).coerceAtLeast(0)
                val snippetEnd = (foundIndex + trimmed.length + 40).coerceAtMost(text.length)
                val prefix = if (snippetStart > 0) "..." else ""
                val suffix = if (snippetEnd < text.length) "..." else ""
                val snippet = prefix + text.substring(snippetStart, snippetEnd).trim() + suffix

                results.add(
                    SearchMatch(
                        id = matchId++,
                        pageNumber = pageNum,
                        snippet = snippet,
                        query = trimmed
                    )
                )

                startIndex = foundIndex + trimmed.length
            }
        }

        return results
    }
}
