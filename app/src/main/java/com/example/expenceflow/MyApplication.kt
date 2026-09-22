package com.example.expenceflow

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        // Fix for Apache POI on Android
        System.setProperty("org.apache.poi.ss.ignoreMissingFontMetrics", "true")
    }
}