package com.formulalatex

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView

/**
 * 一个「永远不弹出系统输入法」的 WebView。
 *
 * 原理：`onCreateInputConnection` 返回 null，等于告诉系统"这个 View 不提供输入连接"，
 * 输入法就不会被拉起。这样 MathQuill 内部那个隐藏 textarea 即使拿到焦点，
 * 也不会把系统键盘叫出来——输入完全交给应用自己的按键面板（字母 / 数字 / 符号）。
 *
 * 实体键盘不受影响：实体键走的是 `onKeyDown`，跟输入法连接无关，
 * 而且 editor.js 里的输入桥仍然保留，插上键盘也能直接打字。
 */
class NoImeWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? = null
}
