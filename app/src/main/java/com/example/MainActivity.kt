package com.example

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.preferences.ThemeMode
import com.example.ui.bookmarks.BookmarksScreen
import com.example.ui.bookmarks.BookmarksViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.navigation.Screen
import com.example.ui.reader.ReaderScreen
import com.example.ui.reader.ReaderViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.PdfReaderTheme

sealed class BottomNavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : BottomNavDestination("home", "Home", Icons.Filled.Home, Icons.Outlined.Home, "bottom_nav_home")
    object Files : BottomNavDestination("files", "Files", Icons.Filled.Folder, Icons.Outlined.Folder, "bottom_nav_files")
    object Bookmarks : BottomNavDestination("bookmarks", "Bookmarks", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "bottom_nav_bookmarks")
    object Settings : BottomNavDestination("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "bottom_nav_settings")
}

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()

            val isDark = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(prefs.keepScreenOn) {
                if (prefs.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            PdfReaderTheme(darkTheme = isDark) {
                PdfReaderApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun PdfReaderApp(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val bookmarksViewModel: BookmarksViewModel = viewModel()
    val readerViewModel: ReaderViewModel = viewModel()

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    androidx.compose.runtime.LaunchedEffect(Unit) {
        activity?.intent?.let { intent ->
            if (intent.action == android.content.Intent.ACTION_VIEW) {
                intent.data?.let { uri ->
                    homeViewModel.importDocument(uri) { importedUri ->
                        navController.navigate(com.example.ui.navigation.Screen.Reader.createRoute(importedUri))
                    }
                    intent.action = null
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavDestination.Home,
        BottomNavDestination.Files,
        BottomNavDestination.Bookmarks,
        BottomNavDestination.Settings
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .height(58.dp)
                        .testTag("app_bottom_nav_bar")
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title, fontSize = 11.5.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() + 2.dp else 0.dp)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToReader = { uri ->
                        navController.navigate(Screen.Reader.createRoute(uri))
                    },
                    onNavigateToBookmarks = {
                        navController.navigate(Screen.Bookmarks.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Files.route) {
                LibraryScreen(
                    homeViewModel = homeViewModel,
                    onNavigateToReader = { uri ->
                        navController.navigate(Screen.Reader.createRoute(uri))
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(Screen.Bookmarks.route) {
                BookmarksScreen(
                    viewModel = bookmarksViewModel,
                    onNavigateToDocument = { uri, page ->
                        navController.navigate(Screen.Reader.createRoute(uri))
                        readerViewModel.onPageChanged(page)
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(
                route = Screen.Reader.route,
                arguments = listOf(
                    navArgument("encodedUri") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("encodedUri") ?: ""
                val decodedUri = Uri.decode(encodedUri)

                ReaderScreen(
                    documentUri = decodedUri,
                    viewModel = readerViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
