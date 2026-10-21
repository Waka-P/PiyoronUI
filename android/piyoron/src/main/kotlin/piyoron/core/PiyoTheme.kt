package piyoron.core

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import piyoron.tokens.PiyoColorRoles
import piyoron.tokens.PiyoPreset
import piyoron.tokens.PiyoTokens
import piyoron.tokens.piyoColorRoles

// 形の組
@Immutable
data class PiyoShapes(
    val small: Shape = RoundedCornerShape(PiyoTokens.Radius.Sm),
    val medium: Shape = RoundedCornerShape(PiyoTokens.Radius.Md),
    val large: Shape = RoundedCornerShape(PiyoTokens.Radius.Lg),
    val pill: Shape = RoundedCornerShape(50),
    val circle: Shape = CircleShape,
)

internal val LocalPiyoColors = staticCompositionLocalOf { piyoColorRoles(PiyoPreset.Ocean.seed) }
internal val LocalPiyoShapes = staticCompositionLocalOf { PiyoShapes() }
internal val LocalPiyoStyle = staticCompositionLocalOf<PiyoStyle> { PiyoStyle.Glass }

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
}

// シード色からテーマを作る。reduceMotion の既定は端末の「アニメーションを削除」
@Composable
fun PiyoTheme(
    seed: Color = PiyoPreset.Ocean.seed,
    style: PiyoStyle = PiyoStyle.Glass,
    shapes: PiyoShapes = PiyoShapes(),
    reduceMotion: Boolean = systemReduceMotion(),
    content: @Composable () -> Unit,
) {
    val colors = remember(seed) { piyoColorRoles(seed) }
    PiyoTheme(colors = colors, style = style, shapes = shapes, reduceMotion = reduceMotion, content = content)
}

// プリセットからテーマを作る
@Composable
fun PiyoTheme(
    preset: PiyoPreset,
    style: PiyoStyle = PiyoStyle.Glass,
    shapes: PiyoShapes = PiyoShapes(),
    reduceMotion: Boolean = systemReduceMotion(),
    content: @Composable () -> Unit,
) {
    val colors = remember(preset) { preset.roles() }
    PiyoTheme(colors = colors, style = style, shapes = shapes, reduceMotion = reduceMotion, content = content)
}

// 作った色の組からテーマを作る
@Composable
fun PiyoTheme(
    colors: PiyoColorRoles,
    style: PiyoStyle = PiyoStyle.Glass,
    shapes: PiyoShapes = PiyoShapes(),
    reduceMotion: Boolean = systemReduceMotion(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPiyoReduceMotion provides reduceMotion,
        LocalPiyoColors provides colors,
        LocalPiyoShapes provides shapes,
        LocalPiyoStyle provides style,
        LocalPiyoContentColor provides colors.contentDefault,
        LocalPiyoTextStyle provides TextStyle(fontSize = PiyoTokens.Text.Size, fontWeight = PiyoTokens.Text.Weight),
    ) { PiyoOverlayHost(content) }
}
