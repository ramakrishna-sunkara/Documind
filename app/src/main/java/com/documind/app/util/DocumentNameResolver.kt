package com.documind.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object DocumentNameResolver {
    fun resolveDisplayName(context: Context, uri: Uri, defaultName: String): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    val displayName: String? = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        return displayName.trim()
                    }
                }
            }
        }
        val pathSegment: String? = uri.lastPathSegment?.let { segment -> Uri.decode(segment) }
        if (!pathSegment.isNullOrBlank()) {
            val fileName: String = pathSegment.substringAfterLast('/')
            if (fileName.isNotBlank() && fileName.contains('.')) {
                return fileName
            }
        }
        return defaultName
    }
}
