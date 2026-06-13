package com.documind.app.util

import com.documind.app.data.llm.ModelState
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.TimeoutCancellationException

enum class ErrorCategory {
    FILE,
    EXTRACTION,
    INDEXING,
    MODEL,
    QUERY,
    NETWORK,
    DEMO,
    VALIDATION
}

object UserFacingErrors {

    fun forMessage(rawMessage: String?, category: ErrorCategory): String {
        val message = rawMessage?.trim().orEmpty()
        if (message.isBlank()) {
            return defaultMessage(category)
        }
        return mapKnownMessage(message, category)
    }

    fun forThrowable(throwable: Throwable, category: ErrorCategory): String {
        return forMessage(throwable.message, category)
    }

    fun extractionFailed(sourceLabel: String, throwable: Throwable): String {
        val mapped = forThrowable(throwable, ErrorCategory.EXTRACTION)
        return if (mapped == defaultMessage(ErrorCategory.EXTRACTION)) {
            "Could not read this $sourceLabel. $mapped"
        } else {
            mapped
        }
    }

    fun modelNotReady(modelState: ModelState?): String {
        return when (modelState) {
            is ModelState.Downloading ->
                "AI models are still downloading (${modelState.progress}%). Please wait, then try again."
            is ModelState.WaitingForWifi ->
                "AI models need Wi‑Fi to download. Connect to Wi‑Fi or allow mobile data, then retry."
            is ModelState.Error ->
                modelState.message.ifBlank { defaultMessage(ErrorCategory.MODEL) }
            else ->
                "On-device AI is not ready yet. Install DocuMind from Google Play and wait for the model download to finish."
        }
    }

    fun ragNotReady(): String {
        return "On-device AI is still starting. Wait a few seconds and try again."
    }

    fun documentNotIndexed(): String {
        return "This document is not ready for questions yet. Tap Retry indexing below, or go back and open it again."
    }

    fun queryFailed(throwable: Throwable): String {
        return forThrowable(throwable, ErrorCategory.QUERY)
    }

    private fun defaultMessage(category: ErrorCategory): String {
        return when (category) {
            ErrorCategory.FILE -> "Could not open the selected file. Please try again."
            ErrorCategory.EXTRACTION -> "Could not extract readable text from this document."
            ErrorCategory.INDEXING -> "Could not prepare this document for on-device AI. Tap Retry indexing."
            ErrorCategory.MODEL -> "On-device AI is not available right now. Check your model download and try again."
            ErrorCategory.QUERY -> "Could not generate an answer. Try rephrasing your question."
            ErrorCategory.NETWORK -> "Could not reach the website. Check your connection and try again."
            ErrorCategory.DEMO -> "Could not load the demo document. Please try again."
            ErrorCategory.VALIDATION -> "Please check your input and try again."
        }
    }

    private fun mapKnownMessage(message: String, category: ErrorCategory): String {
        val lowerMessage = message.lowercase()
        when {
            lowerMessage.contains("no extractable text") ||
                lowerMessage.contains("scanned document") ||
                lowerMessage.contains("image-based pdf") ->
                return "This PDF has no readable text. It may be a scanned image — try a text-based PDF or paste the content manually."
            lowerMessage.contains("no readable text") ||
                lowerMessage.contains("no indexable text") ||
                lowerMessage.contains("cannot be empty") ||
                lowerMessage.contains("empty") && category == ErrorCategory.EXTRACTION ->
                return "No readable text was found in this document. Try another file or paste the text directly."
            lowerMessage.contains("demo document not found") ->
                return "Demo document is missing from this build. Use Get Started, then try another source."
            lowerMessage.contains("timed out") || lowerMessage.contains("timeout") ->
                return when (category) {
                    ErrorCategory.INDEXING ->
                        "Preparing this document took too long. Tap Retry indexing or try a shorter document."
                    ErrorCategory.NETWORK, ErrorCategory.EXTRACTION ->
                        "The request timed out. Check your connection and try again."
                    else ->
                        "This took too long. Please try again."
                }
            lowerMessage.contains("unknownhost") ||
                lowerMessage.contains("unable to resolve host") ||
                lowerMessage.contains("no address associated") ->
                return "Could not reach the website. Check the URL and your internet connection."
            lowerMessage.contains("network") ||
                lowerMessage.contains("connection") ||
                lowerMessage.contains("offline") ->
                return defaultMessage(ErrorCategory.NETWORK)
            lowerMessage.contains("zip") ||
                lowerMessage.contains("invalid docx") ||
                lowerMessage.contains("not a valid") && lowerMessage.contains("office") ->
                return "This Word file appears corrupted or is not a valid .docx file."
            lowerMessage.contains("password") || lowerMessage.contains("encrypted") ->
                return "This file is password-protected. Remove the password and try again."
            lowerMessage.contains("model") && lowerMessage.contains("not available") ->
                return defaultMessage(ErrorCategory.MODEL)
            lowerMessage.contains("play store") || lowerMessage.contains("google play") ->
                return message
            lowerMessage.contains("still starting") || lowerMessage.contains("still being indexed") ->
                return ragNotReady()
            lowerMessage.contains("not indexed") ->
                return documentNotIndexed()
            lowerMessage.contains("degenerate") ->
                return "The AI could not produce a reliable answer for this question. Try asking something more specific."
        }
        if (category == ErrorCategory.QUERY && message.startsWith("Error:", ignoreCase = true)) {
            return message.removePrefix("Error:").trim()
        }
        return message
    }

    fun mapNetworkException(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException ->
                "Could not reach the website. Check the URL and your internet connection."
            is SocketTimeoutException, is TimeoutException, is TimeoutCancellationException ->
                "The website took too long to respond. Check your connection and try again."
            is IOException ->
                "Network error while loading the page. Check your connection and try again."
            else ->
                forThrowable(throwable, ErrorCategory.NETWORK)
        }
    }
}
