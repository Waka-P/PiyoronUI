package piyoron.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

// メニューの置き場所の計算のテスト
class MenuPositionTest {
    private val area = Size(400f, 800f)
    private val size = Size(150f, 120f)

    // ボタンの下に、右端をそろえて置く
    @Test
    fun belowRightAligned() {
        val anchor = Rect(300f, 100f, 338f, 138f)
        assertEquals(Offset(188f, 142f), menuPosition(anchor, size, area, gap = 4f, margin = 8f))
    }

    // 下に入らなければ、ボタンの上に置く
    @Test
    fun flipsAbove() {
        val anchor = Rect(300f, 700f, 338f, 738f)
        assertEquals(Offset(188f, 576f), menuPosition(anchor, size, area, gap = 4f, margin = 8f))
    }

    // 左にはみ出すときは、画面の端から margin 空けて止める
    @Test
    fun clampsToLeftMargin() {
        val anchor = Rect(10f, 100f, 48f, 138f)
        assertEquals(8f, menuPosition(anchor, size, area, gap = 4f, margin = 8f).x, 1e-4f)
    }

    // 上にも下にも入らなければ、下に置いて画面の中に収める
    @Test
    fun clampsInsideWhenNoRoom() {
        val tall = Size(150f, 780f)
        val anchor = Rect(300f, 100f, 338f, 138f)
        assertEquals(12f, menuPosition(anchor, tall, area, gap = 4f, margin = 8f).y, 1e-4f)
    }
}
