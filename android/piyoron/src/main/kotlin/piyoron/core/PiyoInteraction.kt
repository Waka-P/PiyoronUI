package piyoron.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import piyoron.tokens.PiyoTokens
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// 操作の状態を集めて PiyoState にする
@Composable
fun rememberPiyoState(interactionSource: InteractionSource, selected: Boolean = false, enabled: Boolean = true): PiyoState {
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    return PiyoState(hovered = hovered && enabled, pressed = pressed && enabled, focused = focused, selected = selected, enabled = enabled)
}

// 押したときの動き。マウスでは乗せると少し大きく、押すと少し縮む。タッチでは押すと元より小さくなる(動きを減らす設定では動かさない)
// 大きさの変化は dp の量で決め、部品の大きさによらず縁が同じだけ動く(大きいカードでも動きすぎない)
@Composable
fun Modifier.pressMotion(interactionSource: InteractionSource, state: PiyoState): Modifier {
    val grow = remember { Animatable(0f) }
    val hoverGrow = PiyoTokens.Press.HoverGrow.value
    val rest by rememberUpdatedState(if (state.enabled && state.hovered) hoverGrow else 0f)
    val pressed by rememberUpdatedState(
        if (state.hovered) hoverGrow - PiyoTokens.Press.ActiveShrink.value else -PiyoTokens.Press.TouchShrink.value,
    )
    val holding = remember { mutableStateOf(false) }
    if (LocalPiyoReduceMotion.current) return this
    LaunchedEffect(interactionSource) {
        var job: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    holding.value = true
                    job?.cancel()
                    job = launch {
                        grow.animateTo(pressed, tween(PiyoTokens.Press.ActiveDuration))
                        if (!holding.value) grow.animateTo(rest, tween(PiyoTokens.Press.HoverDuration))
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    holding.value = false
                    if (job?.isActive != true) job = launch { grow.animateTo(rest, tween(PiyoTokens.Press.HoverDuration)) }
                }
            }
        }
    }
    LaunchedEffect(rest) {
        if (!holding.value) grow.animateTo(rest, tween(PiyoTokens.Press.HoverDuration))
    }
    return graphicsLayer {
        val amount = grow.value.dp.toPx()
        if (size.width > 0f) scaleX = 1f + amount / size.width
        if (size.height > 0f) scaleY = 1f + amount / size.height
    }
}

// 無効のときに薄くする
fun Modifier.disabledAlpha(enabled: Boolean): Modifier =
    if (enabled) this else alpha(PiyoTokens.Control.DisabledOpacity)

// 見た目より小さくても、触れる範囲を最小サイズまで広げる
fun Modifier.touchTarget(minSize: Dp = PiyoTokens.Touch.MinSize): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val min = minSize.roundToPx()
    val width = constraints.constrainWidth(max(placeable.width, min))
    val height = constraints.constrainHeight(max(placeable.height, min))
    layout(width, height) {
        placeable.place(((width - placeable.width) / 2f).roundToInt(), ((height - placeable.height) / 2f).roundToInt())
    }
}

// キーボードで選んだときの輪郭
@Composable
fun Modifier.focusRing(focused: Boolean, shape: Shape): Modifier {
    if (!focused) return this
    val color = Piyo.colors.primary
    return drawWithContent {
        drawContent()
        val width = PiyoTokens.Control.FocusWidth.toPx()
        val gap = PiyoTokens.Control.FocusOffset.toPx() + width / 2
        val outline = shape.createOutline(Size(size.width + gap * 2, size.height + gap * 2), layoutDirection, this)
        translate(-gap, -gap) { drawOutline(outline, color, style = Stroke(width)) }
    }
}
