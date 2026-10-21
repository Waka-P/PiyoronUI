package piyoron.core

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

// 最前面に重ねるもの1つ(メニューなど)
// content は層に描く中身。press は層のどこかが押されたとき(層から見た位置)。open は開いているか、dismiss は閉じる
internal class OverlayEntry(
    val content: @Composable () -> Unit,
    val press: (Offset) -> Unit,
    val open: () -> Boolean,
    val dismiss: () -> Unit,
)

// 最前面の層。PiyoTheme が持ち、中身の上に重ねて描く。押す操作は横取りせず、見るだけ
@Stable
internal class PiyoOverlay {
    internal val entries = mutableStateListOf<OverlayEntry>()

    // 層の左上(画面の左上から見た位置)
    var origin by mutableStateOf(Offset.Zero)
        internal set

    // 画面の左上から見た位置を、層から見た位置にする
    fun toLocal(root: Offset): Offset = root - origin

    // 開いているものがあるか
    val anyOpen: Boolean get() = entries.any { it.open() }

    // 開いているものを全部閉じる
    fun dismissAll() = entries.toList().forEach { if (it.open()) it.dismiss() }
}

// 今の最前面の層
internal val LocalPiyoOverlay = staticCompositionLocalOf<PiyoOverlay?> { null }

// 中身の上に最前面の層を重ねる(外側にすでに層があれば、それを使う)
@Composable
internal fun PiyoOverlayHost(content: @Composable () -> Unit) {
    if (LocalPiyoOverlay.current != null) {
        content()
        return
    }
    val overlay = remember { PiyoOverlay() }
    CompositionLocalProvider(LocalPiyoOverlay provides overlay) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { overlay.origin = it.positionInRoot() }
                .pointerInput(overlay) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) {
                                val at = event.changes.first().position
                                overlay.entries.toList().forEach { it.press(at) }
                            }
                        }
                    }
                }
                .onPreviewKeyEvent {
                    if (it.key == Key.Escape && it.type == KeyEventType.KeyDown && overlay.anyOpen) {
                        overlay.dismissAll()
                        true
                    } else {
                        false
                    }
                },
            propagateMinConstraints = true,
        ) {
            content()
            Box(Modifier.matchParentSize()) {
                overlay.entries.forEach { entry -> key(entry) { entry.content() } }
            }
        }
        BackHandler(enabled = overlay.anyOpen) { overlay.dismissAll() }
    }
}
