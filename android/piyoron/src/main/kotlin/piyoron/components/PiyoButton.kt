package piyoron.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import piyoron.core.LocalPiyoContentColor
import piyoron.core.Piyo
import piyoron.core.PiyoIcon
import piyoron.core.PiyoState
import piyoron.core.PiyoText
import piyoron.core.PiyoTone
import piyoron.core.SurfaceRole
import piyoron.core.disabledAlpha
import piyoron.core.focusRing
import piyoron.core.piyoSurface
import piyoron.core.pressMotion
import piyoron.core.rememberPiyoState
import piyoron.core.touchTarget
import piyoron.effects.genie.reportGenieAnchor
import piyoron.tokens.PiyoTokens

// ボタンの既定値
object PiyoButtonDefaults {
    val ContentPadding = PaddingValues(horizontal = PiyoTokens.Control.PaddingX)
    val Height = PiyoTokens.Control.Height
}

// ボタン(中身を自由に置く)
@Composable
fun PiyoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: PiyoTone = PiyoTone.Default,
    enabled: Boolean = true,
    shape: Shape = Piyo.shapes.pill,
    contentPadding: PaddingValues = PiyoButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    ButtonFrame(onClick, modifier, tone, enabled, shape, source, state) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = PiyoButtonDefaults.Height)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(PiyoTokens.Control.Gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// 文字(と任意のアイコン)のボタン
@Composable
fun PiyoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: PiyoTone = PiyoTone.Default,
    enabled: Boolean = true,
) {
    PiyoButton(onClick = onClick, modifier = modifier, tone = tone, enabled = enabled) {
        if (icon != null) PiyoIcon(icon, contentDescription = null)
        PiyoText(text, maxLines = 1)
    }
}

// 円形のアイコンボタン
@Composable
fun PiyoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: PiyoTone = PiyoTone.Default,
    enabled: Boolean = true,
    size: Dp = PiyoButtonDefaults.Height,
    iconSize: Dp = PiyoTokens.Control.IconSize,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    ButtonFrame(onClick, modifier, tone, enabled, Piyo.shapes.circle, source, state) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            PiyoIcon(icon, contentDescription, size = iconSize)
        }
    }
}

// ボタンの共通部分(触れる範囲、押下の動き、面、文字色)
@Composable
private fun ButtonFrame(
    onClick: () -> Unit,
    modifier: Modifier,
    tone: PiyoTone,
    enabled: Boolean,
    shape: Shape,
    source: MutableInteractionSource,
    state: PiyoState,
    content: @Composable () -> Unit,
) {
    val color = Piyo.style.contentColor(SurfaceRole.Button, state, tone)
    Box(
        modifier = modifier
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .touchTarget()
            .reportGenieAnchor(shape)
            .disabledAlpha(enabled)
            .pressMotion(source, state)
            .focusRing(state.focused, shape)
            .piyoSurface(shape, SurfaceRole.Button, state, tone),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalPiyoContentColor provides color, content = content)
    }
}
