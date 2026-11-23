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

    // 横幅で決まり、中身で高さが変わっても角は変わらない
    @Test
    fun widthDecides() {
        assertEquals(radius(PiyoTokens.Card.Roundness, 380f, 120f), radius(PiyoTokens.Card.Roundness, 380f, 400f), 0.01f)
    }

    // 横幅が広いほど、半径はゆるやかに増える(既定)
    @Test
    fun growsGently() {
        assertEquals(23.0f, radius(PiyoTokens.Card.Roundness, 380f, 200f), 0.2f)
        assertEquals(20f, radius(PiyoTokens.Card.Roundness, 240f, 200f), 0.1f)
    }

    // 背の低いカードは、高さの 3 割までにする
    @Test
    fun heightCap() {
        assertEquals(18f, radius(PiyoTokens.Card.Roundness, 380f, 60f), 0.01f)
    }

    // growth が 1 なら横幅に比例、0 なら固定の半径
    @Test
    fun growthEnds() {
        assertEquals(40f, radius(0.1f, 400f, 300f, growth = 1f), 0.01f)
        assertEquals(24f, radius(0.1f, 400f, 300f, growth = 0f), 0.01f)
    }

    // 度合いは 0 から 0.5 に収める
    @Test
    fun clamped() {
        assertEquals(0f, radius(-1f, 100f, 80f), 0.01f)
    }

    // テーマの形の組から度合いを変えられる
    @Test
    fun themeShapes() {
        val outline = PiyoShapes(cardRoundness = 0.05f, cardRoundnessGrowth = 1f).card.createOutline(Size(200f, 100f), LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(10f, outline.roundRect.topLeftCornerRadius.x, 0.01f)
    }
}
