package piyoron.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import piyoron.core.LocalPiyoContentColor
import piyoron.core.Piyo
import piyoron.core.PiyoState
import piyoron.core.PiyoTone
import piyoron.core.SurfaceRole
import piyoron.core.disabledAlpha
import piyoron.core.focusRing
import piyoron.core.piyoAnimateSize
import piyoron.core.piyoSurface
import piyoron.core.piyoPress
import piyoron.core.rememberPiyoState
import piyoron.effects.genie.reportGenieAnchor
import piyoron.tokens.PiyoTokens

// カードの既定値
object PiyoCardDefaults {
    val ContentPadding = PaddingValues(horizontal = PiyoTokens.Card.PaddingX, vertical = PiyoTokens.Card.PaddingY)
}

// カード(中身を縦に並べる面)。tone が Primary なら主な色で塗ったカード。中身で大きさが変わるとなめらかに変わる(animateSize で外せる)
@Composable
fun PiyoCard(
    modifier: Modifier = Modifier,
    shape: Shape = Piyo.shapes.card,
    tone: PiyoTone = PiyoTone.Default,
    animateSize: Boolean = true,
    contentPadding: PaddingValues = PiyoCardDefaults.ContentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardFrame(modifier, shape, PiyoState(), SurfaceRole.Card, tone, animateSize, contentPadding, content)
}

// 押せるカード(押下の動きはボタンと同じ。onLongClick で長押しも受ける)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PiyoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    shape: Shape = Piyo.shapes.card,
    tone: PiyoTone = PiyoTone.Default,
    animateSize: Boolean = true,
    contentPadding: PaddingValues = PiyoCardDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    CardFrame(
        modifier
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .disabledAlpha(enabled)
            .piyoPress(source, enabled)
            .focusRing(state.focused, shape),
        shape,
        state,
        SurfaceRole.ClickableCard,
        tone,
        animateSize,
        contentPadding,
        content,
    )
}

// カードの共通部分(面、文字色、余白)
@Composable
private fun CardFrame(
    modifier: Modifier,
    shape: Shape,
    state: PiyoState,
    role: SurfaceRole,
    tone: PiyoTone,
    animateSize: Boolean,
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = Piyo.style.contentColor(role, state, tone)
    CompositionLocalProvider(LocalPiyoContentColor provides color) {
        Column(
            modifier = modifier
                .reportGenieAnchor(shape)
                .piyoSurface(shape, role, state, tone)
                .piyoAnimateSize(animateSize)
                .padding(contentPadding),
            content = content,
        )
    }
}
