package piyoron.components

import androidx.compose.foundation.clickable
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
import piyoron.core.piyoSurface
import piyoron.core.pressMotion
import piyoron.core.rememberPiyoState
import piyoron.effects.genie.reportGenieAnchor
import piyoron.tokens.PiyoTokens

// カードの既定値
object PiyoCardDefaults {
    val ContentPadding = PaddingValues(horizontal = PiyoTokens.Card.PaddingX, vertical = PiyoTokens.Card.PaddingY)
}

// カード(中身を縦に並べる面)
@Composable
fun PiyoCard(
    modifier: Modifier = Modifier,
    shape: Shape = Piyo.shapes.large,
    contentPadding: PaddingValues = PiyoCardDefaults.ContentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardFrame(modifier, shape, PiyoState(), contentPadding, content)
}

// 押せるカード(押下の動きはボタンと同じ)
@Composable
fun PiyoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = Piyo.shapes.large,
    contentPadding: PaddingValues = PiyoCardDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    CardFrame(
        modifier
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .disabledAlpha(enabled)
            .pressMotion(source, state)
            .focusRing(state.focused, shape),
        shape,
        state,
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
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = Piyo.style.contentColor(SurfaceRole.Card, state, PiyoTone.Default)
    CompositionLocalProvider(LocalPiyoContentColor provides color) {
        Column(
            modifier = modifier
                .reportGenieAnchor(shape)
                .piyoSurface(shape, SurfaceRole.Card, state)
                .padding(contentPadding),
            content = content,
        )
    }
}
