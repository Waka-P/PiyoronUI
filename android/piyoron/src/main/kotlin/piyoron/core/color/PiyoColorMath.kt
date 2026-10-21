package piyoron.core.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// 色の計算(tools/generate-colors/color.js と同じ手順)

// 計算用の色(各成分 0〜1)
internal data class Rgba(val r: Double, val g: Double, val b: Double, val a: Double) {

    // Compose の Color にする
    fun toColor(): Color = Color(toArgb())

    // ARGB の整数にする
    fun toArgb(): Int {
        val q = quantize()
        return (toByte(q.a) shl 24) or (toByte(q.r) shl 16) or (toByte(q.g) shl 8) or toByte(q.b)
    }

    // 不透明度を掛ける
    fun alpha(k: Double): Rgba = copy(a = a * k)

    // 8bit に丸める
    fun quantize(): Rgba = Rgba(toByte(r) / 255.0, toByte(g) / 255.0, toByte(b) / 255.0, toByte(a) / 255.0)

    // OKLCH の明度・彩度・色相を動かす。spread は色相のずらし幅の設定
    fun adjustOklch(l: ChannelOp? = null, c: ChannelOp? = null, h: ChannelOp? = null, spread: HueSpread? = null): Rgba {
        val o = toOklch(this)
        val f = spread?.factor(o.h) ?: 1.0
        return fromOklch(l.applyTo(o.l), c.applyTo(o.c), h.applyTo(o.h, f), a)
    }

    companion object {
        // ARGB の整数から作る
        fun argb(v: Long): Rgba = Rgba(
            ((v shr 16) and 0xFF) / 255.0,
            ((v shr 8) and 0xFF) / 255.0,
            (v and 0xFF) / 255.0,
            ((v shr 24) and 0xFF) / 255.0,
        )

        // Compose の Color から作る
        fun of(color: Color): Rgba = argb(color.toArgb().toLong() and 0xFFFFFFFFL)
    }
}

// OKLCH の値(h は度)
internal data class Oklch(val l: Double, val c: Double, val h: Double)

// 明度・彩度・色相の1つへの操作
internal data class ChannelOp(
    val set: Double? = null,
    val add: Double? = null,
    val scale: Double? = null,
    val max: Double? = null,
    val spread: Boolean = false,
)

// 色相のずらし幅の設定(中心の近くは 1 倍、離れるほど min 倍)
internal data class HueSpread(val center: Double, val plateau: Double, val width: Double, val min: Double) {

    // 色相に応じた倍率
    fun factor(hue: Double): Double {
        val d = abs(((hue - center + 540) % 360) - 180)
        val t = ((d - plateau) / width).coerceIn(0.0, 1.0)
        return min + (1 - min) * (1 + cos(PI * t)) / 2
    }
}

// 操作を値にかける。factor は spread のときの倍率
private fun ChannelOp?.applyTo(v: Double, factor: Double = 1.0): Double {
    if (this == null) return v
    var out = v
    if (set != null) out = set
    if (add != null) out += add * (if (spread) factor else 1.0)
    if (scale != null) out *= scale
    if (max != null) out = min(out, max)
    return out
}

// 0〜1 に収める
private fun clamp01(v: Double): Double = min(1.0, max(0.0, v))

// 0〜1 を 0〜255 の整数にする(JS の Math.round と同じ丸め)
private fun toByte(v: Double): Int = kotlin.math.floor(clamp01(v) * 255 + 0.5).toInt()

// sRGB の値を線形にする
private fun toLinear(v: Double): Double = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

// 線形の値を sRGB にする
private fun fromLinear(v: Double): Double = if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - 0.055

// sRGB を OKLCH にする
internal fun toOklch(c: Rgba): Oklch {
    val r = toLinear(c.r)
    val g = toLinear(c.g)
    val b = toLinear(c.b)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val labL = 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s
    val labA = 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s
    val labB = 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s
    var h = atan2(labB, labA) * 180 / PI
    if (h < 0) h += 360
    return Oklch(labL, hypot(labA, labB), h)
}

// OKLab を線形 RGB にする(範囲外もそのまま返す)
private fun oklabToLinear(l0: Double, a: Double, b: Double): DoubleArray {
    val l = (l0 + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m = (l0 - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s = (l0 - 0.0894841775 * a - 1.291485548 * b).pow(3)
    return doubleArrayOf(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
    )
}

// OKLCH を線形 RGB にする
private fun oklchToLinear(l: Double, c: Double, h: Double): DoubleArray {
    val rad = h * PI / 180
    return oklabToLinear(l, c * cos(rad), c * sin(rad))
}

// OKLCH が sRGB の範囲に収まるか
private fun inGamut(l: Double, c: Double, h: Double): Boolean {
    val e = 1e-6
    return oklchToLinear(l, c, h).all { it >= -e && it <= 1 + e }
}

// OKLCH を sRGB にする。範囲外は彩度を下げて収める
internal fun fromOklch(l: Double, c: Double, h: Double, a: Double = 1.0): Rgba {
    val lightness = clamp01(l)
    var chroma = max(0.0, c)
    if (!inGamut(lightness, chroma, h)) {
        var lo = 0.0
        var hi = chroma
        repeat(24) {
            val mid = (lo + hi) / 2
            if (inGamut(lightness, mid, h)) lo = mid else hi = mid
        }
        chroma = lo
    }
    val lin = oklchToLinear(lightness, chroma, h)
    return Rgba(clamp01(fromLinear(lin[0])), clamp01(fromLinear(lin[1])), clamp01(fromLinear(lin[2])), a)
}

// CSS の color-mix(in srgb, a p, b) と同じ混色
internal fun mix(a: Rgba, b: Rgba, p: Double): Rgba {
    val alpha = a.a * p + b.a * (1 - p)
    if (alpha == 0.0) return Rgba(0.0, 0.0, 0.0, 0.0)

    // 成分を1つ混ぜる
    fun ch(x: Double, y: Double) = (x * a.a * p + y * b.a * (1 - p)) / alpha
    return Rgba(ch(a.r, b.r), ch(a.g, b.g), ch(a.b, b.b), alpha)
}

// 上の色を下の色に重ねる(source-over)
internal fun over(top: Rgba, bottom: Rgba): Rgba {
    val alpha = top.a + bottom.a * (1 - top.a)
    if (alpha == 0.0) return Rgba(0.0, 0.0, 0.0, 0.0)

    // 成分を1つ重ねる
    fun ch(x: Double, y: Double) = (x * top.a + y * bottom.a * (1 - top.a)) / alpha
    return Rgba(ch(top.r, bottom.r), ch(top.g, bottom.g), ch(top.b, bottom.b), alpha)
}

// 相対輝度(WCAG)
internal fun luminance(c: Rgba): Double = 0.2126 * toLinear(c.r) + 0.7152 * toLinear(c.g) + 0.0722 * toLinear(c.b)

// コントラスト比(WCAG)
internal fun contrast(a: Rgba, b: Rgba): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

// 背景に対して比が足りるまで OKLCH の明度を動かす
internal fun ensureContrast(fg: Rgba, bg: Rgba, ratio: Double): Rgba {
    if (contrast(fg, bg) >= ratio) return fg
    val o = toOklch(fg)
    val darker = luminance(bg) > 0.18
    var lo = if (darker) 0.0 else o.l
    var hi = if (darker) o.l else 1.0
    repeat(24) {
        val mid = (lo + hi) / 2
        val ok = contrast(fromOklch(mid, o.c, o.h, fg.a).quantize(), bg) >= ratio
        if (darker) { if (ok) lo = mid else hi = mid } else { if (ok) hi = mid else lo = mid }
    }
    return fromOklch(if (darker) lo else hi, o.c, o.h, fg.a).quantize()
}

// 枠の色の1点(l は明度、c は彩度の比、h は色相差)
internal data class EdgeStop(val angle: Double, val l: Double, val c: Double, val h: Double)

// シードから枠の色の並びを作る(最後に最初の色で一周を閉じる)
internal fun edgeStops(seed: Rgba, chromaCap: Double, spread: HueSpread?, stops: List<EdgeStop>): List<Pair<Double, Rgba>> {
    val base = toOklch(seed)
    val chroma = min(base.c, chromaCap)
    val f = spread?.factor(base.h) ?: 1.0
    val out = stops.map { it.angle to fromOklch(it.l, it.c * chroma, base.h + it.h * f).quantize() }
    return out + (360.0 to out.first().second)
}
