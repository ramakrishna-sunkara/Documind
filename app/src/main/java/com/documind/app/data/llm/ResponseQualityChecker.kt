package com.documind.app.data.llm

object ResponseQualityChecker {
    private const val MIN_RESPONSE_LENGTH = 20
    private const val REPETITION_THRESHOLD = 0.55
    private const val THIS_WORD_THRESHOLD = 0.25

    fun isDegenerateResponse(response: String): Boolean {
        val trimmedResponse = response.trim()
        if (trimmedResponse.length < MIN_RESPONSE_LENGTH) {
            return true
        }
        if (hasExcessiveDigitRuns(trimmedResponse)) {
            return true
        }
        val words = trimmedResponse.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 5) {
            return false
        }
        if (hasExcessiveThisRepetition(words)) {
            return true
        }
        if (hasFragmentedLines(trimmedResponse)) {
            return true
        }
        val uniqueWordRatio = words.toSet().size.toDouble() / words.size
        return uniqueWordRatio < REPETITION_THRESHOLD
    }

    private fun hasExcessiveDigitRuns(text: String): Boolean {
        return Regex("\\d{6,}").containsMatchIn(text)
    }

    private fun hasExcessiveThisRepetition(words: List<String>): Boolean {
        val thisCount = words.count { word ->
            word.equals("this", ignoreCase = true) ||
                word.startsWith("this", ignoreCase = true)
        }
        return thisCount.toDouble() / words.size >= THIS_WORD_THRESHOLD
    }

    private fun hasFragmentedLines(text: String): Boolean {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 4) {
            return false
        }
        val shortLines = lines.count { it.length <= 12 }
        return shortLines.toDouble() / lines.size >= 0.6
    }

    fun degenerateResponseMessage(): String {
        return "I couldn't produce a reliable answer from this document. " +
            "Try asking about a specific detail (e.g. invoice number, total amount, or date)."
    }
}
