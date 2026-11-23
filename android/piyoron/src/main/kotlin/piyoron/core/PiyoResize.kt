package piyoron.core

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import piyoron.tokens.PiyoTokens

// 中身で大きさが変わるとき、なめらかに変える(動きを減らす設定のときは、すぐ変える)
@Composable
fun Modifier.piyoAnimateSize(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val duration = if (Piyo.reduceMotion) 0 else PiyoTokens.Resize.Duration
    return animateContentSize(tween(duration, easing = PiyoTokens.Resize.Easing))
}
