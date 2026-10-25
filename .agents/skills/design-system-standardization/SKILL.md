---
name: design-system-standardization
description: >-
  Standardizing Compose UI components, tokens, state hoisting, Atomic Design hierarchy,
  unidirectional data flow (UDF), immutability guarantees, and accessibility guidelines across
  all screens in AirMouse.
---

# Design System & UI Component Standardization Guide

Use this skill when creating new screens or refactoring existing ones to ensure strict design system consistency, performance optimization, and architectural uniformity.

## 1. Unidirectional Data Flow (UDF) Component Contract

Every screen MUST adhere to the standardized UDF triad:
1. **`UiState`**: Immutable data class holding the entire visual state of the screen.
2. **`UiAction`**: Sealed interface representing all possible user interactions.
3. **`UiEffect`**: Single-event channel for side effects (navigation, snackbars, haptics).

```kotlin
// 1. Immutable State
@Immutable
data class TouchpadUiState(
    val isConnected: Boolean = false,
    val pingMs: Long = 0L,
    val packetLossPct: Float = 0.0f,
    val activeMode: MouseMode = MouseMode.AIR_MOUSE,
    val sensitivity: Float = 1.0f,
    val isCalibrating: Boolean = false,
    val errorMessage: String? = null
)

// 2. User Actions
sealed interface TouchpadAction {
    data class OnMove(val dx: Float, val dy: Float) : TouchpadAction
    data class OnClick(val button: MouseButton) : TouchpadAction
    data class OnModeChange(val mode: MouseMode) : TouchpadAction
    data object OnStartCalibration : TouchpadAction
    data object OnDismissError : TouchpadAction
}

// 3. One-off Effects
sealed interface TouchpadEffect {
    data class ShowToast(val message: String) : TouchpadEffect
    data object TriggerHapticSuccess : TouchpadEffect
    data class NavigateTo(val route: Screen) : TouchpadEffect
}
```

---

## 2. Stateful vs. Stateless Screen Decoupling

Always separate the screen into a **Stateful Root** (connected to Hilt ViewModel) and a **Stateless Content Composable** (for easy Compose Previews and isolated screenshot testing):

```kotlin
// Stateful Entry (Wired to ViewModel & Navigation)
@Composable
fun MainRoute(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is MainEffect.NavigateToSettings -> onNavigateToSettings()
            is MainEffect.ShowSnackbar -> { /* handle */ }
        }
    }

    MainScreen(
        state = uiState,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

// Stateless Composable (Pure & Previewable)
@Composable
fun MainScreen(
    state: TouchpadUiState,
    onAction: (TouchpadAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { AirMouseTopBar(title = "AirMouse Touchpad") }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Screen content
        }
    }
}
```

---

## 3. Atomic Component Hierarchy

Organize UI elements under `presentation/components/`:

```
presentation/components/
├── atoms/          # Basic building blocks
│   ├── AirMouseBadge.kt
│   ├── PulsingDot.kt
│   └── TouchpadHapticIcon.kt
├── molecules/      # Reusable combinations
│   ├── StatusPill.kt
│   ├── SensitivitySliderRow.kt
│   └── ModeSelectorTabRow.kt
└── organisms/      # Complex feature sections
    ├── AirMouseTouchSurface.kt
    ├── GyroCalibrationCard.kt
    └── ServerConnectionBanner.kt
```

---

## 4. Design Tokens & Consistency Rules

### 1. Spacing & Dimensions
Never use hardcoded magic integers (`17.dp`, `23.dp`). Always reference `Dimensions.kt`:
```kotlin
object Dimensions {
    val spaceXXS = 2.dp
    val spaceXS  = 4.dp
    val spaceS   = 8.dp
    val spaceM   = 16.dp
    val spaceL   = 24.dp
    val spaceXL  = 32.dp
    
    val touchTargetMin = 48.dp
    val cornerRadiusS  = 8.dp
    val cornerRadiusM  = 16.dp
    val cornerRadiusL  = 24.dp
}
```

### 2. Strict Recomposition Stability
1. Use `@Immutable` or `@Stable` on all UI state classes.
2. If passing standard Kotlin collections (`List<T>`, `Set<T>`), wrap them with `kotlinx.collections.immutable.ImmutableList<T>` to allow Compose compiler to skip recomposition when lists haven't changed.
3. Lambdas passed down should use method references (`viewModel::onAction`) or be remembered to prevent instability.
