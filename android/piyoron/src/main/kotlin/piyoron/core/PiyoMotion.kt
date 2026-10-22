package piyoron.core

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

// 動きを減らすか(true ならアニメーションなしで切り替える)
val LocalPiyoReduceMotion = staticCompositionLocalOf { false }

// 端末の「アニメーションを削除」の設定を読む
@Composable
fun systemReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

// 2つの値の間
fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

// 区切りの位置ごとの値を、進行度に合わせて直線でつなぐ
fun keyframes(t: Float, offsets: FloatArray, values: FloatArray): Float {
    if (t <= offsets.first()) return values.first()
    for (i in 1 until offsets.size) {
        if (t <= offsets[i]) return lerp(values[i - 1], values[i], (t - offsets[i - 1]) / (offsets[i] - offsets[i - 1]))
    }
    return values.last()
}

// 選択の印の動き(どこからどこへ、どこまで進んだか)。PiyoTabs とひょうたんで共有する
@Stable
class IndicatorMotion internal constructor(initial: Int, private val easing: Easing) {
    internal val progress = Animatable(1f)

    // 動き始めの位置(項目の番号。途中で変わったときは小数になる)
    var from by mutableFloatStateOf(initial.toFloat())
        internal set

    // 行き先の項目の番号
    var to by mutableIntStateOf(initial)
        internal set

    // 進み具合(0〜1、イージング後)
    val fraction: Float get() = easing.transform(progress.value)

    // 今の位置(項目の番号。滑るだけの動きに使う)
    val position: Float get() = lerp(from, to.toFloat(), fraction)

    // 動きの途中か
    val moving: Boolean get() = progress.value < 1f
}

// 選んだ項目が変わったら、印を動かす。動いている途中なら start が返す位置(項目の番号)から動き直す
@Composable
fun rememberIndicatorMotion(
    selectedIndex: Int,
    durationMillis: Int,
    easing: Easing,
    start: (IndicatorMotion) -> Float = { it.position },
): IndicatorMotion {
    val motion = remember { IndicatorMotion(selectedIndex, easing) }
    val reduce = LocalPiyoReduceMotion.current
    val currentStart by rememberUpdatedState(start)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex == motion.to) return@LaunchedEffect
        motion.from = if (motion.moving) currentStart(motion) else motion.to.toFloat()
        motion.to = selectedIndex
        if (reduce) {
            motion.progress.snapTo(1f)
        } else {
            motion.progress.snapTo(0f)
            motion.progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
        }
    }
    return motion
}
