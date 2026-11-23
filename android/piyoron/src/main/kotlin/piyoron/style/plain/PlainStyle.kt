package piyoron.style.plain

import piyoron.core.mixSrgb
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import piyoron.core.Piyo
import piyoron.core.PiyoState
import piyoron.core.PiyoStyle
import piyoron.core.PiyoTone
import piyoron.core.SurfaceRole
import piyoron.core.baseContentColor
import piyoron.core.strokeInside
import piyoron.tokens.PiyoColorRoles
import piyoron.tokens.PiyoTokens
import piyoron.tokens.PlainTokens

// 飾りのない平らなスタイル(役割の色で塗るだけ。浮くメニュー、押せるカード、入力欄だけ細い枠)
internal object PlainStyle : PiyoStyle {

    // role ごとに平らな面を描く
    @Composable
    override fun Modifier.surface(shape: Shape, role: SurfaceRole, state: PiyoState, tone: PiyoTone, fill: Color?): Modifier {
        val roles = Piyo.colors
        val duration = if (Piyo.reduceMotion) 0 else if (role == SurfaceRole.MenuItem) PiyoTokens.Menu.ItemDuration else PiyoTokens.Duration.Toggle
        val color = animateColorAsState(fill ?: plainFill(roles, role, state, tone), tween(duration), label = "plain").value
        val edge = when {
            role == SurfaceRole.Field && state.focused -> roles.primary
            role == SurfaceRole.Field || role == SurfaceRole.Menu || role == SurfaceRole.ClickableCard -> roles.ringShadow
            else -> null
        }
        return drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, color)
            if (edge != null) strokeInside(shape, PlainTokens.Edge.Width.toPx(), SolidColor(edge))
        }
    }

    // 面の上の文字・アイコンの色
    @Composable
    override fun contentColor(role: SurfaceRole, state: PiyoState, tone: PiyoTone): Color = baseContentColor(role, state, tone)
}

// role と状態ごとの塗りの色
private fun plainFill(roles: PiyoColorRoles, role: SurfaceRole, state: PiyoState, tone: PiyoTone): Color = when (role) {
    SurfaceRole.Field -> mixSrgb(roles.toneEdge?.takeIf { it.isNotEmpty() }?.let { it[it.size / 2].second } ?: roles.primary, roles.surface, if (state.focused) 0.1f else 0.2f)
    SurfaceRole.Button -> when (tone) {
        PiyoTone.Default -> roles.fillSelectedBottom
        PiyoTone.Primary -> roles.toneFill?.takeIf { it.isNotEmpty() }?.let { plainToneFill(it, state.hovered) } ?: if (state.hovered) roles.primaryStrong else roles.primary
        PiyoTone.Danger -> if (state.hovered) roles.dangerStrong else roles.danger
    }
    SurfaceRole.Chip -> if (state.selected) roles.fillSelectedBottom else roles.background
    SurfaceRole.Card, SurfaceRole.ClickableCard -> when (tone) {
        PiyoTone.Default -> roles.background
        PiyoTone.Primary -> roles.toneFill?.takeIf { it.isNotEmpty() }?.let { plainToneFill(it, hovered = false) } ?: roles.primary
        PiyoTone.Danger -> roles.danger
    }
    SurfaceRole.TabTrack, SurfaceRole.Divider -> roles.background
    SurfaceRole.TabThumb, SurfaceRole.Menu -> roles.surface
    SurfaceRole.MenuItem -> {
        val active = state.enabled && (state.hovered || state.focused || state.pressed)
        if (active) roles.fillSelectedBottom else roles.fillSelectedBottom.copy(alpha = 0f)
    }
}

// 主な操作の色の並びから、Plain の1色を決める(真ん中の色。マウスを乗せたら少し暗く)
internal fun plainToneFill(colors: List<Color>, hovered: Boolean): Color {
    val middle = colors[colors.size / 2]
    return if (hovered) mixSrgb(Color.Black, middle, PlainTokens.Tone.HoverDarken) else middle
}
