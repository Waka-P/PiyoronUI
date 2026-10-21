package piyoron.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

// 影の1層(CSS の box-shadow の1つ分)
data class PiyoShadow(
    val color: Color,
    val offsetX: Dp,
    val offsetY: Dp,
    val blur: Dp,
    val spread: Dp,
    val inset: Boolean,
)

// 角度つきの線形グラデーション(角度は CSS と同じく上が 0deg、時計回り)
data class PiyoGradient(
    val angle: Float,
    val stops: List<Pair<Float, Color>>,
)
