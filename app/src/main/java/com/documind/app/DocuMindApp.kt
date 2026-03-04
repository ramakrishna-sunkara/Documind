package com.documind.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class DocuMindApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        initializePdfBox()
    }
    
    private fun initializePdfBox() {
        PDFBoxResourceLoader.init(this)
    }
}
