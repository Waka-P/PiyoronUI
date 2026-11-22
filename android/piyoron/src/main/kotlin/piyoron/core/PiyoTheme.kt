package piyoron.core

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import piyoron.tokens.PiyoColorRoles
import piyoron.tokens.PiyoPreset
import piyoron.tokens.PiyoTokens
import piyoron.tokens.piyoColorRoles

// 形の組。カードの角は cardRoundness(基準の大きさでの、短い辺に対する半径の割合)と cardRoundnessGrowth(大きさに対する増え方)で決まる
@Immutable
data class PiyoShapes(
    val small: Shape = RoundedCornerShape(PiyoTokens.Radius.Sm),
    val medium: Shape = RoundedCornerShape(PiyoTokens.Radius.Md),
    val large: Shape = RoundedCornerShape(PiyoTokens.Radius.Lg),
    val pill: Shape = RoundedCornerShape(50),
    val circle: Shape = CircleShape,
    val cardRoundness: Float = PiyoTokens.Card.Roundness,
    val cardRoundnessGrowth: Float = PiyoTokens.Card.RoundnessGrowth,
) {
    val card: Shape = piyoCardShape(cardRoundness, cardRoundnessGrowth)
}

// 丸み度合いから、大きさに合わせて角が決まる形を作る
// 角の半径 = 度合い x 基準の長さ x (短い辺 / 基準の長さ)^growth。growth が 1 なら短い辺に比例、0 なら固定。半径は短い辺の半分まで
fun piyoCardShape(roundness: Float, growth: Float = PiyoTokens.Card.RoundnessGrowth): Shape =
    RoundedCornerShape(GrowingCorner(roundness.coerceIn(0f, 0.5f), growth.coerceIn(0f, 1f)))

// 大きさに合わせてゆるやかに増える角の半径
@Immutable
private data class GrowingCorner(private val roundness: Float, private val growth: Float) : CornerSize {

    // 形の大きさから半径(px)を求める
    override fun toPx(shapeSize: Size, density: Density): Float {
        val side = shapeSize.minDimension
        if (side <= 0f) return 0f
        val base = with(density) { PiyoTokens.Card.RoundnessBase.toPx() }
        return minOf(side / 2f, roundness * base * Math.pow((side / base).toDouble(), growth.toDouble()).toFloat())
    }
}

internal val LocalPiyoColors = staticCompositionLocalOf { piyoColorRoles(PiyoPreset.Ocean.seed) }
internal val LocalPiyoShapes = staticCompositionLocalOf { PiyoShapes() }
internal val LocalPiyoStyle = staticCompositionLocalOf<PiyoStyle> { PiyoStyle.Plain }

// 現在のテーマの取り出し口
object Piyo {
    val colors: PiyoColorRoles
        @Composable @ReadOnlyComposable get() = LocalPiyoColors.current

    val shapes: PiyoShapes
        @Composable @ReadOnlyComposable get() = LocalPiyoShapes.current

    val style: PiyoStyle
        @Composable @ReadOnlyComposable get() = LocalPiyoStyle.current

    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalPiyoReduceMotion.current

    val pressMotion: PiyoPressMotion
        @Composable @ReadOnlyComposable get() = LocalPiyoPressMotion.current
}

// シード色からテーマを作る。style の既定は Plain(ガラスは PiyoStyle.Glass)。reduceMotion の既定は端末の「アニメーションを削除」
@Composable
fun PiyoTheme(
    seed: Color = PiyoPreset.Ocean.seed,
    style: PiyoStyle = PiyoStyle.Plain,
    shapes: PiyoShapes = PiyoShapes(),
    reduceMotion: Boolean = systemReduceMotion(),
    pressMotion: PiyoPressMotion = PiyoPressMotion.Default,
    content: @Composable () -> Unit,
) {
    val colors = remember(seed) { piyoColorRoles(seed) }
    PiyoTheme(colors = colors, style = style, shapes = shapes, reduceMotion = reduceMotion, pressMotion = pressMotion, content = content)
}

// プリセットからテーマを作る
@Composable
fun PiyoTheme(
    preset: PiyoPreset,
    style: PiyoStyle = PiyoStyle.Plain,
    shapes: PiyoShapes = PiyoShapes(),
    reduceMotion: Boolean = systemReduceMotion(),
    pressMotion: PiyoPressMotion = PiyoPressMotion.Default,
    content: @Composable () -> Unit,
) {
    val colors = remember(preset) { preset.roles() }
    PiyoTheme(colors = colors, style = style, shapes = shapes, reduceMotion = reduceMotion, pressMotion = pressMotion, content = content)
}

// 作った色の組からテーマを作る
@Composable
fun PiyoTheme(
    colors: PiyoColorRoles,
    style: PiyoStyle = PiyoStyle.Plain,
    shapes: PiyoShapes = PiyoShapes(),
    reduceMotion: Boolean = systemReduceMotion(),
    pressMotion: PiyoPressMotion = PiyoPressMotion.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPiyoReduceMotion provides reduceMotion,
        LocalPiyoPressMotion provides pressMotion,
        LocalPiyoColors provides colors,
        LocalPiyoShapes provides shapes,
        LocalPiyoStyle provides style,
        LocalPiyoContentColor provides colors.contentDefault,
        LocalPiyoTextStyle provides TextStyle(fontSize = PiyoTokens.Text.Size, fontWeight = PiyoTokens.Text.Weight),
    ) { PiyoOverlayHost(content) }
}
