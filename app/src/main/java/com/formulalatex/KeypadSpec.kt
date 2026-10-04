package com.formulalatex

/**
 * 屏幕键盘的按键定义（数据与界面分离，改按钮只动这个文件）。
 *
 * @param label  按钮上显示的文字（全中文，用户不需要懂 LaTeX）
 * @param action 传给网页的动作名，对应 assets/editor/editor.js 里的
 *               TEMPLATES / SYMBOLS / 以及 undo、redo、clear、backspace、prev、next
 * @param group  分组，只用于选择按钮配色
 */
data class KeyItem(val label: String, val action: String, val group: Group) {

    enum class Group { STRUCTURE, GREEK, OPERATOR, EDIT }
}

/** 全部按键：结构 -> 希腊字母 -> 运算符 -> 编辑操作 */
object KeypadSpec {

    /** 结构模板：点一下插入 \frac{□}{□} 这类骨架，光标自动进第一个方框 */
    private val STRUCTURE = listOf(
        KeyItem("分数", "frac", KeyItem.Group.STRUCTURE),
        KeyItem("上标", "sup", KeyItem.Group.STRUCTURE),
        KeyItem("下标", "sub", KeyItem.Group.STRUCTURE),
        KeyItem("上下标", "supsub", KeyItem.Group.STRUCTURE),
        KeyItem("求和", "sum", KeyItem.Group.STRUCTURE),
        KeyItem("极限", "lim", KeyItem.Group.STRUCTURE),
        KeyItem("积分", "int", KeyItem.Group.STRUCTURE),
        KeyItem("根号", "sqrt", KeyItem.Group.STRUCTURE),
        KeyItem("括号", "paren", KeyItem.Group.STRUCTURE)
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

    /** 编辑操作 */
    private val EDIT = listOf(
        KeyItem("上一步", "prev", KeyItem.Group.EDIT),
        KeyItem("下一步", "next", KeyItem.Group.EDIT),
        KeyItem("删除", "backspace", KeyItem.Group.EDIT),
        KeyItem("清空", "clear", KeyItem.Group.EDIT),
        KeyItem("撤销", "undo", KeyItem.Group.EDIT),
        KeyItem("重做", "redo", KeyItem.Group.EDIT)
    )

    /** 按屏幕上的先后顺序排列的全部按钮 */
    val ALL: List<KeyItem> = STRUCTURE + GREEK + OPERATOR + EDIT

    /** 按下这些按钮后主动弹出系统键盘：因为接下来一定要打字 */
    val NEEDS_KEYBOARD: Set<String> = STRUCTURE.map { it.action }.toSet()
}
