package piyoron.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import piyoron.tokens.PiyoTokens

// 押したときの動きの量(dp)と速さ(ミリ秒)
@Immutable
data class PiyoPressMotion(
    val hoverGrow: Dp = PiyoTokens.Press.HoverGrow,
    val activeShrink: Dp = PiyoTokens.Press.ActiveShrink,
    val touchShrink: Dp = PiyoTokens.Press.TouchShrink,
    val hoverDuration: Int = PiyoTokens.Press.HoverDuration,
    val activeDuration: Int = PiyoTokens.Press.ActiveDuration,
) {
    companion object {
        // 既定(Piyoron のボタンと同じ)
        val Default = PiyoPressMotion()

        // 動かさない
        val None = PiyoPressMotion(hoverGrow = 0.dp, activeShrink = 0.dp, touchShrink = 0.dp)
    }
}

// テーマ全体の押したときの動き
internal val LocalPiyoPressMotion = staticCompositionLocalOf { PiyoPressMotion.Default }

// 押したときの動きを付ける(interactionSource は clickable に渡したものと同じもの)
@Composable
fun Modifier.piyoPress(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    motion: PiyoPressMotion = Piyo.pressMotion,
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val grow = remember { Animatable(0f) }
    val current by rememberUpdatedState(motion)
    val rest by rememberUpdatedState(if (enabled && hovered) motion.hoverGrow.value else 0f)
    val pressed by rememberUpdatedState(
        if (hovered) motion.hoverGrow.value - motion.activeShrink.value else -motion.touchShrink.value,
    )
    val active by rememberUpdatedState(enabled)
    val holding = remember { mutableStateOf(false) }
    if (LocalPiyoReduceMotion.current) return this
    LaunchedEffect(interactionSource) {
        var job: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> if (active) {
                    holding.value = true
                    job?.cancel()
                    job = launch {
                        grow.animateTo(pressed, tween(current.activeDuration))
                        if (!holding.value) grow.animateTo(rest, tween(current.hoverDuration))
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    holding.value = false
                    if (job?.isActive != true) job = launch { grow.animateTo(rest, tween(current.hoverDuration)) }
                }
            }
        }
    }
    LaunchedEffect(rest) {
        if (!holding.value) grow.animateTo(rest, tween(current.hoverDuration))
    }
    return graphicsLayer {
        val amount = grow.value.dp.toPx()
        if (size.width > 0f) scaleX = 1f + amount / size.width
        if (size.height > 0f) scaleY = 1f + amount / size.height
    }
}

// 押せるようにし、押したときの動きを付ける
@Composable
fun Modifier.piyoClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    motion: PiyoPressMotion = Piyo.pressMotion,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    return clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    ).piyoPress(source, enabled, motion)
}
