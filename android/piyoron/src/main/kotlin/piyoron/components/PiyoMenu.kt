package piyoron.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import piyoron.core.LocalPiyoContentColor
import piyoron.core.LocalPiyoOverlay
import piyoron.core.LocalPiyoReduceMotion
import piyoron.core.LocalPiyoTextStyle
import piyoron.core.OverlayEntry
import piyoron.core.Piyo
import piyoron.core.PiyoIcon
import piyoron.core.PiyoOverlay
import piyoron.core.PiyoText
import piyoron.core.PiyoTone
import piyoron.core.SurfaceRole
import piyoron.core.disabledAlpha
import piyoron.core.lerp
import piyoron.core.piyoSurface
import piyoron.core.rememberPiyoState
import piyoron.tokens.PiyoTokens
import kotlin.math.abs
import kotlin.math.roundToInt

// メニューの置き場所(左上)。ボタンの下に右端をそろえ、入らなければ上。画面の端から margin は空ける
// anchor はボタン、size はメニュー、area は置ける範囲の大きさ(どれも同じ座標)
internal fun menuPosition(anchor: Rect, size: Size, area: Size, gap: Float, margin: Float): Offset {
    val left = (anchor.right - size.width).coerceIn(margin, maxOf(margin, area.width - margin - size.width))
    var top = anchor.bottom + gap
    if (top + size.height > area.height - margin && anchor.top - gap - size.height >= margin) top = anchor.top - gap - size.height
    top = top.coerceIn(margin, maxOf(margin, area.height - margin - size.height))
    return Offset(left, top)
}

// 1つのメニューの状態
@Stable
internal class MenuEntry {
    // ボタンの見た目と、触れる範囲(画面の左上から見た位置)
    var anchor by mutableStateOf(Rect.Zero)
    var touch by mutableStateOf(Rect.Zero)

    // 中身と、呼んだ場所の CompositionLocal
    var content: @Composable ColumnScope.() -> Unit by mutableStateOf({})
    var locals: CompositionLocalContext? by mutableStateOf(null)

    // 層に出しているか、置いた位置と大きさ(層から見た位置)
    var shown by mutableStateOf(false)
    var placed by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(Size.Zero)

    // 開閉の進み(0〜1)と向き
    val time = Animatable(1f)
    var closing by mutableStateOf(false)

    // 滑りの進み(0〜1)。in なら from から入り、out なら partner の方へ出ていく
    val slide = Animatable(1f)
    var slideIn by mutableStateOf<Boolean?>(null)
    var slideFrom by mutableStateOf(Offset.Zero)
    var partner by mutableStateOf<MenuEntry?>(null)

    // 押した位置が、メニューの面の上か
    fun hits(at: Offset): Boolean = shown && Rect(placed, size).contains(at)
}

// まとめて扱うメニュー。開いているのは1つで、開いている間にほかへ移ると滑って切り替わる
@Stable
internal class MenuGroup(private val scope: CoroutineScope, private val overlay: PiyoOverlay) {
    val entries = mutableListOf<MenuEntry>()
    var current by mutableStateOf<MenuEntry?>(null)
        private set
    var reduceMotion = false

    // ボタンを押したとき(開いていれば閉じ、ほかが開いていれば滑って移る)
    fun toggle(entry: MenuEntry) = when (current) {
        entry -> close(entry)
        null -> open(entry)
        else -> switchTo(entry)
    }

    // マウスを乗せたとき(ほかが開いていれば滑って移る)
    fun hover(entry: MenuEntry) {
        if (current != null && current != entry) switchTo(entry)
    }

    // 開く(ボタンの中心から広がる)
    fun open(entry: MenuEntry) {
        current = entry
        entry.slideIn = null
        entry.closing = false
        entry.shown = true
        scope.launch {
            if (reduceMotion) entry.time.snapTo(1f) else {
                entry.time.snapTo(0f)
                entry.time.animateTo(1f, tween(PiyoTokens.Menu.OpenDuration, easing = LinearEasing))
            }
        }
    }

    // 閉じる(ボタンの中心へ縮む)
    fun close(entry: MenuEntry) {
        if (current == entry) current = null
        if (!entry.shown) return
        entry.closing = true
        scope.launch {
            if (!reduceMotion) {
                entry.time.snapTo(0f)
                entry.time.animateTo(1f, tween(PiyoTokens.Menu.OpenDuration, easing = LinearEasing))
            }
            if (entry.closing) entry.shown = false
        }
    }

    // 開いているメニューから、別のメニューへ滑らせて切り替える
    private fun switchTo(next: MenuEntry) {
        val prev = current ?: return open(next)
        if (reduceMotion) {
            prev.shown = false
            open(next)
            return
        }
        current = next
        next.closing = false
        next.slideFrom = prev.placed
        next.slideIn = true
        next.partner = null
        next.shown = true
        prev.slideIn = false
        prev.partner = next
        scope.launch { next.time.snapTo(1f) }
        scope.launch {
            next.slide.snapTo(0f)
            next.slide.animateTo(1f, tween(PiyoTokens.Menu.SlideDuration, easing = LinearEasing))
            if (next.slideIn == true) next.slideIn = null
        }
        scope.launch {
            prev.slide.snapTo(0f)
            prev.slide.animateTo(1f, tween(PiyoTokens.Menu.SlideDuration, easing = LinearEasing))
            if (prev.slideIn == false && current != prev) {
                prev.shown = false
                prev.slideIn = null
                prev.partner = null
            }
        }
    }

    // 層のどこかが押されたとき、メニューの面とボタンの外なら閉じる
    fun press(at: Offset) {
        val open = current ?: return
        if (open.hits(at)) return
        if (entries.any { overlay.toLocal(it.touch.topLeft).let { p -> Rect(p, it.touch.size) }.contains(at) }) return
        close(open)
    }
}

// まとめて扱うメニューの組(なければメニューごとに作る)
private val LocalPiyoMenuGroup = staticCompositionLocalOf<MenuGroup?> { null }

// 項目を押したときにメニューを閉じる
private val LocalPiyoMenuClose = staticCompositionLocalOf<() -> Unit> { {} }

// メニューの組を作り、最前面の層に描く場所を用意する
@Composable
private fun rememberMenuGroup(): MenuGroup {
    val overlay = LocalPiyoOverlay.current ?: error("PiyoMenu は PiyoTheme の中で使う")
    val scope = rememberCoroutineScope()
    val group = remember(overlay) { MenuGroup(scope, overlay) }
    group.reduceMotion = LocalPiyoReduceMotion.current
    DisposableEffect(group) {
        val entry = OverlayEntry(
            content = { MenuLayer(group, overlay) },
            press = group::press,
            open = { group.current != null },
            dismiss = { group.current?.let(group::close) },
        )
        overlay.entries += entry
        onDispose { overlay.entries -= entry }
    }
    return group
}

// 中のメニューを、開いている間に滑って切り替わる組にする。並べ方は中で決める(Row など)
@Composable
fun PiyoMenuGroup(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPiyoMenuGroup provides rememberMenuGroup(), content = content)
}

// アイコンのボタンと、押すと開くメニュー。PiyoMenuGroup の中に並べると、開いている間にほかのボタンへ滑って移る
@Composable
fun PiyoMenu(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val group = LocalPiyoMenuGroup.current ?: rememberMenuGroup()
    val entry = remember { MenuEntry() }
    val locals = currentCompositionLocalContext
    val latest by rememberUpdatedState(content)
    SideEffect {
        entry.content = { latest(this) }
        entry.locals = locals
    }
    DisposableEffect(group, entry) {
        group.entries += entry
        onDispose {
            group.entries -= entry
            if (group.current == entry) group.close(entry)
        }
    }
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    LaunchedEffect(hovered) { if (hovered) group.hover(entry) }
    val size = PiyoButtonDefaults.Height
    val sizePx = with(LocalDensity.current) { size.toPx() }
    PiyoIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = { group.toggle(entry) },
        modifier = modifier.onGloballyPositioned { coordinates ->
            val touch = coordinates.boundsInRoot()
            val visual = touch.deflate(((touch.width - sizePx) / 2).coerceAtLeast(0f))
            val moved = entry.anchor != Rect.Zero && (abs(visual.left - entry.anchor.left) > 1f || abs(visual.top - entry.anchor.top) > 1f)
            entry.touch = touch
            entry.anchor = visual
            // 開いている間にボタンが動いた(スクロール、画面の大きさの変化)ら閉じる
            if (moved && group.current == entry) group.close(entry)
        },
        enabled = enabled,
        size = size,
        interactionSource = source,
    )
}

// 組のメニューを最前面の層に描く
@Composable
private fun MenuLayer(group: MenuGroup, overlay: PiyoOverlay) {
    group.entries.toList().forEach { entry ->
        if (entry.shown) {
            key(entry) {
                val locals = entry.locals
                if (locals != null) {
                    CompositionLocalProvider(locals) { MenuPanel(entry, group, overlay) }
                } else {
                    MenuPanel(entry, group, overlay)
                }
            }
        }
    }
}

// メニューの面(置き場所を決め、開閉と滑りの動きをかける)
@Composable
private fun MenuPanel(entry: MenuEntry, group: MenuGroup, overlay: PiyoOverlay) {
    val close = remember(entry, group) { { group.close(entry) } }
    CompositionLocalProvider(LocalPiyoMenuClose provides close) {
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
                    val size = Size(placeable.width.toFloat(), placeable.height.toFloat())
                    val area = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                    val anchor = Rect(overlay.toLocal(entry.anchor.topLeft), entry.anchor.size)
                    val pos = menuPosition(anchor, size, area, PiyoTokens.Menu.Gap.toPx(), PiyoTokens.Menu.Margin.toPx())
                    if (entry.placed != pos) entry.placed = pos
                    if (entry.size != size) entry.size = size
                    layout(0, 0) { placeable.place(pos.x.roundToInt(), pos.y.roundToInt()) }
                }
                .graphicsLayer {
                    val w = entry.size.width.coerceAtLeast(1f)
                    val h = entry.size.height.coerceAtLeast(1f)
                    val center = overlay.toLocal(entry.anchor.center) - entry.placed
                    transformOrigin = TransformOrigin(center.x / w, center.y / h)
                    val t = entry.time.value
                    val moved = PiyoTokens.Menu.Easing.transform(t)
                    val faded = PiyoTokens.Menu.FadeEasing.transform((t * PiyoTokens.Menu.OpenDuration / PiyoTokens.Menu.FadeDuration).coerceAtMost(1f))
                    val small = PiyoTokens.Menu.OpenScale
                    val scale = if (entry.closing) lerp(1f, small, moved) else lerp(small, 1f, moved)
                    scaleX = scale
                    scaleY = scale
                    var a = if (entry.closing) 1f - faded else faded
                    val s = PiyoTokens.Menu.Easing.transform(entry.slide.value)
                    when (entry.slideIn) {
                        true -> {
                            val d = entry.slideFrom - entry.placed
                            translationX = d.x * (1 - s)
                            translationY = d.y * (1 - s)
                            a *= s
                        }
                        false -> {
                            val d = (entry.partner?.placed ?: entry.placed) - entry.placed
                            translationX = d.x * s
                            translationY = d.y * s
                            a *= 1 - s
                        }
                        null -> Unit
                    }
                    alpha = a
                    // 薄くするとき、面の大きさの画像を経由しない(外側に描く影が切れないように)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .piyoSurface(RoundedCornerShape(PiyoTokens.Menu.Radius), SurfaceRole.Menu)
                .width(IntrinsicSize.Max)
                .widthIn(min = PiyoTokens.Menu.MinWidth)
                .padding(PiyoTokens.Menu.Padding),
        ) { entry.content(this) }
    }
}

// メニューの項目。押すとメニューを閉じてから onClick を呼ぶ。detail は右寄せの補足
@Composable
fun PiyoMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: ImageVector? = null,
    tone: PiyoTone = PiyoTone.Default,
    enabled: Boolean = true,
) {
    val close = LocalPiyoMenuClose.current
    val source = remember { MutableInteractionSource() }
    val state = rememberPiyoState(source, enabled = enabled)
    val color = Piyo.style.contentColor(SurfaceRole.MenuItem, state, tone)
    val textStyle = LocalPiyoTextStyle.current.copy(fontSize = PiyoTokens.Menu.TextSize)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PiyoTokens.Touch.MinSize)
            .clickable(source, null, enabled = enabled, role = Role.Button) {
                close()
                onClick()
            }
            .disabledAlpha(enabled)
            .piyoSurface(RoundedCornerShape(PiyoTokens.Menu.ItemRadius), SurfaceRole.MenuItem, state, tone)
            .padding(horizontal = PiyoTokens.Menu.ItemPaddingX, vertical = PiyoTokens.Menu.ItemPaddingY),
        horizontalArrangement = Arrangement.spacedBy(PiyoTokens.Menu.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalPiyoContentColor provides color, LocalPiyoTextStyle provides textStyle) {
            if (icon != null) PiyoIcon(icon, contentDescription = null, size = PiyoTokens.Menu.IconSize)
            PiyoText(text, maxLines = 1)
            if (detail != null) {
                Spacer(Modifier.weight(1f).widthIn(min = PiyoTokens.Menu.DetailGap - PiyoTokens.Menu.ItemGap))
                PiyoText(
                    detail,
                    color = Piyo.colors.contentMuted,
                    style = textStyle.copy(fontSize = PiyoTokens.Menu.DetailSize, fontWeight = FontWeight.Normal),
                    maxLines = 1,
                )
            }
        }
    }
}

// メニューの項目の区切り線
@Composable
fun PiyoMenuDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(horizontal = PiyoTokens.Menu.DividerInset, vertical = PiyoTokens.Menu.DividerSpace)
            .fillMaxWidth()
            .height(1.dp)
            .piyoSurface(RectangleShape, SurfaceRole.Divider),
    )
}
