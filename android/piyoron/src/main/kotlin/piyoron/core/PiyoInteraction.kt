package piyoron.core

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.Dp
import piyoron.tokens.PiyoTokens
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
