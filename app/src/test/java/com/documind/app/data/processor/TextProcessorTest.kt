package com.documind.app.data.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextProcessorTest {
    private val textProcessor = TextProcessor()

    @Test
    fun cleanText_preservesLineBreaks() {
        val rawText = "Invoice No : 123\n\n\nGrand Total :    7454.00"
        val cleaned = textProcessor.cleanText(rawText)
        assertEquals("Invoice No : 123\nGrand Total : 7454.00", cleaned)
    }

    @Test
    fun isSummaryQuery_matchesSummarizePrompt() {
        assertTrue(textProcessor.isSummaryQuery("Summarize this document"))
        assertTrue(textProcessor.isSummaryQuery("What are the key points?"))
        assertFalse(textProcessor.isSummaryQuery("What is the invoice number?"))
    }

    @Test
    fun extractKeyFacts_findsInvoiceFields() {
        val invoiceText = """
            Mr. SUNKARA RAMAKRISHNA
            TAX INVOICE
            Invoice No : IRVMVI2627000470
            InvoiceDate:10/06/2026
            Model: NEXON
            Vehicle Regn. No : AP40F3888
            Service Request Type: Running Repairs
            Grand Total :    7454.00
        """.trimIndent()
        val facts = textProcessor.extractKeyFacts(invoiceText)
        assertTrue(facts.contains("Invoice No: IRVMVI2627000470"))
        assertTrue(facts.contains("Grand Total: 7454.00"))
        assertTrue(facts.contains("Vehicle Regn. No: AP40F3888"))
    }

    @Test
    fun buildSummaryContext_includesKeyFactsAndTail() {
        val body = buildString {
            append("Invoice No : IRVMVI2627000470\n")
            repeat(200) { append("Line item $it with service details. ") }
            append("\nGrand Total : 7454.00")
        }
        val context = textProcessor.buildSummaryContext(body, maxContextLength = 1200)
        assertTrue(context.contains("Key facts:"))
        assertTrue(context.contains("Grand Total"))
        assertTrue(context.contains("7454.00"))
    }
}
