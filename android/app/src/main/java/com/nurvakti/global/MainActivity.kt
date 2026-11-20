package com.nurvakti.global

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.nurvakti.global.ui.AppVm
import com.nurvakti.global.ui.LocalStrings
import com.nurvakti.global.ui.NurRoot
import com.nurvakti.global.ui.NurTheme
import com.nurvakti.global.ui.Strings

class MainActivity : ComponentActivity() {
    private val vm: AppVm by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openTarget = intent?.getStringExtra("open")
        setContent {
            val s by vm.settings.collectAsState()
            val lang = s?.lang ?: "tr"
            val strings = remember(lang) { Strings(lang, vm.content.i18n) }
            NurTheme(s?.theme ?: "SYSTEM") {
                CompositionLocalProvider(LocalStrings provides strings,
                    LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    NurRoot(vm, openTarget)
                }
            }
        }
    }
}
