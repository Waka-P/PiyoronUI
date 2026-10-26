package piyoron.core

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import piyoron.tokens.PiyoTokens

// 長押ししたまま指を動かして選ぶ項目を移す仕組み(タブ、ひょうたん、メニューなどで共有する)
// 入れ物に Modifier.dragSelect、項目に Modifier.dragSelectItem を付ける。動かしている間は preview が指の下の項目になる
@Stable
class DragSelectState internal constructor() {
    // 項目(画面から見た範囲と、決めたときの動き)
    internal val items = LinkedHashMap<Any, DragSelectItem>()

    // 入れ物の画面から見た範囲
    internal var container: Rect = Rect.Zero

    // 長押しで動かしている間の、指の下の項目の key(動かしていないとき、指が入れ物から離れたときは null)
    var preview: Any? by mutableStateOf(null)
        internal set

    // 長押しで動かしている途中か
    var active: Boolean by mutableStateOf(false)
        internal set

    // 入れ物から見た位置にいちばん近い、選べる項目(入れ物から離れすぎていれば null)
    internal fun hit(local: Offset, tolerance: Float): Any? {
        val p = local + container.topLeft
        if (!container.inflate(tolerance).contains(p)) return null
        return items.entries
            .filter { it.value.enabled }
            .minByOrNull { (_, item) ->
                val b = item.bounds
                val dx = maxOf(b.left - p.x, 0f, p.x - b.right)
                val dy = maxOf(b.top - p.y, 0f, p.y - b.bottom)
                dx * dx + dy * dy
            }?.key
    }
}

// 項目1つ(画面から見た範囲、選べるか、決めたときの動き)
internal class DragSelectItem(var bounds: Rect, var enabled: Boolean, var onSelect: () -> Unit)

// 長押しで選ぶ仕組みの状態を作る
@Composable
fun rememberDragSelectState(): DragSelectState = remember { DragSelectState() }

// 入れ物に付ける。長押しで始め、指の下の項目を preview にし、離した所の項目を選ぶ(入れ物から離れて離すと取り消す)
// 動かしている間は指の動きを先に受け取って消費する(項目のクリックや、画面のスクロールが反応しないように)
@Composable
fun Modifier.dragSelect(state: DragSelectState, enabled: Boolean = true): Modifier {
    val haptic = LocalHapticFeedback.current
    val currentHaptic by rememberUpdatedState(haptic)
    return this
        .onGloballyPositioned { state.container = it.boundsInRoot() }
        .pointerInput(state, enabled) {
            if (!enabled) return@pointerInput
            val tolerance = DragTolerance.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                // 長押しになるまで待つ(その前に離すか、指が動いたらやめる)
                val stopped = withTimeoutOrNull(PiyoTokens.Press.LongPressDelay.toLong()) {
                    var stop = false
                    while (!stop) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                        stop = change == null || !change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    }
                    true
                }
                if (stopped == true) return@awaitEachGesture
                currentHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.active = true
                state.preview = state.hit(down.position, tolerance)
                var released = false
                try {
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        state.preview = state.hit(change.position, tolerance)
                        if (!change.pressed) {
                            released = true
                            break
                        }
                    }
                } finally {
                    val target = if (released) state.preview else null
                    state.active = false
                    state.preview = null
                    target?.let { state.items[it]?.onSelect?.invoke() }
                }
            }
        }
}

// 項目に付ける。key は項目を見分ける値、onSelect は離して決めたときの動き
@Composable
fun Modifier.dragSelectItem(state: DragSelectState?, key: Any, enabled: Boolean = true, onSelect: () -> Unit): Modifier {
    if (state == null) return this
    val currentSelect by rememberUpdatedState(onSelect)
    DisposableEffect(state, key) {
        onDispose { state.items.remove(key) }
    }
    SideEffect { state.items[key]?.enabled = enabled }
    return onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        val item = state.items[key]
        if (item == null) {
            state.items[key] = DragSelectItem(bounds, enabled) { currentSelect() }
        } else {
            item.bounds = bounds
            item.enabled = enabled
        }
    }
}

// 指が入れ物からこれ以上離れたら、指の下の項目をなしにする
private val DragTolerance = 24.dp
