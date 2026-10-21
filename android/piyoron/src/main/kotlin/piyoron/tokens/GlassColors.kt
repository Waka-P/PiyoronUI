// 自動生成。手で書き換えない。元は tokens/(npm run tokens で作り直す)
package piyoron.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import piyoron.core.PiyoGradient
import piyoron.core.PiyoShadow
import piyoron.core.color.Rgba
import piyoron.core.color.mix

// Glass スタイルの、色によって変わる値
data class GlassColors(
    val fillHoverTop: Color,
    val fillHoverBottom: Color,
    val shadowIdle: List<PiyoShadow>,
    val shadowHover: List<PiyoShadow>,
    val tonePrimaryFill: Color,
    val tonePrimaryFillHover: Color,
    val toneDangerFill: Color,
    val toneDangerFillHover: Color,
    val toneEdgePrimary: List<Pair<Float, Color>>,
    val toneEdgeDanger: List<Pair<Float, Color>>,
    val menuFill: Color,
    val menuEdge: Color,
    val menuShadow: List<PiyoShadow>,
    val menuItemHover: Color,
    val menuItemDangerHover: Color,
    val menuDivider: Color,
    val chipGlossTop: Color,
    val chipGlossBottom: Color,
    val chipShadowRest: List<PiyoShadow>,
    val chipShadowHover: List<PiyoShadow>,
    val tabsTrackFill: PiyoGradient,
    val tabsThumbFill: PiyoGradient,
    val tabsThumbShadow: List<PiyoShadow>,
    val tabsRingColor: Color,
    val cardFill: PiyoGradient,
)

// role から Glass の色・影・グラデーションを作る
fun glassColors(roles: PiyoColorRoles): GlassColors {
    val contentDefault = Rgba.of(roles.contentDefault)
    val contentMuted = Rgba.of(roles.contentMuted)
    val danger = Rgba.of(roles.danger)
    val dangerStrong = Rgba.of(roles.dangerStrong)
    val fillBottom = Rgba.of(roles.fillBottom)
    val fillSelectedBottom = Rgba.of(roles.fillSelectedBottom)
    val fillSelectedTop = Rgba.of(roles.fillSelectedTop)
    val fillTop = Rgba.of(roles.fillTop)
    val innerGlow = Rgba.of(roles.innerGlow)
    val innerHighlight = Rgba.of(roles.innerHighlight)
    val primary = Rgba.of(roles.primary)
    val primaryStrong = Rgba.of(roles.primaryStrong)
    val ringShadow = Rgba.of(roles.ringShadow)
    val surface = Rgba.of(roles.surface)
    return GlassColors(
        fillHoverTop = surface.alpha(0.82).toColor(),
        fillHoverBottom = mix(primary, surface, 0.09).alpha(0.66).toColor(),
        shadowIdle = listOf(PiyoShadow(innerGlow.toColor(), 0.dp, 0.dp, 6.dp, 0.dp, true), PiyoShadow(innerHighlight.toColor(), 4.dp, 4.dp, 8.dp, -5.dp, true)),
        shadowHover = listOf(PiyoShadow(primary.alpha(0.1).toColor(), 0.dp, 0.dp, 6.dp, 0.dp, true), PiyoShadow(surface.alpha(0.85).toColor(), 4.dp, 4.dp, 8.dp, -5.dp, true)),
        tonePrimaryFill = primary.alpha(0.85).toColor(),
        tonePrimaryFillHover = primaryStrong.alpha(0.85).toColor(),
        toneDangerFill = danger.alpha(0.92).toColor(),
        toneDangerFillHover = dangerStrong.alpha(0.94).toColor(),
        toneEdgePrimary = roles.toneEdge ?: listOf(0f to mix(primary, surface, 0.55).toColor(), 0.25f to mix(primary, surface, 0.85).toColor(), 0.4444444444444444f to mix(primary, surface, 1.0).toColor(), 0.6666666666666666f to mix(primary, surface, 0.6).toColor(), 0.8666666666666667f to mix(primary, surface, 0.3).toColor(), 1f to mix(primary, surface, 0.55).toColor()),
        toneEdgeDanger = listOf(0f to mix(danger, surface, 0.55).toColor(), 0.25f to mix(danger, surface, 0.85).toColor(), 0.4444444444444444f to mix(danger, surface, 1.0).toColor(), 0.6666666666666666f to mix(danger, surface, 0.6).toColor(), 0.8666666666666667f to mix(danger, surface, 0.3).toColor(), 1f to mix(danger, surface, 0.55).toColor()),
        menuFill = surface.alpha(0.94).toColor(),
        menuEdge = primary.alpha(0.16).toColor(),
        menuShadow = listOf(PiyoShadow(contentDefault.alpha(0.08).toColor(), 0.dp, 1.dp, 2.dp, 0.dp, false), PiyoShadow(primaryStrong.alpha(0.22).toColor(), 0.dp, 12.dp, 32.dp, -6.dp, false)),
        menuItemHover = mix(primary, surface, 0.09).toColor(),
        menuItemDangerHover = mix(danger, surface, 0.11).toColor(),
        menuDivider = mix(contentMuted, surface, 0.15).toColor(),
        chipGlossTop = surface.alpha(0.5).toColor(),
        chipGlossBottom = surface.alpha(0.0).toColor(),
        chipShadowRest = listOf(PiyoShadow(surface.alpha(0.9).toColor(), 0.dp, 0.5.dp, 0.dp, 0.dp, true), PiyoShadow(primaryStrong.alpha(0.1).toColor(), 0.dp, 0.5.dp, 0.5.dp, 0.dp, false)),
        chipShadowHover = listOf(PiyoShadow(surface.alpha(0.9).toColor(), 0.dp, 1.dp, 0.dp, 0.dp, true), PiyoShadow(primaryStrong.alpha(0.18).toColor(), 0.dp, 1.dp, 2.dp, 0.dp, false)),
        tabsTrackFill = PiyoGradient(180f, listOf(0f to mix(primary, surface, 0.01).toColor(), 1f to mix(primary, surface, 0.03).toColor())),
        tabsThumbFill = PiyoGradient(180f, listOf(0f to mix(fillTop, fillSelectedTop, 0.5).toColor(), 1f to mix(fillBottom, fillSelectedBottom, 0.5).toColor())),
        tabsThumbShadow = listOf(PiyoShadow(primary.alpha(0.12).toColor(), 0.dp, 1.dp, 3.dp, 0.dp, false), PiyoShadow(surface.alpha(0.95).toColor(), 0.dp, 2.dp, 4.dp, 0.dp, true), PiyoShadow(primary.alpha(0.07).toColor(), 0.dp, -2.dp, 5.dp, 0.dp, true)),
        tabsRingColor = ringShadow.alpha(0.6).toColor(),
        cardFill = PiyoGradient(160f, listOf(0f to surface.alpha(0.96).toColor(), 1f to mix(primary, surface, 0.05).alpha(0.9).toColor())),
    )
}
