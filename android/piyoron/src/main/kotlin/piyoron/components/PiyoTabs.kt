package piyoron.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import piyoron.core.LocalPiyoContentColor
import piyoron.core.LocalPiyoTextStyle
import piyoron.core.Piyo
import piyoron.core.PiyoState
import piyoron.core.PiyoText
import piyoron.core.PiyoTone
import piyoron.core.SurfaceRole
import piyoron.core.disabledAlpha
import piyoron.core.focusRing
import piyoron.core.piyoSurface
import piyoron.core.rememberIndicatorMotion
import piyoron.core.rememberPiyoState
import piyoron.effects.genie.reportGenieAnchor
import piyoron.tokens.PiyoTokens

// タブの既定値
object PiyoTabsDefaults {

    // 選んだ項目のつまみ(現在のスタイルの面)
    @Composable
    fun Indicator() {
        Box(Modifier.fillMaxSize().piyoSurface(Piyo.shapes.pill, SurfaceRole.TabThumb, PiyoState(selected = true)))
    }
}

// 切り替えのタブ。選んだ項目へつまみが滑る
@Composable
fun PiyoTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indicator: @Composable () -> Unit = { PiyoTabsDefaults.Indicator() },
) {
    val count = tabs.size.coerceAtLeast(1)
    val motion = rememberIndicatorMotion(selectedIndex, PiyoTokens.Duration.Segmented, PiyoTokens.Easing.Standard)
    val inset = PiyoTokens.Tabs.TrackPadding + PiyoTokens.Tabs.TrackBorder
    Box(
        modifier = modifier
            .reportGenieAnchor(Piyo.shapes.pill)
            .piyoSurface(Piyo.shapes.pill, SurfaceRole.TabTrack)
            .disabledAlpha(enabled)
            .padding(inset)
            .selectableGroup(),
    ) {
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(1f / count)
                    .graphicsLayer { translationX = motion.position * size.width },
            ) { indicator() }
        }
        Row {
            tabs.forEachIndexed { i, label ->
                TabItem(label, i == selectedIndex, enabled, { onSelect(i) }, Modifier.weight(1f))
            }
        }
    }
}

// タブの1項目
@Composable
private fun TabItem(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, selected = selected, enabled = enabled)
    val color = Piyo.style.contentColor(SurfaceRole.TabTrack, state, PiyoTone.Default)
    val minHeight = PiyoTokens.Touch.MinSize - (PiyoTokens.Tabs.TrackPadding + PiyoTokens.Tabs.TrackBorder) * 2
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .focusRing(state.focused, Piyo.shapes.pill)
            .selectable(selected, source, null, enabled, Role.Tab, onClick)
            .padding(horizontal = PiyoTokens.Tabs.ItemPaddingX, vertical = PiyoTokens.Tabs.ItemPaddingY),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalPiyoContentColor provides color) {
            PiyoText(label, style = LocalPiyoTextStyle.current.copy(textAlign = TextAlign.Center), maxLines = 1)
        }
    }
}
