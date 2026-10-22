package piyoron.effects.gourd

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import piyoron.core.IndicatorMotion
import piyoron.core.LocalPiyoContentColor
import piyoron.core.LocalPiyoReduceMotion
import piyoron.core.Piyo
import piyoron.core.PiyoChangingShape
import piyoron.core.PiyoIcon
import piyoron.core.PiyoState
import piyoron.core.PiyoTone
import piyoron.core.SurfaceRole
import piyoron.core.focusRing
import piyoron.core.lerp
import piyoron.core.piyoSurface
import piyoron.core.rememberIndicatorMotion
import piyoron.core.rememberPiyoState
import piyoron.effects.genie.GenieFrame
import piyoron.effects.genie.genie
import piyoron.effects.genie.reportGenieAnchor
import piyoron.tokens.PiyoTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ひょうたん型のタブの1項目
@Immutable
data class GourdTab(val icon: ImageVector, val label: String)

// ひょうたん型のタブの既定値
object GourdTabRailDefaults {

    // 選んだ項目の印(現在のスタイルの面)。shape は動きに合わせて変わる印の形
    @Composable
    fun Indicator(shape: Shape) {
        Box(Modifier.fillMaxSize().piyoSurface(shape, SurfaceRole.TabThumb, PiyoState(selected = true)))
    }
}

// 丸が n 個並んだひょうたんの形(縦か横に並ぶ)
@Immutable
data class GourdShape(val count: Int, val orientation: Orientation = Orientation.Vertical) : Shape {

    // 輪郭を作る
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(
            if (orientation == Orientation.Vertical) gourdPath(size.width, size.height, count) else gourdPathHorizontal(size.width, size.height, count),
        )
}

// ひょうたんの大きさ(縦なら幅 x 長さ、横なら長さ x 幅)
private fun railSize(n: Int, orientation: Orientation): DpSize =
    if (orientation == Orientation.Vertical) DpSize(RailWidth, railHeight(n)) else DpSize(railHeight(n), RailWidth)

// ひょうたんの幅
private val RailWidth: Dp get() = PiyoTokens.Gourd.ButtonSize + PiyoTokens.Gourd.Padding * 2

// ひょうたんの高さ
private fun railHeight(n: Int): Dp =
    PiyoTokens.Gourd.ButtonSize * n + PiyoTokens.Gourd.Gap * (n - 1).coerceAtLeast(0) + PiyoTokens.Gourd.Padding * 2

// i 番目のボタンの上端
private fun buttonTop(i: Float): Dp = PiyoTokens.Gourd.Padding + (PiyoTokens.Gourd.ButtonSize + PiyoTokens.Gourd.Gap) * i

// 印の形(レール上の上端、長さ、幅、くびれに合わせる強さ 0〜1。単位は px)
private data class Blob(val top: Float, val length: Float, val width: Float, val fit: Float)

// くびれに合わせ始めと終わりの時間の幅
private const val FIT_RAMP = 0.12f

// 伸び縮みしたときの幅の変わりやすさ(長さの比の何乗で幅を変えるか)
private const val STRETCH_POWER = 0.25f

// 0〜1 に収めた割合
private fun part(t: Float, from: Float, to: Float): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

// 前の端の進み(0〜1)。初動はゆっくり
private fun frontAt(t: Float): Float = PiyoTokens.Gourd.SwitchEasing.transform(part(t, 0f, 1 - PiyoTokens.Gourd.Trail))

// 後ろの端の進み(0〜1)。前より遅れて同じ動きをする
private fun backAt(t: Float): Float = PiyoTokens.Gourd.SwitchEasing.transform(part(t, PiyoTokens.Gourd.Trail, 1f))

// 隣へ移るときの、前と後ろの差の最大(移動距離に対する割合)
private val PeakLead: Float by lazy { (0..200).maxOf { frontAt(it / 200f) - backAt(it / 200f) } }

// 弾みありの、前の端と後ろの端の動き(時間に対する区切り)
// 縮んで後ろへためる → 前が伸びて通り過ぎる → 後ろが追いついて縮む → 戻る
private val BounceFrontOffsets = floatArrayOf(0f, 0.2f, 0.55f, 0.75f, 1f)
private val BounceBackOffsets = floatArrayOf(0f, 0.2f, 0.4f, 0.72f, 0.88f, 1f)

// 区切りの値を、区切りの間はなめらかにつなぐ
private fun smoothKeyframes(t: Float, offsets: FloatArray, values: FloatArray): Float {
    if (t <= offsets.first()) return values.first()
    for (i in 1 until offsets.size) {
        if (t <= offsets[i]) return lerp(values[i - 1], values[i], smooth(part(t, offsets[i - 1], offsets[i])))
    }
    return values.last()
}

// 時間 t(0〜1、直線)での印の形。bouncy なら、始めに縮んでため、終わりに通り過ぎて弾む
private fun Density.blobAt(motion: IndicatorMotion, t: Float, bouncy: Boolean): Blob {
    val s = PiyoTokens.Gourd.ButtonSize.toPx()
    val step = (PiyoTokens.Gourd.ButtonSize + PiyoTokens.Gourd.Gap).toPx()
    val from = buttonTop(motion.from).toPx()
    val to = buttonTop(motion.to.toFloat()).toPx()
    if (!motion.moving || from == to) return Blob(to, s, s, 0f)
    val dist = abs(to - from)
    val front: Float
    var back: Float
    if (bouncy) {
        val a = PiyoTokens.Gourd.Anticipation.toPx()
        val o = PiyoTokens.Gourd.Overshoot.toPx()
        // 遠くへ移るときも伸び縮みは隣へ移るときと同じにし、残りの距離は前の端と同じ割合で進める
        val near = min(dist, step)
        val extra = (dist - near) * smoothKeyframes(t, BounceFrontOffsets, floatArrayOf(0f, 0f, 1f, 1f, 1f))
        front = smoothKeyframes(t, BounceFrontOffsets, floatArrayOf(0f, -a, dist + o, dist - o * 0.3f, dist))
        back = smoothKeyframes(t, BounceBackOffsets, floatArrayOf(0f, -a * 0.25f, -a * 0.25f, near + o * 0.8f, near - o * 0.2f, near)) + extra
    } else {
        front = dist * frontAt(t)
        back = dist * backAt(t)
        if (dist > step) {
            val cap = step * PeakLead
            back = front - smoothMin(front - back, cap, cap * 0.2f)
        }
    }
    val top = if (to > from) from + back else from - front
    val length = s + front - back
    val width = (s * (s / length).pow(STRETCH_POWER)).coerceIn(s * PiyoTokens.Gourd.Waist, s * 1.08f)
    val fit = smooth(t / FIT_RAMP) * smooth((1 - t) / FIT_RAMP)
    return Blob(top, length, width, fit)
}

// なめらかな小さいほう(差が k より小さいところを丸める)
private fun smoothMin(a: Float, b: Float, k: Float): Float {
    val h = (0.5f + 0.5f * (b - a) / k).coerceIn(0f, 1f)
    return lerp(b, a, h) - k * h * (1 - h)
}

// 輪郭を何点で表すか(片側)
private const val BLOB_SAMPLES = 40

// 伸びた印の形。両端は丸く、くびれのところはひょうたんの内側に合わせて細る。horizontal なら横に伸びる
private class BlobShape(
    private val blob: Blob,
    private val profile: GourdProfile,
    private val inset: Float,
    private val soft: Float,
    private val horizontal: Boolean,
) : PiyoChangingShape {
    private var cache: Pair<Size, Outline>? = null

    // 輪郭を作る(同じ大きさなら使い回す)
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        cache?.let { (s, o) -> if (s == size) return o }
        return Outline.Generic(build(size)).also { cache = size to it }
    }

    // 左右の縁を点でたどり、点の間をなめらかにつないだ形(縦向きで計算し、横なら縦横を入れ替える)
    private fun build(actual: Size): Path {
        val size = if (horizontal) Size(actual.height, actual.width) else actual
        val half = size.width / 2
        val len = size.height
        val cap = min(half, len / 2)
        val right = mutableListOf<Offset>()
        for (i in 0..BLOB_SAMPLES) {
            val y = len / 2 * (1 - cos(PI * i / BLOB_SAMPLES).toFloat())
            val e = min(y, len - y)
            val k = (cap - e) / cap
            val round = if (e < cap) half * sqrt(max(0f, 1 - k * k)) else half
            val room = profile.halfWidth(blob.top + y) - inset
            val hw = max(0f, lerp(round, smoothMin(round, room, soft), blob.fit))
            right += Offset(half + hw, y)
        }
        val left = right.subList(1, right.size - 1).reversed().map { Offset(size.width - it.x, it.y) }
        val points = (right + left).map { if (horizontal) Offset(it.y, it.x) else it }
        return Path().apply {
            val start = (points.last() + points.first()) / 2f
            moveTo(start.x, start.y)
            points.forEachIndexed { i, p ->
                val next = (p + points[(i + 1) % points.size]) / 2f
                quadraticTo(p.x, p.y, next.x, next.y)
            }
            close()
        }
    }
}

// 選んだ項目の印(動いている間は、形を毎コマ作り直す。内側の影も形に合わせるため)
@Composable
private fun RailIndicator(motion: IndicatorMotion, count: Int, horizontal: Boolean, bouncy: Boolean, indicator: @Composable (Shape) -> Unit) {
    val density = LocalDensity.current
    val profile = remember(count, density) { with(density) { GourdProfile(RailWidth.toPx(), railHeight(count).toPx(), count) } }
    val blob = with(density) { blobAt(motion, motion.progress.value, bouncy) }
    // 止まっているときも同じ描き方にする(動き始めと終わりで内側の影の描き方が変わらないように)
    val shape = remember(blob, profile, horizontal, density) {
        with(density) { BlobShape(blob, profile, PiyoTokens.Gourd.FitInset.toPx(), PiyoTokens.Gourd.FitInset.toPx() * 1.5f, horizontal) }
    }
    Box(
        Modifier.layout { measurable, _ ->
            val w = blob.width.roundToInt()
            val len = blob.length.roundToInt()
            val cross = ((RailWidth.toPx() - w) / 2).roundToInt()
            val main = blob.top.roundToInt()
            val placeable = measurable.measure(if (horizontal) Constraints.fixed(len, w) else Constraints.fixed(w, len))
            layout(0, 0) { if (horizontal) placeable.place(main, cross) else placeable.place(cross, main) }
        },
    ) { indicator(shape) }
}

// ボタンの列と印(単体のタブとパネル付きで共有する)
@Composable
private fun RailContent(
    tabs: List<GourdTab>,
    selectedIndex: Int,
    onTap: (Int) -> Unit,
    clickLabel: (Int) -> String?,
    orientation: Orientation,
    bouncy: Boolean,
    indicator: @Composable (Shape) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = if (bouncy) PiyoTokens.Gourd.BounceDuration else PiyoTokens.Gourd.SwitchDuration
    val motion = rememberIndicatorMotion(selectedIndex, duration, PiyoTokens.Gourd.SwitchEasing)
    val arrivedIndex by remember(motion) {
        derivedStateOf { if (!motion.moving || motion.progress.value >= PiyoTokens.Gourd.ArriveAt) motion.to else -1 }
    }
    val horizontal = orientation == Orientation.Horizontal
    Box(modifier.size(railSize(tabs.size, orientation)).selectableGroup()) {
        RailIndicator(motion, tabs.size, horizontal, bouncy, indicator)
        tabs.forEachIndexed { i, tab ->
            RailButton(tab, i, horizontal, i == arrivedIndex, i == selectedIndex, { onTap(i) }, clickLabel(i))
        }
    }
}

// ひょうたんの丸ボタン1つ
@Composable
private fun RailButton(tab: GourdTab, index: Int, horizontal: Boolean, arrived: Boolean, selected: Boolean, onClick: () -> Unit, clickLabel: String?) {
    val source = remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, selected = arrived)
    val color = Piyo.style.contentColor(SurfaceRole.TabTrack, state, PiyoTone.Default)
    val shape = Piyo.shapes.circle
    Box(
        modifier = Modifier
            .then(
                if (horizontal) {
                    Modifier.offset(buttonTop(index.toFloat()), PiyoTokens.Gourd.Padding)
                } else {
                    Modifier.offset(PiyoTokens.Gourd.Padding, buttonTop(index.toFloat()))
                },
            )
            .size(PiyoTokens.Gourd.ButtonSize)
            .focusRing(state.focused, shape)
            .clickable(source, null, onClickLabel = clickLabel, role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = tab.label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalPiyoContentColor provides color) {
            PiyoIcon(tab.icon, contentDescription = null, size = PiyoTokens.Gourd.IconSize)
        }
    }
}

// ひょうたん型のタブ(縦か横に並ぶ丸ボタンを、くびれのある形で包む)。bouncy で印の動きに弾みを付ける
@Composable
fun GourdTabRail(
    tabs: List<GourdTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    bouncy: Boolean = false,
    indicator: @Composable (Shape) -> Unit = { GourdTabRailDefaults.Indicator(it) },
) {
    val shape = GourdShape(tabs.size, orientation)
    Box(modifier.size(railSize(tabs.size, orientation)).reportGenieAnchor(shape).piyoSurface(shape, SurfaceRole.TabTrack)) {
        RailContent(tabs, selectedIndex, onSelect, { null }, orientation, bouncy, indicator)
    }
}

// 継ぎ目の動きの区切り(参照実装の page-rail.js と同じ。フィレットは時間ではなく形で決めるので使わない)
private const val RETRACT_FROM = 0.4f
private const val ROUND_UNTIL = 0.65f
private const val EXTEND_UNTIL = 0.6f
private const val STRAIGHT_FROM = 0.35f
private const val STRAIGHT_UNTIL = 0.9f

// なめらかに 0 から 1 へ(smoothstep)
private fun smooth(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3 - 2 * c)
}

// 継ぎ目の状態(伸び 0〜1、角の丸み 0〜1)
internal data class Seam(val extend: Float, val round: Float)

// 開閉の時間 t(0〜1)での継ぎ目の状態
internal fun seamAt(t: Float, closing: Boolean): Seam =
    if (closing) {
        Seam(1 - smooth((t - RETRACT_FROM) / (1 - RETRACT_FROM)), smooth(t / ROUND_UNTIL))
    } else {
        Seam(smooth(t / EXTEND_UNTIL), 1 - smooth((t - STRAIGHT_FROM) / (STRAIGHT_UNTIL - STRAIGHT_FROM)))
    }

// ひょうたんをパネルのどちら側に置くか(左右なら縦、上下なら横のひょうたん)
enum class GourdSide { Left, Right, Top, Bottom }

// その側に置くひょうたんの向き
private val GourdSide.orientation: Orientation
    get() = if (this == GourdSide.Top || this == GourdSide.Bottom) Orientation.Horizontal else Orientation.Vertical

// ひょうたんを辺のどこに寄せるか。bias は -1 で先頭(上か左)、0 で真ん中、1 で末尾(下か右)。間の値も使える
@Immutable
@JvmInline
value class GourdAlignment(val bias: Float) {
    companion object {
        val Start = GourdAlignment(-1f)
        val Center = GourdAlignment(0f)
        val End = GourdAlignment(1f)
    }
}

// 左に縦のひょうたんを置いた向き(基準の向き)から、実際の向きへの写し方。actual は実際の大きさ
private class SideMap(side: GourdSide, actual: Size) {

    // 基準の向きでの大きさ
    val size = if (side.orientation == Orientation.Horizontal) Size(actual.height, actual.width) else actual

    // x' = ax + by + e、y' = cx + dy + f
    private val a: Float
    private val b: Float
    private val c: Float
    private val d: Float
    private val e: Float
    private val f: Float

    init {
        when (side) {
            GourdSide.Left -> { a = 1f; b = 0f; c = 0f; d = 1f; e = 0f; f = 0f }
            GourdSide.Right -> { a = -1f; b = 0f; c = 0f; d = 1f; e = actual.width; f = 0f }
            GourdSide.Top -> { a = 0f; b = 1f; c = 1f; d = 0f; e = 0f; f = 0f }
            GourdSide.Bottom -> { a = 0f; b = 1f; c = -1f; d = 0f; e = 0f; f = actual.height }
        }
    }

    // 基準の向きの点を、実際の向きへ
    fun map(p: Offset): Offset = Offset(a * p.x + b * p.y + e, c * p.x + d * p.y + f)

    // 基準の向きの四角を、実際の向きへ
    fun map(r: Rect): Rect {
        val p = map(r.topLeft)
        val q = map(r.bottomRight)
        return Rect(min(p.x, q.x), min(p.y, q.y), max(p.x, q.x), max(p.y, q.y))
    }

    // 形にかける行列
    fun matrix(): Matrix = Matrix().apply {
        values[Matrix.ScaleX] = a
        values[Matrix.SkewX] = b
        values[Matrix.TranslateX] = e
        values[Matrix.SkewY] = c
        values[Matrix.ScaleY] = d
        values[Matrix.TranslateY] = f
    }
}

// 部品の配置(実際の向きでの px)。ひょうたん、パネル、ジニーの行き先(パネルから見た位置)
// railTop と panelWidth は基準の向きでの、ひょうたんの上端とパネルの幅
private class RailPanelLayout(val rail: Rect, val panel: Rect, val target: Offset, val railTop: Float, val panelWidth: Float)

// 選んだ番号と寄せ方から配置を求める
private fun Density.railPanelLayout(map: SideMap, count: Int, selected: Int, panelSize: Dp?, align: GourdAlignment): RailPanelLayout {
    val pl = PiyoTokens.Gourd.PanelLeft.toPx()
    val pw = panelSize?.toPx() ?: (map.size.width - pl)
    val len = railHeight(count).toPx()
    val top = max(0f, map.size.height - len) * (align.bias.coerceIn(-1f, 1f) + 1) / 2
    val rail = map.map(Rect(0f, top, RailWidth.toPx(), top + len))
    val panel = map.map(Rect(pl, 0f, pl + pw, map.size.height))
    val center = map.map(Offset(RailWidth.toPx() / 2, top + (buttonTop(selected.toFloat()) + PiyoTokens.Gourd.ButtonSize / 2).toPx()))
    return RailPanelLayout(rail, panel, center - panel.topLeft, top, pw)
}

// 合成の状態(継ぎ目、パネルの進行度)。パネルの進行度は 0 で開いた形、null で消えた
private data class JoinKey(val seam: Seam, val panel: Float?)

// ひょうたん、継ぎ目、フィレット、パネルをつないだ1つの形(つなぎ目に線が出ず、線がジニーの変形に沿う)
// すべて基準の向きで作って写す。状態は輪郭を作るときに読む(開閉の間に組み直さず、描き直すだけで済ませるため)
private class JoinShape(
    private val count: Int,
    private val side: GourdSide,
    private val panelSize: Dp?,
    private val align: GourdAlignment,
    private val key: () -> JoinKey,
    private val selected: () -> Int,
) : Shape {
    // 合成は重いので、同じ大きさと状態なら作った輪郭を使い回す
    private var cache: Triple<Size, Pair<JoinKey, Int>, Outline>? = null

    // 輪郭を作る
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val k = key() to selected()
        cache?.let { (s, ck, o) -> if (s == size && ck == k) return o }
        return build(size, density, k.first, k.second).also { cache = Triple(size, k, it) }
    }

    // 形を合成する(すべて基準の向きで作り、最後に写す。px は density から求める)
    private fun build(size: Size, density: Density, k: JoinKey, selected: Int): Outline = with(density) {
        val map = SideMap(side, size)
        val layout = railPanelLayout(map, count, selected, panelSize, align)
        val w = RailWidth.toPx()
        val h = railHeight(count).toPx()
        val r = PiyoTokens.Radius.Lg.toPx()
        val pl = PiyoTokens.Gourd.PanelLeft.toPx()
        val f = PiyoTokens.Gourd.Fillet.toPx()
        val cx = w / 2
        val top = layout.railTop
        val ch = map.size.height
        val (rTop, fTop) = cornerFit(top, r, f)
        val (rBottom, fBottom) = cornerFit(ch - top - h, r, f)
        val seam = k.seam
        val frame = k.panel?.let { p ->
            val target = Offset(cx - pl, top + (buttonTop(selected.toFloat()) + PiyoTokens.Gourd.ButtonSize / 2).toPx())
            GenieFrame(p, Size(layout.panelWidth, ch), target, PiyoTokens.Genie.Neck.toPx(), r, rTop, rBottom)
        }
        // 継ぎ目の上端と下端は、パネルの行き先側の辺に合わせる(縮んでいくパネルから継ぎ目がはみ出さないように)
        val bridgeTop = ((frame?.a ?: 0f) - top).coerceIn(0f, h)
        val bridgeBottom = ((frame?.d ?: ch) - top).coerceIn(0f, h)
        val bridgeEnd = cx + (pl + r - cx) * seam.extend
        // パネルがある間は、継ぎ目の先をパネルの角の丸みの終わりまで必ず届かせ、角の丸みはパネルに重なる分だけにする(隙間を作らない)
        val reach = frame?.let { pl + it.placeX(0f) + it.r1 }
        val joinEnd = if (reach != null) max(bridgeEnd, reach) else bridgeEnd
        val joinRound = if (reach != null) min(cx * seam.round, joinEnd - reach) else cx * seam.round
        var path = gourdPath(w, h, count)
        if (bridgeBottom > bridgeTop) {
            path = union(path, bridgePath(w, bridgeBottom, joinEnd, joinRound, bridgeTop))
        }
        // フィレットは、今のパネルの行き先側の辺に付け、辺の残りと継ぎ目の重なりに合わせて縮める(時間ではなく形で決める)
        frame?.let { fr ->
            val edge = pl + fr.placeX(0f)
            val k = smooth((bridgeEnd - edge) / f)
            val rx = min(f, edge - cx).coerceAtLeast(0f) * k
            val below = min(fBottom, fr.d - fr.r1dY - (top + h)).coerceAtLeast(0f) * k
            val above = min(fTop, top - (fr.a + fr.r1aY)).coerceAtLeast(0f) * k
            if (rx > 0f && below > 0f) path = union(path, filletPath(h, edge, rx, below))
            if (rx > 0f && above > 0f) path = union(path, filletPathUp(0f, edge, rx, above))
        }
        path.translate(Offset(0f, top))
        frame?.let {
            val panel = it.placedPath()
            panel.translate(Offset(pl, 0f))
            path = union(path, panel)
        }
        path.transform(map.matrix())
        Outline.Generic(path)
    }
}

// ひょうたんとパネルの端の間の余りに合わせた、パネルの角とフィレットの縦の丸み(横の丸みは変えない)
// 余りが角の丸みとフィレットの合計より狭いときは、縦だけを同じ割合で縮め、ゆるい S 字の段にする(端にぴったりなら角は継ぎ目に隠れる)
private fun cornerFit(gap: Float, r: Float, f: Float): Pair<Float, Float> = when {
    gap <= 0.5f -> r to 0f
    gap >= r + f -> r to f
    else -> (gap / (r + f)).let { r * it to f * it }
}

// パネルから見た、ひょうたんの形(中身を描かない場所)
private fun Density.railHole(layout: RailPanelLayout, count: Int, side: GourdSide): Path {
    val outline = GourdShape(count, side.orientation).createOutline(layout.rail.size, LayoutDirection.Ltr, this)
    return Path().apply {
        addOutline(outline)
        translate(layout.rail.topLeft - layout.panel.topLeft)
    }
}

// ひょうたんの上に中身を描かない(開閉中に縮んだ中身が、ひょうたんの前に出ないように)
private fun Modifier.withoutRail(hole: Path): Modifier = drawWithContent {
    clipPath(hole, ClipOp.Difference) { this@drawWithContent.drawContent() }
}

// 2つの形を合わせる
private fun union(a: Path, b: Path): Path = Path().apply { op(a, b, PathOperation.Union) }

// 実際の大きさ(px)で要素を置く
private fun Modifier.placeAt(rect: Rect): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints.fixed(rect.width.roundToInt(), rect.height.roundToInt()))
    layout(0, 0) { placeable.place(rect.left.roundToInt(), rect.top.roundToInt()) }
}

// ひょうたん型のタブとパネルをつないだ部品。表示中のタブをもう一度押すと、パネルがジニー効果で開閉する。閉じているときは、どのタブを押しても開く
// side でひょうたんをパネルの左右上下に置く(左右なら縦、上下なら横)。railAlignment で辺のどこに寄せるか(先頭、真ん中、末尾、間の値)
// panelSize はひょうたんから離れる向きのパネルの大きさ
@Composable
fun GourdRailPanel(
    tabs: List<GourdTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    panelOpen: Boolean,
    onPanelOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    side: GourdSide = GourdSide.Left,
    railAlignment: GourdAlignment = GourdAlignment.Start,
    panelSize: Dp? = null,
    openLabel: String = "パネルを開く",
    closeLabel: String = "パネルを閉じる",
    bouncy: Boolean = false,
    indicator: @Composable (Shape) -> Unit = { GourdTabRailDefaults.Indicator(it) },
    panel: @Composable () -> Unit,
) {
    val time = remember { Animatable(if (panelOpen) 0f else 1f) }
    var closing by remember { mutableStateOf(!panelOpen) }
    val reduce = LocalPiyoReduceMotion.current
    LaunchedEffect(panelOpen, reduce) {
        closing = !panelOpen
        val goal = if (panelOpen) 0f else 1f
        if (reduce) time.snapTo(goal) else time.animateTo(goal, tween(PiyoTokens.Genie.Duration, easing = LinearEasing))
    }
    val atRest by remember { derivedStateOf { time.value <= 0f } }
    val gone by remember { derivedStateOf { time.value >= 1f } }
    val showPanel = panelOpen || !gone
    val currentSelected by rememberUpdatedState(selectedIndex)
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val layout = with(density) { railPanelLayout(SideMap(side, size), tabs.size, selectedIndex, panelSize, railAlignment) }
        val currentTarget by rememberUpdatedState(layout.target)
        val shape = remember(tabs.size, side, panelSize, railAlignment) {
            JoinShape(
                count = tabs.size,
                side = side,
                panelSize = panelSize,
                align = railAlignment,
                key = {
                    val t = time.value
                    JoinKey(seamAt(if (closing) t else 1 - t, closing), if (t >= 1f) null else PiyoTokens.Genie.Easing.transform(t))
                },
                selected = { currentSelected },
            )
        }
        Box(Modifier.fillMaxSize().graphicsLayer().piyoSurface(shape, SurfaceRole.Card))
        if (showPanel) {
            val content = if (atRest) Modifier else Modifier.genie({ PiyoTokens.Genie.Easing.transform(time.value) }, { currentTarget })
            val hole = remember(layout.rail, layout.panel, tabs.size, side, density) { with(density) { railHole(layout, tabs.size, side) } }
            Box(Modifier.placeAt(layout.panel).withoutRail(hole).then(content)) { panel() }
        }
        RailContent(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTap = { i ->
                if (i == selectedIndex) {
                    onPanelOpenChange(!panelOpen)
                } else {
                    onSelect(i)
                    if (!panelOpen) onPanelOpenChange(true)
                }
            },
            clickLabel = { i -> if (!panelOpen) openLabel else if (i == selectedIndex) closeLabel else null },
            orientation = side.orientation,
            bouncy = bouncy,
            indicator = indicator,
            modifier = Modifier.offset { IntOffset(layout.rail.left.roundToInt(), layout.rail.top.roundToInt()) },
        )
    }
}
