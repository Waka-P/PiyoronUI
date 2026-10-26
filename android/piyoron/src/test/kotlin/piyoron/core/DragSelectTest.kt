package piyoron.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 長押ししたまま動かして選ぶときの、指の下の項目の求め方のテスト
class DragSelectTest {

    // 横に3つ並んだ項目(入れ物は 300x40、画面の (100, 200) にある)を持つ状態を作る
    private fun state(): DragSelectState = DragSelectState().apply {
        container = Rect(100f, 200f, 400f, 240f)
        listOf(0, 1, 2).forEach { i -> items[i] = DragSelectItem(Rect(100f + i * 100f, 200f, 200f + i * 100f, 240f), true) {} }
    }

    // 項目の上なら、その項目
    @Test
    fun hitsItemUnderFinger() {
        assertEquals(1, state().hit(Offset(150f, 20f), 24f))
    }

    // 入れ物の少し外(許す幅の内側)なら、いちばん近い項目
    @Test
    fun nearestWhenSlightlyOutside() {
        assertEquals(2, state().hit(Offset(310f, 55f), 24f))
    }

    // 入れ物から離れすぎたら、なし
    @Test
    fun noneWhenFarOutside() {
        assertNull(state().hit(Offset(150f, 100f), 24f))
    }

    // 選べない項目は飛ばし、次に近い項目
    @Test
    fun skipsDisabledItems() {
        val s = state()
        s.items[1]?.enabled = false
        assertEquals(0, s.hit(Offset(140f, 20f), 24f))
    }
}
