package com.example.myproject.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import com.example.myproject.ui.theme.Dimens
import com.example.myproject.ui.theme.ScaleAnimationNode

/**
 * Reusable Compose micro-interaction modifiers.
 *
 * These modifiers add tactile feedback to interactive UI elements without
 * requiring any external dependencies. They complement the existing
 * [pressedScale] in Animation.kt with richer behaviour variants.
 *
 * Quick reference:
 * - [pressableScale]       – scale down on press + optional click handler
 * - [bounceClick]          – scale up briefly on tap (chips, toggles)
 * - [animateSelectionScale]– drive scale from selection state (chips, radio buttons)
 * - [animatedScale]        – apply scale driven by external pressed state
 * - [shortDurationTween]   – 150 ms tween spec helper
 * - [mediumDurationTween]  – 300 ms tween spec helper
 */

private object InteractionConstants {
    /** Scale factor when pressed */
    const val PRESSED_SCALE = 0.96f

    /** Normal resting scale */
    const val NORMAL_SCALE = 1f

    /** Scale factor at peak of bounce animation */
    const val BOUNCE_SCALE_PEAK = 1.05f

    const val SPRING_STIFFNESS_PRESS = Spring.StiffnessMediumLow
    const val SPRING_DAMPING_PRESS = Spring.DampingRatioMediumBouncy
}

/**
 * Scales down the component when pressed and springs back on release.
 * Provides tactile feedback for cards and large tappable surfaces.
 *
 * This is a richer alternative to the existing [pressedScale] in Animation.kt —
 * it accepts an optional [onClick] handler so you don't need a separate
 * `.clickable {}` modifier.
 *
 * Usage:
 * ```kotlin
 * Card(
 *     modifier = Modifier.pressableScale(onClick = { navigateTo(item) })
 * ) { ... }
 * ```
 *
 * @param enabled   Whether the animation is active
 * @param pressedScale Scale when pressed (default 0.96)
 * @param onClick   Optional tap callback
 */
fun Modifier.pressableScale(
    enabled: Boolean = true,
    pressedScale: Float = InteractionConstants.PRESSED_SCALE,
    onClick: (() -> Unit)? = null,
): Modifier = this then PressableScaleElement(enabled, pressedScale, onClick)

private val PressSpring = spring<Float>(
    dampingRatio = InteractionConstants.SPRING_DAMPING_PRESS,
    stiffness = InteractionConstants.SPRING_STIFFNESS_PRESS,
)

private data class PressableScaleElement(
    val enabled: Boolean,
    val pressedScale: Float,
    val onClick: (() -> Unit)?,
) : ModifierNodeElement<PressableScaleNode>() {
    override fun create() = PressableScaleNode(enabled, pressedScale, onClick)

    override fun update(node: PressableScaleNode) {
        node.update(enabled, pressedScale, onClick)
    }
}

private class PressableScaleNode(
    private var enabled: Boolean,
    private var pressedScale: Float,
    private var onClick: (() -> Unit)?,
) : ScaleAnimationNode() {
    private val pointerInput = delegate(
        SuspendingPointerInputModifierNode {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        animateScale { animateTo(pressedScale, PressSpring) }
                        try {
                            awaitRelease()
                        } finally {
                            animateScale { animateTo(InteractionConstants.NORMAL_SCALE, PressSpring) }
                        }
                    },
                    onTap = { onClick?.invoke() },
                )
            }
        },
    )

    fun update(enabled: Boolean, pressedScale: Float, onClick: (() -> Unit)?) {
        this.pressedScale = pressedScale
        this.onClick = onClick
        if (this.enabled != enabled) {
            this.enabled = enabled
            pointerInput.resetPointerInputHandler()
            animateScale { animateTo(InteractionConstants.NORMAL_SCALE, PressSpring) }
        }
    }
}

/**
 * Adds a bounce animation when tapped — scales up briefly then springs back.
 * Best for small interactive elements like filter chips and toggle buttons.
 *
 * Usage:
 * ```kotlin
 * FilterChip(
 *     modifier = Modifier.bounceClick(onClick = { toggleFilter() }),
 *     ...
 * )
 * ```
 *
 * @param enabled Whether the animation is active
 * @param onClick Callback invoked on tap
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = this then BounceClickElement(enabled, onClick)

private val BounceSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessHigh,
)

private data class BounceClickElement(
    val enabled: Boolean,
    val onClick: () -> Unit,
) : ModifierNodeElement<BounceClickNode>() {
    override fun create() = BounceClickNode(enabled, onClick)

    override fun update(node: BounceClickNode) {
        node.update(enabled, onClick)
    }
}

private class BounceClickNode(
    private var enabled: Boolean,
    private var onClick: () -> Unit,
) : ScaleAnimationNode() {
    private val pointerInput = delegate(
        SuspendingPointerInputModifierNode {
            if (enabled) {
                detectTapGestures(
                    onTap = {
                        animateScale {
                            animateTo(InteractionConstants.BOUNCE_SCALE_PEAK, BounceSpring)
                            animateTo(InteractionConstants.NORMAL_SCALE, BounceSpring)
                        }
                        onClick()
                    },
                )
            }
        },
    )

    fun update(enabled: Boolean, onClick: () -> Unit) {
        this.onClick = onClick
        if (this.enabled != enabled) {
            this.enabled = enabled
            pointerInput.resetPointerInputHandler()
        }
    }
}

/**
 * Returns an animated scale value driven by [isSelected] state.
 * The selected state pulses to [InteractionConstants.BOUNCE_SCALE_PEAK] then settles at 1f.
 *
 * Use with `Modifier.graphicsLayer { scaleX = scale; scaleY = scale }` for custom layouts.
 *
 * Usage:
 * ```kotlin
 * val scale = animateSelectionScale(isSelected = isSelected)
 * Box(modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
 *     FilterChip(selected = isSelected, ...)
 * }
 * ```
 *
 * @param isSelected Whether the component is currently selected
 * @return Animated scale value (1f when unselected, pulses to 1.05f when selected)
 */
@Composable
fun animateSelectionScale(isSelected: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) {
            InteractionConstants.BOUNCE_SCALE_PEAK
        } else {
            InteractionConstants.NORMAL_SCALE
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "selectionScale",
    )
    return scale
}

/**
 * Applies a scale animation based on an externally tracked [isPressed] state.
 * Use this when the click handling is done elsewhere (e.g. by [Card]'s `onClick`)
 * and you just need the visual scale effect.
 *
 * Usage:
 * ```kotlin
 * val interactionSource = remember { MutableInteractionSource() }
 * val isPressed by interactionSource.collectIsPressedAsState()
 *
 * Card(
 *     modifier = Modifier.animatedScale(isPressed),
 *     interactionSource = interactionSource,
 *     onClick = { ... }
 * ) { ... }
 * ```
 *
 * @param isPressed Whether the element is currently pressed
 * @param pressedScale Target scale when pressed (default 0.96)
 */
@Composable
fun Modifier.animatedScale(
    isPressed: Boolean,
    pressedScale: Float = InteractionConstants.PRESSED_SCALE,
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else InteractionConstants.NORMAL_SCALE,
        animationSpec = spring(
            dampingRatio = InteractionConstants.SPRING_DAMPING_PRESS,
            stiffness = InteractionConstants.SPRING_STIFFNESS_PRESS,
        ),
        label = "animatedScale",
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A tween spec using [Dimens.ANIMATION_DURATION_SHORT] (150 ms).
 * Ideal for color and opacity transitions that should feel instant.
 *
 * Usage:
 * ```kotlin
 * val color by animateColorAsState(
 *     targetValue = if (selected) primary else surface,
 *     animationSpec = shortDurationTween()
 * )
 * ```
 */
fun <T> shortDurationTween() = tween<T>(durationMillis = Dimens.ANIMATION_DURATION_SHORT)

/**
 * A tween spec using [Dimens.ANIMATION_DURATION_MEDIUM] (300 ms).
 * Ideal for more noticeable layout or colour transitions.
 *
 * Usage:
 * ```kotlin
 * val alpha by animateFloatAsState(
 *     targetValue = if (visible) 1f else 0f,
 *     animationSpec = mediumDurationTween()
 * )
 * ```
 */
fun <T> mediumDurationTween() = tween<T>(durationMillis = Dimens.ANIMATION_DURATION_MEDIUM)
