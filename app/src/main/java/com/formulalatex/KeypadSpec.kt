package com.formulalatex

/**
 * 屏幕键盘的按键定义（数据与界面分离，改按钮只动这个文件）。
 *
 * @param label  按钮上显示的**数学符号**（不给用户看"极限/根号"这类中文词）
 * @param action 传给网页的动作名，对应 assets/editor/editor.js 里的
 *               TEMPLATES / SYMBOLS / "char:x" / 以及 undo、redo、clear、backspace、prev、next、left、right、up、down
 * @param group  分组，决定用哪种 Material 按钮样式
 */
data class KeyItem(val label: String, val action: String, val group: Group) {

    /**
     * 按键分组，对应几种 Material 3 按钮样式：
     *   STRUCTURE / CURSOR —— 实心淡色按钮（FilledTonalButton），最显眼
     *   GREEK / OPERATOR   —— 描边按钮（OutlinedButton）
     *   DIGIT / LETTER     —— 数字用实心淡色、字母用描边（数字更常用）
     *   EDIT               —— 文字按钮（TextButton），最轻
     */
    enum class Group { STRUCTURE, GREEK, OPERATOR, DIGIT, LETTER, CURSOR, EDIT }
}

/**
 * 键盘布局。为了不用上下翻动，按键分成「两页 + 一条固定行」：
 *
 *   ① 符号页（6 列 × 6 行 = 36 格，放 35 键）
 *      结构模板 9 + 希腊字母 10 + 运算符 11 + 撤销/重做/清空/方框跳转 5
 *   ② 字母数字页（6 列 × 6 行 = 36 格，正好 36 键）
 *      0-9 + a-z
 *   ③ 固定行（不随页切换，永远可见）
 *      ← ↑ ↓ → = ⌫
 *      方向键放在这里是因为它们最常用，而且位置刚好在拇指区；
 *      `=` 也放这里：填 "i=1"、"n=0" 这类上下限时不用来回切页。
 *
 * 典型流程（全程不弹系统输入法）：
 *   点 ∑ → 光标已在下限方框 → 字母页 i → 固定行 = → 字母页 1
 *        → 固定行 ↑（跳到上限方框）→ 字母页 n   得到 \sum_{i=1}^{n}
 */
object KeypadSpec {

    /** ① 符号页 */
    val SYMBOL_PAGE: List<KeyItem> = buildList {
        // 结构模板：□/□ 分数、x² 上标、x₂ 下标、x₂² 上下标、∑ 求和、lim 极限、∫ 积分、√ 根号、( ) 括号
        add(KeyItem("□/□", "frac", KeyItem.Group.STRUCTURE))
        add(KeyItem("x²", "sup", KeyItem.Group.STRUCTURE))
        add(KeyItem("x₂", "sub", KeyItem.Group.STRUCTURE))
        add(KeyItem("x₂²", "supsub", KeyItem.Group.STRUCTURE))
        add(KeyItem("∑", "sum", KeyItem.Group.STRUCTURE))
        add(KeyItem("lim", "lim", KeyItem.Group.STRUCTURE))
        add(KeyItem("∫", "int", KeyItem.Group.STRUCTURE))
        add(KeyItem("√", "sqrt", KeyItem.Group.STRUCTURE))
        add(KeyItem("( )", "paren", KeyItem.Group.STRUCTURE))

        // 希腊字母
        listOf(
            "α" to "alpha", "β" to "beta", "γ" to "gamma", "θ" to "theta", "π" to "pi",
            "λ" to "lambda", "μ" to "mu", "σ" to "sigma", "ω" to "omega", "∞" to "infty"
        ).forEach { (label, action) -> add(KeyItem(label, action, KeyItem.Group.GREEK)) }

        // 运算符（= 已提到固定行，这里不再重复）
        listOf(
            "+" to "plus", "−" to "minus", "×" to "times", "÷" to "div",
            "≠" to "ne", "≤" to "le", "≥" to "ge", "→" to "to", "⇒" to "rArr",
            "∈" to "isin", "⊂" to "subset"
        ).forEach { (label, action) -> add(KeyItem(label, action, KeyItem.Group.OPERATOR)) }

        // 撤销 / 重做 / 清空 / 方框跳转（□← 表示"跳到上一个方框"）
        add(KeyItem("↶", "undo", KeyItem.Group.EDIT))
        add(KeyItem("↷", "redo", KeyItem.Group.EDIT))
        add(KeyItem("C", "clear", KeyItem.Group.EDIT))
        add(KeyItem("□←", "prev", KeyItem.Group.CURSOR))
        add(KeyItem("□→", "next", KeyItem.Group.CURSOR))
    }

    /** ② 字母数字页：0-9 在前（更常用），接着 a-z，刚好 36 键铺满 6 × 6 */
    val CHAR_PAGE: List<KeyItem> = buildList {
        (0..9).forEach { add(KeyItem("$it", "char:$it", KeyItem.Group.DIGIT)) }
        ('a'..'z').forEach { add(KeyItem("$it", "char:$it", KeyItem.Group.LETTER)) }
    }

    /**
     * ③ 固定行（永远可见，键更大）：
     * ← → 逐字符移动；↑ ↓ 在分母/分子、求和下限/上限之间跳；
     * `=` 直接插入等号（填 i=1 这类上下限时最常用）；⌫ 删除。
     */
    val FIXED_ROW: List<KeyItem> = listOf(
        KeyItem("←", "left", KeyItem.Group.CURSOR),
        KeyItem("↑", "up", KeyItem.Group.CURSOR),
        KeyItem("↓", "down", KeyItem.Group.CURSOR),
        KeyItem("→", "right", KeyItem.Group.CURSOR),
        KeyItem("=", "eq", KeyItem.Group.CURSOR),
        KeyItem("⌫", "backspace", KeyItem.Group.CURSOR)
    )

    /** 兼容旧调用：全部按键 */
    val ALL: List<KeyItem> = SYMBOL_PAGE + CHAR_PAGE + FIXED_ROW
}
