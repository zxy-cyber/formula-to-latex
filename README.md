# 公式转LaTeX · FormulaToLatex

> 一屏搞定的安卓小工具：上面可视化编辑公式，下面实时显示 LaTeX 源码，一键复制。
> 面向完全不懂 LaTeX 的人——字母、数字、符号全在应用自带键盘上，**不弹系统输入法**，
> 也不需要输入任何反斜杠命令。

*An Android app that turns button taps into LaTeX: a WYSIWYG math editor (WebView + MathQuill,
fully offline) with a built-in keypad, live LaTeX source, formula history and one-tap copy.*

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![minSdk](https://img.shields.io/badge/minSdk-26-green.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-35-green.svg)
![APK](https://img.shields.io/badge/release%20APK-~1.4%20MB-orange.svg)
![UI](https://img.shields.io/badge/UI-Material%203-6750A4.svg)

## 界面

```
┌────────────────────────────────────┐
│ 公式转LaTeX                    ☰   │  标题栏；☰ 打开公式历史
├────────────────────────────────────┤
│        x² + 1      ∑ᵢ₌₁ⁿ   √x      │  ① 可视化编辑区（所见即所得，空位是方框 □）
├────────────────────────────────────┤
│      [ 符号 ] [ 字母数字 ]          │  ② 键盘分两页，各 6 行铺满，不用上下翻
├────────────────────────────────────┤
│  □/□  x²  x₂  x₂²  ∑  lim  ∫  √  ( )│     结构模板 + 希腊字母 + 运算符
│  α β γ θ π λ μ σ ω ∞  + − × ÷ …    │
├────────────────────────────────────┤
│  ←   ↑   ↓   →   =   ⌫             │  ③ 固定行：光标 / 等号 / 删除，永远可见
├────────────────────────────────────┤
│ \sum_{i=1}^{n}            [ 复制 ] │  ④ 实时 LaTeX 源码 + 小巧的复制按钮
└────────────────────────────────────┘
```

## 特性

- **不用懂 LaTeX**：结构、希腊字母、运算符都是按钮，按钮上直接显示数学符号（`□/□` `x²` `∑` `lim` `∫` `√`）
- **不弹系统输入法**：`0-9` 与 `a-z` 都在应用自带的「字母数字」页，不会出现键盘遮住半屏
- **不用上下翻动**：键盘固定两页，各 6 行正好铺满一屏；方向键做成常驻固定行，就在拇指区
- **可嵌套**：在方框里继续点结构按钮即可，例如 `x^{a^{b}}`
- **公式历史**：点「复制」自动记录（去重、最多 50 条），底部弹层里以**渲染好的公式**呈现，可点选载回、单条删除
- **完全离线**：编辑内核打包在 `assets`，不申请任何系统权限（没有网络权限）
- **Material 3 界面**：官方 Material Components，跟随系统深色模式

## 下载安装

从 [Releases](https://github.com/zxy-cyber/formula-to-latex/releases/latest) 下载 APK，
拷进手机点安装（或 `adb install -r <apk>`），要求 **Android 8.0（API 26）及以上**。

> 应用不申请任何系统权限。合并清单里那条 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
> 是 AndroidX 自动声明的自身签名级权限（保护动态广播接收器），不涉及隐私能力。

## 构建

| 组件 | 版本 |
|---|---|
| Android Gradle Plugin | 9.4.1（**AGP 9 内置 Kotlin 编译**，不要再加 `org.jetbrains.kotlin.android` 插件） |
| Gradle | 9.8.0（见 `gradle/wrapper`） |
| JDK | 17+ |
| compileSdk / targetSdk / minSdk | 37 / 35 / 26 |
| UI | Material Components 1.14.0（唯一第三方依赖） |
| 编辑内核 | MathQuill 0.10.1 + jQuery 1.12.4（离线 assets） |

```bash
./gradlew assembleDebug        # 调试包
./gradlew assembleRelease      # 正式包（需自备签名，见下）
```

Android Studio：`File → Open` 选仓库根目录（不是 `app` 子目录），Sync 后直接 Build。

两个常见问题：

1. **工程路径不要含中文等非 ASCII 字符**，AGP 默认会拒绝构建。本仓库已用
   `android.overridePathCheck=true` 放行；路径是纯英文的话可以删掉这一行。
2. **国内网络**：`settings.gradle.kts` 里把阿里云镜像放在官方仓库前面，
   wrapper 的 Gradle 地址指向腾讯云镜像（SHA-256 与官方一致）。有代理可自行删除。

## 签名

仓库不含任何签名文件（`keystore/`、`keystore.properties` 都在 `.gitignore`）。
自己生成一份：

```bash
keytool -genkeypair -v -keystore keystore/my-release.jks \
  -alias mykey -keyalg RSA -keysize 2048 -validity 10000
```

再建 `keystore.properties`（`storeFile` / `storePassword` / `keyAlias` / `keyPassword`），
`app/build.gradle.kts` 会自动读取；文件不存在时 release 走未签名（装不上），debug 不受影响。

> 签名文件决定应用能否覆盖升级，丢了只能卸载重装。请妥善保存，切勿提交。

## 项目结构

```
app/src/main/
├── java/com/formulalatex/
│   ├── MainActivity.kt      # 唯一 Activity：编辑区 / 键盘 / 历史弹层 / 复制
│   ├── KeypadSpec.kt        # 按键定义（符号 + 动作 + 分组 + 分页）
│   ├── NoImeWebView.kt      # 永不弹出系统输入法的 WebView
│   └── HistoryStore.kt      # 历史存储（SharedPreferences + org.json）
├── res/layout/              # activity_main / sheet_history / key_*.xml
├── res/values{,-night}/     # 日间 + 夜间配色
└── assets/editor/           # index.html+editor.js（编辑区）/ history.*（历史列表）/ lib（jQuery+MathQuill）
_test/                       # 开发期自动化验收，不参与打包
```

## 实现要点

- **不弹系统输入法**：编辑区用 `NoImeWebView`（`onCreateInputConnection` 返回 `null`），
  字母数字键通过 `char:x` 动作走 `mq.typedText()`，与输入法打字同一条路径；实体键盘照常可用
- **光标与方框**：方向键直接走 MathQuill 的按键通道（↑↓ 在分子/分母、下限/上限之间跳）；
  `□← □→` 用公开 API `mq.clickAt()` 在方框之间跳（极限等模板的两个输入位在同一个块里，只能这样跳）
- **历史用渲染而不是文本**：目标用户看不懂 LaTeX，所以用 `MathQuill.StaticMath` 把每条历史排版出来

更细的踩坑记录（含安卓输入法的 `input` 事件桥、正则回溯把 `\frac` 拆成 `\fra c`、
`MaterialButton` 没有带样式的构造函数等）见 [docs/IMPLEMENTATION.md](docs/IMPLEMENTATION.md)。

## 自动化验收

`_test/` 下有两套脚本，用真实浏览器内核（Blink，与 Android WebView 同源）加载与 App 一致的页面，
模拟「点按钮 + 输入」并逐条核对结果：

```powershell
pwsh -NoProfile -File _test/run-selftest.ps1
```

| 用例页 | 项数 | 覆盖 |
|---|---|---|
| `acceptance.html` | 46 | 全部模板 / 光标落点 / 方向键 / 撤销重做 / 输入法路径 / 字母数字键 / 历史载入 |
| `history-acceptance.html` | 14 | 历史弹层渲染、点选与删除回调、空状态、深浅色 |

合计 **60 项**，当前全部通过。

## 已知限制

- 只支持上述模板与符号，不做任意 LaTeX 解析（面向零基础用户，有意保持精简）
- 编辑内核锁定 MathQuill 0.10.1（上游最后一个发布版）
- 历史只存本机，不导出、不跨设备同步（应用不联网）
- 界面为中文；未附带 Symbola 字体，个别生僻符号依赖系统字体回退
- 仓库暂无截图，欢迎 PR 补充

## 许可证

本项目代码：[MIT](LICENSE)。
打包的第三方库：jQuery（MIT）、MathQuill（MPL-2.0，其中 `mathquill.css` 有修改），
详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
