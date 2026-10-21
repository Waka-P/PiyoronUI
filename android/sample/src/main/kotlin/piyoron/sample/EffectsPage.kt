package piyoron.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import piyoron.components.PiyoButton
import piyoron.components.PiyoChipGroup
import piyoron.components.PiyoIconButton
import piyoron.core.LocalPiyoTextStyle
import piyoron.core.Piyo
import piyoron.core.PiyoText
import piyoron.core.PiyoTone
import piyoron.effects.genie.GenieVisibility
import piyoron.effects.genie.genieAnchor
import piyoron.effects.genie.rememberGenieAnchor
import piyoron.effects.gourd.GourdAlignment
import piyoron.effects.gourd.GourdRailPanel
import piyoron.effects.gourd.GourdSide
import piyoron.effects.gourd.GourdTab
import piyoron.effects.gourd.GourdTabRail

private val RailTabs = listOf(
    GourdTab(SampleIcons.Home, "ホーム"),
    GourdTab(SampleIcons.Search, "検索"),
    GourdTab(SampleIcons.Gear, "設定"),
)

// ひょうたんを置く側の選択肢
private val Sides = listOf("左" to GourdSide.Left, "右" to GourdSide.Right, "上" to GourdSide.Top, "下" to GourdSide.Bottom)

// 寄せ方の選択肢(名前は辺の向きで変える。最後は間の値の例)
private val VerticalAligns = listOf("上", "中央", "下", "カスタム")
private val HorizontalAligns = listOf("左", "真ん中", "右", "カスタム")
private val Aligns = listOf(GourdAlignment.Start, GourdAlignment.Center, GourdAlignment.End)

// ジニーとひょうたんの確認ページ
@Composable
fun EffectsPage() {
    Section("ひょうたんとパネル") {
        var side by rememberSaveable { mutableStateOf("左") }
        var align by rememberSaveable { mutableIntStateOf(0) }
        val gourdSide = Sides.first { it.first == side }.second
        val alignNames = if (gourdSide == GourdSide.Left || gourdSide == GourdSide.Right) VerticalAligns else HorizontalAligns
        Caption("表示中のボタンをもう一度押すと、パネルが開閉する")
        PiyoChipGroup(Sides.map { it.first }, side, { side = it })
        var custom by rememberSaveable { mutableFloatStateOf(0.5f) }
        PiyoChipGroup(alignNames, alignNames[align], { align = alignNames.indexOf(it) })
        if (align == 3) {
            BiasSlider(custom, { custom = it })
            Caption("GourdAlignment(%.2f)".format(custom))
        }
        RailWithPanel(gourdSide, if (align == 3) GourdAlignment(custom) else Aligns[align])
    }
    Section("ひょうたん単体") {
        var index by rememberSaveable { mutableIntStateOf(0) }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            GourdTabRail(RailTabs, index, { index = it })
            GourdTabRail(RailTabs, index, { index = it }, bouncy = true)
            Caption("選ぶと、印が伸びて移る。右は弾みあり(bouncy)")
        }
        GourdTabRail(RailTabs, index, { index = it }, orientation = Orientation.Horizontal)
        GourdTabRail(RailTabs, index, { index = it }, orientation = Orientation.Horizontal, bouncy = true)
        Caption("横向き(orientation = Horizontal)")
    }
    Section("ジニー") {
        GenieDemo()
    }
}

// ひょうたんとパネルの組み合わせ(side でひょうたんを置く側を選ぶ)
@Composable
private fun RailWithPanel(side: GourdSide, align: GourdAlignment) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var open by rememberSaveable { mutableStateOf(true) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(440.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Backdrop()
        GourdRailPanel(
            tabs = RailTabs,
            selectedIndex = index,
            onSelect = { index = it },
            panelOpen = open,
            onPanelOpenChange = { open = it },
            modifier = Modifier.fillMaxSize().padding(14.dp),
            side = side,
            railAlignment = align,
        ) {
            PanelContent(index)
        }
    }
}

// パネルの中身(選んだタブごと)
@Composable
private fun PanelContent(index: Int) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PiyoText(RailTabs[index].label, style = LocalPiyoTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        when (index) {
            0 -> {
                Caption("最近の項目")
                listOf("項目 A", "項目 B", "項目 C").forEach { PiyoText(it) }
                PiyoButton("追加", {}, icon = SampleIcons.Plus)
            }
            1 -> {
                var kind by rememberSaveable { mutableStateOf("すべて") }
                Caption("絞り込み")
                PiyoChipGroup(listOf("すべて", "画像"), kind, { kind = it })
                PiyoButton("検索", {}, tone = PiyoTone.Primary)
            }
            else -> {
                Caption("表示の設定")
                PiyoText("文字の大きさ、通知、色")
            }
        }
    }
}

// パネルの後ろの背景(格子と、ぼんやりした丸)
@Composable
private fun Backdrop() {
    val c = Piyo.colors
    Canvas(Modifier.fillMaxSize()) {
        drawRect(c.background)
        val step = 36.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(c.surfaceMuted, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(c.surfaceMuted, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            y += step
        }
        drawCircle(c.primary.copy(alpha = 0.18f), size.width * 0.28f, Offset(size.width * 0.8f, size.height * 0.25f))
        drawCircle(c.primary.copy(alpha = 0.12f), size.width * 0.35f, Offset(size.width * 0.3f, size.height * 0.85f))
    }
}

// ボタンに吸い込まれるカード
@Composable
private fun GenieDemo() {
    var visible by rememberSaveable { mutableStateOf(true) }
    val anchor = rememberGenieAnchor()
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        PiyoIconButton(
            SampleIcons.Sparkle,
            if (visible) "カードをしまう" else "カードを出す",
            { visible = !visible },
            modifier = Modifier.genieAnchor(anchor),
            tone = if (visible) PiyoTone.Default else PiyoTone.Primary,
        )
        Box(Modifier.size(width = 240.dp, height = 150.dp)) {
            GenieVisibility(visible, anchor, cornerRadius = 20.dp) {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PiyoText("お知らせ", style = LocalPiyoTextStyle.current.copy(fontWeight = FontWeight.Bold))
                    Caption("左のボタンを押すと、このカードがボタンに吸い込まれる")
                }
            }
        }
    }
}

// -1〜1 の値を選ぶスライドバー(サンプル用)
@Composable
internal fun BiasSlider(value: Float, onValue: (Float) -> Unit) {
    val c = Piyo.colors
    // 横の位置から値を求める
    fun toValue(x: Float, width: Float) = ((x / width).coerceIn(0f, 1f) * 2 - 1)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) { detectTapGestures { onValue(toValue(it.x, size.width.toFloat())) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> onValue(toValue(change.position.x, size.width.toFloat())) } },
    ) {
        val y = size.height / 2
        val x = (value + 1) / 2 * size.width
        drawLine(c.surfaceMuted, Offset(0f, y), Offset(size.width, y), 6.dp.toPx(), StrokeCap.Round)
        drawLine(c.primary, Offset(0f, y), Offset(x, y), 6.dp.toPx(), StrokeCap.Round)
        drawCircle(c.surface, 12.dp.toPx(), Offset(x, y))
        drawCircle(c.primary, 12.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
    }
}
