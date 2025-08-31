package com.ririv.quickoutline.view

import androidx.compose.runtime.Composable
import com.ririv.quickoutline.utils.LocalizationManager

@Composable
fun stringResource(key: String): String {
    return LocalizationManager.getString(key)
}
