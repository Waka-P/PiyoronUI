package piyoron.effects.gourd

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

// ひょうたんの形の計算(参照実装の rail-shape.js を移したもの)

// 90度の円弧を3次ベジェで近似する係数
private const val K = 0.5523f

// 丸からくびれへ移る角度
private const val NECK_ANGLE = 35.0

// くびれの曲線の膨らみ(半径に対する割合)
private const val NECK_BULGE = 8f / 30f

// 3次ベジェの1区間(始点、制御点2つ、終点)
internal data class Segment(val p0: Offset, val c1: Offset, val c2: Offset, val p3: Offset)

// 区間の途中の点(t は 0〜1)
internal fun Segment.at(t: Float): Offset {
    val u = 1 - t
    return p0 * (u * u * u) + c1 * (3 * u * u * t) + c2 * (3 * u * t * t) + p3 * (t * t * t)
}

// 直線を3次ベジェの区間にする
private fun straight(p: Offset, q: Offset) = Segment(p, p + (q - p) / 3f, p + (q - p) * (2f / 3f), q)

// 区間の並びをつないだ閉じた形にする
internal fun pathOf(segments: List<Segment>): Path = Path().apply {
    moveTo(segments.first().p0.x, segments.first().p0.y)
    segments.forEach { cubicTo(it.c1.x, it.c1.y, it.c2.x, it.c2.y, it.p3.x, it.p3.y) }
    close()
}

// 右半分を左右反転して左半分にする
private fun mirrorLeft(right: List<Segment>, cx: Float): List<Segment> {
    // 中心線で反転した点
    fun m(p: Offset) = Offset(2 * cx - p.x, p.y)
    return right.reversed().map { Segment(m(it.p3), m(it.c2), m(it.c1), m(it.p0)) }
}

// ひょうたんの右半分(幅 w、高さ h、丸の数 n)
private fun gourdRight(w: Float, h: Float, n: Int): List<Segment> {
    val r = w / 2
    val cx = r
    val a = NECK_ANGLE * PI / 180
    val kr = K * r
    val k = (4.0 / 3 * tan(a / 4) * r).toFloat()
    val t = NECK_BULGE * r
    val sinA = sin(a).toFloat()
    val cosA = cos(a).toFloat()
    val centers = if (n <= 1) listOf(r) else List(n) { r + it * (h - 2 * r) / (n - 1) }
    val first = centers.first()
    val last = centers.last()
    val segs = mutableListOf(Segment(Offset(cx, 0f), Offset(cx + kr, 0f), Offset(cx + r, first - kr), Offset(cx + r, first)))
    for (i in 0 until centers.size - 1) {
        val c1 = centers[i]
        val c2 = centers[i + 1]
        val p1 = Offset(cx + r * cosA, c1 + r * sinA)
        val p2 = Offset(cx + r * cosA, c2 - r * sinA)
        segs += Segment(Offset(cx + r, c1), Offset(cx + r, c1 + k), Offset(p1.x + k * sinA, p1.y - k * cosA), p1)
        segs += Segment(p1, Offset(p1.x - t * sinA, p1.y + t * cosA), Offset(p2.x - t * sinA, p2.y - t * cosA), p2)
        segs += Segment(p2, Offset(p2.x + k * sinA, p2.y + k * cosA), Offset(cx + r, c2 - k), Offset(cx + r, c2))
    }
    segs += Segment(Offset(cx + r, last), Offset(cx + r, last + kr), Offset(cx + kr, h), Offset(cx, h))
    return segs
}

// ひょうたんの輪郭の区間(右半分から下へ回り、左半分を上へ戻る)
internal fun gourdSegments(w: Float, h: Float, n: Int): List<Segment> {
    val right = gourdRight(w, h, n)
    return right + mirrorLeft(right, w / 2)
}

// ひょうたんの輪郭(縦向き)
internal fun gourdPath(w: Float, h: Float, n: Int): Path = pathOf(gourdSegments(w, h, n))

// 縦横を入れ替えた区間
private fun Segment.swapped(): Segment {
    // 縦横を入れ替えた点
    fun s(p: Offset) = Offset(p.y, p.x)
    return Segment(s(p0), s(c1), s(c2), s(p3))
}

// 横向きのひょうたんの輪郭(幅 w、高さ h。丸は横に並ぶ)
internal fun gourdPathHorizontal(w: Float, h: Float, n: Int): Path = pathOf(gourdSegments(h, w, n).map { it.swapped() })

// 1区間を何点で表にするか
private const val PROFILE_STEPS = 16

// ひょうたんの、高さごとの中心から縁までの幅(右半分を上から下へたどった表)
internal class GourdProfile(w: Float, h: Float, n: Int) {
    private val ys: FloatArray
    private val xs: FloatArray

    init {
        val cx = w / 2
        val points = gourdRight(w, h, n).flatMap { s -> List(PROFILE_STEPS) { s.at(it / PROFILE_STEPS.toFloat()) } } + Offset(cx, h)
        ys = FloatArray(points.size) { points[it].y }
        xs = FloatArray(points.size) { points[it].x - cx }
    }

    // 高さ y での中心から縁までの幅
    fun halfWidth(y: Float): Float {
        if (y <= ys.first() || y >= ys.last()) return 0f
        var lo = 0
        var hi = ys.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (ys[mid] <= y) lo = mid else hi = mid
        }
        val span = ys[hi] - ys[lo]
        return if (span <= 0f) max(xs[lo], xs[hi]) else xs[lo] + (xs[hi] - xs[lo]) * (y - ys[lo]) / span
    }
}

// 継ぎ目(ひょうたんの中心から x まで伸びる帯。上端 top から下端 h まで。rc は右側の角の丸み)
internal fun bridgePath(w: Float, h: Float, x: Float, rc: Float, top: Float = 0f): Path {
    val cx = w / 2
    val r = max(0f, min(rc, min(x - cx, (h - top) / 2)))
    val kr = K * r
    return pathOf(
        listOf(
            straight(Offset(cx, top), Offset(x - r, top)),
            Segment(Offset(x - r, top), Offset(x - r + kr, top), Offset(x, top + r - kr), Offset(x, top + r)),
            straight(Offset(x, top + r), Offset(x, h - r)),
            Segment(Offset(x, h - r), Offset(x, h - r + kr), Offset(x - r + kr, h), Offset(x - r, h)),
            straight(Offset(x - r, h), Offset(cx, h)),
        ),
    )
}

// 上向きのフィレット(ひょうたんの上端 y とパネルの左端の間をなめらかにつなぐ角。rf は横、ry は縦の大きさ)
internal fun filletPathUp(y: Float, pl: Float, rf: Float, ry: Float): Path = pathOf(
    listOf(
        straight(Offset(pl, y), Offset(pl, y - ry)),
        Segment(Offset(pl, y - ry), Offset(pl, y - ry + K * ry), Offset(pl - rf + K * rf, y), Offset(pl - rf, y)),
    ),
)

// フィレット(ひょうたんの下端とパネルの左端の間をなめらかにつなぐ角。pl はパネルの左端、rf は横、ry は縦の大きさ)
internal fun filletPath(h: Float, pl: Float, rf: Float, ry: Float): Path = pathOf(
    listOf(
        straight(Offset(pl, h), Offset(pl, h + ry)),
        Segment(Offset(pl, h + ry), Offset(pl, h + ry - K * ry), Offset(pl - rf + K * rf, h), Offset(pl - rf, h)),
    ),
)
