package com.ririv.quickoutline.utils

import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

object LocalizationManager {
    private var bundle: ResourceBundle

    init {
        val systemLocale = Locale.getDefault()
        val locale = if ("zh" == systemLocale.language && "CN" == systemLocale.country) {
            Locale.SIMPLIFIED_CHINESE
        } else {
            Locale.US
        }
        bundle = ResourceBundle.getBundle("messages", locale)
    }

    fun loadBundle(locale: Locale) {
        bundle = ResourceBundle.getBundle("messages", locale)
    }

    fun getString(key: String): String {
        return try {
            bundle.getString(key)
        } catch (e: MissingResourceException) {
            key
        }
    }
    
    fun getBundle(): ResourceBundle {
        return bundle
    }
}
