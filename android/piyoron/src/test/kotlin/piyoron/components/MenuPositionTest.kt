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

    // 下に出せたら下、入らずに上へ出たら上と分かる
    @Test
    fun openedSideFollowsFlip() {
        val low = Rect(300f, 100f, 338f, 138f)
        val high = Rect(300f, 700f, 338f, 738f)
        val below = menuPosition(low, size, area, overlap = 10f, margin = 8f)
        val above = menuPosition(high, size, area, overlap = 10f, margin = 8f)
        assertEquals(PiyoMenuSide.Bottom, openedSide(low, below, size, PiyoMenuSide.Bottom))
        assertEquals(PiyoMenuSide.Top, openedSide(high, above, size, PiyoMenuSide.Bottom))
    }

    // ボタンを大きくした分だけ、メニューの側へずらす(大きさは変えない)
    @Test
    fun shiftsTowardMenu() {
        val anchor = Rect(100f, 50f, 138f, 88f)
        assertEquals(Rect(100f, 54f, 138f, 92f), shiftToward(anchor, PiyoMenuSide.Bottom, 4f))
        assertEquals(Rect(100f, 46f, 138f, 84f), shiftToward(anchor, PiyoMenuSide.Top, 4f))
        assertEquals(Rect(104f, 50f, 142f, 88f), shiftToward(anchor, PiyoMenuSide.Right, 4f))
        assertEquals(Rect(96f, 50f, 134f, 88f), shiftToward(anchor, PiyoMenuSide.Left, 4f))
    }

    // ずらしたボタンで置くと、メニューも同じだけ下へ動く
    @Test
    fun menuMovesWithGrownButton() {
        val anchor = Rect(300f, 100f, 338f, 138f)
        val shifted = shiftToward(anchor, PiyoMenuSide.Bottom, 4f)
        assertEquals(Offset(169f, 132f), menuPosition(shifted, size, area, overlap = 10f, margin = 8f))
    }

    // 拡大の中心は、触れる範囲の中の、ボタンの見た目の反対側の縁
    @Test
    fun growOriginOnOppositeEdge() {
        val box = Size(48f, 48f)
        val bottom = growOrigin(box, 38f, PiyoMenuSide.Bottom)
        assertEquals(0.5f, bottom.pivotFractionX, 0.001f)
        assertEquals(5f / 48f, bottom.pivotFractionY, 0.001f)
        assertEquals(1f - 5f / 48f, growOrigin(box, 38f, PiyoMenuSide.Top).pivotFractionY, 0.001f)
        assertEquals(5f / 48f, growOrigin(box, 38f, PiyoMenuSide.Right).pivotFractionX, 0.001f)
    }
}
