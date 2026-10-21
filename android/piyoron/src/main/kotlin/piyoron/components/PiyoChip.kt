package piyoron.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import piyoron.core.LocalPiyoContentColor
import piyoron.core.LocalPiyoTextStyle
import piyoron.core.Piyo
import piyoron.core.PiyoIcon
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

// 選び方(1つだけ選ぶか、いくつでも選ぶか)
enum class PiyoChipMode { Single, Multiple }

// 選ぶボタン
@Composable
fun PiyoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    mode: PiyoChipMode = PiyoChipMode.Multiple,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, selected = selected, enabled = enabled)
    val shape = Piyo.shapes.pill
    val color = Piyo.style.contentColor(SurfaceRole.Chip, state, PiyoTone.Default)
    val select = when (mode) {
        PiyoChipMode.Single -> Modifier.selectable(selected, source, null, enabled, Role.RadioButton, onClick)
        PiyoChipMode.Multiple -> Modifier.toggleable(selected, source, null, enabled, Role.Checkbox) { onClick() }
    }
    Box(
        modifier = modifier
            .then(select)
            .touchTarget()
            .reportGenieAnchor(shape)
            .disabledAlpha(enabled)
            .pressMotion(source, state)
            .focusRing(state.focused, shape)
            .piyoSurface(shape, SurfaceRole.Chip, state),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalPiyoContentColor provides color,
            LocalPiyoTextStyle provides LocalPiyoTextStyle.current.copy(fontSize = PiyoTokens.Text.SizeSmall),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = PiyoTokens.Chip.PaddingX, vertical = PiyoTokens.Chip.PaddingY),
                horizontalArrangement = Arrangement.spacedBy(PiyoTokens.Control.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) PiyoIcon(icon, contentDescription = null, size = PiyoTokens.Control.IconSize)
                PiyoText(text, maxLines = 1)
            }
        }
    }
}

// 1つだけ選ぶ、選ぶボタンの並び
@Composable
fun <T> PiyoChipGroup(
    items: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (T) -> String = { it.toString() },
) {
    ChipFlow(modifier) {
        items.forEach { item ->
            PiyoChip(label(item), item == selected, { onSelect(item) }, enabled = enabled, mode = PiyoChipMode.Single)
        }
    }
}

// いくつでも選べる、選ぶボタンの並び
@Composable
fun <T> PiyoMultiChipGroup(
    items: List<T>,
    selected: Set<T>,
    onSelectedChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (T) -> String = { it.toString() },
) {
    ChipFlow(modifier) {
        items.forEach { item ->
            val on = item in selected
            PiyoChip(label(item), on, { onSelectedChange(if (on) selected - item else selected + item) }, enabled = enabled)
        }
    }
}

// 折り返して並べる
@Composable
private fun ChipFlow(modifier: Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(PiyoTokens.Chip.Gap),
        verticalArrangement = Arrangement.Center,
    ) { content() }
}
