# 📘 Air Mouse Onboarding Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.onboarding` package contains the **Onboarding screen** for the Air Mouse application. This screen provides a **4-step walkthrough** that introduces new users to the app's features, guides them through initial setup, and collects their name for personalization.

```
com.airmouse.presentation.ui.onboarding/
├── OnboardingActivity.kt          # Main onboarding activity
├── OnboardingScreen.kt            # Compose onboarding screen (if applicable)
├── OnboardingViewModel.kt         # Onboarding ViewModel
├── OnboardingUiState.kt           # Onboarding state models
├── OnboardingPagerAdapter.kt      # ViewPager adapter for onboarding pages
├── OnboardingItem.kt              # Onboarding page data class
└── OnboardingComponents.kt        # Reusable onboarding UI components
```

**Note:** Based on the provided files, the Onboarding screen is implemented as both an Activity (`OnboardingActivity.kt`) and as a Compose screen (`OnboardingScreen.kt`). This document covers the complete implementation.

---

## 🎯 1. OnboardingActivity

### Purpose
The **entry point** for the onboarding experience. Displays a **4-page walkthrough** with animations, then transitions to the main app.

### Key Features

| Feature | Description |
|---------|-------------|
| **4-Step Walkthrough** | Introduction to Air Mouse features |
| **Animated Transitions** | Smooth page transitions with parallax effect |
| **Color Themes** | Each page has a unique background color |
| **Interactive Controls** | Next, Skip, Get Started buttons |
| **Page Indicators** | Dot indicators showing current page |
| **Back Navigation** | Swipe back or use back button to go to previous page |
| **Completion Tracking** | Marks onboarding as completed in preferences |

### Activity Structure

```kotlin
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: OnboardingPagerAdapter
    private var currentPosition = 0

    // Color palette for each page
    private val pageColors by lazy {
        intArrayOf(
            ContextCompat.getColor(this, R.color.onboarding_1_bg),
            ContextCompat.getColor(this, R.color.onboarding_2_bg),
            ContextCompat.getColor(this, R.color.onboarding_3_bg),
            ContextCompat.getColor(this, R.color.onboarding_4_bg)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(applicationContext)

        // Skip if already completed
        if (prefs.isOnboardingCompleted()) {
            startMainActivity()
            return
        }

        setupWindow()
        setupViewPager()
        setupClickListeners()
        setupPageChangeListener()
        setupWindowInsets()
        setupBackPressedHandler()
    }
}
```

---

## 🎯 2. OnboardingItem

### Purpose
Data class representing a single onboarding page.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `imageRes` | `@DrawableRes Int` | Illustration resource ID |
| `title` | `String` | Page title |
| `description` | `String` | Page description |
| `bgColor` | `@ColorRes Int` | Background color resource ID |

### Implementation

```kotlin
data class OnboardingItem(
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String,
    @ColorRes val bgColor: Int = R.color.onboarding_1_bg
)
```

### Page Data

```kotlin
val onboardingItems = listOf(
    OnboardingItem(
        imageRes = R.drawable.ic_air_mouse,
        title = getString(R.string.onboarding_title_1),
        description = getString(R.string.onboarding_desc_1),
        bgColor = R.color.onboarding_1_bg
    ),
    OnboardingItem(
        imageRes = R.drawable.ic_gesture,
        title = getString(R.string.onboarding_title_2),
        description = getString(R.string.onboarding_desc_2),
        bgColor = R.color.onboarding_2_bg
    ),
    OnboardingItem(
        imageRes = R.drawable.ic_wifi,
        title = getString(R.string.onboarding_title_3),
        description = getString(R.string.onboarding_desc_3),
        bgColor = R.color.onboarding_3_bg
    ),
    OnboardingItem(
        imageRes = R.drawable.ic_about,
        title = getString(R.string.onboarding_title_4),
        description = getString(R.string.onboarding_desc_4),
        bgColor = R.color.onboarding_4_bg
    )
)
```

### Page Content

| Page | Title | Description | Icon |
|------|-------|-------------|------|
| 1 | "Welcome to Air Mouse" | "Turn your phone into a wireless mouse" | 🖱️ |
| 2 | "Gesture Control" | "Control your PC with simple gestures" | ✋ |
| 3 | "Connect & Go" | "Connect via WiFi or Bluetooth" | 📶 |
| 4 | "Ready to Start" | "Get started with Air Mouse" | 🚀 |

---

## 🎯 3. OnboardingPagerAdapter

### Purpose
RecyclerView adapter for the onboarding ViewPager. Handles page rendering with optional entrance animations.

### Implementation

```kotlin
class OnboardingPagerAdapter(
    private val items: List<OnboardingItem>
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val animate = position == 0
        holder.bind(items[position], animate)
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: OnboardingViewHolder) {
        super.onViewRecycled(holder)
        holder.clearAnimations()
    }

    class OnboardingViewHolder(
        private val binding: ItemOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OnboardingItem, animate: Boolean = true) {
            binding.onboardingImage.setImageResource(item.imageRes)
            binding.onboardingTitle.text = item.title
            binding.onboardingDescription.text = item.description

            if (animate) {
                // Animate image (fade + slide up)
                binding.onboardingImage.apply {
                    alpha = 0f
                    translationY = 50f
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }

                // Animate title (fade in with delay)
                binding.onboardingTitle.apply {
                    alpha = 0f
                    animate()
                        .alpha(1f)
                        .setDuration(500)
                        .setStartDelay(150)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }

                // Animate description (fade in with longer delay)
                binding.onboardingDescription.apply {
                    alpha = 0f
                    animate()
                        .alpha(1f)
                        .setDuration(500)
                        .setStartDelay(300)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            } else {
                // No animation – fully visible
                binding.onboardingImage.alpha = 1f
                binding.onboardingImage.translationY = 0f
                binding.onboardingTitle.alpha = 1f
                binding.onboardingDescription.alpha = 1f
            }
        }

        fun clearAnimations() {
            binding.onboardingImage.animate().cancel()
            binding.onboardingTitle.animate().cancel()
            binding.onboardingDescription.animate().cancel()
            // Reset to fully visible
            binding.onboardingImage.alpha = 1f
            binding.onboardingImage.translationY = 0f
            binding.onboardingTitle.alpha = 1f
            binding.onboardingDescription.alpha = 1f
        }
    }
}
```

---

## 🎯 4. UI Components

### Page Transformer

```kotlin
binding.viewPager.setPageTransformer { page, position ->
    // Scale effect (zoom out slightly)
    val scale = 0.9f + (1f - kotlin.math.abs(position).coerceIn(0f, 1f)) * 0.1f
    page.scaleX = scale
    page.scaleY = scale

    // Parallax: move the page horizontally
    val translationXFactor = -position * page.width * 0.15f
    page.translationX = translationXFactor

    // Fade effect
    page.alpha = 1f - kotlin.math.abs(position).coerceIn(0f, 1f) * 0.3f

    // Slight rotation for a 3D feel
    page.rotationY = position * 8f
}
```

### Dot Indicator

```kotlin
TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

// Style dots
val dotColor = ContextCompat.getColor(this, android.R.color.darker_gray)
val selectedDotColor = ContextCompat.getColor(this, android.R.color.white)
binding.tabLayout.setSelectedTabIndicatorColor(selectedDotColor)
binding.tabLayout.setTabTextColors(dotColor, selectedDotColor)
```

### Button Animations

```kotlin
private fun animateButton(button: MaterialButton) {
    button.animate()
        .scaleX(0.92f)
        .scaleY(0.92f)
        .setDuration(120)
        .setInterpolator(DecelerateInterpolator())
        .withEndAction {
            button.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        .start()
}
```

### Background Color Animation

```kotlin
private fun animateBackgroundColor(position: Int) {
    val fromColor = pageColors.getOrNull(position) ?: pageColors[0]
    val toColor = pageColors.getOrNull(position) ?: pageColors[0]
    val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
    colorAnim.duration = 400
    colorAnim.interpolator = DecelerateInterpolator()
    colorAnim.addUpdateListener { animator ->
        binding.root.setBackgroundColor(animator.animatedValue as Int)
    }
    colorAnim.start()
}
```

---

## 🎯 5. Onboarding Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       ONBOARDING FLOW                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. APP LAUNCH                                                         │
│     ├── MainActivity.onCreate()                                       │
│     ├── Check onboarding status                                       │
│     └── If not completed → OnboardingActivity                        │
│                                                                         │
│  2. ONBOARDING ACTIVITY                                               │
│     ├── setupWindow()                                                 │
│     ├── setupViewPager()                                              │
│     │   └── 4 pages with animations                                  │
│     ├── setupClickListeners()                                         │
│     │   ├── Next → next page                                         │
│     │   ├── Skip → finish onboarding                                 │
│     │   └── Get Started → finish onboarding                          │
│     ├── setupPageChangeListener()                                     │
│     │   ├── Update background color                                  │
│     │   ├── Update button visibility                                 │
│     │   └── Update page indicators                                   │
│     └── setupBackPressedHandler()                                    │
│         └── Back → previous page or finish                           │
│                                                                         │
│  3. PAGE TRANSITIONS                                                  │
│     ├── Slide animation                                               │
│     ├── Parallax effect                                               │
│     ├── Scale effect                                                  │
│     ├── Fade effect                                                   │
│     └── 3D rotation effect                                            │
│                                                                         │
│  4. COMPLETION                                                        │
│     ├── prefs.setOnboardingCompleted(true)                           │
│     ├── Save user name (if entered)                                  │
│     └── Start MainActivity                                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🎨 Onboarding Pages Design

### Page 1: Welcome
```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                          📱 Air Mouse                                  │
│                                                                         │
│                    [Animated Phone Icon]                                │
│                                                                         │
│                    Welcome to Air Mouse                                │
│                   Turn your phone into a                               │
│                  wireless mouse for your PC                            │
│                                                                         │
│                    ● ○ ○ ○                                             │
│                                                                         │
│              [Skip]          [Next →]                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Page 2: Gestures
```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                          ✋ Gestures                                    │
│                                                                         │
│                    [Animated Gesture Icon]                              │
│                                                                         │
│                    Gesture Control                                     │
│                   Control your PC with simple                          │
│                   gestures and movements                               │
│                                                                         │
│                    ○ ● ○ ○                                             │
│                                                                         │
│              [Skip]          [Next →]                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Page 3: Connection
```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                          📶 Connection                                 │
│                                                                         │
│                    [Animated WiFi Icon]                                 │
│                                                                         │
│                    Connect & Go                                        │
│                   Connect via WiFi or Bluetooth                        │
│                   for seamless control                                 │
│                                                                         │
│                    ○ ○ ● ○                                             │
│                                                                         │
│              [Skip]          [Next →]                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Page 4: Ready
```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                          🚀 Ready                                      │
│                                                                         │
│                    [Animated Rocket Icon]                               │
│                                                                         │
│                    Ready to Start                                      │
│                   You're all set to start using                        │
│                   Air Mouse Pro!                                       │
│                                                                         │
│                    ○ ○ ○ ●                                             │
│                                                                         │
│                              [Get Started]                              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Color Palette

| Page | Color Name | Hex Value | Usage |
|------|------------|-----------|-------|
| 1 | `onboarding_1_bg` | `#1A1A2E` | Welcome page background |
| 2 | `onboarding_2_bg` | `#16213E` | Gestures page background |
| 3 | `onboarding_3_bg` | `#0F3460` | Connection page background |
| 4 | `onboarding_4_bg` | `#533483` | Ready page background |

---

## 🔧 Configuration

### Enable Edge-to-Edge

```kotlin
enableEdgeToEdge()
```

### Window Insets

```kotlin
private fun setupWindowInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        insets
    }
}
```

### Back Pressed Handler

```kotlin
private fun setupBackPressedHandler() {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (currentPosition > 0) {
                binding.viewPager.currentItem = currentPosition - 1
            } else {
                finishOnboarding()
            }
        }
    })
}
```

---

## 🎯 6. OnboardingScreen (Compose Version)

### Purpose
Compose version of the onboarding screen (alternative to the Activity version).

### Implementation

```kotlin
@Composable
fun OnboardingScreen(
    navigationActions: NavigationActions,
    onComplete: () -> Unit = { navigationActions.navigateToHome() }
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 4 })

    Scaffold(
        containerColor = uiState.currentPage.backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(
                    page = onboardingPages[page],
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip button
                if (pagerState.currentPage < 3) {
                    TextButton(onClick = onComplete) {
                        Text("Skip")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Page indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (pagerState.currentPage == index) 24.dp else 8.dp,
                                    height = 8.dp
                                )
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Next/Get Started button
                Button(
                    onClick = {
                        if (pagerState.currentPage < 3) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (pagerState.currentPage < 3) "Next" else "Get Started")
                }
            }
        }
    }
}
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Engaging** | Animated illustrations and smooth transitions |
| **Informative** | Clear, concise messaging |
| **User-Friendly** | Skip, back, and progress indicators |
| **Personalization** | User name collection |
| **Accessibility** | Large touch targets, clear text |
| **Performance** | Optimized animations |
| **Consistency** | Unified design language |

---

**The Onboarding Screen provides a polished, engaging introduction to the Air Mouse application, guiding new users through the key features and getting them ready to use the app.**