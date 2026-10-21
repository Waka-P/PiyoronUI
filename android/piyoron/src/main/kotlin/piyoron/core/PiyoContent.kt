package piyoron.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import piyoron.tokens.PiyoTokens

// 今の場所の文字・アイコンの色
val LocalPiyoContentColor = compositionLocalOf { Color.Unspecified }

// 今の場所の文字の書式
val LocalPiyoTextStyle = compositionLocalOf { TextStyle.Default }

// テーマの色と書式で文字を出す
@Composable
fun PiyoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPiyoContentColor.current,
    style: TextStyle = LocalPiyoTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = color)),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
    )
}

// テーマの色でアイコンを出す
@Composable
fun PiyoIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalPiyoContentColor.current,
    size: Dp = PiyoTokens.Control.IconSize,
) {
    val semantics = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = null,
        modifier = modifier.size(size).then(semantics),
        colorFilter = ColorFilter.tint(tint),
    )
}
