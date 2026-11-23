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
        intent?.getStringExtra("ci")?.let { CiHooks.run(this, vm, it) }
        if (BuildConfig.DEBUG && intent?.getStringExtra("ci") == "widgetpreview") { showWidgetPreview(); return }
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

    /** Yalnızca debug/CI: iki widget'ı gerçek RemoteViews ile ekranda gösterir (ekran görüntüsü için). */
    private fun showWidgetPreview() {
        val s = kotlinx.coroutines.runBlocking { com.nurvakti.global.data.SettingsStore(this@MainActivity).get() }
        val root = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(40, 200, 40, 40); setBackgroundColor(0xFF2B3A55.toInt()) }
        val d = resources.displayMetrics.density
        for ((large, h) in listOf(false to 90, true to 170)) {
            val v = com.nurvakti.global.widget.Widgets.build(this, s, large).apply(this, root)
            root.addView(v, android.widget.LinearLayout.LayoutParams(if (large) -1 else (200 * d).toInt(), (h * d).toInt()).apply { bottomMargin = (30 * d).toInt() })
        }
        setContentView(root)
    }
}
