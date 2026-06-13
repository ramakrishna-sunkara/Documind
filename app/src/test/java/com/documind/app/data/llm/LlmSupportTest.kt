package com.documind.app.data.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseQualityCheckerTest {
    @Test
    fun isDegenerateResponse_detectsRepetitiveGarbage() {
        val garbage = List(20) { "This7777777717777776676" }.joinToString(" ")
        assertTrue(ResponseQualityChecker.isDegenerateResponse(garbage))
    }

    @Test
    fun isDegenerateResponse_detectsScreenshotPattern() {
        val garbage = """
            This as 6 124 in
            This (4
            This-1-
            This were and
            66
            3
            **
            This
            This
            This
        """.trimIndent()
        assertTrue(ResponseQualityChecker.isDegenerateResponse(garbage))
    }

    @Test
    fun isDegenerateResponse_acceptsNormalSummary() {
        val summary = "This invoice is for vehicle service repairs totaling Rs 7,454. " +
            "It includes AC compressor replacement, oil change, and major service."
        assertFalse(ResponseQualityChecker.isDegenerateResponse(summary))
    }
}
