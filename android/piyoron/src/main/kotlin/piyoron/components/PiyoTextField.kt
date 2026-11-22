package piyoron.components

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

// 入力欄(文字列)。面は入力欄の役割(タグより濃い塗りで、少し凹んで見える)。フォーカス中は枠で示す。無効はほかの部品と同じく全体を薄くする
// leading は左(検索の印など)、trailing は右(単位、消すボタンなど)に置く。singleLine が false なら minHeight まで縦に広がる
@Composable
fun PiyoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minHeight: Dp = PiyoTextFieldDefaults.MinHeight,
    textStyle: TextStyle = LocalPiyoTextStyle.current,
    textAlign: TextAlign = TextAlign.Start,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    val style = textStyle.copy(color = Piyo.colors.contentDefault, textAlign = textAlign)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = source,
        cursorBrush = SolidColor(Piyo.colors.primary),
        decorationBox = { inner -> FieldFrame(state, value.isEmpty(), placeholder, style, minHeight, singleLine, leading, trailing, inner) },
    )
}

// 入力欄(カーソルや選んだ範囲も扱う TextFieldValue)。見た目は文字列の入力欄と同じ
@Composable
fun PiyoTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minHeight: Dp = PiyoTextFieldDefaults.MinHeight,
    textStyle: TextStyle = LocalPiyoTextStyle.current,
    textAlign: TextAlign = TextAlign.Start,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    val style = textStyle.copy(color = Piyo.colors.contentDefault, textAlign = textAlign)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = source,
        cursorBrush = SolidColor(Piyo.colors.primary),
        decorationBox = { inner -> FieldFrame(state, value.text.isEmpty(), placeholder, style, minHeight, singleLine, leading, trailing, inner) },
    )
}

// 入力欄の面と中身(左の印、空のときの案内、入力、右の印)
@Composable
private fun FieldFrame(
    state: PiyoState,
    empty: Boolean,
    placeholder: String,
    style: TextStyle,
    minHeight: Dp,
    singleLine: Boolean,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    inner: @Composable () -> Unit,
) {
    Row(
        Modifier
            .alpha(if (state.enabled) 1f else PiyoTokens.Control.DisabledOpacity)
            .piyoSurface(Piyo.shapes.medium, SurfaceRole.Field, state, PiyoTone.Default)
            .heightIn(min = minHeight)
            .padding(horizontal = PiyoTextFieldDefaults.PaddingX, vertical = PiyoTextFieldDefaults.PaddingY),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalPiyoContentColor provides Piyo.colors.contentMuted) {
            leading?.invoke()
        }
        Box(Modifier.weight(1f), contentAlignment = if (style.textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart) {
            if (empty && placeholder.isNotEmpty()) PiyoText(placeholder, color = Piyo.colors.contentMuted.copy(alpha = PlaceholderAlpha), style = style)
            inner()
        }
        CompositionLocalProvider(LocalPiyoContentColor provides Piyo.colors.contentMuted) {
            trailing?.invoke()
        }
    }
}
