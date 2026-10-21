// 自動生成。手で書き換えない。元は tokens/(npm run tokens で作り直す)
package piyoron.tokens

import androidx.compose.ui.graphics.Color
import piyoron.core.color.ChannelOp
import piyoron.core.color.EdgeStop
import piyoron.core.color.HueSpread
import piyoron.core.color.Rgba
import piyoron.core.color.edgeStops
import piyoron.core.color.ensureContrast
import piyoron.core.color.mix
import piyoron.core.color.over

// Ocean に合わせた色相のずらし幅を、シードの色相が離れるほど小さくする
private val hueSpread = HueSpread(234.6, 15.0, 60.0, 0.3)

// 色の役割(全スタイル共通の語彙)
data class PiyoColorRoles(
    // 唯一の入力
    val seed: Color,
    val primary: Color,
    val primaryStrong: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val background: Color,
    val fillTop: Color,
    val fillBottom: Color,
    val fillSelectedTop: Color,
    val fillSelectedBottom: Color,
    val ringShadow: Color,
    val innerGlow: Color,
    val innerHighlight: Color,
    val contentDefault: Color,
    val contentMuted: Color,
    val contentSelected: Color,
    val contentOnPrimary: Color,
    val danger: Color,
    val dangerStrong: Color,
    val ok: Color,
    val warn: Color,
    // 枠のスイープグラデーション。h はシードからの色相差、c はシードの彩度に掛ける比。位置は 0〜1(CSS の conic-gradient と同じく上が 0)
    val edgeStops: List<Pair<Float, Color>>,
    // 色付きの枠(選んだもの、主な操作)にも枠の色の並びを使うとき、その並び
    val toneEdge: List<Pair<Float, Color>>? = null,
)

// シード色から全roleを作る。danger / ok / warn は任意で上書きできる
fun piyoColorRoles(seed: Color, danger: Color? = null, ok: Color? = null, warn: Color? = null): PiyoColorRoles =
    generateRoles(seed, listOfNotNull(danger?.let { "danger" to it }, ok?.let { "ok" to it }, warn?.let { "warn" to it }).toMap())

// シード色と、role 名ごとの上書き、手で決めた枠の色から全roleを作る
internal fun generateRoles(
    seed: Color,
    overrides: Map<String, Color> = emptyMap(),
    edge: List<Pair<Float, Color>>? = null,
    toneUsesEdge: Boolean = false,
): PiyoColorRoles {
    val o = overrides.mapValues { Rgba.of(it.value) }
    val seedRole = o["seed"] ?: Rgba.of(seed).quantize()
    val primary = o["primary"] ?: seedRole.quantize()
    val primaryStrong = o["primaryStrong"] ?: seedRole.adjustOklch(l = ChannelOp(add = -0.0623), c = ChannelOp(scale = 1.0907), h = ChannelOp(add = 11.5, spread = true), spread = hueSpread).quantize()
    val surface = o["surface"] ?: Rgba.argb(0xFFFFFFFF).quantize()
    val surfaceMuted = o["surfaceMuted"] ?: seedRole.adjustOklch(l = ChannelOp(set = 0.9801), c = ChannelOp(scale = 0.0283), h = ChannelOp(add = -15.07, spread = true), spread = hueSpread).quantize()
    val background = o["background"] ?: seedRole.adjustOklch(l = ChannelOp(set = 0.9677), c = ChannelOp(scale = 0.0412), h = ChannelOp(add = -5.79, spread = true), spread = hueSpread).quantize()
    val fillTop = o["fillTop"] ?: surface.alpha(0.62).quantize()
    val fillBottom = o["fillBottom"] ?: mix(primary, surface, 0.12).alpha(0.5).quantize()
    val fillSelectedBottom = o["fillSelectedBottom"] ?: primary.alpha(0.1).quantize()
    val fillSelectedTop = o["fillSelectedTop"] ?: over(surface.alpha(0.5), fillSelectedBottom).quantize()
    val contentDefault = o["contentDefault"] ?: ensureContrast(seedRole.adjustOklch(l = ChannelOp(set = 0.2607), c = ChannelOp(scale = 0.2455), h = ChannelOp(add = 30.41, spread = true), spread = hueSpread).quantize(), over(fillBottom, surface).quantize(), 4.5).quantize()
    val ringShadow = o["ringShadow"] ?: mix(primaryStrong, contentDefault, 0.45).alpha(0.38).quantize()
    val innerGlow = o["innerGlow"] ?: primary.alpha(0.14).quantize()
    val innerHighlight = o["innerHighlight"] ?: surface.alpha(0.7).quantize()
    val contentMuted = o["contentMuted"] ?: ensureContrast(seedRole.adjustOklch(l = ChannelOp(set = 0.5578), c = ChannelOp(scale = 0.2446), h = ChannelOp(add = 29.69, spread = true), spread = hueSpread).quantize(), over(fillBottom, surface).quantize(), 4.5).quantize()
    val contentSelected = o["contentSelected"] ?: ensureContrast(primaryStrong.quantize(), over(fillSelectedBottom, surface).quantize(), 4.5).quantize()
    val contentOnPrimary = o["contentOnPrimary"] ?: Rgba.argb(0xFFFFFFFF).quantize()
    val danger = o["danger"] ?: Rgba.argb(0xFFE5484D).quantize()
    val dangerStrong = o["dangerStrong"] ?: danger.adjustOklch(l = ChannelOp(add = -0.0676), c = ChannelOp(scale = 0.9498), h = ChannelOp(add = 0.75)).quantize()
    val ok = o["ok"] ?: Rgba.argb(0xFF1E9E6A).quantize()
    val warn = o["warn"] ?: Rgba.argb(0xFFE8920C).quantize()
    val generated = edgeStops(seedRole, 0.1226, hueSpread, listOf(
        EdgeStop(0.0, 0.731, 0.851, 27.49),
        EdgeStop(35.0, 0.7642, 0.7305, 27.12),
        EdgeStop(70.0, 0.7908, 0.7648, 2.99),
        EdgeStop(100.0, 0.7845, 0.7183, 16.91),
        EdgeStop(125.0, 0.8242, 0.708, -46.38),
        EdgeStop(150.0, 0.8705, 0.5183, -38.56),
        EdgeStop(175.0, 0.7966, 0.7023, -49.36),
        EdgeStop(200.0, 0.721, 0.8202, 24.84),
        EdgeStop(230.0, 0.65, 1.0365, 24.88),
        EdgeStop(260.0, 0.6792, 0.9758, 30.01),
        EdgeStop(290.0, 0.767, 0.7561, 21.2),
        EdgeStop(315.0, 0.8442, 0.5797, -7.9),
        EdgeStop(340.0, 0.728, 0.8687, 25.06),
    ))
    val stops = edge?.let { it + (1f to it.first().second) } ?: generated.map { (it.first / 360).toFloat() to it.second.toColor() }
    return PiyoColorRoles(
        seed = seedRole.toColor(),
        primary = primary.toColor(),
        primaryStrong = primaryStrong.toColor(),
        surface = surface.toColor(),
        surfaceMuted = surfaceMuted.toColor(),
        background = background.toColor(),
        fillTop = fillTop.toColor(),
        fillBottom = fillBottom.toColor(),
        fillSelectedTop = fillSelectedTop.toColor(),
        fillSelectedBottom = fillSelectedBottom.toColor(),
        ringShadow = ringShadow.toColor(),
        innerGlow = innerGlow.toColor(),
        innerHighlight = innerHighlight.toColor(),
        contentDefault = contentDefault.toColor(),
        contentMuted = contentMuted.toColor(),
        contentSelected = contentSelected.toColor(),
        contentOnPrimary = contentOnPrimary.toColor(),
        danger = danger.toColor(),
        dangerStrong = dangerStrong.toColor(),
        ok = ok.toColor(),
        warn = warn.toColor(),
        edgeStops = stops,
        toneEdge = if (toneUsesEdge) stops else null,
    )
}
