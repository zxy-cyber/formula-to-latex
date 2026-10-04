package com.formulalatex

import android.content.Context
import org.json.JSONArray

/**
 * 公式历史。
 *
 * - 存在应用私有的 SharedPreferences 里（一个 JSON 数组），**不联网、不上传、不写外部存储**
 * - 最新的排在最前面；同一条公式再次记录时只把它提到最前（去重）
 * - 最多保留 [MAX] 条，超出丢弃最旧的
 * - 只用系统自带的 org.json，不引入任何依赖
 */
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 读出全部历史（最新的在前） */
    fun items(): List<String> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length())
                .map { arr.optString(it) }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            // 数据损坏时当作空历史，不要让 App 崩掉
            emptyList()
        }
    }

    /** 记一条；返回记录之后的完整列表 */
    fun add(latex: String): List<String> {
        if (latex.isBlank()) return items()
        val list = items().toMutableList()
        list.remove(latex)          // 去重：已存在就提到最前
        list.add(0, latex)
        while (list.size > MAX) list.removeAt(list.size - 1)
        save(list)
        return list
    }

    /** 删掉第 index 条；返回删除之后的列表 */
    fun removeAt(index: Int): List<String> {
        val list = items().toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            save(list)
        }
        return list
    }

    fun clear() {
        prefs.edit().remove(KEY_ITEMS).apply()
    }

    private fun save(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "formula_history"
        private const val KEY_ITEMS = "items"

        /** 最多保留多少条 */
        const val MAX = 50
    }
}
