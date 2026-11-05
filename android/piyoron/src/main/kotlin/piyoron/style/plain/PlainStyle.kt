package piyoron.style.plain

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

// 飾りのない平らなスタイル(役割の色で塗るだけ。浮くメニューだけ細い枠)
internal object PlainStyle : PiyoStyle {

    // role ごとに平らな面を描く
    @Composable
    override fun Modifier.surface(shape: Shape, role: SurfaceRole, state: PiyoState, tone: PiyoTone, fill: Color?): Modifier {
        val roles = Piyo.colors
        val duration = if (Piyo.reduceMotion) 0 else if (role == SurfaceRole.MenuItem) PiyoTokens.Menu.ItemDuration else PiyoTokens.Duration.Toggle
        val color = animateColorAsState(fill ?: plainFill(roles, role, state, tone), tween(duration), label = "plain").value
        val edge = if (role == SurfaceRole.Menu) roles.ringShadow else null
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
    SurfaceRole.Button, SurfaceRole.Field -> when (tone) {
        PiyoTone.Default -> roles.fillSelectedBottom
        PiyoTone.Primary -> if (state.hovered) roles.primaryStrong else roles.primary
        PiyoTone.Danger -> if (state.hovered) roles.dangerStrong else roles.danger
    }
    SurfaceRole.Chip -> if (state.selected) roles.fillSelectedBottom else roles.background
    SurfaceRole.TabTrack, SurfaceRole.Card, SurfaceRole.Divider -> roles.background
    SurfaceRole.TabThumb, SurfaceRole.Menu -> roles.surface
    SurfaceRole.MenuItem -> {
        val active = state.enabled && (state.hovered || state.focused || state.pressed)
        if (active) roles.fillSelectedBottom else roles.fillSelectedBottom.copy(alpha = 0f)
    }
}
