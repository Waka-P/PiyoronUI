package piyoron.components

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.draw.drawBehind
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import piyoron.effects.genie.GenieSurfaceShape
import piyoron.effects.genie.genie
import piyoron.effects.genie.genieAlpha
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

// メニューの置き場所(左上)。ボタンの side の側に overlap だけ重ねて置き、入らなければ反対側に置く。画面の端から margin は空ける
// ボタンに沿った向きの位置は align で決める(横並びなら左右、縦並びなら上下)
// anchor はボタン、size はメニュー、area は置ける範囲の大きさ(どれも同じ座標)
internal fun menuPosition(
    anchor: Rect,
    size: Size,
    area: Size,
    overlap: Float,
    margin: Float,
    side: PiyoMenuSide = PiyoMenuSide.Bottom,
    align: PiyoMenuAlignment = PiyoMenuAlignment.EndCorner,
): Offset {
    val vertical = side == PiyoMenuSide.Bottom || side == PiyoMenuSide.Top
    // 沿う向きの位置(ボタンの始まり、終わり、メニューの長さ、画面の長さ)
    val cross = if (vertical) {
        alongPosition(anchor.left, anchor.right, size.width, area.width, margin, align)
    } else {
        alongPosition(anchor.top, anchor.bottom, size.height, area.height, margin, align)
    }
    // 離れる向きの位置(先に置く側に入らなければ反対側)
    val length = if (vertical) size.height else size.width
    val limit = if (vertical) area.height else area.width
    val near = if (vertical) anchor.top else anchor.left
    val far = if (vertical) anchor.bottom else anchor.right
    val after = far - overlap
    val before = near + overlap - length
    val fitsAfter = after + length <= limit - margin
    val fitsBefore = before >= margin
    val main = when (side) {
        PiyoMenuSide.Bottom, PiyoMenuSide.Right -> if (!fitsAfter && fitsBefore) before else after
        PiyoMenuSide.Top, PiyoMenuSide.Left -> if (!fitsBefore && fitsAfter) after else before
    }.coerceIn(margin, maxOf(margin, limit - margin - length))
    return if (vertical) Offset(cross, main) else Offset(main, cross)
}

// ボタンに沿った向きの位置。角はボタンの中心がメニューの角に、端はメニューとボタンの端どうしをそろえる
private fun alongPosition(start: Float, end: Float, length: Float, limit: Float, margin: Float, align: PiyoMenuAlignment): Float {
    val t = (align.bias.coerceIn(-1f, 1f) + 1) / 2
    val center = (start + end) / 2
    val wanted = if (align.edges) lerp(start, end - length, t) else lerp(center, center - length, t)
    return wanted.coerceIn(margin, maxOf(margin, limit - margin - length))
}

// メニューをボタンのどちら側に出すか(入らなければ反対側に出す)
enum class PiyoMenuSide { Bottom, Top, Right, Left }

// メニューの開閉の動き
enum class PiyoMenuTransition {
    // 薄く現れて薄く消える(既定)
    Fade,

    // ボタンの中心から広がり、縮んで消える
    Scale,

    // ボタンに吸い込まれる(ジニー効果)
    Genie,

    // アニメーションなし(ほかのメニューへ滑る動きは残る)
    None,
}

// ボタンに沿った向きのメニューの位置(横並びなら左右、縦並びなら上下。Start は左か上)
// 名前で選ぶか、PiyoMenuAlignment(bias) で数値で決める(-1 でボタンの中心がメニューの始まりの角、0 で真ん中、1 で終わりの角)
@Immutable
class PiyoMenuAlignment private constructor(val bias: Float, internal val edges: Boolean) {

    // 同じ位置か
    override fun equals(other: Any?): Boolean = other is PiyoMenuAlignment && other.bias == bias && other.edges == edges

    // 同じ位置なら同じ値
    override fun hashCode(): Int = bias.hashCode() * 31 + edges.hashCode()

    companion object {
        // ボタンの中心がメニューの始まりの角(左か上)に来る
        val StartCorner = PiyoMenuAlignment(-1f, edges = false)

        // メニューとボタンの始まりの端(左か上)をそろえる
        val StartEdge = PiyoMenuAlignment(-1f, edges = true)

        // ボタンの中心がメニューの真ん中に来る
        val Center = PiyoMenuAlignment(0f, edges = false)

        // メニューとボタンの終わりの端(右か下)をそろえる
        val EndEdge = PiyoMenuAlignment(1f, edges = true)

        // ボタンの中心がメニューの終わりの角(右か下)に来る
        val EndCorner = PiyoMenuAlignment(1f, edges = false)

        // 数値で決める(-1 で始まりの角、1 で終わりの角)
        operator fun invoke(bias: Float): PiyoMenuAlignment = PiyoMenuAlignment(bias, edges = false)
    }
}

// メニューの既定値
object PiyoMenuDefaults {
    val ContentPadding = PaddingValues(PiyoTokens.Menu.Padding)
    val MinWidth = PiyoTokens.Menu.MinWidth
    val CornerRadius = PiyoTokens.Menu.Radius
    val Transition = PiyoMenuTransition.Fade
    val Alignment = PiyoMenuAlignment.EndCorner
    val Side = PiyoMenuSide.Bottom
    val BackdropBlur = PiyoTokens.Menu.BackdropBlur
}

// メニューの中身を置く場所。close() でメニューを閉じる(項目を自分で作るとき用)。中身は縦に並ぶ
interface PiyoMenuScope {

    // メニューを閉じる
    fun close()
}

// 閉じる処理を持つ中身の置き場所
private class MenuScopeImpl(private val onClose: () -> Unit) : PiyoMenuScope {

    // メニューを閉じる
    override fun close() = onClose()
}

// 1つのメニューの状態
@Stable
internal class MenuEntry {
    // ボタンの見た目と、触れる範囲(画面の左上から見た位置)
    var anchor by mutableStateOf(Rect.Zero)
    var touch by mutableStateOf(Rect.Zero)

    // 中身と、呼んだ場所の CompositionLocal
    var content: @Composable PiyoMenuScope.() -> Unit by mutableStateOf({})
    var locals: CompositionLocalContext? by mutableStateOf(null)

    // 面の見た目と開閉の動き(動きを指定しなければ組の既定)
    var group: MenuGroup? = null
    var ownTransition by mutableStateOf<PiyoMenuTransition?>(null)
    val transition: PiyoMenuTransition get() = ownTransition ?: group?.transition ?: PiyoMenuDefaults.Transition

    // ボタンに対する横の位置(指定しなければ組の既定)
    var ownAlignment by mutableStateOf<PiyoMenuAlignment?>(null)
    val alignment: PiyoMenuAlignment get() = ownAlignment ?: group?.alignment ?: PiyoMenuDefaults.Alignment

    // ボタンのどちら側に出すか(指定しなければ組の既定)
    var ownSide by mutableStateOf<PiyoMenuSide?>(null)
    val side: PiyoMenuSide get() = ownSide ?: group?.side ?: PiyoMenuDefaults.Side
    var surface by mutableStateOf<SurfaceRole?>(SurfaceRole.Menu)
    var color by mutableStateOf<Color?>(null)
    var backdropBlur by mutableStateOf(PiyoTokens.Menu.BackdropBlur)
    var cornerRadius by mutableStateOf(PiyoTokens.Menu.Radius)
    var contentPadding by mutableStateOf(PaddingValues(PiyoTokens.Menu.Padding))
    var minWidth by mutableStateOf(PiyoTokens.Menu.MinWidth)

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

    // 開閉にかける時間(動きの種類ごと)
    val duration: Int
        get() = when (transition) {
            PiyoMenuTransition.Fade -> PiyoTokens.Menu.FadeDuration
            PiyoMenuTransition.Scale -> PiyoTokens.Menu.OpenDuration
            PiyoMenuTransition.Genie -> PiyoTokens.Genie.Duration
            PiyoMenuTransition.None -> 0
        }

    // 滑っている間のずれ
    fun slideShift(): Offset {
        val s = PiyoTokens.Menu.Easing.transform(slide.value)
        return when (slideIn) {
            true -> (slideFrom - placed) * (1 - s)
            false -> ((partner?.placed ?: placed) - placed) * s
            null -> Offset.Zero
        }
    }

    // 滑っている間の濃さ
    fun slideAlpha(): Float {
        val s = PiyoTokens.Menu.Easing.transform(slide.value)
        return when (slideIn) {
            true -> s
            false -> 1 - s
            null -> 1f
        }
    }

    // ジニーの進み(0 で開いた形、1 で消えた形)
    fun genieProgress(): Float {
        val t = time.value
        return PiyoTokens.Genie.Easing.transform(if (closing) t else 1f - t)
    }
}

// まとめて扱うメニュー。開いているのは1つで、開いている間にほかへ移ると滑って切り替わる
@Stable
internal class MenuGroup(private val scope: CoroutineScope, private val overlay: PiyoOverlay) {
    val entries = mutableListOf<MenuEntry>()
    var current by mutableStateOf<MenuEntry?>(null)
        private set
    var reduceMotion = false
    var transition by mutableStateOf(PiyoMenuTransition.Fade)
    var alignment by mutableStateOf(PiyoMenuAlignment.EndCorner)
    var side by mutableStateOf(PiyoMenuSide.Bottom)

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

    // 開く(動きはメニューごとの種類で)
    fun open(entry: MenuEntry) {
        current = entry
        entry.slideIn = null
        entry.closing = false
        entry.shown = true
        scope.launch {
            if (reduceMotion || entry.duration == 0) entry.time.snapTo(1f) else {
                entry.time.snapTo(0f)
                entry.time.animateTo(1f, tween(entry.duration, easing = LinearEasing))
            }
        }
    }

    // 閉じる(動きはメニューごとの種類で)
    fun close(entry: MenuEntry) {
        if (current == entry) current = null
        if (!entry.shown) return
        entry.closing = true
        scope.launch {
            if (!reduceMotion && entry.duration > 0) {
                entry.time.snapTo(0f)
                entry.time.animateTo(1f, tween(entry.duration, easing = LinearEasing))
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
private fun rememberMenuGroup(transition: PiyoMenuTransition, alignment: PiyoMenuAlignment, side: PiyoMenuSide): MenuGroup {
    val overlay = LocalPiyoOverlay.current ?: error("PiyoMenu は PiyoTheme の中で使う")
    val scope = rememberCoroutineScope()
    val group = remember(overlay) { MenuGroup(scope, overlay) }
    group.reduceMotion = LocalPiyoReduceMotion.current
    group.transition = transition
    group.alignment = alignment
    group.side = side
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
// transition は中のメニューの開閉の動き、side はボタンのどちら側に出すか、alignment はボタンに沿った向きの位置の既定
// ボタンを縦に並べるときは side を Right か Left にする
@Composable
fun PiyoMenuGroup(
    transition: PiyoMenuTransition = PiyoMenuDefaults.Transition,
    side: PiyoMenuSide = PiyoMenuDefaults.Side,
    alignment: PiyoMenuAlignment = PiyoMenuDefaults.Alignment,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPiyoMenuGroup provides rememberMenuGroup(transition, alignment, side), content = content)
}

// アイコンのボタンと、押すと開くメニュー。PiyoMenuGroup の中に並べると、開いている間にほかのボタンへ滑って移る
// 中身は自由に置ける(PiyoMenuItem を使わなくてもよい。閉じるときは close())。surface を null にすると面も描かない
// color は面の塗りの色(省くとスタイルの色)、backdropBlur は面の下に敷く、後ろの画面のぼかしの強さ(0 でぼかさない)
@Composable
fun PiyoMenu(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    transition: PiyoMenuTransition? = null,
    side: PiyoMenuSide? = null,
    alignment: PiyoMenuAlignment? = null,
    surface: SurfaceRole? = SurfaceRole.Menu,
    color: Color? = null,
    backdropBlur: Dp = PiyoMenuDefaults.BackdropBlur,
    cornerRadius: Dp = PiyoMenuDefaults.CornerRadius,
    contentPadding: PaddingValues = PiyoMenuDefaults.ContentPadding,
    minWidth: Dp = PiyoMenuDefaults.MinWidth,
    content: @Composable PiyoMenuScope.() -> Unit,
) {
    val group = LocalPiyoMenuGroup.current ?: rememberMenuGroup(transition ?: PiyoMenuDefaults.Transition, alignment ?: PiyoMenuDefaults.Alignment, side ?: PiyoMenuDefaults.Side)
    val entry = remember { MenuEntry() }
    val locals = currentCompositionLocalContext
    val latest by rememberUpdatedState(content)
    SideEffect {
        entry.content = { latest(this) }
        entry.locals = locals
        entry.group = group
        entry.ownTransition = transition
        entry.ownAlignment = alignment
        entry.ownSide = side
        entry.surface = surface
        entry.color = color
        entry.backdropBlur = backdropBlur
        entry.cornerRadius = cornerRadius
        entry.contentPadding = contentPadding
        entry.minWidth = minWidth
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

// メニュー(置き場所を決め、開閉と滑りの動きをかけ、面と中身を描く)
@Composable
private fun MenuPanel(entry: MenuEntry, group: MenuGroup, overlay: PiyoOverlay) {
    val close = remember(entry, group) { { group.close(entry) } }
    val genie = entry.transition == PiyoMenuTransition.Genie
    // 層から見た、パネルの左上からボタンの中心まで(ジニーの行き先)
    val target = { overlay.toLocal(entry.anchor.center) - entry.placed }
    // パネルから見た、くり抜く円(ボタンに余白を足した範囲)
    val notchGap = with(LocalDensity.current) { PiyoTokens.Menu.NotchGap.toPx() }
    val notch = { Rect(overlay.toLocal(entry.anchor.topLeft) - entry.placed, entry.anchor.size).inflate(notchGap) }
    CompositionLocalProvider(LocalPiyoMenuClose provides close) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
                    val size = Size(placeable.width.toFloat(), placeable.height.toFloat())
                    val area = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                    val anchor = Rect(overlay.toLocal(entry.anchor.topLeft), entry.anchor.size)
                    val pos = menuPosition(anchor, size, area, PiyoTokens.Menu.Overlap.toPx(), PiyoTokens.Menu.Margin.toPx(), entry.side, entry.alignment)
                    if (entry.placed != pos) entry.placed = pos
                    if (entry.size != size) entry.size = size
                    layout(0, 0) { placeable.place(pos.x.roundToInt(), pos.y.roundToInt()) }
                }
                .graphicsLayer {
                    var a = 1f
                    if (!genie) {
                        val t = entry.time.value
                        when (entry.transition) {
                            PiyoMenuTransition.Scale -> {
                                val w = entry.size.width.coerceAtLeast(1f)
                                val h = entry.size.height.coerceAtLeast(1f)
                                val center = target()
                                transformOrigin = TransformOrigin(center.x / w, center.y / h)
                                val moved = PiyoTokens.Menu.Easing.transform(t)
                                val faded = PiyoTokens.Menu.FadeEasing.transform((t * PiyoTokens.Menu.OpenDuration / PiyoTokens.Menu.FadeDuration).coerceAtMost(1f))
                                val small = PiyoTokens.Menu.OpenScale
                                val scale = if (entry.closing) lerp(1f, small, moved) else lerp(small, 1f, moved)
                                scaleX = scale
                                scaleY = scale
                                a = if (entry.closing) 1f - faded else faded
                            }
                            PiyoMenuTransition.Fade -> {
                                val faded = PiyoTokens.Menu.FadeEasing.transform(t)
                                a = if (entry.closing) 1f - faded else faded
                            }
                            else -> Unit
                        }
                    }
                    val shift = entry.slideShift()
                    translationX = shift.x
                    translationY = shift.y
                    alpha = a * entry.slideAlpha()
                    // 薄くするとき、面の大きさの画像を経由しない(外側に描く影が切れないように)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .withoutNotch(notch),
        ) {
            entry.surface?.let { role -> MenuSurface(entry, role, genie, target, notch, overlay) }
            Column(
                modifier = Modifier
                    .then(if (genie) Modifier.genie({ entry.genieProgress() }, target, entry.cornerRadius) else Modifier)
                    .animateContentSize(tween(if (group.reduceMotion) 0 else PiyoTokens.Menu.ResizeDuration, easing = PiyoTokens.Menu.Easing))
                    .width(IntrinsicSize.Max)
                    .widthIn(min = entry.minWidth)
                    .padding(entry.contentPadding),
            ) {
                val scope = remember(close) { MenuScopeImpl(close) }
                entry.content(scope)
            }
        }
    }
}

// メニューの面。ボタンと重なる所はくり抜き、枠線もその縁に沿わせる
// ジニーのときは変形に沿った形で描く(内側の影も形に合わせるため、進み具合が変わるたびに作り直す)
@Composable
private fun BoxScope.MenuSurface(
    entry: MenuEntry,
    role: SurfaceRole,
    genie: Boolean,
    target: () -> Offset,
    notch: () -> Rect,
    overlay: PiyoOverlay,
) {
    val base = if (genie) {
        val p = entry.genieProgress()
        GenieSurfaceShape(p, if (p > 0f) target() else Offset.Zero, entry.cornerRadius)
    } else {
        RoundedCornerShape(entry.cornerRadius)
    }
    val shape = remember(base, notch) { NotchedShape(base, notch) }
    val blur = rememberGraphicsLayer()
    val blurPx = with(LocalDensity.current) { entry.backdropBlur.toPx() }
    Box(
        Modifier
            .matchParentSize()
            .then(
                if (genie) {
                    Modifier.graphicsLayer {
                        alpha = genieAlpha(entry.genieProgress())
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                } else {
                    Modifier
                },
            )
            .backdrop(entry, overlay, shape, blur, blurPx)
            .piyoSurface(shape, role, fill = entry.color),
    )
}

// 面の下に、後ろの画面をぼかして敷く(Android 12 以上。それより前は何も描かない)
// 自分の下にあたる所だけを切り出してぼかし、面の形で切り抜く。滑っている間は、今いる位置の後ろを使う
private fun Modifier.backdrop(entry: MenuEntry, overlay: PiyoOverlay, shape: Shape, layer: GraphicsLayer, radius: Float): Modifier = drawBehind {
    val back = overlay.backdrop ?: return@drawBehind
    if (radius <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@drawBehind
    val origin = entry.placed + entry.slideShift()
    val pad = radius * 2
    layer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
    layer.record(size = IntSize((size.width + pad * 2).roundToInt(), (size.height + pad * 2).roundToInt())) {
        translate(pad - origin.x, pad - origin.y) { drawLayer(back) }
    }
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
    clipPath(path) { translate(-pad, -pad) { drawLayer(layer) } }
}

// 円でくり抜いた形(面の枠線もくり抜いた縁に沿う)
private class NotchedShape(private val base: Shape, private val hole: () -> Rect) : Shape {

    // 元の形から円を引く
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val outline = base.createOutline(size, layoutDirection, density)
        val circle = hole()
        if (circle.isEmpty || !circle.overlaps(Rect(Offset.Zero, size))) return outline
        val path = Path().apply { addOutline(outline) }
        val cut = Path().apply { addOval(circle) }
        return Outline.Generic(Path().apply { op(path, cut, PathOperation.Difference) })
    }
}

// ボタンに重なった所(余白付きの円)には、面も中身も描かない
private fun Modifier.withoutNotch(notch: () -> Rect): Modifier = drawWithContent {
    val hole = Path().apply { addOval(notch()) }
    clipPath(hole, ClipOp.Difference) { this@drawWithContent.drawContent() }
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
