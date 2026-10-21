package piyoron.sample

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// サンプル用の線のアイコン(24x24 の SVG のパス)
object SampleIcons {
    val Plus = lineIcon("plus", "M12 5v14M5 12h14")
    val Check = lineIcon("check", "M5 12.5l5 5L19 7")
    val Trash = lineIcon("trash", "M4 7h16M9 7V4.5h6V7M6.5 7l1 12.5h9l1-12.5")
    val Star = lineIcon("star", "M12 4l2.4 5.2 5.6.6-4.2 3.8 1.2 5.6-5-2.8-5 2.8 1.2-5.6L4 9.8l5.6-.6z")
    val Home = lineIcon("home", "M4 10.5L12 4l8 6.5V20h-5v-6H9v6H4z")
    val Search = lineIcon("search", "M17 10.5a6.5 6.5 0 1 1-13 0 6.5 6.5 0 0 1 13 0zM15.2 15.2L20 20")
    val Gear = lineIcon("gear", "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1")
    val Sparkle = lineIcon("sparkle", "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z")
}

// SVG のパスから線のアイコンを作る
private fun lineIcon(name: String, d: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
        pathData = addPathNodes(d),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
