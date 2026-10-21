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

    // ボタンの下に、ボタンの中心がメニューの右の角に来るよう少し重ねて置く
    @Test
    fun belowRightCorner() {
        val anchor = Rect(300f, 100f, 338f, 138f)
        assertEquals(Offset(169f, 128f), menuPosition(anchor, size, area, overlap = 10f, margin = 8f))
    }

    // 下に入らなければ、ボタンの上に少し重ねて置く
    @Test
    fun flipsAbove() {
        val anchor = Rect(300f, 700f, 338f, 738f)
        assertEquals(Offset(169f, 590f), menuPosition(anchor, size, area, overlap = 10f, margin = 8f))
    }

    // ボタンの中心をメニューの左の角、真ん中に置ける
    @Test
    fun alignsByBias() {
        val anchor = Rect(100f, 100f, 138f, 138f)
        assertEquals(119f, menuPosition(anchor, size, area, overlap = 10f, margin = 8f, align = PiyoMenuAlignment.StartCorner).x, 1e-4f)
        assertEquals(44f, menuPosition(anchor, size, area, overlap = 10f, margin = 8f, align = PiyoMenuAlignment.Center).x, 1e-4f)
    }

    // 右にはみ出すときは、画面の端から margin 空けて止める(左の角に置くとき)
    @Test
    fun clampsToRightMargin() {
        val anchor = Rect(330f, 100f, 368f, 138f)
        assertEquals(242f, menuPosition(anchor, size, area, overlap = 10f, margin = 8f, align = PiyoMenuAlignment.StartCorner).x, 1e-4f)
    }

    // 左にはみ出すときは、画面の端から margin 空けて止める
    @Test
    fun clampsToLeftMargin() {
        val anchor = Rect(10f, 100f, 48f, 138f)
        assertEquals(8f, menuPosition(anchor, size, area, overlap = 10f, margin = 8f).x, 1e-4f)
    }

    // 上にも下にも入らなければ、下に置いて画面の中に収める
    @Test
    fun clampsInsideWhenNoRoom() {
        val tall = Size(150f, 780f)
        val anchor = Rect(300f, 100f, 338f, 138f)
        assertEquals(12f, menuPosition(anchor, tall, area, overlap = 10f, margin = 8f).y, 1e-4f)
    }

    // 端どうしをそろえる
    @Test
    fun alignsEdges() {
        val anchor = Rect(100f, 100f, 138f, 138f)
        assertEquals(100f, menuPosition(anchor, size, area, overlap = 10f, margin = 8f, align = PiyoMenuAlignment.StartEdge).x, 1e-4f)
        assertEquals(8f, menuPosition(anchor, size, area, overlap = 10f, margin = 8f, align = PiyoMenuAlignment.EndEdge).x, 1e-4f)
    }

    // 縦に並んだボタンの右に出し、上の角にボタンの中心が来る
    @Test
    fun rightSideOfVerticalButtons() {
        val anchor = Rect(20f, 300f, 58f, 338f)
        val pos = menuPosition(anchor, size, area, overlap = 10f, margin = 8f, side = PiyoMenuSide.Right, align = PiyoMenuAlignment.StartCorner)
        assertEquals(Offset(48f, 319f), pos)
    }

    // 右に入らなければ左に出す
    @Test
    fun rightFlipsToLeft() {
        val anchor = Rect(340f, 300f, 378f, 338f)
        val pos = menuPosition(anchor, size, area, overlap = 10f, margin = 8f, side = PiyoMenuSide.Right, align = PiyoMenuAlignment.StartCorner)
        assertEquals(Offset(200f, 319f), pos)
    }
}
