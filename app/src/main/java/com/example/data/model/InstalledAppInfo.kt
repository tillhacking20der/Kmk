package com.example.data.model

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null
)
