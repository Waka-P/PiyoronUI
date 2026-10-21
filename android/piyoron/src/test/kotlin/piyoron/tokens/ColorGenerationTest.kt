package piyoron.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import piyoron.core.color.Rgba
import piyoron.core.color.contrast
import piyoron.core.color.over
import kotlin.math.abs

// 色生成のテスト(JS の結果と一致するか、コントラストを満たすか)
class ColorGenerationTest {

    // 各成分の差が1以内か
    private fun close(actual: Color, expected: Long): Boolean {
        val a = actual.toArgb()
        val e = expected.toInt()
        return (0 until 32 step 8).all { abs(((a shr it) and 0xFF) - ((e shr it) and 0xFF)) <= 1 }
    }

    // 色を色コードの文字にする
    private fun hex(v: Long): String = "#%08X".format(v and 0xFFFFFFFFL)

    // 期待値と食い違う名前を集める
    private fun <T> mismatches(value: T, getters: List<Getter<T>>, expected: Map<String, Long>): List<String> =
        getters.mapNotNull { g ->
            val want = expected[g.name] ?: return@mapNotNull "${g.name}: 期待値がない"
            val got = g.get(value)
            if (close(got, want)) null else "${g.name}: ${hex(got.toArgb().toLong())} != ${hex(want)}"
        }

    // JS と同じ role と Glass の色ができる
    @Test
    fun matchesJsFixtures() {
        val errors = ColorFixtures.cases.flatMap { c ->
            val roles = generateRoles(Color(c.seed), c.overrides.mapValues { Color(it.value) }, c.edge?.map { it.first to Color(it.second) }, c.toneUsesEdge)
            (mismatches(roles, ColorFixtures.roles, c.roles) + mismatches(glassColors(roles), ColorFixtures.glass, c.glass))
                .map { "${hex(c.seed)} $it" }
        }
        if (errors.isNotEmpty()) fail(errors.joinToString("\n"))
    }

    // どのシードでも文字のコントラストが 4.5:1 以上になる
    @Test
    fun contentMeetsContrast() {
        for (i in 0 until 360 step 15) {
            for (seed in listOf(hsv(i, 0.8, 0.8), hsv(i, 1.0, 1.0), hsv(i, 0.3, 0.5))) {
                val r = piyoColorRoles(seed)
                val fill = Rgba.of(r.fillBottom)
                val selected = Rgba.of(r.fillSelectedBottom)
                val surface = Rgba.of(r.surface)
                assertTrue(contrast(Rgba.of(r.contentDefault), over(fill, surface).quantize()) >= 4.5)
                assertTrue(contrast(Rgba.of(r.contentSelected), over(selected, surface).quantize()) >= 4.5)
            }
        }
    }

    // プリセットは全部作れて、Ocean は既定のシードと同じ
    @Test
    fun presets() {
        PiyoPreset.entries.forEach { it.roles() }
        assertTrue(PiyoPreset.Ocean.roles() == piyoColorRoles(Color(0xFF2293C7)))
    }

    // テスト用に HSV から色を作る
    private fun hsv(h: Int, s: Double, v: Double): Color = Color.hsv(h.toFloat(), s.toFloat(), v.toFloat())
}
