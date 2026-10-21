package piyoron.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import piyoron.style.glass.GlassStyle

// 面の種類。スタイルはこれを見て描き分ける
enum class SurfaceRole { Button, Chip, TabTrack, TabThumb, Card, Field, Menu, MenuItem, Divider }

// 色の調子
enum class PiyoTone { Default, Primary, Danger }

// 部品の状態
@Immutable
data class PiyoState(
    val hovered: Boolean = false,
    val pressed: Boolean = false,
    val focused: Boolean = false,
    val selected: Boolean = false,
    val enabled: Boolean = true,
)

// 見た目のスタイル。部品は面の描画と文字色をここに頼む
@Stable
interface PiyoStyle {

    // 面(塗り、枠、影)を描く
    @Composable
    fun Modifier.surface(shape: Shape, role: SurfaceRole, state: PiyoState, tone: PiyoTone): Modifier

    // 面の上の文字・アイコンの色
    @Composable
    fun contentColor(role: SurfaceRole, state: PiyoState, tone: PiyoTone): Color

    companion object {
        val Glass: PiyoStyle = GlassStyle
    }
}

// 現在のスタイルで面を描く
@Composable
fun Modifier.piyoSurface(
    shape: Shape,
    role: SurfaceRole,
    state: PiyoState = PiyoState(),
    tone: PiyoTone = PiyoTone.Default,
): Modifier = with(Piyo.style) { this@piyoSurface.surface(shape, role, state, tone) }
