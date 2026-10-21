package piyoron.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import piyoron.components.PiyoButton
import piyoron.components.PiyoCard
import piyoron.components.PiyoMenu
import piyoron.components.PiyoMenuDivider
import piyoron.components.PiyoMenuGroup
import piyoron.components.PiyoMenuItem
import piyoron.components.PiyoChip
import piyoron.components.PiyoChipGroup
import piyoron.components.PiyoIconButton
import piyoron.components.PiyoMultiChipGroup
import piyoron.components.PiyoTabs
import piyoron.core.LocalPiyoTextStyle
import piyoron.core.Piyo
import piyoron.core.PiyoText
import piyoron.core.PiyoTheme
import piyoron.core.PiyoTone
import piyoron.core.systemReduceMotion
import piyoron.tokens.PiyoPreset

private const val CUSTOM = "カスタム"

// 色を選べる、全部品のカタログ
@Composable
fun CatalogScreen() {
    var palette by rememberSaveable { mutableStateOf(PiyoPreset.Ocean.name) }
    var hue by rememberSaveable { mutableFloatStateOf(265f) }
    var reduce by rememberSaveable { mutableStateOf(false) }
    val reduceMotion = reduce || systemReduceMotion()
    val content: @Composable () -> Unit = {
        CatalogContent(palette, { palette = it }, hue, { hue = it }, reduce, { reduce = it })
    }
    val preset = PiyoPreset.entries.firstOrNull { it.name == palette }
    if (preset != null) PiyoTheme(preset = preset, reduceMotion = reduceMotion, content = content)
    else PiyoTheme(seed = seedFromHue(hue), reduceMotion = reduceMotion, content = content)
}

// 色相からシード色を作る
private fun seedFromHue(hue: Float): Color = Color.hsv(hue, 0.72f, 0.78f)

// カタログの中身
@Composable
private fun CatalogContent(
    palette: String,
    onPalette: (String) -> Unit,
    hue: Float,
    onHue: (Float) -> Unit,
    reduce: Boolean,
    onReduce: (Boolean) -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Piyo.colors.background)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PiyoText("Piyoron カタログ", style = LocalPiyoTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))

        Section("色") {
            PiyoChipGroup(PiyoPreset.entries.map { it.name } + CUSTOM, palette, onPalette)
            if (palette == CUSTOM) HueBar(hue, onHue)
            Swatches()
            PiyoChip("動きを減らす", reduce, { onReduce(!reduce) }, icon = SampleIcons.Sparkle)
        }

        PiyoTabs(listOf("部品", "エフェクト"), page, { page = it }, Modifier.fillMaxWidth())

        if (page == 1) {
            EffectsPage()
            return@Column
        }

        Section("ボタン") {
            var count by remember { mutableIntStateOf(0) }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                PiyoIconButton(SampleIcons.Plus, "追加", { count++ }, size = 64.dp, iconSize = 30.dp)
                PiyoIconButton(SampleIcons.Plus, "追加", { count++ })
                PiyoIconButton(SampleIcons.Check, "決定", { count++ }, tone = PiyoTone.Primary)
                PiyoIconButton(SampleIcons.Trash, "削除", { count++ }, tone = PiyoTone.Danger)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PiyoButton("保存", { count++ })
                PiyoButton("お気に入り", { count++ }, icon = SampleIcons.Star)
                PiyoButton("確定", { count++ }, tone = PiyoTone.Primary)
                PiyoButton("削除", { count++ }, icon = SampleIcons.Trash, tone = PiyoTone.Danger)
                PiyoButton("無効", { count++ }, enabled = false)
                PiyoButton("無効", { count++ }, tone = PiyoTone.Primary, enabled = false)
            }
            Caption("押した回数: $count")
        }

        Section("選ぶボタン") {
            var sort by rememberSaveable { mutableStateOf("新しい順") }
            var size by rememberSaveable { mutableStateOf("中") }
            var theme by rememberSaveable { mutableStateOf("オフ") }
            var extras by remember { mutableStateOf(setOf("画像")) }
            Caption("並び順")
            PiyoChipGroup(listOf("新しい順", "古い順"), sort, { sort = it })
            Caption("文字の大きさ")
            PiyoChipGroup(listOf("小", "中", "大"), size, { size = it })
            Caption("通知")
            PiyoChipGroup(listOf("オン", "オフ"), theme, { theme = it })
            Caption("複数選択")
            PiyoMultiChipGroup(listOf("文書", "画像", "動画"), extras, { extras = it })
            Caption("無効")
            PiyoChipGroup(listOf("選べない", "選択中"), "選択中", {}, enabled = false)
        }

        Section("タブ") {
            var kind by rememberSaveable { mutableIntStateOf(0) }
            var mode by rememberSaveable { mutableIntStateOf(1) }
            var view by rememberSaveable { mutableIntStateOf(0) }
            PiyoTabs(listOf("すべて", "未読", "既読", "保存"), kind, { kind = it }, Modifier.fillMaxWidth())
            PiyoTabs(listOf("カード", "一覧"), mode, { mode = it }, Modifier.fillMaxWidth())
            PiyoTabs(listOf("今日", "今週", "今月"), view, { view = it }, Modifier.fillMaxWidth(0.7f))
        }

        Section("メニュー") {
            var last by rememberSaveable { mutableStateOf("まだ選んでいない") }
            Caption("開いている間に、ほかのボタンへマウスを乗せる(スマホでは押す)と、滑って切り替わる")
            PiyoMenuGroup {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    PiyoMenu(SampleIcons.Copy, "コピー") {
                        PiyoMenuItem("テキスト", { last = "テキストをコピー" }, detail = "本文と日付")
                        PiyoMenuItem("テキスト", { last = "名前なしでコピー" }, detail = "名前なし")
                        PiyoMenuItem("リンク", { last = "リンクをコピー" }, detail = "共有用")
                    }
                    PiyoMenu(SampleIcons.Download, "ダウンロード") {
                        PiyoMenuItem("PDF", { last = "PDF" }, detail = "印刷と同じ紙面")
                        PiyoMenuItem("JSON", { last = "JSON" }, detail = "元のデータ")
                    }
                    PiyoMenu(SampleIcons.More, "その他") {
                        PiyoMenuItem("名前を変える", { last = "名前を変える" }, icon = SampleIcons.Star)
                        PiyoMenuItem("複製", { last = "複製" }, icon = SampleIcons.Copy)
                        PiyoMenuDivider()
                        PiyoMenuItem("削除", { last = "削除" }, icon = SampleIcons.Trash, tone = PiyoTone.Danger)
                    }
                }
            }
            Caption("選んだもの: $last")
        }

        Section("カード") {
            var opened by rememberSaveable { mutableIntStateOf(0) }
            PiyoCard(Modifier.fillMaxWidth()) {
                PiyoText("お知らせ", style = LocalPiyoTextStyle.current.copy(fontWeight = FontWeight.Bold))
                Caption("中身を縦に並べる面。斜めのグラデーション、薄い枠、線状の影")
            }
            PiyoCard(onClick = { opened++ }, modifier = Modifier.fillMaxWidth()) {
                PiyoText("押せるカード", style = LocalPiyoTextStyle.current.copy(fontWeight = FontWeight.Bold))
                Caption("押すと縮む。押した回数: $opened")
            }
        }
    }
}

// 見出しつきのまとまり
@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PiyoText(
            title,
            color = Piyo.colors.contentMuted,
            style = LocalPiyoTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Piyo.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

// 小さな説明の文字
@Composable
internal fun Caption(text: String) {
    PiyoText(text, color = Piyo.colors.contentMuted, style = LocalPiyoTextStyle.current.copy(fontSize = 12.sp))
}

// 主な role の色見本と、シード色の色コード
@Composable
private fun Swatches() {
    val c = Piyo.colors
    val list = listOf(c.primary, c.primaryStrong, c.contentSelected, c.contentMuted, c.contentDefault, c.background, c.danger)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        list.forEach { Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(it)) }
        Box(Modifier.size(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            c.edgeStops.dropLast(1).forEach { (_, color) -> Box(Modifier.size(8.dp, 22.dp).background(color)) }
        }
    }
    Caption("seed #%06X".format(c.seed.toArgb() and 0xFFFFFF))
}

// 色相を選ぶ帯
@Composable
private fun HueBar(hue: Float, onHue: (Float) -> Unit) {
    val colors = remember { (0..12).map { Color.hsv(it * 30f % 360f, 0.72f, 0.78f) } }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) { detectTapGestures { onHue(it.x / size.width * 360f) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> onHue((change.position.x / size.width).coerceIn(0f, 1f) * 359.9f) } },
    ) {
        drawRoundRect(Brush.horizontalGradient(colors), cornerRadius = CornerRadius(size.height / 2))
        val x = hue / 360f * size.width
        drawCircle(Color.White, radius = size.height / 2 - 2.dp.toPx(), center = Offset(x, size.height / 2), style = Stroke(3.dp.toPx()))
    }
}
