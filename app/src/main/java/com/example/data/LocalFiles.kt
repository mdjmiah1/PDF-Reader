package com.example.data

import android.net.Uri
import java.io.File

/**
 * Resolves a stored document identifier to a local [File].
 * Accepts plain absolute paths and legacy "file://" URI strings
 * (older scans stored the latter, which File() cannot open directly).
 */
fun resolveLocalFile(uriString: String): File {
    if (uriString.startsWith("file://")) {
        val path = Uri.parse(uriString).path
        if (!path.isNullOrEmpty()) return File(path)
    }
    return File(uriString)
}
