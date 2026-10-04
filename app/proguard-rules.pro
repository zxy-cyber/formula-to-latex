# ---------------------------------------------------------------------------
# WebView 的 @JavascriptInterface 方法是靠反射调用的，混淆时必须保留，
# 否则 release 包会出现「点按钮没反应 / 历史点不开」这类问题。
# 这里用通配保住本应用所有桥接方法（编辑区、历史弹层各一个桥）。
# ---------------------------------------------------------------------------
-keepclassmembers class com.formulalatex.** {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.formulalatex.MainActivity$EditorBridge {
    public *;
}
-keepclassmembers class com.formulalatex.MainActivity$HistoryBridge {
    public *;
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# 保留行号，方便定位线上崩溃
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
