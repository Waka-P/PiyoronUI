package piyoron.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import piyoron.core.keyframes
import piyoron.effects.genie.GenieFrame
import piyoron.effects.gourd.GourdProfile
import piyoron.effects.gourd.gourdSegments
import piyoron.effects.gourd.seamAt

// ひょうたんの形と、開閉の動きの計算のテスト
class EffectsTest {

    // ひょうたんの区間がつながって一周し、幅と高さに収まる
    @Test
    fun gourdIsClosedAndInBounds() {
        for (n in 1..4) {
            val w = 60f
            val h = n * 48f + (n - 1) * 12f + 12f
            val segs = gourdSegments(w, h, n)
            segs.zipWithNext().forEach { (a, b) -> assertEquals(a.p3, b.p0) }
            assertEquals(segs.last().p3, segs.first().p0)
            val points = segs.flatMap { listOf(it.p0, it.p3) }
            assertTrue(points.all { it.x in -0.01f..w + 0.01f && it.y in -0.01f..h + 0.01f })
            assertEquals(w / 2, segs.first().p0.x, 0.001f)
        }
    }

    // くびれは丸より細い
    @Test
    fun gourdHasNeck() {
        val segs = gourdSegments(60f, 120f, 2)
        val neck = segs.filter { it.p0.y in 40f..80f && it.p0.x > 30f }.minOf { it.p0.x }
        assertTrue(neck < 60f)
    }

    // 継ぎ目は、閉じ終わりで縮んで丸く、開き終わりで伸びて角がある
    @Test
    fun seamEnds() {
        val closed = seamAt(1f, closing = true)
        assertEquals(0f, closed.extend, 1e-4f)
        assertEquals(1f, closed.round, 1e-4f)
        val open = seamAt(1f, closing = false)
        assertEquals(1f, open.extend, 1e-4f)
        assertEquals(0f, open.round, 1e-4f)
        val start = seamAt(0f, closing = true)
        assertEquals(Pair(1f, 0f), Pair(start.extend, start.round))
    }

    // 区切りの値を直線でつなぐ
    @Test
    fun keyframesInterpolate() {
        val offsets = floatArrayOf(0f, 0.5f, 1f)
        val values = floatArrayOf(1f, 0.55f, 0.02f)
        assertEquals(1f, keyframes(0f, offsets, values), 1e-6f)
        assertEquals(0.775f, keyframes(0.25f, offsets, values), 1e-6f)
        assertEquals(0.02f, keyframes(1f, offsets, values), 1e-6f)
        assertEquals(0.02f, keyframes(2f, offsets, values), 1e-6f)
    }

    // 内側の輪郭の表は、丸の中心で半径、くびれで細く、両端で 0
    @Test
    fun gourdProfileWidths() {
        val profile = GourdProfile(60f, 132f, 2)
        assertEquals(30f, profile.halfWidth(30f), 0.05f)
        assertEquals(30f, profile.halfWidth(102f), 0.05f)
        assertTrue(profile.halfWidth(66f) < 23f)
        assertEquals(0f, profile.halfWidth(0f), 1e-4f)
        assertEquals(0f, profile.halfWidth(132f), 1e-4f)
    }

    // ジニーの面の形は、進行度 0 で元の四角、1 で行き先の近くに縮む
    @Test
    fun genieFrameEnds() {
        val size = Size(300f, 400f)
        val target = Offset(-20f, 100f)
        val open = GenieFrame(0f, size, target, 16f, 16f)
        assertEquals(0f, open.placeX(0f), 0.01f)
        assertEquals(300f, open.placeX(300f), 0.01f)
        assertEquals(Pair(0f, 400f), Pair(open.a, open.d))
        val closed = GenieFrame(1f, size, target, 16f, 16f)
        assertEquals(-20f, closed.placeX(0f), 0.01f)
        assertTrue(closed.placeX(300f) - closed.placeX(0f) < 10f)
        assertEquals(84f, closed.a, 0.01f)
        assertEquals(116f, closed.d, 0.01f)
    }
}
