package piyoron.core

import android.graphics.Matrix
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.SweepGradient
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

// CSS の linear-gradient(角度) と同じ向きのブラシ
fun PiyoGradient.toBrush(size: Size): Brush {
    val rad = Math.toRadians(angle.toDouble())
    val dx = sin(rad).toFloat()
    val dy = -cos(rad).toFloat()
    val half = (abs(size.width * dx) + abs(size.height * dy)) / 2
    val center = Offset(size.width / 2, size.height / 2)
    return Brush.linearGradient(
        *stops.toTypedArray(),
        start = center - Offset(dx * half, dy * half),
        end = center + Offset(dx * half, dy * half),
    )
}

// CSS の conic-gradient(上が 0、時計回り)と同じ並びの塗り。start は継ぎ目の位置(0〜1)
// 継ぎ目から一周する並びに直し、右が 0 の SweepGradient を回して合わせる
private fun conicShader(stops: List<Pair<Float, Color>>, center: Offset, start: Float): android.graphics.Shader {
    val edge = colorAt(stops, start)
    val shifted = stops.map { (f, c) -> ((f - start + 1f) % 1f) to c }
        .filter { it.first > 1e-4f && it.first < 1f - 1e-4f }
        .sortedBy { it.first }
    val all = listOf(0f to edge) + shifted + listOf(1f to edge)
    return SweepGradient(
        center.x,
        center.y,
        IntArray(all.size) { all[it].second.toArgb() },
        FloatArray(all.size) { all[it].first },
    ).apply { setLocalMatrix(Matrix().apply { setRotate(start * 360f - 90f, center.x, center.y) }) }
}

// conic-gradient の枠を形の内側に描く
// 色は、面の大きさの画像に一度だけ描いて使い回す(色の多いスイープで毎コマ線を描くと、RenderThread で重い)
fun DrawScope.strokeConicInside(shape: Shape, width: Float, stops: List<Pair<Float, Color>>, alpha: Float = 1f) {
    val image = ConicImages.get(stops, size) ?: return
    strokeInside(shape, width, ShaderBrush(image), alpha)
}

// 面の大きさで描いたスイープの画像(新しく使ったものから順に、画素数の上限まで持つ)
private object ConicImages {
    private const val MAX_PIXELS = 6_000_000L
    private val map = LinkedHashMap<Pair<List<Pair<Float, Color>>, IntSize>, Pair<android.graphics.Shader, Long>>(16, 0.75f, true)
    private var pixels = 0L

    // 画像の塗りを返す(なければ描く)
    fun get(stops: List<Pair<Float, Color>>, size: Size): android.graphics.Shader? {
        val w = ceil(size.width).toInt()
        val h = ceil(size.height).toInt()
        if (w <= 0 || h <= 0) return null
        val key = stops to IntSize(w, h)
        map[key]?.let { return it.first }
        val shader = draw(stops, w, h)
        val count = w.toLong() * h
        map[key] = shader to count
        pixels += count
        val it = map.entries.iterator()
        while (pixels > MAX_PIXELS && map.size > 1 && it.hasNext()) {
            pixels -= it.next().value.second
            it.remove()
        }
        return shader
    }

    // 継ぎ目の画素が抜けないよう、上下で継ぎ目の位置を変えて描く。外へはみ出す形には、端の色を伸ばして使う
    private fun draw(stops: List<Pair<Float, Color>>, w: Int, h: Int): android.graphics.Shader {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = Offset(w / 2f, h / 2f)
        val paint = Paint()
        canvas.save()
        canvas.clipRect(0f, 0f, w.toFloat(), center.y)
        paint.shader = conicShader(stops, center, 0.5f)
        canvas.drawPaint(paint)
        canvas.restore()
        canvas.clipRect(0f, center.y, w.toFloat(), h.toFloat())
        paint.shader = conicShader(stops, center, 0f)
        canvas.drawPaint(paint)
        return BitmapShader(bitmap, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
    }
}

// 色の並びの途中の色
private fun colorAt(stops: List<Pair<Float, Color>>, f: Float): Color {
    val after = stops.indexOfFirst { it.first >= f }
    if (after < 0) return stops.last().second
    if (after == 0) return stops.first().second
    val (f0, c0) = stops[after - 1]
    val (f1, c1) = stops[after]
    return if (f1 == f0) c1 else mixSrgb(c1, c0, (f - f0) / (f1 - f0))
}

// 2つの色の並びを、同じ角度どうしで混ぜる(t は b の割合)
fun blendStops(a: List<Pair<Float, Color>>, b: List<Pair<Float, Color>>, t: Float): List<Pair<Float, Color>> =
    (a.map { it.first } + b.map { it.first }).distinct().sorted().map { f -> f to mixSrgb(colorAt(b, f), colorAt(a, f), t) }

// CSS の color-mix(in srgb, a p, b) と同じ混色
fun mixSrgb(a: Color, b: Color, p: Float): Color {
    val alpha = a.alpha * p + b.alpha * (1 - p)
    if (alpha == 0f) return Color.Transparent
    val r = (a.red * a.alpha * p + b.red * b.alpha * (1 - p)) / alpha
    val g = (a.green * a.alpha * p + b.green * b.alpha * (1 - p)) / alpha
    val bl = (a.blue * a.alpha * p + b.blue * b.alpha * (1 - p)) / alpha
    return Color(r, g, bl, alpha)
}

// 形の内側に収まる線を描く(任意の形は、縮めた輪郭を作り直さず、輪郭で切り抜いて太さ2倍の線を描く)
fun DrawScope.strokeInside(shape: Shape, width: Float, brush: Brush, alpha: Float = 1f) {
    val full = shape.createOutline(size, layoutDirection, this)
    if (full is Outline.Generic) {
        clipPath(full.path) { drawPath(full.path, brush, alpha, Stroke(width * 2)) }
        return
    }
    val outline = shape.createOutline(Size(size.width - width, size.height - width), layoutDirection, this)
    translate(width / 2, width / 2) { drawOutline(outline, brush, alpha, Stroke(width)) }
}

// 形に沿った線を、ずらしてぼかして描く(線状の影)
// ぼかしは、太さと濃さの違う線を重ねて近づける。BlurMaskFilter は形が変わるたびに CPU で作り直すので、動く形では重い
fun DrawScope.blurredRing(shape: Shape, width: Dp, blur: Dp, offset: DpOffset, color: Color) {
    val w = width.toPx()
    val sigma = blur.toPx()
    val full = shape.createOutline(size, layoutDirection, this)
    val outline = if (full is Outline.Generic) full else shape.createOutline(Size(size.width - w, size.height - w), layoutDirection, this)
    val inset = if (full is Outline.Generic) 0f else w / 2
    translate(offset.x.toPx() + inset, offset.y.toPx() + inset) {
        RingLayers.forEach { (spread, weight) ->
            drawOutline(outline, color.copy(alpha = color.alpha * weight), style = Stroke(w + sigma * spread))
        }
    }
}

// 近似のぼかしの線の重ね方(標準偏差に対する太さの足し分、濃さ)。重ねた中心の濃さが、ぼかした細い線の山と同じくらいになる
private val RingLayers = listOf(4f to 0.12f, 2f to 0.16f, 0f to 0.2f)

// 外側だけに影を描く(CSS の box-shadow と同じく形の内側には出さない)
// ぼかしは、少しずつ広げた形を薄く重ねて近づける(線状の影と同じ理由で BlurMaskFilter は使わない)
// ただし大きなぼかしは重ねた段が見えるので、本物のぼかしで描く(メニューの影など。形が変わらない面に使う)
fun DrawScope.outerShadow(shape: Shape, shadow: PiyoShadow) {
    val spread = shadow.spread.toPx()
    val sigma = shadow.blur.toPx() / 2
    val inner = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@outerShadow)) }
    clipPath(inner, ClipOp.Difference) {
        if (shadow.blur > SMOOTH_BLUR_FROM) {
            val outline = shape.createOutline(Size(size.width + spread * 2, size.height + spread * 2), layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            translate(shadow.offsetX.toPx() - spread, shadow.offsetY.toPx() - spread) {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        isAntiAlias = true
                        color = shadow.color.toArgb()
                        maskFilter = BlurMaskFilter(blurRadius(sigma), BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
                }
            }
            return@clipPath
        }
        ShadowLayers.forEach { (grow, weight) ->
            val e = spread + sigma * grow
            val outline = shape.createOutline(Size(size.width + e * 2, size.height + e * 2), layoutDirection, this)
            translate(shadow.offsetX.toPx() - e, shadow.offsetY.toPx() - e) {
                drawOutline(outline, shadow.color.copy(alpha = shadow.color.alpha * weight))
            }
        }
    }
}

// これより大きなぼかしは、本物のぼかしで描く
private val SMOOTH_BLUR_FROM = 8.dp

// ぼかしの標準偏差を、BlurMaskFilter の半径にする(Skia の換算 σ = 0.57735r + 0.5 の逆)
private fun blurRadius(sigma: Float): Float = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.5f)

// 近似のぼかしの影の重ね方(標準偏差に対する広げ幅、濃さ)
private val ShadowLayers = listOf(1.5f to 0.25f, 0.75f to 0.25f, 0f to 0.3f)

// 毎コマ形が変わる形の目印。内側の影を軽い近似で描く(Compose の innerShadow は形が変わるたびにぼかしを作り直して重い)
interface PiyoChangingShape : Shape

// 内側の影を、ずらした向きの縁から内へ薄れるグラデーションで形を塗って近づける(切り抜きを使わない)
// ずらした向きと反対側の縁に影が出る。縁での濃さと届く幅は、ぼかしとずれから決める
fun DrawScope.innerShadowApprox(shape: Shape, shadow: PiyoShadow) {
    val dx = shadow.offsetX.toPx()
    val dy = shadow.offsetY.toPx()
    if (dx == 0f && dy == 0f) return
    val reach = abs(if (abs(dy) >= abs(dx)) dy else dx) + shadow.blur.toPx() * INSET_REACH
    val color = shadow.color
    val stops = arrayOf(0f to color.copy(alpha = color.alpha * INSET_EDGE), 1f to color.copy(alpha = 0f))
    val brush = when {
        abs(dy) >= abs(dx) && dy > 0f -> Brush.verticalGradient(*stops, startY = 0f, endY = reach)
        abs(dy) >= abs(dx) -> Brush.verticalGradient(*stops, startY = size.height, endY = size.height - reach)
        dx > 0f -> Brush.horizontalGradient(*stops, startX = 0f, endX = reach)
        else -> Brush.horizontalGradient(*stops, startX = size.width, endX = size.width - reach)
    }
    drawOutline(shape.createOutline(size, layoutDirection, this), brush)
}

// 近似の内側の影の、縁での濃さ(影の色に対する割合)と、ぼかしに対する届く幅
private const val INSET_EDGE = 0.6f
private const val INSET_REACH = 0.75f

// トークンの影を Compose の影にする
fun PiyoShadow.toShadow(): Shadow = Shadow(radius = blur, color = color, spread = spread, offset = DpOffset(offsetX, offsetY))
