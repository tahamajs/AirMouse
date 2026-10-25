---
name: compose-ui-polish-craftsmanship
description: >-
  Architectural patterns and implementation recipes for high-craft UI polish in Jetpack Compose:
  subtle micro-interactions, 120 FPS spring physics, tactile haptic feedback, frosted glass
  surfaces, dynamic glowing accents, and AGSL shader effects.
---

# Jetpack Compose UI Polish & Craftsmanship Guide

This skill guides transforming standard, functional Compose components into production-grade, visually stunning, fluid user experiences.

## 1. Physics-Based Micro-Interactions (Spring Bounces)

Standard linear and tween animations feel robotic. Replace them with physics-based springs that provide tactile responsiveness:

```kotlin
@Composable
fun BouncyInteractiveSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    // Bouncy scale compression on touch down
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "PressScale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPressed) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
        )
    ) {
        content()
    }
}
```

---

## 2. Frosted Glass & Surface Translucency (Glassmorphism)

Create high-craft frosted glass cards and floating HUD elements:

```kotlin
@Composable
fun FrostedGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp),
        content = content
    )
}
```

---

## 3. Dynamic Status Pill with Live Pulsing Indicator

The connection indicator in AirMouse should visually communicate health instantly:

```kotlin
@Composable
fun StatusHealthIndicator(
    pingMs: Long,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected) 1.35f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val indicatorColor = when {
        !isConnected -> MaterialTheme.colorScheme.error
        pingMs < 15 -> Color(0xFF10B981) // Emerald Green
        pingMs < 45 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444)        // Red
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(
                color = indicatorColor.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .background(color = indicatorColor, shape = CircleShape)
        )
        Text(
            text = if (isConnected) "${pingMs}ms" else "Offline",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = indicatorColor
            )
        )
    }
}
```

---

## 4. AGSL Shaders on Android 13+ (API 33+)

Use runtime AGSL shaders for dynamic GPU lighting, ripples, and chromatic effects:

```kotlin
private const val GLOW_SHADER_SRC = """
    uniform float2 resolution;
    uniform float time;
    uniform float4 glowColor;
    
    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        float dist = distance(uv, float2(0.5, 0.5));
        float intensity = 0.05 / (dist + 0.02);
        return half4(glowColor.rgb * intensity, glowColor.a * intensity);
    }
"""

fun Modifier.dynamicGlowEffect(color: Color): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(GLOW_SHADER_SRC) }
        graphicsLayer {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setColorUniform("glowColor", color.toArgb())
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }
    } else {
        this // Fallback gracefully on older Android versions
    }
}
```

---

## 5. UI Polish Checklist
1. **No Hard Cuts**: Always use AnimatedVisibility or Crossfade when elements appear/disappear.
2. **Predictable Feedback**: Every button must emit haptic feedback and visual elevation or scale change upon press.
3. **Target Sizes**: Touch zones MUST be at least 48x48 dp even if the visual icon inside is 24 dp (`Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`).
4. **Theme Contrast**: Check that text on colored cards adheres to WCAG AA (minimum 4.5:1 contrast).
