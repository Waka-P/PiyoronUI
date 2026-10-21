package piyoron.effects.genie

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import piyoron.core.LocalPiyoReduceMotion
import piyoron.core.PiyoChangingShape
import piyoron.core.SurfaceRole
import piyoron.core.keyframes
import piyoron.core.lerp
import piyoron.core.piyoSurface
import piyoron.tokens.PiyoTokens

// 区切りの位置(開いた形、途中、閉じた形)
private val Offsets = floatArrayOf(0f, 0.5f, 1f)

// 円弧を3次ベジェで近似するとき、制御点を端から離す割合の残り(1 - 0.5523)
private const val ARC = 1f - 0.5523f

// パネルの輪郭(参照実装の outline)。左端が行き先の側。a〜d は左上・右上・右下・左下の高さ。mirror で左右を反転する
// r1 は行き先側の角の横の丸み、r1aY と r1dY はその上と下の角の縦の丸み、r2 は反対側の角の丸み
// 角は円弧にする(進行度 0 で角丸の四角と同じ形になる)。map は反転のあとにかける点の変換
internal fun genieOutline(
    w: Float,
    a: Float,
    b: Float,
    c: Float,
    d: Float,
    r1: Float,
    r1aY: Float,
    r1dY: Float,
    r2: Float,
    mirror: Boolean,
    map: (Float, Float) -> Offset,
): Path {
    val m = w / 2
    // 反転と変換をかけた点
    fun pt(x: Float, y: Float) = map(if (mirror) w - x else x, y)
    return Path().apply {
        // 3次ベジェを1区間足す
        fun curve(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            val p1 = pt(x1, y1)
            val p2 = pt(x2, y2)
            val p3 = pt(x3, y3)
            cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        }
        val start = pt(0f, a + r1aY)
        moveTo(start.x, start.y)
        curve(0f, a + r1aY * ARC, r1 * ARC, a, r1, a)
        curve(m, a, m, b, w - r2, b)
        curve(w - r2 * ARC, b, w, b + r2 * ARC, w, b + r2)
        val side = pt(w, c - r2)
        lineTo(side.x, side.y)
        curve(w, c - r2 * ARC, w - r2 * ARC, c, w - r2, c)
        curve(m, c, m, d, r1, d)
        curve(r1 * ARC, d, 0f, d - r1dY * ARC, 0f, d - r1dY)
        close()
    }
}

// 進行度 p での不透明度(終わり際に消える)
internal fun genieAlpha(p: Float): Float = keyframes(p, floatArrayOf(0f, 0.8f, 1f), floatArrayOf(1f, 1f, 0f))

// 長さ len の範囲から v がはみ出している量
private fun outside(v: Float, len: Float): Float = if (v < 0f) -v else if (v > len) v - len else 0f

// 進行度 p(0 で開いた形、1 で閉じた形)でのジニーの変形。大きさ、行き先、首、角の丸みは px
// 行き先が上下にあるときは縦に縮める(縦横を入れ替えて、横に縮めるときと同じ計算をする)
// nearStart と nearEnd は、行き先側の角(計算する向きで上と下)の始めの縦の丸み(横の丸みは radius)
internal class GenieFrame(
    p: Float,
    size: Size,
    target: Offset,
    neck: Float,
    radius: Float,
    nearStart: Float = radius,
    nearEnd: Float = radius,
) {

    // 縦に縮めるか
    private val vertical = outside(target.y, size.height) > outside(target.x, size.width)

    // 縮める向きの長さと、もう一方の長さ
    private val w = if (vertical) size.height else size.width
    private val h = if (vertical) size.width else size.height

    // 縮める向きでの行き先と、もう一方での行き先
    private val tMain = if (vertical) target.y else target.x
    private val tCross = if (vertical) target.x else target.y

    // 行き先が右(縦なら下)にあるときは反転する
    private val mirror = tMain > w / 2

    // 縮みの中心
    private val origin = if (mirror) w else 0f

    // 縮める向きの移動
    private val shift = keyframes(p, Offsets, floatArrayOf(0f, 0.45f, 1f)) * (if (mirror) tMain - w else tMain)

    // 縮める向きの縮み
    private val scale = keyframes(p, Offsets, floatArrayOf(1f, 0.55f, 0.02f))

    // 左上・右上・右下・左下の高さ(縦に縮めるときは縦横を入れ替えた向きで)
    val a: Float
    val b: Float
    val c: Float
    val d: Float
    // 行き先側の角の横の丸み
    val r1 = keyframes(p, Offsets, floatArrayOf(radius, 0f, 0f))
    // 行き先側の上と下の角の縦の丸み
    val r1aY = keyframes(p, Offsets, floatArrayOf(nearStart, 0f, 0f))
    val r1dY = keyframes(p, Offsets, floatArrayOf(nearEnd, 0f, 0f))
    private val r2 = keyframes(p, Offsets, floatArrayOf(radius, radius / 2, 0f))

    init {
        val top = tCross - neck
        val bottom = tCross + neck
        a = keyframes(p, Offsets, floatArrayOf(0f, lerp(0f, top, 0.9f), top))
        b = keyframes(p, Offsets, floatArrayOf(0f, lerp(0f, top, 0.45f), top))
        c = keyframes(p, Offsets, floatArrayOf(h, lerp(h, bottom, 0.45f), bottom))
        d = keyframes(p, Offsets, floatArrayOf(h, lerp(h, bottom, 0.9f), bottom))
    }

    // 計算した向きの点を、要素の向きに戻す
    private fun out(x: Float, y: Float) = if (vertical) Offset(y, x) else Offset(x, y)

    // 層の中で切り抜く形(縮める前)
    fun clipPath(): Path = genieOutline(w, a, b, c, d, r1, r1aY, r1dY, r2, mirror, ::out)

    // 縮めて動かしたあとの、縮める向きの位置
    fun placeX(x: Float): Float = origin + (x - origin) * scale + shift

    // 縮めて動かしたあとの形(面を描く用。線の太さは変わらない)
    fun placedPath(): Path = genieOutline(w, a, b, c, d, r1, r1aY, r1dY, r2, mirror) { x, y -> out(placeX(x), y) }

    // 層に縮みと移動をかける
    fun applyTo(layer: GraphicsLayerScope) {
        val pivot = if (mirror) 1f else 0f
        if (vertical) {
            layer.transformOrigin = TransformOrigin(0.5f, pivot)
            layer.translationY = shift
            layer.scaleY = scale
        } else {
            layer.transformOrigin = TransformOrigin(pivot, 0.5f)
            layer.translationX = shift
            layer.scaleX = scale
        }
    }
}

// 層の中で切り抜く形
private class GenieClip(private val frame: GenieFrame) : Shape {

    // 輪郭を作る
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(frame.clipPath())
}

// ジニーで変形した面の形。面の塗りと線を、変形した輪郭に沿わせるときに使う(この形は縮めずに描く)
// progress は 0 で開いた形、1 で消えた形(イージング後の値)。target はこの要素から見た行き先の中心(px)
@Immutable
data class GenieSurfaceShape(
    val progress: Float,
    val target: Offset,
    val cornerRadius: Dp = PiyoTokens.Radius.Lg,
    val neck: Dp = PiyoTokens.Genie.Neck,
) : PiyoChangingShape {

    // 輪郭を作る(進行度 0 では角丸の四角)
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        if (progress <= 0f) {
            Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(cornerRadius.toPx())))
        } else {
            Outline.Generic(GenieFrame(progress, size, target, neck.toPx(), cornerRadius.toPx()).placedPath())
        }
    }
}

// ジニーの変形をかける。progress は 0 で開いた形、1 で消えた形(イージング後の値)。target はこの要素から見た行き先の中心
// 中身を切り抜いて縮める。面の線を変形に沿わせるには、面は GenieSurfaceShape で別に描く
fun Modifier.genie(progress: () -> Float, target: () -> Offset, cornerRadius: Dp = PiyoTokens.Radius.Lg): Modifier = graphicsLayer {
    val p = progress()
    if (p <= 0f) return@graphicsLayer
    val frame = GenieFrame(p, size, target(), PiyoTokens.Genie.Neck.toPx(), cornerRadius.toPx())
    frame.applyTo(this)
    alpha = genieAlpha(p)
    shape = GenieClip(frame)
    clip = true
}

// 吸い込まれる先(ボタンなど)の位置と形
// 付けた要素の位置(outer)と、中の Piyo の部品が知らせる見た目の位置(visual)を持つ。見た目が付けた要素の真ん中にあれば見た目を使う
@Stable
class GenieAnchor internal constructor() {
    internal var outer by mutableStateOf(Rect.Zero)
    internal var explicitShape: Shape? by mutableStateOf(null)
    internal var visual: Rect? by mutableStateOf(null)
    internal var visualShape: Shape? by mutableStateOf(null)

    // 見た目の位置を使えるか(タッチ範囲などで広がった要素の、真ん中にあるとき)
    private fun usesVisual(): Boolean {
        val v = visual ?: return false
        return outer != Rect.Zero && (v.center - outer.center).getDistance() < 1f && outer.contains(v.center)
    }

    // くり抜く範囲
    internal val bounds: Rect get() = if (usesVisual()) visual!! else outer

    // くり抜く形(指定 > 部品が知らせた形 > 円)
    internal val shape: Shape get() = explicitShape ?: (if (usesVisual()) visualShape else null) ?: CircleShape

    // 行き先の中心
    internal val center: Offset get() = if (outer == Rect.Zero) Offset.Unspecified else bounds.center
}

// 吸い込まれる先を、中の部品に知らせる
internal val LocalGenieAnchor = modifierLocalOf<GenieAnchor?> { null }

// 吸い込まれる先を作る
@Composable
fun rememberGenieAnchor(): GenieAnchor = remember { GenieAnchor() }

// この要素を吸い込まれる先にする。出し入れの間、この要素の形の上には描かない
// Piyo のボタンなら、タッチ範囲を除いた見た目の位置と形を使う。shape を渡すと形はそれを使う(省くと部品の形、なければ円)
fun Modifier.genieAnchor(anchor: GenieAnchor, shape: Shape? = null): Modifier = this then GenieAnchorElement(anchor, shape)

// 吸い込まれる先の要素
private data class GenieAnchorElement(val anchor: GenieAnchor, val shape: Shape?) : ModifierNodeElement<GenieAnchorNode>() {

    // 部品を作る
    override fun create() = GenieAnchorNode(anchor, shape)

    // 部品を更新する
    override fun update(node: GenieAnchorNode) = node.update(anchor, shape)
}

// 吸い込まれる先の位置を覚え、中の部品に知らせる
private class GenieAnchorNode(private var anchor: GenieAnchor, private var shape: Shape?) :
    Modifier.Node(), ModifierLocalModifierNode, GlobalPositionAwareModifierNode {

    override val providedValues = modifierLocalMapOf(LocalGenieAnchor to anchor)

    // 吸い込まれる先と形を差し替える
    fun update(anchor: GenieAnchor, shape: Shape?) {
        this.anchor = anchor
        this.shape = shape
        provide(LocalGenieAnchor, anchor)
    }

    // 位置を覚える
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        anchor.outer = coordinates.boundsInRoot()
        anchor.explicitShape = shape
    }
}

// Piyo の部品が、見た目の位置と形を吸い込まれる先に知らせる(外側に genieAnchor があるときだけ)
internal fun Modifier.reportGenieAnchor(shape: Shape): Modifier = this then GenieReportElement(shape)

// 見た目を知らせる要素
private data class GenieReportElement(val shape: Shape) : ModifierNodeElement<GenieReportNode>() {

    // 部品を作る
    override fun create() = GenieReportNode(shape)

    // 部品を更新する
    override fun update(node: GenieReportNode) {
        node.shape = shape
    }
}

// 見た目の位置と形を知らせる
private class GenieReportNode(var shape: Shape) : Modifier.Node(), ModifierLocalModifierNode, GlobalPositionAwareModifierNode {

    // 位置を知らせる
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val anchor = LocalGenieAnchor.current ?: return
        anchor.visual = coordinates.boundsInRoot()
        anchor.visualShape = shape
    }
}

// 吸い込まれる先の形の上には描かない(縮んでいく中身や面が、ボタンの前に出ないように)
private fun Modifier.withoutAnchor(anchor: GenieAnchor, origin: () -> Offset): Modifier = drawWithContent {
    val b = anchor.bounds
    if (b == Rect.Zero) {
        drawContent()
        return@drawWithContent
    }
    val hole = Path().apply {
        addOutline(anchor.shape.createOutline(b.size, layoutDirection, this@drawWithContent))
        translate(b.topLeft - origin())
    }
    clipPath(hole, ClipOp.Difference) { this@drawWithContent.drawContent() }
}

// ジニー効果で出し入れする。閉じるときは anchor に吸い込まれ、開くときはそこから出てくる
// surface を渡すと、その面を変形に沿わせて描く(中身には面を付けない)。null なら中身だけを切り抜いて縮める
@Composable
fun GenieVisibility(
    visible: Boolean,
    anchor: GenieAnchor,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = PiyoTokens.Radius.Lg,
    surface: SurfaceRole? = SurfaceRole.Card,
    content: @Composable () -> Unit,
) {
    val time = remember { Animatable(if (visible) 0f else 1f) }
    val reduce = LocalPiyoReduceMotion.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(visible, reduce) {
        val goal = if (visible) 0f else 1f
        if (reduce) time.snapTo(goal) else time.animateTo(goal, tween(PiyoTokens.Genie.Duration, easing = LinearEasing))
    }
    if (time.value >= 1f && !visible) return
    val progress = { PiyoTokens.Genie.Easing.transform(time.value) }
    val target = { if (anchor.center.isSpecified) anchor.center - origin else Offset.Zero }
    Box(
        modifier
            .onGloballyPositioned { origin = it.boundsInRoot().topLeft }
            .withoutAnchor(anchor) { origin }
            .then(if (visible) Modifier else Modifier.semantics { hideFromAccessibility() }),
    ) {
        if (surface != null) GenieSurface(progress, target, cornerRadius, surface)
        Box(Modifier.genie(progress, target, cornerRadius)) { content() }
    }
}

// 変形に沿った面(内側の影も形に合わせるため、進行度が変わるたびに形を作り直す)
@Composable
private fun BoxScope.GenieSurface(progress: () -> Float, target: () -> Offset, cornerRadius: Dp, role: SurfaceRole) {
    val p = progress()
    val shape = GenieSurfaceShape(p, if (p > 0f) target() else Offset.Zero, cornerRadius)
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer { alpha = genieAlpha(progress()) }
            .piyoSurface(shape, role),
    )
}
