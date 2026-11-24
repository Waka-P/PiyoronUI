package piyoron.core

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

// 押せるもの以外の所を押したら、入力欄のフォーカスを外す(キーボードも閉じる)
// PiyoTheme が画面の土台に付ける。ダイアログなど別の窓では、その中身に付ける
@Composable
fun Modifier.piyoClearFocusOnTap(): Modifier {
    val focus = LocalFocusManager.current
    return pointerInput(focus) { detectTapGestures(onTap = { focus.clearFocus() }) }
}
