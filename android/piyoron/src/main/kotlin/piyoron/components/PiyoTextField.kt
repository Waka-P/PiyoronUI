package piyoron.components

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import piyoron.core.LocalPiyoContentColor
import piyoron.core.LocalPiyoTextStyle
import piyoron.core.Piyo
import piyoron.core.PiyoState
import piyoron.core.PiyoText
import piyoron.core.PiyoTone
import piyoron.core.SurfaceRole
import piyoron.core.piyoSurface
import piyoron.core.rememberPiyoState
import piyoron.tokens.PiyoTokens

// 空のときの案内の濃さ(控えめな文字色に、さらに掛ける)
private const val PlaceholderAlpha = 0.55f

// 入力欄の既定値
object PiyoTextFieldDefaults {
    val MinHeight = PiyoTokens.Touch.MinSize
    val PaddingX = 14.dp
    val PaddingY = 10.dp
}

// 入力欄(文字列)
@Composable
fun PiyoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minHeight: Dp = PiyoTextFieldDefaults.MinHeight,
    shape: Shape = Piyo.shapes.medium,
    textStyle: TextStyle = LocalPiyoTextStyle.current,
    textAlign: TextAlign = TextAlign.Start,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    drawBehindText: (DrawScope.(TextLayoutResult) -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    val style = textStyle.copy(color = textStyle.color.takeOrElse { Piyo.colors.contentDefault }, textAlign = textAlign)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val behind = drawBehindText?.let { draw -> { scope: DrawScope -> layout?.let { scope.draw(it) }; Unit } }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = source,
        cursorBrush = SolidColor(Piyo.colors.primary),
        onTextLayout = { layout = it },
        decorationBox = { inner -> FieldFrame(state, shape, value.isEmpty(), placeholder, style, minHeight, singleLine, leading, trailing, behind, inner) },
    )
}

// 入力欄(カーソルや選んだ範囲も扱う TextFieldValue)。drawBehindText で文字の後ろに描ける(文字の配置を受け取る)
@Composable
fun PiyoTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minHeight: Dp = PiyoTextFieldDefaults.MinHeight,
    shape: Shape = Piyo.shapes.medium,
    textStyle: TextStyle = LocalPiyoTextStyle.current,
    textAlign: TextAlign = TextAlign.Start,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    drawBehindText: (DrawScope.(TextLayoutResult) -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    val style = textStyle.copy(color = textStyle.color.takeOrElse { Piyo.colors.contentDefault }, textAlign = textAlign)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val behind = drawBehindText?.let { draw -> { scope: DrawScope -> layout?.let { scope.draw(it) }; Unit } }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = source,
        cursorBrush = SolidColor(Piyo.colors.primary),
        onTextLayout = { layout = it },
        decorationBox = { inner -> FieldFrame(state, shape, value.text.isEmpty(), placeholder, style, minHeight, singleLine, leading, trailing, behind, inner) },
    )
}

// 入力欄の面と中身(左の印、空のときの案内、入力、右の印)
@Composable
private fun FieldFrame(
    state: PiyoState,
    shape: Shape,
    empty: Boolean,
    placeholder: String,
    style: TextStyle,
    minHeight: Dp,
    singleLine: Boolean,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    behind: ((DrawScope) -> Unit)?,
    inner: @Composable () -> Unit,
) {
    Row(
        Modifier
            .alpha(if (state.enabled) 1f else PiyoTokens.Control.DisabledOpacity)
            .piyoSurface(shape, SurfaceRole.Field, state, PiyoTone.Default)
            .heightIn(min = minHeight)
            .padding(horizontal = PiyoTextFieldDefaults.PaddingX, vertical = PiyoTextFieldDefaults.PaddingY),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalPiyoContentColor provides Piyo.colors.contentMuted) {
            leading?.invoke()
        }
        val align = when (style.textAlign) {
            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }
        Box(Modifier.weight(1f), contentAlignment = align) {
            if (empty && placeholder.isNotEmpty()) {
                PiyoText(
                    placeholder,
                    Modifier.fillMaxWidth(),
                    color = Piyo.colors.contentMuted.copy(alpha = PlaceholderAlpha),
                    style = style,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                )
            }
            // 文字の後ろに描くものがあれば、入力の文字と同じ位置で描く
            Box(if (behind != null) Modifier.drawBehind { behind(this) } else Modifier) { inner() }
        }
        CompositionLocalProvider(LocalPiyoContentColor provides Piyo.colors.contentMuted) {
            trailing?.invoke()
        }
    }
}
