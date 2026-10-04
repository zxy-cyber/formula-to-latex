import java.util.Properties

plugins {
    // AGP 9 内置 Kotlin 编译，不需要（也不能）再加 org.jetbrains.kotlin.android
    id("com.android.application")
}

// ---------------------------------------------------------------------------
// 发布签名：签名文件放在工程根目录 keystore/ 下，密码放在 keystore.properties。
// 这两个文件都不进版本库；万一没有（比如别人 clone 下来），release 走未签名包，
// debug 包不受影响，照样能跑。
// ---------------------------------------------------------------------------
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "com.formulalatex"
    // 你机器上装的是 platform android-37.0，所以用 37 编译；
    // targetSdk 仍然按需求停在 35。
    compileSdk = 37

    defaultConfig {
        applicationId = "com.formulalatex"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // 调试包不混淆，方便看崩溃堆栈
            isMinifyEnabled = false
        }
        release {
            // 打开混淆 + 资源压缩，APK 尽量小
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.version",
                "META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
    }
}

dependencies {
    // 刻意不引入任何第三方库：
    //   * 编辑区内核 MathQuill + jQuery 直接以 assets 形式离线打包，运行时不需要网络；
    //   * 界面只用系统控件（Activity / WebView / GridLayout），不引入 AndroidX 和 Material，
    //     这样 release 包体积极小，也没有多余字体和资源。
}
