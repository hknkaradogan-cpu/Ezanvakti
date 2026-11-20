package com.nurvakti.global.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nurvakti.global.ui.screens.*

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val TABS = listOf(Tab("home", "Ana sayfa", Icons.Filled.Home), Tab("times", "Vakitler", Icons.Filled.AccessTime),
    Tab("quran", "Kur'an", Icons.AutoMirrored.Filled.MenuBook), Tab("qibla", "Kıble", Icons.Filled.Explore), Tab("menu", "Menü", Icons.Filled.GridView))

@Composable fun NurRoot(vm: AppVm, openTarget: String?) {
    val s by vm.settings.collectAsState()
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(strings.t(it)) } }
    
    val cur = s
    if (cur == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    if (!cur.onboarded) { Onboarding(vm); return }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (TABS.any { it.route == route }) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                TABS.forEach { tab ->
                    NavigationBarItem(selected = route == tab.route, onClick = { nav.navigate(tab.route) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(tab.icon, null) }, label = { Text(t(tab.label)) })
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) { routes(vm, nav) }
        LaunchedEffect(openTarget) { if (openTarget != null && openTarget != "home") runCatching { nav.navigate(openTarget) } }
    }
}

private fun androidx.navigation.NavGraphBuilder.routes(vm: AppVm, nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    val go: (String) -> Unit = { nav.navigate(it) }
    composable("home") { HomeScreen(vm, go) }
    composable("times") { TimesScreen(vm, go) }
    composable("quran") { QuranList(vm, go) }
    composable("qibla") { QiblaScreen(vm) }
    composable("menu") { MenuScreen(go) }
    composable("reader/{s}?a={a}", arguments = listOf(navArgument("s") { type = NavType.IntType }, navArgument("a") { type = NavType.IntType; defaultValue = 1 })) {
        ReaderScreen(vm, it.arguments?.getInt("s") ?: 1, it.arguments?.getInt("a") ?: 1, back, go)
    }
    composable("reciters") { RecitersScreen(vm, back, go) }
    composable("yasin") { ReaderScreen(vm, 36, 1, back, go, yasin = true) }
    composable("player") { PlayerScreen(vm, back) }
    composable("bookmarks") { BookmarksScreen(vm, back, go) }
    composable("settings") { SettingsScreen(vm, back, go) }
    composable("location") { LocationScreen(vm, back) }
    composable("ezan") { EzanSettingsScreen(vm, back, go) }
    composable("battery") { BatteryScreen(back) }
    composable("downloads") { DownloadsScreen(vm, back) }
    composable("dualar") { DualarScreen(vm, back) }
    composable("zikir") { ZikirScreen(vm, back) }
    composable("esma") { EsmaScreen(vm, back) }
    composable("ramazan") { RamazanScreen(vm, back, go) }
    composable("takvim") { TakvimScreen(vm, back) }
    composable("tracker") { TrackerScreen(vm, back) }
    composable("zekat") { ZekatScreen(vm, back) }
    composable("camiler") { CamilerScreen(vm, back) }
    composable("rehber") { RehberScreen(vm, back) }
    composable("about") { AboutScreen(vm, back) }
}
