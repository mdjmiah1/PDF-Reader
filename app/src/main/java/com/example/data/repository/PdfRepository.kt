package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.database.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.DocumentEntity
import com.example.data.sample.SamplePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class PdfRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val documentDao = database.documentDao()
    private val bookmarkDao = database.bookmarkDao()
    private val annotationDao = database.annotationDao()

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val recentDocuments: Flow<List<DocumentEntity>> = documentDao.getRecentDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments()
    val sampleDocuments: Flow<List<DocumentEntity>> = documentDao.getSampleDocuments()
    val lastOpenedDocument: Flow<DocumentEntity?> = documentDao.getLastOpenedDocument()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun initializeSampleDocumentsIfEmpty() = withContext(Dispatchers.IO) {
        val samples = SamplePdfGenerator.generateSamplePdfsIfMissing(context)
        for (sample in samples) {
            val found = documentDao.getDocumentSync(sample.uriString)
            if (found == null) {
                documentDao.insertDocument(sample)
            }
        }
        scanDeviceForPdfs()
    }

    suspend fun scanDeviceForPdfs(): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        try {
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
            } else {
                android.provider.MediaStore.Files.getContentUri("external")
            }

            val projection = arrayOf(
                android.provider.MediaStore.Files.FileColumns._ID,
                android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
                android.provider.MediaStore.Files.FileColumns.DATA,
                android.provider.MediaStore.Files.FileColumns.SIZE,
                android.provider.MediaStore.Files.FileColumns.DATE_ADDED
            )

            val selection = "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("application/pdf")
            val sortOrder = "${android.provider.MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val nameCol = cursor.getColumnIndex(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(android.provider.MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndex(android.provider.MediaStore.Files.FileColumns.SIZE)

                while (cursor.moveToNext()) {
                    val name = if (nameCol != -1) cursor.getString(nameCol) else "Document.pdf"
                    val path = if (dataCol != -1) cursor.getString(dataCol) else null
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L

                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && file.canRead()) {
                            if (addFileToDatabase(file, name, size)) {
                                addedCount++
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Direct folder scan of common storage directories 
        try {
            val publicDirs = listOf(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                File(android.os.Environment.getExternalStorageDirectory(), "Download"),
                File(android.os.Environment.getExternalStorageDirectory(), "Documents"),
                File(android.os.Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Documents"),
                File(context.filesDir, "documents")
            )
            for (dir in publicDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }?.forEach { file ->
                        if (file.exists() && file.canRead()) {
                            if (addFileToDatabase(file, file.name, file.length())) {
                                addedCount++
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        addedCount
    }

    private suspend fun addFileToDatabase(file: File, title: String, size: Long): Boolean {
        try {
            val uriString = Uri.fromFile(file).toString()
            if (documentDao.getDocumentSync(uriString) == null) {
                var pageCount = 1
                runCatching {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount
                        }
                    }
                }
                val doc = DocumentEntity(
                    uriString = uriString,
                    title = title,
                    pageCount = pageCount,
                    fileSizeBytes = size.takeIf { it > 0 } ?: file.length(),
                    lastOpenedTimestamp = file.lastModified(),
                    lastReadPage = 1,
                    isFavorite = false,
                    isSample = false
                )
                documentDao.insertDocument(doc)
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    fun getDocument(uriString: String): Flow<DocumentEntity?> = documentDao.getDocumentByUri(uriString)

    suspend fun getDocumentSync(uriString: String): DocumentEntity? = documentDao.getDocumentSync(uriString)

    suspend fun recordDocumentOpened(uriString: String, page: Int = 1) = withContext(Dispatchers.IO) {
        documentDao.updateReadingProgress(uriString, page, System.currentTimeMillis())
    }

    suspend fun updateReadingProgress(uriString: String, page: Int) = withContext(Dispatchers.IO) {
        documentDao.updateReadingProgress(uriString, page, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(uriString: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        documentDao.setFavorite(uriString, isFavorite)
    }

    suspend fun deleteDocument(uriString: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmarksForDocument(uriString)
        annotationDao.clearAnnotationsForDocument(uriString)
        documentDao.deleteDocumentByUri(uriString)

        // If it was an internal copied file, remove it
        if (!uriString.startsWith("content://")) {
            val file = File(uriString)
            if (file.exists() && file.parentFile?.name != "samples") {
                file.delete()
            }
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        documentDao.clearAll()
        initializeSampleDocumentsIfEmpty()
    }

    // Storage Access Framework Import
    suspend fun importDocumentFromUri(uri: Uri): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            // Take persistent read permission if supported
            runCatching {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }

            var title = "Document_${System.currentTimeMillis()}.pdf"
            var size = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        title = cursor.getString(nameIndex) ?: title
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }

            // Immediately copy file to app's internal documents directory to ensure permanent access
            val docsDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
            val cleanName = title.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(docsDir, "doc_${System.currentTimeMillis()}_$cleanName")

            var copied = false
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                copied = targetFile.exists() && targetFile.length() > 0
            } catch (e: Exception) {
                copied = false
            }

            if (!copied) {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { inputStream ->
                            java.io.FileOutputStream(targetFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                    copied = targetFile.exists() && targetFile.length() > 0
                } catch (e: Exception) {
                    throw SecurityException("Permission Denied: Unable to read document. Please select the file again.")
                }
            }

            val finalUriString = if (targetFile.exists() && targetFile.length() > 0) {
                size = targetFile.length()
                targetFile.absolutePath
            } else {
                uri.toString()
            }

            // Inspect page count via PdfRenderer using the local file or content PFD
            var pageCount = 1
            runCatching {
                if (finalUriString.startsWith("content://")) {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount
                        }
                    }
                } else {
                    ParcelFileDescriptor.open(File(finalUriString), ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount
                        }
                    }
                }
            }

            val doc = DocumentEntity(
                uriString = finalUriString,
                title = title,
                pageCount = pageCount,
                fileSizeBytes = size,
                lastOpenedTimestamp = System.currentTimeMillis(),
                lastReadPage = 1,
                isFavorite = false,
                isSample = false
            )

            documentDao.insertDocument(doc)
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Bookmark operations
    fun getBookmarksForDocument(documentUri: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForDocument(documentUri)

    fun getBookmarkForPage(documentUri: String, pageNumber: Int): Flow<BookmarkEntity?> =
        bookmarkDao.getBookmarkForPage(documentUri, pageNumber)

    suspend fun isPageBookmarked(documentUri: String, pageNumber: Int): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.getBookmarkForPageSync(documentUri, pageNumber) != null
    }

    suspend fun toggleBookmark(documentUri: String, pageNumber: Int, title: String = "Page $pageNumber"): Boolean = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarkForPageSync(documentUri, pageNumber)
        if (existing != null) {
            bookmarkDao.deleteBookmarkById(existing.id)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    documentUri = documentUri,
                    pageNumber = pageNumber,
                    title = title
                )
            )
            true
        }
    }

    suspend fun deleteBookmark(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmarkById(id)
    }

    // Annotation operations
    fun getAnnotationsForPage(documentUri: String, pageNumber: Int): Flow<List<AnnotationEntity>> =
        annotationDao.getAnnotationsForPage(documentUri, pageNumber)

    suspend fun addAnnotation(annotation: AnnotationEntity): Long = withContext(Dispatchers.IO) {
        annotationDao.insertAnnotation(annotation)
    }

    suspend fun undoLastAnnotation(documentUri: String, pageNumber: Int) = withContext(Dispatchers.IO) {
        annotationDao.deleteLastAnnotationForPage(documentUri, pageNumber)
    }

    suspend fun clearAnnotationsForPage(documentUri: String, pageNumber: Int) = withContext(Dispatchers.IO) {
        annotationDao.clearAnnotationsForPage(documentUri, pageNumber)
    }

    suspend fun importAllPdfsFromTree(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val treeDocumentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)
            val pdfList = mutableListOf<Triple<Uri, String, Long>>()
            collectTreeUris(treeUri, childrenUri, pdfList)

            for ((uri, name, size) in pdfList) {
                val existing = documentDao.getDocumentSync(uri.toString())
                if (existing == null) {
                    var pageCount = 1
                    runCatching {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                                pageCount = renderer.pageCount
                            }
                        }
                    }
                    val doc = DocumentEntity(
                        uriString = uri.toString(),
                        title = name,
                        pageCount = pageCount,
                        fileSizeBytes = size,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        lastReadPage = 1,
                        isFavorite = false,
                        isSample = false
                    )
                    documentDao.insertDocument(doc)
                    count++
                }
            }
        } catch (_: Exception) {}
        count
    }

    private fun collectTreeUris(treeUri: Uri, dirUri: Uri, list: MutableList<Triple<Uri, String, Long>>) {
        try {
            val projection = arrayOf(
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                android.provider.DocumentsContract.Document.COLUMN_SIZE
            )
            context.contentResolver.query(dirUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = if (idCol != -1) cursor.getString(idCol) else null
                    val name = if (nameCol != -1) cursor.getString(nameCol) else "Document.pdf"
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) else null
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L

                    if (docId != null) {
                        val fileUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        if (mime == android.provider.DocumentsContract.Document.MIME_TYPE_DIR) {
                            val subChildren = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                            collectTreeUris(treeUri, subChildren, list)
                        } else if (mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)) {
                            list.add(Triple(fileUri, name, size))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
