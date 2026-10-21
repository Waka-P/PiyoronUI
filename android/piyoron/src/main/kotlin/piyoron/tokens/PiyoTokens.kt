// 自動生成。手で書き換えない。元は tokens/(npm run tokens で作り直す)
package piyoron.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 全スタイル共通の値(寸法は dp、文字は sp、時間はミリ秒)
object PiyoTokens {
    object Radius {
        val Sm = 8.dp
        val Md = 12.dp
        val Lg = 20.dp
        val Pill = 999.dp
    }

    object Space {
        val Xs = 4.dp
        val Sm = 8.dp
        val Md = 12.dp
        val Lg = 16.dp
        val Xl = 20.dp
        val Xxl = 24.dp
    }

    object Duration {
        val Fast = 150
        val Toggle = 180
        val Segmented = 250
    }

    object Easing {
        val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    }

    object Font {
        val Family = listOf("Zen Kaku Gothic New", "Hiragino Sans", "Meiryo", "sans-serif")
    }

    object Press {
        val HoverScale = 1.04f
        val ActiveShrink = 0.03f
        val TouchShrink = 0.04f
        val HoverDuration = 180
        val ActiveDuration = 70
    }

    object Touch {
        val MinSize = 48.dp
    }

    object Text {
        val Size = 14.sp
        val SizeSmall = 13.sp
        val Weight = FontWeight(500)
    }

    object Control {
        val Height = 38.dp
        val PaddingX = 16.dp
        val Gap = 6.dp
        val IconSize = 20.dp
        val DisabledOpacity = 0.45f
        val FocusWidth = 2.dp
        val FocusOffset = 2.dp
    }

    object Chip {
        val PaddingX = 14.dp
        val PaddingY = 6.dp
        val Gap = 8.dp
    }

    object Card {
        // カードの左右の余白(参照実装の 1.5rem)
        val PaddingX = 24.dp
        // カードの上下の余白(参照実装の 1.3rem)
        val PaddingY = 20.8.dp
    }

    object Tabs {
        val TrackPadding = 3.dp
        val TrackBorder = 1.dp
        val ItemPaddingX = 4.dp
        val ItemPaddingY = 7.dp
    }

    object Genie {
        val Duration = 340
        val Easing = CubicBezierEasing(0.45f, 0f, 0.8f, 0.4f)
        val Neck = 16.dp
    }

    object Gourd {
        val ButtonSize = 48.dp
        val Gap = 12.dp
        val Padding = 6.dp
        val IconSize = 22.dp
        val Fillet = 12.dp
        // ひょうたんの左端からパネルの左端まで(ボタンの大きさ / 1.1 + 余白)
        val PanelLeft = 49.6.dp
        val SwitchDuration = 380
        // 印の前の端と後ろの端の動き(初動はゆっくり)
        val SwitchEasing = CubicBezierEasing(0.7f, 0f, 0.4f, 1f)
        // 印の後ろの端が前の端より遅れて動き出す時点(移動の時間に対する割合)
        val Trail = 0.2f
        // 弾みありの印の移動の時間
        val BounceDuration = 560
        // 伸びた印の最も細い幅(ボタンの大きさに対する割合)
        val Waist = 0.7f
        // 弾みを付けたとき、伸びる前に印が縮んで後ろへためる量
        val Anticipation = 4.dp
        // 弾みを付けたとき、印の先が行き先を通り過ぎる量
        val Overshoot = 5.dp
        // 印をくびれに合わせるときの、ひょうたんの縁からの余白
        val FitInset = 4.dp
        // 移った先のアイコンの色が変わる時点(移動の時間に対する割合)
        val ArriveAt = 0.55f
    }
}

// Glass スタイルの、色によらない値
object GlassTokens {
    object Edge {
        val Width = 1.dp
    }

    object Ring {
        val Width = 1.dp
        val Blur = 1.dp
        val OffsetX = 1.dp
        val OffsetY = 2.dp
    }

    object Backdrop {
        val Blur = 12.dp
        val Saturate = 1.4f
    }

    object Chip {
        val TintRest = 0.03f
        val TintHover = 0.08f
        val EdgeRest = 0.26f
        val EdgeHover = 0.6f
        val EdgeSideRatio = 0.65f
        val EdgeAngle = 100f
        val Border = 1.dp
        val RingWidth = 0.8.dp
        val RingBlur = 0.8.dp
        val RingOffsetX = 0.5.dp
        val RingOffsetY = 0.8.dp
    }

    object Tabs {
        // 枠を通常ボタン(0)と選んだチップ(1)のどちらに寄せるか
        val ThumbEdgeBlend = 0.5f
        val TrackEdgeOpacity = 0.3f
        val RingBlur = 0.7.dp
        val RingOffsetX = 1.dp
        val RingOffsetY = 1.5.dp
    }

    object Card {
        val EdgeOpacity = 0.7f
        val RingWidth = 0.5.dp
        val RingBlur = 1.dp
        val RingOffsetX = 2.dp
        val RingOffsetY = 2.dp
        val BackdropBlur = 14.dp
        val BackdropSaturate = 1.5f
    }
}
