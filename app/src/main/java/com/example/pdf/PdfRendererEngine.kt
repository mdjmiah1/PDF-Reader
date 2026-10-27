package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

data class PageSize(val width: Int, val height: Int)

class PdfRendererEngine(private val context: Context) {

    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val mutex = Mutex()

    private val maxCacheMemory = (Runtime.getRuntime().maxMemory() / 8).coerceAtLeast(32 * 1024 * 1024).toInt()
    private val bitmapCache = object : LruCache<String, Bitmap>(maxCacheMemory) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    private val pageSizes = mutableMapOf<Int, PageSize>()
    var pageCount: Int = 0
        private set

    suspend fun open(uriString: String): Result<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                closeInternal()
                val descriptor = try {
                    if (uriString.startsWith("content://")) {
                        val contentUri = Uri.parse(uriString)
                        try {
                            context.contentResolver.openFileDescriptor(contentUri, "r")
                        } catch (se: SecurityException) {
                            // Try copying using openInputStream as fallback
                            val cacheFile = File(context.cacheDir, "cached_${contentUri.lastPathSegment?.replace("[^a-zA-Z0-9._-]".toRegex(), "_") ?: "doc"}.pdf")
                            context.contentResolver.openInputStream(contentUri)?.use { input ->
                                java.io.FileOutputStream(cacheFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (cacheFile.exists() && cacheFile.length() > 0) {
                                ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                            } else {
                                throw SecurityException("Storage permission has expired for this document. Please re-open it using 'Open PDF' to grant fresh access.")
                            }
                        }
                    } else {
                        val file = com.example.data.resolveLocalFile(uriString)
                        if (!file.exists()) {
                            return@withContext Result.failure(IllegalStateException("File not found at $uriString"))
                        }
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                } catch (se: SecurityException) {
                    return@withContext Result.failure(se)
                } ?: return@withContext Result.failure(IllegalStateException("Cannot open file descriptor for $uriString"))

                pfd = descriptor
                val pdfRenderer = PdfRenderer(descriptor)
                renderer = pdfRenderer
                pageCount = pdfRenderer.pageCount

                // Pre-cache dimensions
                pageSizes.clear()
                for (i in 0 until pageCount) {
                    val page = pdfRenderer.openPage(i)
                    pageSizes[i] = PageSize(page.width, page.height)
                    page.close()
                }

                Result.success(pageCount)
            } catch (e: Exception) {
                closeInternal()
                Result.failure(e)
            }
        }
    }

    fun getPageSize(pageIndex: Int): PageSize {
        return pageSizes[pageIndex] ?: PageSize(595, 842)
    }

    suspend fun renderPage(
        pageIndex: Int,
        destWidth: Int,
        destHeight: Int,
        rotationDegrees: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        val cacheKey = "$pageIndex-$destWidth-$destHeight-$rotationDegrees"
        bitmapCache.get(cacheKey)?.let { return@withContext it }

        mutex.withLock {
            // Check cache again inside lock
            bitmapCache.get(cacheKey)?.let { return@withContext it }
            val currentRenderer = renderer ?: return@withContext null

            try {
                val page = currentRenderer.openPage(pageIndex)
                // The page must always be closed, otherwise every later openPage()
                // fails with "Current page not closed".
                val bitmap = try {
                    val originalWidth = page.width
                    val originalHeight = page.height

                    // Calculate scaled dimensions keeping aspect ratio
                    val scale = minOf(
                        destWidth.toFloat() / originalWidth,
                        destHeight.toFloat() / originalHeight
                    ).coerceIn(0.5f, 4.0f)

                    val bmpWidth = (originalWidth * scale).roundToInt().coerceAtLeast(100)
                    val bmpHeight = (originalHeight * scale).roundToInt().coerceAtLeast(100)

                    val created = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                    // Draw white background
                    val canvas = android.graphics.Canvas(created)
                    canvas.drawColor(Color.WHITE)

                    val transformMatrix = Matrix().apply {
                        postScale(scale, scale)
                    }

                    page.render(created, null, transformMatrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    created
                } finally {
                    page.close()
                }

                val finalBitmap = if (rotationDegrees != 0) {
                    val rotMatrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotMatrix, true)
                    bitmap.recycle()
                    rotated
                } else {
                    bitmap
                }

                bitmapCache.put(cacheKey, finalBitmap)
                finalBitmap
            } catch (e: OutOfMemoryError) {
                // Free cached pages so the next render has room, instead of crashing.
                bitmapCache.evictAll()
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun clearCache() {
        bitmapCache.evictAll()
    }

    private fun closeInternal() {
        try {
            renderer?.close()
        } catch (_: Exception) {}
        renderer = null

        try {
            pfd?.close()
        } catch (_: Exception) {}
        pfd = null

        bitmapCache.evictAll()
        pageSizes.clear()
        pageCount = 0
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeInternal()
        }
    }
}
