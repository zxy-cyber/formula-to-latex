package com.formulalatex

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast

/**
 * 整个应用只有这一个界面：
 *
 *   ① 上：公式编辑区   —— 系统 WebView + MathQuill（资源全部离线在 assets 里）
 *   ② 中：结构按钮键盘 —— 全中文，由 [KeypadSpec] 生成，不写死布局
 *   ③ 下：只读 LaTeX 源码（随编辑实时刷新）
 *   ④ 底：复制 LaTeX 按钮
 *
 * 没有输入法、历史、收藏、设置、分享、引导、账号、网络：
 * Manifest 里一个权限都没申请，WebView 也显式关掉了网络加载。
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var latexView: TextView

    /** 网页每次改动公式都会把 LaTeX 推过来，复制按钮优先用它兜底 */
    @Volatile
    private var currentLatex: String = ""

    /** 网页回调对象；WebView 只持弱引用，所以这里用字段强引用住 */
    private val bridge = EditorBridge()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.mathWebView)
        latexView = findViewById(R.id.latexText)

        setupWebView()
        buildKeypad(findViewById(R.id.keypad))
        findViewById<TextView>(R.id.copyButton).setOnClickListener { copyLatex() }

        renderLatex("")
    }

    /* ==================================================================
     * 一、WebView 配置：只用系统内核，只读 assets，不联网
     * ================================================================ */

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true          // MathQuill 必须用 JS
        settings.domStorageEnabled = false         // 不需要任何本地存储
        settings.databaseEnabled = false
        settings.allowFileAccess = false           // 不去读文件系统
        settings.allowContentAccess = false
        settings.blockNetworkLoads = true          // 双保险：禁止网络加载
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.mediaPlaybackRequiresUserGesture = true
        settings.textZoom = 100

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            settings.forceDark = WebSettings.FORCE_DARK_OFF    // 别把公式反色
        }

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.setBackgroundColor(0xFFFFFFFF.toInt())
        webView.webViewClient = WebViewClient()    // 站内跳转，绝不打开浏览器
        webView.addJavascriptInterface(bridge, "Android")
        webView.loadUrl(EDITOR_URL)
    }

    /* ==================================================================
     * 二、按键面板：用代码铺进 GridLayout
     * ================================================================ */

    private fun buildKeypad(grid: GridLayout) {
        val margin = dp(2)
        val cellHeight = dp(42)

        KeypadSpec.ALL.forEach { item ->
            /* 用 TextView 而不是 Button：Button 在 Material 主题下会自带
               backgroundTint、minHeight、stateListAnimator 和全大写，
               外观不好控制；TextView + 选择器 drawable 更可控也更小。 */
            val key = TextView(this).apply {
                text = item.label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                setTextColor(getColor(R.color.key_text))
                setBackgroundResource(
                    if (item.group == KeyItem.Group.EDIT) R.drawable.key_edit_bg
                    else R.drawable.key_bg
                )
                isClickable = true
                isFocusable = true
                setOnClickListener { pressKey(item.action) }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = cellHeight
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)   // 每列等宽
                setMargins(margin, margin, margin, margin)
            }
            grid.addView(key, params)
        }
    }

    /** 把一个按键转发给网页里的 MQK.press(action) */
    private fun pressKey(action: String) {
        webView.evaluateJavascript("window.MQK && window.MQK.press('$action');", null)
        if (KeypadSpec.NEEDS_KEYBOARD.contains(action)) {
            showKeyboardLater()
        }
    }

    /**
     * 结构按钮按下后主动唤起系统键盘。
     * 网页里 focus 是异步完成的，所以延后一点点再弹，成功率高。
     */
    private fun showKeyboardLater() {
        webView.requestFocus()
        webView.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
        }, 80)
    }

    /* ==================================================================
     * 三、下方只读 LaTeX 区
     * ================================================================ */

    private fun renderLatex(latex: String) {
        if (latex.isEmpty()) {
            latexView.text = getString(R.string.latex_placeholder)
            latexView.setTextColor(getColor(R.color.code_hint))
        } else {
            latexView.text = latex
            latexView.setTextColor(getColor(R.color.code_text))
        }
    }

    /* ==================================================================
     * 四、复制 LaTeX
     * ================================================================ */

    private fun copyLatex() {
        // 直接向网页要一次最新值，避免最后一次输入还在路上
        webView.evaluateJavascript("window.MQK ? window.MQK.currentLatex() : ''") { raw ->
            val latex = unescapeJson(raw).ifEmpty { currentLatex }
            if (latex.isEmpty()) {
                toast(getString(R.string.nothing_to_copy))
                return@evaluateJavascript
            }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("LaTeX", latex))
            // Android 13 起系统自己会弹「已复制」，就不重复提示了
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                toast(getString(R.string.copied))
            }
        }
    }

    /** evaluateJavascript 回调给的是 JSON 字符串字面量，这里做一次反转义 */
    private fun unescapeJson(raw: String?): String {
        if (raw == null || raw == "null") return ""
        var text = raw
        if (text.length >= 2 && text.first() == '"' && text.last() == '"') {
            text = text.substring(1, text.length - 1)
        }
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (val next = text[i + 1]) {
                    'n' -> { out.append('\n'); i += 2 }
                    't' -> { out.append('\t'); i += 2 }
                    'r' -> { out.append('\r'); i += 2 }
                    'b' -> { out.append('\b'); i += 2 }
                    'f' -> { out.append('\u000C'); i += 2 }
                    'u' -> {
                        if (i + 5 < text.length) {
                            out.append(text.substring(i + 2, i + 6).toInt(16).toChar())
                            i += 6
                        } else {
                            out.append(next); i += 2
                        }
                    }
                    else -> { out.append(next); i += 2 }
                }
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    /* ==================================================================
     * 五、网页 -> 原生 的回调
     * ================================================================ */

    private inner class EditorBridge {

        /** 公式每变一次都会调用（运行在 WebView 的 JS 线程上） */
        @JavascriptInterface
        fun onLatexChanged(latex: String) {
            currentLatex = latex
            runOnUiThread { renderLatex(latex) }
        }

        @JavascriptInterface
        fun onEditorReady() {
            runOnUiThread { renderLatex(currentLatex) }
        }

        /** 网页出错时把原因带出来，便于定位（不弹引导，只提示一句） */
        @JavascriptInterface
        fun onError(message: String) {
            runOnUiThread { toast(message) }
        }
    }

    /* ==================================================================
     * 六、杂项
     * ================================================================ */

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("Android")
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        /** assets 下的离线页面 */
        const val EDITOR_URL = "file:///android_asset/editor/index.html"
    }
}
