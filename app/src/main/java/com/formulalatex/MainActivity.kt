package com.formulalatex

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject

/**
 * 整个应用只有这一个界面（Material 3）：
 *
 *   ① 顶部：标题 + 「历史」按钮
 *   ② 公式编辑区   —— 系统 WebView + MathQuill（资源全部离线在 assets 里）
 *   ③ 按键区       —— 数学符号按钮，由 [KeypadSpec] 生成
 *   ④ LaTeX 源码   —— 实时刷新的只读代码块
 *   ⑤ 复制 LaTeX   —— 复制到剪贴板，并自动记入公式历史
 *
 * 没有输入法、收藏、设置、分享、引导、账号、网络：
 * Manifest 里一个权限都没申请，WebView 也显式关掉了网络加载。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var latexView: TextView

    private lateinit var historyStore: HistoryStore

    /** 网页每次改动公式都会把 LaTeX 推过来，复制按钮优先用它兜底 */
    @Volatile
    private var currentLatex: String = ""

    /** 网页回调对象；WebView 只持弱引用，所以这里用字段强引用住 */
    private val editorBridge = EditorBridge()
    private val historyBridge = HistoryBridge()

    /** 历史弹层（只创建一次，之后复用） */
    private var historyDialog: BottomSheetDialog? = null
    private var historyWebView: WebView? = null
    private var historyReady = false
    private var historyItems: List<String> = emptyList()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.mathWebView)
        latexView = findViewById(R.id.latexText)
        historyStore = HistoryStore(this)

        configureWebView(webView)
        webView.addJavascriptInterface(editorBridge, JS_BRIDGE)
        webView.loadUrl(EDITOR_URL)

        buildKeypad()

        findViewById<MaterialButton>(R.id.copyButton).setOnClickListener { copyLatex() }
        findViewById<MaterialButton>(R.id.historyButton).setOnClickListener { showHistory() }

        renderLatex("")
        applySystemBarAppearance()
    }

    /* ==================================================================
     * 一、WebView 配置：只用系统内核，只读 assets，不联网
     * ================================================================ */

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(view: WebView) {
        val settings = view.settings
        settings.javaScriptEnabled = true          // MathQuill 必须用 JS
        settings.domStorageEnabled = false         // 不需要任何本地存储
        settings.allowFileAccess = false           // 不去读文件系统（assets 仍可加载）
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
            settings.forceDark = WebSettings.FORCE_DARK_OFF    // 深浅色由页面自己控制
        }

        view.isVerticalScrollBarEnabled = false
        view.isHorizontalScrollBarEnabled = false
        view.overScrollMode = View.OVER_SCROLL_NEVER
        view.setBackgroundColor(Color.TRANSPARENT)
        view.webViewClient = WebViewClient()       // 站内跳转，绝不打开浏览器
    }

    /* ==================================================================
     * 二、按键面板：用代码按 Material 3 样式铺进三段 GridLayout
     * ================================================================ */

    private fun buildKeypad() {
        fillGrid(
            findViewById(R.id.keypadStructure), KeypadSpec.STRUCTURE_KEYS,
            labelSize = 20f, cellHeightDp = 48
        )
        fillGrid(
            findViewById(R.id.keypadSymbol), KeypadSpec.SYMBOL_KEYS,
            labelSize = 17f, cellHeightDp = 44
        )
        fillGrid(
            findViewById(R.id.keypadEdit), KeypadSpec.EDIT_KEYS,
            labelSize = 19f, cellHeightDp = 46
        )
    }

    private fun fillGrid(grid: GridLayout, keys: List<KeyItem>, labelSize: Float, cellHeightDp: Int) {
        val density = resources.displayMetrics.density
        val margin = (2 * density).toInt()

        keys.forEach { item ->
            // 从 XML 模板 inflate：MaterialButton 没有公开的"带样式"构造函数，
            // 用布局模板套 style 是最稳的做法
            val button = layoutInflater
                .inflate(templateFor(item.group), grid, false) as MaterialButton

            button.apply {
                text = item.label
                textSize = labelSize
                insetTop = 0                   // 去掉 Material 按钮自带的上下留白，
                insetBottom = 0                // 让按键铺满整个格子
                cornerRadius = (10 * density).toInt()
                setPadding(0, 0, 0, 0)
                setOnClickListener { view ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    pressKey(item.action)
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = (cellHeightDp * density).toInt()
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)   // 每列等宽
                setMargins(margin, margin, margin, margin)
            }
            grid.addView(button, params)
        }
    }

    /** 分组 -> 按键布局模板：结构/光标最显眼，符号次之，编辑操作最轻 */
    private fun templateFor(group: KeyItem.Group): Int = when (group) {
        KeyItem.Group.STRUCTURE, KeyItem.Group.CURSOR -> R.layout.key_tonal
        KeyItem.Group.GREEK, KeyItem.Group.OPERATOR -> R.layout.key_outlined
        KeyItem.Group.EDIT -> R.layout.key_text
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
     * 三、LaTeX 源码区 / 深浅色
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

    private val isNightMode: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

    /** 状态栏图标跟随深浅色 */
    private fun applySystemBarAppearance() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }
    }

    /** 把深浅色告诉网页，让编辑区跟着系统主题走 */
    private fun applyWebTheme() {
        val mode = if (isNightMode) "dark" else "light"
        webView.evaluateJavascript("window.MQK && window.MQK.setTheme('$mode');", null)
        historyWebView?.evaluateJavascript("window.MQHistory && window.MQHistory.setTheme('$mode');", null)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySystemBarAppearance()
        applyWebTheme()
    }

    /* ==================================================================
     * 四、复制 LaTeX（同时记入历史）
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

            // 复制成功 = 这条公式"用上了"，记进历史
            historyItems = historyStore.add(latex)
            pushHistoryToWeb()

            // Android 13 起系统自己会弹「已复制」，就不再重复提示
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
     * 五、公式历史（底部弹层，公式用 MathQuill 静态排版显示）
     * ================================================================ */

    private fun showHistory() {
        if (historyDialog == null) {
            val view = layoutInflater.inflate(R.layout.sheet_history, null)
            val web = view.findViewById<WebView>(R.id.historyWebView)
            configureWebView(web)
            web.addJavascriptInterface(historyBridge, JS_BRIDGE)
            web.loadUrl(HISTORY_URL)

            view.findViewById<MaterialButton>(R.id.clearHistoryButton).setOnClickListener {
                historyStore.clear()
                historyItems = emptyList()
                pushHistoryToWeb()
                toast(getString(R.string.history_cleared))
            }

            historyDialog = BottomSheetDialog(this).apply { setContentView(view) }
            historyWebView = web
        }
        historyItems = historyStore.items()
        pushHistoryToWeb()
        historyDialog?.show()
    }

    /** 把历史列表推给网页渲染（页面还没就绪就先不发，等 onReady 回调） */
    private fun pushHistoryToWeb() {
        val web = historyWebView ?: return
        if (!historyReady) return
        val arr = JSONArray()
        historyItems.forEach { arr.put(it) }
        val mode = if (isNightMode) "dark" else "light"
        web.evaluateJavascript(
            "window.MQHistory && window.MQHistory.render($arr, '$mode');", null
        )
    }

    /* ==================================================================
     * 六、网页 -> 原生 的回调
     * ================================================================ */

    /** 编辑区（WebView 里跑在 JS 线程上，所以要切回 UI 线程） */
    private inner class EditorBridge {

        @JavascriptInterface
        fun onLatexChanged(latex: String) {
            currentLatex = latex
            runOnUiThread { renderLatex(latex) }
        }

        @JavascriptInterface
        fun onEditorReady() {
            runOnUiThread {
                renderLatex(currentLatex)
                applyWebTheme()
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            runOnUiThread { toast(message) }
        }
    }

    /** 历史弹层 */
    private inner class HistoryBridge {

        @JavascriptInterface
        fun onReady() {
            runOnUiThread {
                historyReady = true
                pushHistoryToWeb()
            }
        }

        @JavascriptInterface
        fun onPick(index: Int) {
            runOnUiThread {
                val latex = historyItems.getOrNull(index) ?: return@runOnUiThread
                loadLatex(latex)
                historyDialog?.dismiss()
            }
        }

        @JavascriptInterface
        fun onDelete(index: Int) {
            runOnUiThread {
                historyItems = historyStore.removeAt(index)
                pushHistoryToWeb()
            }
        }
    }

    /** 把某条历史载回编辑器（用 JSONObject.quote 安全转义 LaTeX 里的反斜杠） */
    private fun loadLatex(latex: String) {
        val quoted = JSONObject.quote(latex)
        webView.evaluateJavascript("window.MQK && window.MQK.setLatex($quoted);", null)
    }

    /* ==================================================================
     * 七、杂项
     * ================================================================ */

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        historyWebView?.let { web ->
            web.removeJavascriptInterface(JS_BRIDGE)
            web.destroy()
        }
        historyWebView = null
        historyDialog = null
        webView.removeJavascriptInterface(JS_BRIDGE)
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val JS_BRIDGE = "Android"

        /** assets 下的离线页面 */
        const val EDITOR_URL = "file:///android_asset/editor/index.html"
        const val HISTORY_URL = "file:///android_asset/editor/history.html"
    }
}
