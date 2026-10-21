// 自動生成。手で書き換えない。元は tokens/(npm run tokens で作り直す)
package piyoron.tokens

import androidx.compose.ui.graphics.Color

// 手で調整した色の組み合わせ
enum class PiyoPreset(
    val seed: Color,
    internal val overrides: Map<String, Color>,
    internal val edge: List<Pair<Float, Color>>?,
    internal val toneUsesEdge: Boolean,
) {
    Ocean(Color(0xFF2293C7), mapOf(), null, false),
    Mint(Color(0xFF14A08E), mapOf(), null, false),
    Rose(Color(0xFFDD84AF), mapOf(), null, false),
    Violet(Color(0xFF7A5FD6), mapOf(), null, false),
    Amber(Color(0xFFD48A0A), mapOf("primaryStrong" to Color(0xFFB0640A), "contentDefault" to Color(0xFF2B241D), "contentMuted" to Color(0xFF736A60)), null, false),
    Gold(Color(0xFFD4B045), mapOf("primaryStrong" to Color(0xFF8D7023), "contentDefault" to Color(0xFF2A261B), "contentMuted" to Color(0xFF726B5C)), listOf(0f to Color(0xFFE3D18C), 0.09722222222222222f to Color(0xFFC8A747), 0.19444444444444445f to Color(0xFFAB862A), 0.2777777777777778f to Color(0xFFD7BC66), 0.3472222222222222f to Color(0xFFEDDFA2), 0.4166666666666667f to Color(0xFFF0E8B6), 0.4861111111111111f to Color(0xFFE3CD80), 0.5555555555555556f to Color(0xFFBD9939), 0.6388888888888888f to Color(0xFFA4802B), 0.7222222222222222f to Color(0xFFCBAA4B), 0.8055555555555556f to Color(0xFFE8D897), 0.875f to Color(0xFFEEE6B5), 0.9444444444444444f to Color(0xFFDABF69)), false),
    Bubble(Color(0xFF71ADDA), mapOf("contentSelected" to Color(0xFF3376A6)), listOf(0f to Color(0xFFC5C1F5), 0.09722222222222222f to Color(0xFF9FCEF2), 0.19444444444444445f to Color(0xFF8DDCF1), 0.2777777777777778f to Color(0xFFB4E2D4), 0.3472222222222222f to Color(0xFFAEE5C4), 0.4166666666666667f to Color(0xFF9EE4CA), 0.4861111111111111f to Color(0xFF83E2E4), 0.5555555555555556f to Color(0xFF97BAFA), 0.6388888888888888f to Color(0xFF9EA6F7), 0.7222222222222222f to Color(0xFFBCACF6), 0.8055555555555556f to Color(0xFFE4A2F3), 0.875f to Color(0xFFE1B3F6), 0.9444444444444444f to Color(0xFFCABDF9)), true);

    // このプリセットの全roleを作る
    fun roles(): PiyoColorRoles = generateRoles(seed, overrides, edge, toneUsesEdge)
}
