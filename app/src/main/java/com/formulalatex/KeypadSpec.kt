package com.formulalatex

/**
 * 屏幕键盘的按键定义（数据与界面分离，改按钮只动这个文件）。
 *
 * @param label  按钮上显示的**数学符号**（不给用户看"极限/根号"这类中文词）
 * @param action 传给网页的动作名，对应 assets/editor/editor.js 里的
 *               TEMPLATES / SYMBOLS / ARROWS / 以及 undo、redo、clear、backspace、prev、next
 * @param group  分组，决定用哪种 Material 按钮样式
 */
data class KeyItem(val label: String, val action: String, val group: Group) {

    /**
     * 按键分组，直接对应三种 Material 3 按钮样式：
     *   STRUCTURE / CURSOR —— 实心淡色按钮（FilledTonalButton），最显眼
     *   GREEK / OPERATOR   —— 描边按钮（OutlinedButton）
     *   EDIT               —— 文字按钮（TextButton），最轻
     */
    enum class Group { STRUCTURE, GREEK, OPERATOR, CURSOR, EDIT }
}

/** 全部按键：结构 -> 希腊字母与运算符 -> 光标与编辑 */
object KeypadSpec {

    /**
     * 结构模板：点一下插入骨架，光标自动进第一个方框。
     * 标签一律用数学符号：□/□ 表示分数、x² 上标、x₂ 下标、x₂² 上下标、
     * ∑ 求和、lim 极限、∫ 积分、√ 根号、( ) 括号。
     */
    private val STRUCTURE = listOf(
        KeyItem("□/□", "frac", KeyItem.Group.STRUCTURE),
        KeyItem("x²", "sup", KeyItem.Group.STRUCTURE),
        KeyItem("x₂", "sub", KeyItem.Group.STRUCTURE),
        KeyItem("x₂²", "supsub", KeyItem.Group.STRUCTURE),
        KeyItem("∑", "sum", KeyItem.Group.STRUCTURE),
        KeyItem("lim", "lim", KeyItem.Group.STRUCTURE),
        KeyItem("∫", "int", KeyItem.Group.STRUCTURE),
        KeyItem("√", "sqrt", KeyItem.Group.STRUCTURE),
        KeyItem("( )", "paren", KeyItem.Group.STRUCTURE)
    )

    /** 希腊字母 */
    private val GREEK = listOf(
        KeyItem("α", "alpha", KeyItem.Group.GREEK),
        KeyItem("β", "beta", KeyItem.Group.GREEK),
        KeyItem("γ", "gamma", KeyItem.Group.GREEK),
        KeyItem("θ", "theta", KeyItem.Group.GREEK),
        KeyItem("π", "pi", KeyItem.Group.GREEK),
        KeyItem("λ", "lambda", KeyItem.Group.GREEK),
        KeyItem("μ", "mu", KeyItem.Group.GREEK),
        KeyItem("σ", "sigma", KeyItem.Group.GREEK),
        KeyItem("ω", "omega", KeyItem.Group.GREEK),
        KeyItem("∞", "infty", KeyItem.Group.GREEK)
    )

    /** 运算符 */
    private val OPERATOR = listOf(
        KeyItem("+", "plus", KeyItem.Group.OPERATOR),
        KeyItem("−", "minus", KeyItem.Group.OPERATOR),
        KeyItem("×", "times", KeyItem.Group.OPERATOR),
        KeyItem("÷", "div", KeyItem.Group.OPERATOR),
        KeyItem("=", "eq", KeyItem.Group.OPERATOR),
        KeyItem("≠", "ne", KeyItem.Group.OPERATOR),
        KeyItem("≤", "le", KeyItem.Group.OPERATOR),
        KeyItem("≥", "ge", KeyItem.Group.OPERATOR),
        KeyItem("→", "to", KeyItem.Group.OPERATOR),
        KeyItem("⇒", "rArr", KeyItem.Group.OPERATOR),
        KeyItem("∈", "isin", KeyItem.Group.OPERATOR),
        KeyItem("⊂", "subset", KeyItem.Group.OPERATOR)
    )

    /**
     * 光标方向键 + 方框跳转：
     *   ← ↑ ↓ →   移动光标（↑↓ 在分母/分子、下限/上限之间切换）
     *   ⇦ ⇨       在「方框」之间跳（填求和上下限、极限条件时最顺手）
     */
    private val CURSOR = listOf(
        KeyItem("←", "left", KeyItem.Group.CURSOR),
        KeyItem("↑", "up", KeyItem.Group.CURSOR),
        KeyItem("↓", "down", KeyItem.Group.CURSOR),
        KeyItem("→", "right", KeyItem.Group.CURSOR),
        KeyItem("⇦", "prev", KeyItem.Group.CURSOR),
        KeyItem("⇨", "next", KeyItem.Group.CURSOR)
    )

    /** 编辑操作：撤销 ↶、重做 ↷、删除 ⌫、清空 C */
    private val EDIT = listOf(
        KeyItem("↶", "undo", KeyItem.Group.EDIT),
        KeyItem("↷", "redo", KeyItem.Group.EDIT),
        KeyItem("⌫", "backspace", KeyItem.Group.EDIT),
        KeyItem("C", "clear", KeyItem.Group.EDIT)
    )

    /** 三段键盘的内容：结构 / 希腊字母+运算符 / 光标+编辑 */
    val STRUCTURE_KEYS: List<KeyItem> = STRUCTURE
    val SYMBOL_KEYS: List<KeyItem> = GREEK + OPERATOR
    val EDIT_KEYS: List<KeyItem> = CURSOR + EDIT

    /** 兼容旧调用：按屏幕顺序排列的全部按钮 */
    val ALL: List<KeyItem> = STRUCTURE + SYMBOL_KEYS + EDIT_KEYS

    /** 按下这些按钮后主动弹出系统键盘：因为接下来一定要打字 */
    val NEEDS_KEYBOARD: Set<String> = STRUCTURE.map { it.action }.toSet()
}
