package com.example.myproject.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

/**
 * Animation specs derived from Airbnb/Spotify/Revolut motion patterns.
 * Use these instead of ad-hoc durations/easing throughout the app.
 */
object AppAnimationSpecs {
    /** Button press feedback */
    const val PRESS_DURATION_MS = 100

    /** Card expand/collapse */
    val cardSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)

    /** Page/screen transitions */
    const val PAGE_TRANSITION_MS = 400

    /** Bottom sheet open/close */
    val sheetSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 200f)

    /** Skeleton shimmer fade-in */
    const val SKELETON_FADE_MS = 200

    /** Success confirmation animation */
    const val SUCCESS_DURATION_MS = 600

    /** Fade-in for content appearing */
    fun <T> fadeIn() = tween<T>(durationMillis = 300)

    /** Gentle bounce for items entering */
    fun <T> gentleBounce() = spring<T>(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
}

/**
 * Press-scale feedback modifier. All interactive elements should use this.
 * Scales down to [targetScale] on press, springs back on release.
 *
 * Usage:
 * ```kotlin
 * Card(modifier = Modifier.pressedScale().clickable { ... })
 * ```
 */
fun Modifier.pressedScale(targetScale: Float = 0.96f): Modifier = this then PressedScaleElement(targetScale)

private val PressedScaleSpring = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

private data class PressedScaleElement(val targetScale: Float) : ModifierNodeElement<PressedScaleNode>() {
    override fun create() = PressedScaleNode(targetScale)

    override fun update(node: PressedScaleNode) {
        node.targetScale = targetScale
    }
}

private class PressedScaleNode(var targetScale: Float) : ScaleAnimationNode() {
    init {
        delegate(
            SuspendingPointerInputModifierNode {
                detectTapGestures(
                    onPress = {
                        animateScale { animateTo(targetScale, PressedScaleSpring) }
                        tryAwaitRelease()
                        animateScale { animateTo(1f, PressedScaleSpring) }
                    },
                )
            },
        )
    }
}

/**
 * Base [Modifier.Node] for scale feedback (the replacement for `composed {}` modifiers). The
 * scale is only read in the placement layer, so animation frames skip recomposition and
 * measurement. Subclasses delegate a pointer input node and call [animateScale].
 */
internal open class ScaleAnimationNode :
    DelegatingNode(),
    LayoutModifierNode {
    private var scale = Animatable(1f)

    /** Runs [animation] on the current scale; a new call cancels the running one. */
    protected fun animateScale(animation: suspend Animatable<Float, AnimationVector1D>.() -> Unit) {
        if (!isAttached) return
        val current = scale
        coroutineScope.launch { current.animation() }
    }

    // A recycled node (lazy list item reuse) starts unscaled, like freshly remembered state.
    override fun onReset() {
        scale = Animatable(1f)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale.value
                scaleY = scale.value
            }
        }
    }
}
