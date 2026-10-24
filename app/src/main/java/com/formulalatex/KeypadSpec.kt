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
 * 键盘布局：两页 + 一条固定行。两页都是 5 行，**切页时高度不变**，布局不会跳动。
 *
 *   ① 符号页（7 列 × 5 行 = 35 格，正好 35 键）
 *      结构模板 9 + 希腊字母 10 + 运算符 11 + 撤销/重做/清空/方框跳转 5
 *   ② 字母数字页（仿实体键盘，5 行）
 *      数字行 1234567890 → QWERTY → ASDF（缩进半格）→ ZXCV（缩进一格半）→ 常用运算符
 *   ③ 固定行（不随页切换，永远可见）：← ↑ ↓ → = ⌫
 *
 * 典型流程（全程不弹系统输入法）：
 *   点 ∑ → 光标已在下限方框 → 字母页 i → 固定行 = → 字母页 1
 *        → 固定行 ↑（跳到上限方框）→ 字母页 n   得到 \sum_{i=1}^{n}
 */
object KeypadSpec {

    /** ① 符号页：7 列 × 5 行 */
    val SYMBOL_PAGE: List<KeyItem> = listOf(
        // 第 1 行：主要结构模板
        KeyItem("□/□", "frac", KeyItem.Group.STRUCTURE),
        KeyItem("x²", "sup", KeyItem.Group.STRUCTURE),
        KeyItem("x₂", "sub", KeyItem.Group.STRUCTURE),
        KeyItem("x₂²", "supsub", KeyItem.Group.STRUCTURE),
        KeyItem("∑", "sum", KeyItem.Group.STRUCTURE),
        KeyItem("lim", "lim", KeyItem.Group.STRUCTURE),
        KeyItem("∫", "int", KeyItem.Group.STRUCTURE),
        // 第 2 行：其余结构 + 希腊字母
        KeyItem("√", "sqrt", KeyItem.Group.STRUCTURE),
        KeyItem("( )", "paren", KeyItem.Group.STRUCTURE),
        KeyItem("α", "alpha", KeyItem.Group.GREEK),
        KeyItem("β", "beta", KeyItem.Group.GREEK),
        KeyItem("γ", "gamma", KeyItem.Group.GREEK),
        KeyItem("θ", "theta", KeyItem.Group.GREEK),
        KeyItem("π", "pi", KeyItem.Group.GREEK),
        // 第 3 行：希腊字母 + 运算符
        KeyItem("λ", "lambda", KeyItem.Group.GREEK),
        KeyItem("μ", "mu", KeyItem.Group.GREEK),
        KeyItem("σ", "sigma", KeyItem.Group.GREEK),
        KeyItem("ω", "omega", KeyItem.Group.GREEK),
        KeyItem("∞", "infty", KeyItem.Group.GREEK),
        KeyItem("+", "plus", KeyItem.Group.OPERATOR),
        KeyItem("−", "minus", KeyItem.Group.OPERATOR),
        // 第 4 行：运算符
        KeyItem("×", "times", KeyItem.Group.OPERATOR),
        KeyItem("÷", "div", KeyItem.Group.OPERATOR),
        KeyItem("≠", "ne", KeyItem.Group.OPERATOR),
        KeyItem("≤", "le", KeyItem.Group.OPERATOR),
        KeyItem("≥", "ge", KeyItem.Group.OPERATOR),
        KeyItem("→", "to", KeyItem.Group.OPERATOR),
        KeyItem("⇒", "rArr", KeyItem.Group.OPERATOR),
        // 第 5 行：剩余运算符 + 撤销/重做/清空/方框跳转（□← = 跳到上一个方框）
        KeyItem("∈", "isin", KeyItem.Group.OPERATOR),
        KeyItem("⊂", "subset", KeyItem.Group.OPERATOR),
        KeyItem("↶", "undo", KeyItem.Group.EDIT),
        KeyItem("↷", "redo", KeyItem.Group.EDIT),
        KeyItem("C", "clear", KeyItem.Group.EDIT),
        KeyItem("□←", "prev", KeyItem.Group.CURSOR),
        KeyItem("□→", "next", KeyItem.Group.CURSOR)
    )

    /**
     * 字母数字页的一行。
     * @param indentStart / indentEnd 左右缩进，单位是"一个键宽"（0.5 = 半格、1.5 = 一格半），
     *        用来做出实体键盘那种错位感。
     */
    data class CharRow(val keys: List<KeyItem>, val indentStart: Float = 0f, val indentEnd: Float = 0f)

    /** ② 字母数字页：仿实体键盘 */
    val CHAR_ROWS: List<CharRow> = listOf(
        // 数字行（手机键盘的最上面一排）
        CharRow(chars("1234567890", KeyItem.Group.DIGIT)),
        // QWERTY 三排，后两排按键盘习惯左右缩进
        CharRow(chars("qwertyuiop", KeyItem.Group.LETTER)),
        CharRow(chars("asdfghjkl", KeyItem.Group.LETTER), indentStart = 0.5f, indentEnd = 0.5f),
        CharRow(chars("zxcvbnm", KeyItem.Group.LETTER), indentStart = 1.5f, indentEnd = 1.5f),
        // 打字时最常用的运算符
        CharRow(
            listOf(
                KeyItem("+", "plus", KeyItem.Group.OPERATOR),
                KeyItem("−", "minus", KeyItem.Group.OPERATOR),
                KeyItem("×", "times", KeyItem.Group.OPERATOR),
                KeyItem("÷", "div", KeyItem.Group.OPERATOR),
                KeyItem("( )", "paren", KeyItem.Group.STRUCTURE)
            )
        )
    )

    /** 把一串字符做成一排按键（字母 / 数字都用它） */
    private fun chars(text: String, group: KeyItem.Group): List<KeyItem> =
        text.map { KeyItem("$it", "char:$it", group) }

    /** ③ 固定行（永远可见，键更大）：光标 / 等号 / 删除 */
    val FIXED_ROW: List<KeyItem> = listOf(
        KeyItem("←", "left", KeyItem.Group.CURSOR),
        KeyItem("↑", "up", KeyItem.Group.CURSOR),
        KeyItem("↓", "down", KeyItem.Group.CURSOR),
        KeyItem("→", "right", KeyItem.Group.CURSOR),
        KeyItem("=", "eq", KeyItem.Group.CURSOR),
        KeyItem("⌫", "backspace", KeyItem.Group.CURSOR)
    )

    /** 兼容旧调用：全部按键 */
    val ALL: List<KeyItem> = SYMBOL_PAGE + CHAR_ROWS.flatMap { it.keys } + FIXED_ROW
}
