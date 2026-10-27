package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.pdf.AnnotationParser
import com.example.pdf.AnnotationTool
import com.example.pdf.NormalizedPoint
import com.example.pdf.PdfSearchEngine
import com.example.pdf.UiAnnotation
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PDF Reader", appName)
    }

    @Test
    fun `search engine finds occurrences in indexed documents`() {
        val engine = PdfSearchEngine()
        engine.indexSampleDocument("UI_Design_Guide_2024.pdf", 12)
        val results = engine.search("Spatial")
        assertTrue(results.isNotEmpty())
        assertEquals(2, results[0].pageNumber)
    }

    @Test
    fun `annotation serialization preserves normalized coordinates`() {
        val points = listOf(
            NormalizedPoint(0.1f, 0.2f),
            NormalizedPoint(0.5f, 0.8f)
        )
        val serialized = AnnotationParser.serializePoints(points)
        val deserialized = AnnotationParser.deserializePoints(serialized)
        assertEquals(2, deserialized.size)
        assertEquals(0.1f, deserialized[0].x, 0.001f)
        assertEquals(0.2f, deserialized[0].y, 0.001f)
        assertEquals(0.5f, deserialized[1].x, 0.001f)
        assertEquals(0.8f, deserialized[1].y, 0.001f)
    }
}
