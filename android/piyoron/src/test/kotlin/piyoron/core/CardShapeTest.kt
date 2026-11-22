package piyoron.core

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import piyoron.tokens.PiyoTokens

class CardShapeTest {

    private val density = Density(1f)

    // 形を大きさに当てはめたときの角の半径(角がなければ 0)
    private fun radius(roundness: Float, width: Float, height: Float, growth: Float = PiyoTokens.Card.RoundnessGrowth): Float =
        when (val outline = piyoCardShape(roundness, growth).createOutline(Size(width, height), LayoutDirection.Ltr, density)) {
            is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
            else -> 0f
        }

    // 既定の度合いで、カタログのジニーの「お知らせ」(240x150、基準の大きさ)は角丸 20(Web の cardRadius と同じ)
    @Test
    fun baseCard() {
        assertEquals(20f, radius(PiyoTokens.Card.Roundness, 240f, 150f), 0.1f)
    }

    // 短い辺で決まる(向きを変えても同じ)
    @Test
    fun shorterSide() {
        assertEquals(radius(0.2f, 150f, 600f), radius(0.2f, 600f, 150f), 0.01f)
    }

    // 既定では、小さいカードは割合として丸く、大きいカードは割合として角ばる(半径はゆるやかに増える)
    @Test
    fun growsGently() {
        val small = radius(PiyoTokens.Card.Roundness, 300f, 60f)
        val large = radius(PiyoTokens.Card.Roundness, 400f, 250f)
        assertEquals(15.2f, small, 0.2f)
        assertEquals(23.3f, large, 0.2f)
        assert(small / 60f > large / 250f)
    }

    // growth が 1 なら短い辺に比例、0 なら固定の半径
    @Test
    fun growthEnds() {
        assertEquals(30f, radius(0.2f, 600f, 150f * 1f, growth = 1f) * 1f, 0.01f)
        assertEquals(40f, radius(0.2f, 600f, 200f, growth = 1f), 0.01f)
        assertEquals(30f, radius(0.2f, 600f, 200f, growth = 0f), 0.01f)
    }

    // 度合いは 0 から 0.5 に収め、半径は短い辺の半分まで
    @Test
    fun clamped() {
        assertEquals(40f, radius(0.9f, 100f, 80f), 0.01f)
        assertEquals(0f, radius(-1f, 100f, 80f), 0.01f)
    }

    // テーマの形の組から度合いを変えられる
    @Test
    fun themeShapes() {
        val outline = PiyoShapes(cardRoundness = 0.25f, cardRoundnessGrowth = 1f).card.createOutline(Size(200f, 100f), LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(25f, outline.roundRect.topLeftCornerRadius.x, 0.01f)
    }
}
