// 仓库地址：国内直连 Maven Central 经常超时，所以阿里云镜像放前面，
// 官方仓库留作兜底。有代理/能直连的话，把前三个 maven(...) 删掉即可。
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}

rootProject.name = "FormulaToLatex"
include(":app")
