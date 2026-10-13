package com.pianotype.android.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private sealed class Page(val label: String, val icon: ImageVector) {
    data object Settings : Page("設定", Icons.Default.Settings)
    data object About : Page("このアプリについて", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(context: Context, onOpenImeSettings: () -> Unit) {
    var current by remember { mutableStateOf<Page>(Page.Settings) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Pianotype",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                DrawerItem(
                    label = Page.Settings.label,
                    icon = Page.Settings.icon,
                    selected = current === Page.Settings,
                    onClick = {
                        current = Page.Settings
                        scope.launch { drawerState.close() }
                    }
                )
                DrawerItem(
                    label = Page.About.label,
                    icon = Page.About.icon,
                    selected = current === Page.About,
                    onClick = {
                        current = Page.About
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (current) {
                                is Page.Settings -> "Pianotype 設定"
                                is Page.About -> "このアプリについて"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "メニュー")
                        }
                    },
                    actions = {
                        if (current === Page.Settings) {
                            TextButton(onClick = onOpenImeSettings) { Text("IME設定") }
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                when (current) {
                    is Page.Settings -> SettingsContent(context = context)
                    is Page.About -> AboutContent()
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}