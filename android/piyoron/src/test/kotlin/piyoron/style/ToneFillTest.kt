package piyoron.style

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import piyoron.style.plain.plainToneFill

// 主な操作の色の並び(toneFill)から Plain の1色を決める
class ToneFillTest {

    // 真ん中の色を使い、マウスを乗せると少し暗くなる
    @Test
    fun middleAndHover() {
        val colors = listOf(Color.Red, Color.Green, Color.Blue)
        assertEquals(Color.Green, plainToneFill(colors, hovered = false))
        val hover = plainToneFill(colors, hovered = true)
        assertNotEquals(Color.Green, hover)
        assert(hover.green < Color.Green.green)
    }

    // 1色だけでも落ちない
    @Test
    fun singleColor() {
        assertEquals(Color.Blue, plainToneFill(listOf(Color.Blue), hovered = false))
    }
}
