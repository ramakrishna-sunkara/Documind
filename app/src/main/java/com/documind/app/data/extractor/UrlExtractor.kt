package com.documind.app.data.extractor

import android.util.Log
import com.documind.app.util.UserFacingErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class UrlExtractor : ContentExtractor {
    
    override val sourceType = SourceType.URL
    
    companion object {
        private const val TAG = "UrlExtractor"
        private const val TIMEOUT_MS = 15000
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) DocuMind/1.0"
    }
    
    override suspend fun extract(source: Any): Result<String> {
        return when (source) {
            is String -> extractFromUrl(source)
            else -> Result.failure(IllegalArgumentException("UrlExtractor requires URL string"))
        }
    }
    
    private suspend fun extractFromUrl(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedUrl = url.trim()
                if (normalizedUrl.isBlank()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Please enter a website URL.")
                    )
                }
                val document = Jsoup.connect(normalizedUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get()
                document.select("script, style, nav, footer, header, aside").remove()
                val title = document.title()
                val bodyText = document.body()?.text()?.trim().orEmpty()
                if (bodyText.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("No readable text was found on this page.")
                    )
                }
                val content = if (title.isNotBlank()) {
                    "$title\n\n$bodyText"
                } else {
                    bodyText
                }
                Result.success(content)
            } catch (e: Exception) {
                Log.e(TAG, "URL extraction failed", e)
                Result.failure(Exception(UserFacingErrors.mapNetworkException(e)))
            }
        }
    }
}
