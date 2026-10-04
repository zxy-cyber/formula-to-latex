# ---------------------------------------------------------------------------
# WebView 的 @JavascriptInterface 方法是靠反射调用的，混淆时必须保留，
# 否则 release 包会出现「点按钮没反应」的问题。
# ---------------------------------------------------------------------------
-keepclassmembers class com.formulalatex.MainActivity$EditorBridge {
    public *;
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# 保留行号，方便定位线上崩溃
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
