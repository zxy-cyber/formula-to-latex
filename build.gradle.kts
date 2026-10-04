// 只需要 Android Gradle Plugin：
// AGP 9 自带 Kotlin 编译支持，再叠加 org.jetbrains.kotlin.android 会报
// "Cannot add extension with name 'kotlin'"，所以这里不声明 Kotlin 插件。
plugins {
    id("com.android.application") version "9.4.1" apply false
}
