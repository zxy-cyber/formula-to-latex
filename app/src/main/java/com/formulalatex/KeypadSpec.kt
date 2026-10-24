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
     *   STRUCTURE / CURSOR / FIXED —— 实心淡色按钮（FilledTonalButton），最显眼
     *   GREEK / OPERATOR           —— 描边按钮（OutlinedButton）
     *   DIGIT / LETTER             —— 数字用实心淡色、字母用描边（数字更常用）
     *   EDIT                       —— 文字按钮（TextButton），最轻
     */
    enum class Group { STRUCTURE, GREEK, OPERATOR, DIGIT, LETTER, CURSOR, FIXED, EDIT }
}

/**
 * 键盘布局：
 *
 *   ① 符号页（6 列 × 5 行）
 *      结构模板 8 + 希腊字母 10 + 比较/关系运算符 6 + 撤销重做清空方框跳转 5
 *   ② 字母数字页（5 行，仿实体键盘）
 *      数字 1-5 → 数字 6-0 → QWERTY → ASDF（缩进半格）→ ZXCV（缩进一格半）
 *   ③ 固定区（两行，**永远可见、位置永远不变**，不随页切换）：
 *      ＋ − × ÷ ( )      —— 六个基本符号，两页都在同一个位置
 *      ←  ↑  ↓  →  =  ⌫  —— 光标 / 等号 / 删除，紧贴屏幕底部（拇指区）
 *
 * 说明：＋ − × ÷ ( ) 以前在符号页和字母数字页各有一份、位置还不一样，
 * 现在它们只放在固定区，位置固定不变。
 */
object KeypadSpec {

    /** ① 符号页：6 列 × 5 行（29 键，最后留 1 格空位） */
    val SYMBOL_PAGE: List<KeyItem> = listOf(
        // 第 1 行：结构模板
        KeyItem("□/□", "frac", KeyItem.Group.STRUCTURE),
        KeyItem("x²", "sup", KeyItem.Group.STRUCTURE),
        KeyItem("x₂", "sub", KeyItem.Group.STRUCTURE),
        KeyItem("x₂²", "supsub", KeyItem.Group.STRUCTURE),
        KeyItem("∑", "sum", KeyItem.Group.STRUCTURE),
        KeyItem("lim", "lim", KeyItem.Group.STRUCTURE),
        // 第 2 行：其余结构 + 希腊字母
        KeyItem("∫", "int", KeyItem.Group.STRUCTURE),
        KeyItem("√", "sqrt", KeyItem.Group.STRUCTURE),
        KeyItem("α", "alpha", KeyItem.Group.GREEK),
        KeyItem("β", "beta", KeyItem.Group.GREEK),
        KeyItem("γ", "gamma", KeyItem.Group.GREEK),
        KeyItem("θ", "theta", KeyItem.Group.GREEK),
        // 第 3 行：希腊字母
        KeyItem("π", "pi", KeyItem.Group.GREEK),
        KeyItem("λ", "lambda", KeyItem.Group.GREEK),
        KeyItem("μ", "mu", KeyItem.Group.GREEK),
        KeyItem("σ", "sigma", KeyItem.Group.GREEK),
        KeyItem("ω", "omega", KeyItem.Group.GREEK),
        KeyItem("∞", "infty", KeyItem.Group.GREEK),
        // 第 4 行：比较 / 关系运算符（＋ − × ÷ ( ) 在固定区，这里不重复）
        KeyItem("≠", "ne", KeyItem.Group.OPERATOR),
        KeyItem("≤", "le", KeyItem.Group.OPERATOR),
        KeyItem("≥", "ge", KeyItem.Group.OPERATOR),
        KeyItem("→", "to", KeyItem.Group.OPERATOR),
        KeyItem("⇒", "rArr", KeyItem.Group.OPERATOR),
        KeyItem("∈", "isin", KeyItem.Group.OPERATOR),
        // 第 5 行：集合 + 撤销/重做/清空/方框跳转（□← = 跳到上一个方框）
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

    /** ② 字母数字页：仿实体键盘，5 行（数字拆两排，键更大） */
    val CHAR_ROWS: List<CharRow> = listOf(
        CharRow(chars("12345", KeyItem.Group.DIGIT)),
        CharRow(chars("67890", KeyItem.Group.DIGIT)),
        CharRow(chars("qwertyuiop", KeyItem.Group.LETTER)),
        CharRow(chars("asdfghjkl", KeyItem.Group.LETTER), indentStart = 0.5f, indentEnd = 0.5f),
        CharRow(chars("zxcvbnm", KeyItem.Group.LETTER), indentStart = 1.5f, indentEnd = 1.5f)
    )

    /** 把一串字符做成一排按键（字母 / 数字都用它） */
    private fun chars(text: String, group: KeyItem.Group): List<KeyItem> =
        text.map { KeyItem("$it", "char:$it", group) }

    /** ③ 固定区第一行：六个基本符号，两页都在同一位置（= 也放这里，不再重复） */
    val FIXED_SYMBOLS: List<KeyItem> = listOf(
        KeyItem("+", "plus", KeyItem.Group.FIXED),
        KeyItem("−", "minus", KeyItem.Group.FIXED),
        KeyItem("×", "times", KeyItem.Group.FIXED),
        KeyItem("÷", "div", KeyItem.Group.FIXED),
        KeyItem("=", "eq", KeyItem.Group.FIXED),
        KeyItem("( )", "paren", KeyItem.Group.FIXED)
    )

    /** ③ 固定区第二行：光标 + 删除，紧贴屏幕底部（5 个键，宽一些更好按） */
    val FIXED_CURSOR: List<KeyItem> = listOf(
        KeyItem("←", "left", KeyItem.Group.FIXED),
        KeyItem("↑", "up", KeyItem.Group.FIXED),
        KeyItem("↓", "down", KeyItem.Group.FIXED),
        KeyItem("→", "right", KeyItem.Group.FIXED),
        KeyItem("⌫", "backspace", KeyItem.Group.FIXED)
    )

    /** 兼容旧调用 */
    val ALL: List<KeyItem> = SYMBOL_PAGE + CHAR_ROWS.flatMap { it.keys } + FIXED_SYMBOLS + FIXED_CURSOR
}
