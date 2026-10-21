package piyoron.style.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import piyoron.core.Piyo
import piyoron.core.PiyoGradient
import piyoron.core.PiyoShadow
import piyoron.core.PiyoState
import piyoron.core.PiyoStyle
import piyoron.core.PiyoTone
import piyoron.core.SurfaceRole
import piyoron.core.blendStops
import piyoron.core.PiyoChangingShape
import piyoron.core.blurredRing
import piyoron.core.innerShadowApprox
import piyoron.core.strokeConicInside
import piyoron.core.mixSrgb
import piyoron.core.outerShadow
import piyoron.core.strokeInside
import piyoron.core.toBrush
import piyoron.core.toShadow
import piyoron.tokens.GlassColors
import piyoron.tokens.GlassTokens
import piyoron.tokens.PiyoColorRoles
import piyoron.tokens.PiyoTokens
import piyoron.tokens.glassColors

// ガラス風のスタイル(塗り + スイープグラデーションの枠 + 線状の影)
internal object GlassStyle : PiyoStyle {

    // role ごとにガラスの面を組み立てる
    @Composable
    override fun Modifier.surface(shape: Shape, role: SurfaceRole, state: PiyoState, tone: PiyoTone): Modifier {
        val roles = Piyo.colors
        val glass = remember(roles) { glassColors(roles) }
        val look = when (role) {
            SurfaceRole.Button, SurfaceRole.Field -> buttonLook(roles, glass, state, tone)
            SurfaceRole.Chip -> chipLook(roles, glass, state)
            SurfaceRole.TabTrack -> trackLook(roles, glass)
            SurfaceRole.TabThumb -> thumbLook(roles, glass)
            SurfaceRole.Card -> cardLook(roles, glass)
        }
        return glassLayers(shape, look)
    }

    // 面の上の文字・アイコンの色
    @Composable
    override fun contentColor(role: SurfaceRole, state: PiyoState, tone: PiyoTone): Color {
        val roles = Piyo.colors
        val target = when (role) {
            SurfaceRole.Button, SurfaceRole.Field -> if (tone == PiyoTone.Default) roles.contentSelected else roles.contentOnPrimary
            SurfaceRole.Chip -> when {
                state.selected -> roles.contentSelected
                state.hovered -> roles.primary
                else -> roles.contentMuted
            }
            SurfaceRole.TabTrack, SurfaceRole.TabThumb -> if (state.selected) roles.contentSelected else roles.contentMuted
            SurfaceRole.Card -> roles.contentDefault
        }
        val duration = when {
            Piyo.reduceMotion -> 0
            role == SurfaceRole.TabTrack || role == SurfaceRole.TabThumb -> PiyoTokens.Duration.Segmented
            else -> PiyoTokens.Duration.Toggle
        }
        return animateColorAsState(target, tween(duration), label = "content").value
    }
}

// 線状の影
private class Ring(val color: Color, val width: Dp, val blur: Dp, val offset: DpOffset)

// 面の描き方一式
private class GlassLook(
    val fill: DrawScope.(Shape) -> Unit,
    val shadows: List<PiyoShadow>,
    val ring: Ring?,
    val edge: (DrawScope.(Shape) -> Unit)?,
    val ringOnTop: Boolean = false,
)

// 外の影、塗り、内側の影、線状の影、枠の順に重ねる(外の影は内側に透けないよう自前で描く)
private fun Modifier.glassLayers(shape: Shape, look: GlassLook): Modifier {
    var m = this
    m = m.drawBehind {
        look.shadows.filter { !it.inset }.forEach { outerShadow(shape, it) }
        look.fill(this, shape)
    }
    val insets = look.shadows.filter { it.inset }
    val changing = shape is PiyoChangingShape
    if (!changing) insets.forEach { m = m.innerShadow(shape, it.toShadow()) }
    return m.drawBehind {
        if (changing) insets.forEach { innerShadowApprox(shape, it) }
        if (!look.ringOnTop) look.ring?.let { blurredRing(shape, it.width, it.blur, it.offset, it.color) }
        look.edge?.invoke(this, shape)
        if (look.ringOnTop) look.ring?.let { blurredRing(shape, it.width, it.blur, it.offset, it.color) }
    }
}

// 形をブラシで塗る
private fun DrawScope.fillShape(shape: Shape, brush: Brush) {
    drawOutline(shape.createOutline(size, layoutDirection, this), brush)
}

// 上から下へのグラデーションで塗る
private fun verticalFill(top: Color, bottom: Color): DrawScope.(Shape) -> Unit =
    { shape -> fillShape(shape, Brush.verticalGradient(listOf(top, bottom))) }

// 角度つきのグラデーションで塗る
private fun gradientFill(gradient: PiyoGradient): DrawScope.(Shape) -> Unit =
    { shape -> fillShape(shape, gradient.toBrush(size)) }

// CSS の conic-gradient と同じ並びの枠
private fun sweepEdge(stops: List<Pair<Float, Color>>, alpha: Float = 1f): DrawScope.(Shape) -> Unit =
    { shape -> strokeConicInside(shape, GlassTokens.Edge.Width.toPx(), stops, alpha) }

// 角度つきのグラデーションの枠
private fun linearEdge(gradient: PiyoGradient): DrawScope.(Shape) -> Unit =
    { shape -> strokeInside(shape, GlassTokens.Edge.Width.toPx(), gradient.toBrush(size)) }

// 標準の線状の影
private fun defaultRing(roles: PiyoColorRoles) = Ring(
    roles.ringShadow,
    GlassTokens.Ring.Width,
    GlassTokens.Ring.Blur,
    DpOffset(GlassTokens.Ring.OffsetX, GlassTokens.Ring.OffsetY),
)

// ボタン(tone で塗りと枠を染める)
private fun buttonLook(roles: PiyoColorRoles, glass: GlassColors, state: PiyoState, tone: PiyoTone): GlassLook {
    val hover = state.hovered
    val shadows = if (hover) glass.shadowHover else glass.shadowIdle
    val ring = defaultRing(roles)
    return when (tone) {
        PiyoTone.Default -> GlassLook(
            fill = if (hover) verticalFill(glass.fillHoverTop, glass.fillHoverBottom) else verticalFill(roles.fillTop, roles.fillBottom),
            shadows = shadows,
            ring = ring,
            edge = sweepEdge(roles.edgeStops),
        )
        PiyoTone.Primary -> GlassLook(
            fill = solidFill(if (hover) glass.tonePrimaryFillHover else glass.tonePrimaryFill),
            shadows = shadows,
            ring = ring,
            edge = sweepEdge(glass.toneEdgePrimary),
        )
        PiyoTone.Danger -> GlassLook(
            fill = solidFill(if (hover) glass.toneDangerFillHover else glass.toneDangerFill),
            shadows = shadows,
            ring = ring,
            edge = sweepEdge(glass.toneEdgeDanger),
        )
    }
}

// 1色で塗る
private fun solidFill(color: Color): DrawScope.(Shape) -> Unit = { shape -> fillShape(shape, SolidColor(color)) }

// 選ぶボタン(未選択は淡く、選ぶと塗りが濃くなり線状の影が出る)
@Composable
private fun chipLook(roles: PiyoColorRoles, glass: GlassColors, state: PiyoState): GlassLook {
    val spec = tween<Float>(if (Piyo.reduceMotion) 0 else PiyoTokens.Duration.Toggle)
    val tintTarget = when {
        state.selected -> roles.fillSelectedBottom.alpha
        state.hovered -> GlassTokens.Chip.TintHover
        else -> GlassTokens.Chip.TintRest
    }
    val edgeTarget = if (state.hovered && !state.selected) GlassTokens.Chip.EdgeHover else GlassTokens.Chip.EdgeRest
    val tint by animateFloatAsState(tintTarget, spec, label = "tint")
    val edgeMix by animateFloatAsState(edgeTarget, spec, label = "edge")
    val primary = roles.primary
    val fill: DrawScope.(Shape) -> Unit = { shape ->
        fillShape(shape, SolidColor(primary.copy(alpha = tint)))
        fillShape(shape, Brush.verticalGradient(listOf(glass.chipGlossTop, glass.chipGlossBottom)))
    }
    if (state.selected) {
        return GlassLook(
            fill = fill,
            shadows = glass.shadowIdle,
            ring = Ring(
                roles.ringShadow,
                GlassTokens.Chip.RingWidth,
                GlassTokens.Chip.RingBlur,
                DpOffset(GlassTokens.Chip.RingOffsetX, GlassTokens.Chip.RingOffsetY),
            ),
            edge = sweepEdge(glass.toneEdgePrimary),
        )
    }
    val side = mixSrgb(primary, roles.surface, edgeMix * GlassTokens.Chip.EdgeSideRatio)
    val middle = mixSrgb(primary, roles.surface, edgeMix)
    val edge = PiyoGradient(GlassTokens.Chip.EdgeAngle, listOf(0f to side, 0.5f to middle, 1f to side))
    return GlassLook(
        fill = fill,
        shadows = if (state.hovered) glass.chipShadowHover else glass.chipShadowRest,
        ring = null,
        edge = linearEdge(edge),
    )
}

// タブの土台(極薄。枠はボタンと同じ並びを薄く)
private fun trackLook(roles: PiyoColorRoles, glass: GlassColors) = GlassLook(
    fill = gradientFill(glass.tabsTrackFill),
    shadows = emptyList(),
    ring = null,
    edge = sweepEdge(roles.edgeStops, GlassTokens.Tabs.TrackEdgeOpacity),
)

// タブのつまみ(立体的な内側影 + 線状の影。塗りと枠は通常ボタンと選んだチップの中間)
private fun thumbLook(roles: PiyoColorRoles, glass: GlassColors) = GlassLook(
    fill = gradientFill(glass.tabsThumbFill),
    shadows = glass.tabsThumbShadow,
    ring = Ring(
        glass.tabsRingColor,
        GlassTokens.Ring.Width,
        GlassTokens.Tabs.RingBlur,
        DpOffset(GlassTokens.Tabs.RingOffsetX, GlassTokens.Tabs.RingOffsetY),
    ),
    edge = sweepEdge(blendStops(roles.edgeStops, glass.toneEdgePrimary, GlassTokens.Tabs.ThumbEdgeBlend)),
    ringOnTop = true,
)

// カード(斜めのグラデーション、薄い枠)
private fun cardLook(roles: PiyoColorRoles, glass: GlassColors) = GlassLook(
    fill = gradientFill(glass.cardFill),
    shadows = emptyList(),
    ring = Ring(
        roles.ringShadow,
        GlassTokens.Card.RingWidth,
        GlassTokens.Card.RingBlur,
        DpOffset(GlassTokens.Card.RingOffsetX, GlassTokens.Card.RingOffsetY),
    ),
    edge = sweepEdge(roles.edgeStops, GlassTokens.Card.EdgeOpacity),
)
