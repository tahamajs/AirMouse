---
name: ui-screen-navigation-orchestration
description: >-
  Standardizing and orchestrating screen navigation, multi-screen data flow, shared element
  transitions, bottom bar visibility coordination, backstack hygiene, and modal sheets in
  Jetpack Compose and modern Android architectures.
---

# Screen Navigation & Interconnection Orchestration

This skill provides comprehensive rules, code patterns, and architectural standards for wiring, connecting, and passing state between screens cleanly in AirMouse and Compose applications.

## 1. Type-Safe Navigation & Destination Modeling

All navigation routes MUST be modeled as typed, sealed structures instead of loose raw strings:

```kotlin
package com.airmouse.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Main : Screen
    @Serializable data object Calibration : Screen
    @Serializable data class CalibrationResult(val score: Float, val timestamp: Long) : Screen
    @Serializable data object Themes : Screen
    @Serializable data object ScreenMirroring : Screen
    @Serializable data object Accessibility : Screen
    @Serializable data object Help : Screen
    @Serializable data object Settings : Screen
}
```

### Destination Registry with Metadata
Always attach metadata (titles, icons, top/bottom bar visibility flags) to destination declarations:

```kotlin
sealed class Destination(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
    val showBottomBar: Boolean = true,
    val showFloatingStatusPill: Boolean = true
) {
    object Main : Destination("main", R.string.nav_main, Icons.Default.TouchApp)
    object Calibration : Destination("calibration", R.string.nav_calibration, Icons.Default.Speed, showBottomBar = false)
    object Themes : Destination("themes", R.string.nav_themes, Icons.Default.Palette)
    object Settings : Destination("settings", R.string.nav_settings, Icons.Default.Settings)
}
```

---

## 2. NavHost Setup with Seamless Transitions

Configure `NavHost` with polished enter/exit animations that give tactile depth to screen transitions:

```kotlin
@Composable
fun AirMouseNavHost(
    navController: NavHostController,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(220))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(180))
        }
    ) {
        composable<Screen.Main> {
            MainScreen(
                onNavigateToCalibration = { navController.navigate(Screen.Calibration) },
                onNavigateToThemes = { navController.navigate(Screen.Themes) }
            )
        }
        composable<Screen.Calibration> {
            CalibrationScreen(
                onCalibrationComplete = { score ->
                    navController.navigate(Screen.CalibrationResult(score, System.currentTimeMillis())) {
                        popUpTo(Screen.Calibration) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

---

## 3. Backstack Hygiene & Multi-Stack State Preservation

### 1. Single-Top & State Restoration for Bottom Navigation
When switching between top-level bottom bar tabs, avoid pushing duplicate instances to the backstack and preserve user scroll/input state:

```kotlin
fun NavHostController.navigateSingleTopTo(route: Screen) {
    this.navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
```

### 2. Contextual Bottom Bar Visibility
Synchronize bottom bar and floating overlay visibility with current backstack entry:

```kotlin
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentDestination = navBackStackEntry?.destination

val shouldShowBottomBar = remember(currentDestination) {
    // Only show on primary hub screens
    listOf(Screen.Main::class.qualifiedName, Screen.Themes::class.qualifiedName)
        .contains(currentDestination?.route)
}

Scaffold(
    bottomBar = {
        AnimatedVisibility(
            visible = shouldShowBottomBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            AirMouseBottomBar(navController = navController)
        }
    }
) { innerPadding ->
    // Screen content with innerPadding
}
```

---

## 4. Persistent Global State Injection

Never recreate connection listeners or viewmodels on screen change. Hoist session state to a shared singleton or activity-scoped ViewModel:

```kotlin
// In MainActivity:
val connectionViewModel: ConnectionViewModel = hiltViewModel()
val connectionState by connectionViewModel.uiState.collectAsStateWithLifecycle()

// Global Floating Status Pill overlaid across all screens
Box(modifier = Modifier.fillMaxSize()) {
    AirMouseNavHost(navController, connectionState)
    
    // Floating Status Pill remains smooth and uninterrupted across page slides
    FloatingStatusPill(
        state = connectionState,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 8.dp)
    )
}
```
