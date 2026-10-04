# 公式转LaTeX · FormulaToLatex

> 一个只有一屏的安卓小工具：**上面可视化编辑公式，下面实时显示 LaTeX 源码，底部一键复制。**
> 面向完全不懂 LaTeX 的人——全程只用符号按钮和系统键盘打字，**不需要输入任何反斜杠命令**。

*An Android app with a single screen that turns button taps into LaTeX.
Visual formula editor (WebView + MathQuill, fully offline) on top, live LaTeX source below,
formula history in a bottom sheet, one-tap copy at the bottom.
UI is in Chinese; no LaTeX knowledge required.*

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![minSdk](https://img.shields.io/badge/minSdk-26-green.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-35-green.svg)
![APK](https://img.shields.io/badge/release%20APK-~1.4%20MB-orange.svg)
![System permissions](https://img.shields.io/badge/system%20permissions-0-brightgreen.svg)
![UI](https://img.shields.io/badge/UI-Material%203-6750A4.svg)

---

## 界面

```
┌────────────────────────────────────┐
│ 公式转LaTeX                    ☰   │  ← 标题栏；☰ 打开公式历史
├────────────────────────────────────┤
│                                    │
│        x² + 1      ∑ᵢ₌₁ⁿ   √x      │  ← ① 可视化编辑区（所见即所得，空位是方框 □）
│                                    │
├────────────────────────────────────┤
│      [ 符号 ] [ 字母数字 ]          │  ← ② 键盘分两页，各 6 行铺满，不用上下翻
├────────────────────────────────────┤
│  □/□   x²    x₂    x₂²   ∑    lim  │  ← 符号页：点一下插入骨架，
│  ∫     √     ( )   α     β    γ    │     光标自动进第一个方框
│  θ     π     λ     μ     σ    ω    │  ← 希腊字母
│  ∞     +     −     ×     ÷    ≠    │  ← 运算符
│  ≤     ≥     →     ⇒     ∈    ⊂    │
│  ↶     ↷     C     □←    □→         │  ← 撤销 / 重做 / 清空 / 方框跳转
├────────────────────────────────────┤
│  ←     ↑     ↓     →     =    ⌫    │  ← ③ 固定行：光标 / 等号 / 删除（永远可见、键更大）
├────────────────────────────────────┤
│ \sum_{i=1}^{n}            [ 复制 ] │  ← ④ 实时 LaTeX 源码 + 小巧的复制按钮
└────────────────────────────────────┘
```

「字母数字」页是 `0-9` + `a-z`（6 × 6 正好铺满）。**系统输入法永远不会弹出来**——
输入只用应用自己的按键，所以屏幕上不会出现键盘遮住半屏的情况。

点右上角 **☰** 会从底部弹出**公式历史**：每条历史都用 MathQuill 渲染成公式（不是给用户看 LaTeX 源码），
点一下载回编辑器，点右侧 ✕ 删除单条，右上「清空」清空全部。

## 特性

- **自带字母数字键盘，不弹系统输入法**：`0-9` + `a-z` 都在应用内，点「字母数字」页即可输入；
  系统键盘永远不会弹出来遮住半个屏幕
- **不用上下翻动**：键盘分「符号」「字母数字」两页，各 6 行正好铺满一屏；两页切换是毫秒级的显隐切换
- **方向键在拇指区**：`← ↑ ↓ →` `=` `⌫` 做成常驻固定行（不随页切换），键更大、位置更靠下
- **两种光标移动，各有用途**：
  - `← →` 逐字符移动，`↑ ↓` 在分母/分子、求和下限/上限这类**上下结构之间跳**
  - `□← □→` 在**方框之间跳**：公式里的空位是"块"，可能藏在一个块内部的左右两端
    （例如极限 `lim` 的下标里放了 `→`，两个输入位在同一个块内），
    这时 `← →` 得按很多下还会迷路，`□← □→` 一步到位
- **公式历史**：点「复制」即自动记录（去重、最多 50 条），底部弹层里以渲染好的公式呈现，可点选载回、单条删除、全部清空
- **撤销 / 重做 / 清空 / 删除**，以及实时 LaTeX 输出
- **完全离线**：编辑内核打包在 `assets` 里，Manifest 里**不申请任何系统权限**（没有网络权限）
- **Material 3 界面**：官方 Material Components，自带日间/夜间两套配色，跟随系统深色模式

## 体积说明

| 版本 | release 包 | 说明 |
|---|---|---|
| v1.0 | 99 KB | 不引入任何 AndroidX，纯系统控件 |
| v1.1 | 约 1.4 MB | 引入官方 Material Components（Material 3 主题、按钮、卡片、底部弹层） |
| v1.2 | 约 1.4 MB | 自带字母数字键盘、键盘分页、固定方向键行、复制按钮缩小 |

之所以没用 Jetpack Compose：Compose 会把包体推到 2.5–3 MB，而这个项目是单屏工具，
View 体系的 Material 3 已经足够表达，属于"主流但不臃肿"的取舍。

> 权限说明：应用**没有申请任何系统权限**。合并后的清单里会出现一条
> `com.formulalatex.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`——那是 AndroidX 自动声明的
> **自身签名级权限**，用于在 Android 13+ 保护动态注册的广播接收器，不涉及任何隐私能力。

## 下载安装

- 从 [Releases](https://github.com/zxy-cyber/formula-to-latex/releases/latest) 页面下载 APK
- 拷进手机点一下安装，按提示允许「安装未知应用」；或插数据线执行 `adb install -r <apk>`
- 要求 **Android 8.0（API 26）及以上**

## 环境与版本

本仓库验证过的组合：

| 组件 | 版本 | 说明 |
|---|---|---|
| Android Gradle Plugin | 9.4.1 | **AGP 9 已内置 Kotlin 编译**，不要再加 `org.jetbrains.kotlin.android` 插件，否则报 `Cannot add extension with name 'kotlin'` |
| Gradle | 9.8.0 | 见 `gradle/wrapper/gradle-wrapper.properties` |
| JDK | 17+（实测 25 可用） | 用 Android Studio 自带的 JBR 即可 |
| compileSdk / targetSdk / minSdk | 37 / 35 / 26 | |
| UI | Material Components 1.14.0 | 唯一的第三方依赖（会带入 appcompat / core 等 AndroidX 基础库） |
| 编辑内核 | MathQuill 0.10.1 + jQuery 1.12.4 | 离线 assets，见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) |

## 构建

### Android Studio

1. `File → Open` 选择本仓库根目录（**不是** `app` 子目录）
2. 等 Gradle Sync 完成（首次会下载 Gradle、AGP、Material 依赖）
3. 打包：
   - `Build → Build App Bundle(s) / APK(s) → Build APK(s)` → `app/build/outputs/apk/debug/app-debug.apk`
   - 正式包：`Build → Generate Signed App Bundle / APK…`（签名方式见下一节）

### 命令行

```bash
./gradlew assembleDebug      # 调试包
./gradlew assembleRelease    # 正式包（需自备签名，见下）
```

Windows 用 `gradlew.bat`。

### 两个容易踩的坑

1. **工程路径不要含中文（以及其它非 ASCII 字符）**。AGP 会直接拒绝构建：
   > Your project path contains non-ASCII characters.

   救急可以在 `gradle.properties` 里加 `android.overridePathCheck=true`，但**更推荐把目录改成纯英文**。

2. **国内网络**。Maven Central 直连经常超时，`settings.gradle.kts` 里已经把阿里云镜像放在官方仓库前面；
   Gradle 发行版地址也指向了腾讯云镜像（SHA-256 与官方一致）。有代理的话删掉那些镜像行即可。

## 自己生成签名（仓库里没有 keystore）

本仓库**不包含**任何签名文件（`keystore/`、`keystore.properties` 都在 `.gitignore` 里）。
要打可安装的 release 包，自己生成一份：

```bash
keytool -genkeypair -v -keystore keystore/my-release.jks \
  -alias mykey -keyalg RSA -keysize 2048 -validity 10000
```

然后建 `keystore.properties`：

```properties
storeFile=keystore/my-release.jks
storePassword=你的口令
keyAlias=mykey
keyPassword=你的口令
```

`app/build.gradle.kts` 会自动读取它；文件不存在时 release 走未签名（装不上），debug 包不受影响。

> ⚠️ 签名文件决定了应用能否覆盖安装升级，**丢了就只能卸载重装**。请自行妥善保存，切勿提交到仓库。

## 项目结构

```
.
├── app/
│   ├── build.gradle.kts                 # 唯一依赖：Material Components
│   ├── proguard-rules.pro               # 保住 @JavascriptInterface（否则 release 包按钮失灵）
│   └── src/main/
│       ├── AndroidManifest.xml          # 不申请任何系统权限
│       ├── java/com/formulalatex/
│       │   ├── MainActivity.kt          # 唯一 Activity：编辑区 / 键盘 / 历史弹层 / 复制
│       │   ├── KeypadSpec.kt            # 按键定义（符号 + 动作 + 分组 + 分页）
│       │   ├── NoImeWebView.kt          # 永不弹出系统输入法的 WebView
│       │   └── HistoryStore.kt          # 公式历史存储（SharedPreferences + org.json）
│       ├── res/
│       │   ├── layout/activity_main.xml # Material 3 单屏布局
│       │   ├── layout/sheet_history.xml # 历史底部弹层
│       │   ├── layout/key_{tonal,outlined,text}.xml   # 三种按键模板
│       │   ├── values{,-night}/         # 日间/夜间配色
│       │   └── drawable/                # 只有矢量图标，无 PNG
│       └── assets/editor/
│           ├── index.html / editor.js / editor.css    # ★ 编辑区（模板插入 / 方框跳转 / 输入桥）
│           ├── history.html / history.js / history.css # 历史列表（MathQuill 静态渲染）
│           └── lib/                     # jQuery + MathQuill（离线，见第三方声明）
└── _test/                               # 开发期自动化验收，不参与打包
```

## 实现要点（踩过的坑）

### 1. 安卓输入法打字（最关键的一个坑）

MathQuill 0.10.1 只监听 `keydown` / `keypress`：它的 `keydown` 处理里可打印字符分支直接
`return`，真正的文字是靠 `keypress` 之后回头去读隐藏 `textarea` 的值。
而安卓输入法（Gboard 等）提交文字走的是 **composition + `input` 事件**，
`keydown` 只有 `keyCode 229`，也不会补 `keypress` —— 结果是文字卡在 `textarea` 里，永远进不了公式。

所以 `editor.js` 里自己加了一座**输入桥**（`bindInputBridge`）：

- 监听 `input`，把提交的文字交给 `mq.typedText()`；
- 输入法的退格只有 `inputType: deleteContentBackward`、没有按键事件，补一次 `Backspace`；
- 用"最近是否发生过真实按键"判断，避免在桌面 / 外接键盘上把同一个字符输入两次。

### 2. 方向键直接用 MathQuill 的按键通道

`mq.keystroke('Left' / 'Right' / 'Up' / 'Down')` 就是真实键盘方向键走的那条路，
所以行为和用户预期完全一致：← → 逐字符移动，↑ ↓ 在分母/分子、求和下限/上限之间跳
（`Fraction.finalizeTree` / `SummationNotation.finalizeTree` 定义了这些上下关系）。

### 3. 方框跳转不依赖私有 API

用 MathQuill 公开的 `mq.clickAt(x, y, 元素)`：传入方框元素 + 其左（右）缘外一点的坐标，
光标就精确落在该方框的左端 / 右端。方框按 DOM 顺序枚举，并做了一处修正：
`\sum` 的上限在 DOM 里排在下限前面，而用户习惯先填下限，所以排序时把下限提前。
（`\int` 用 `.mq-supsub/.mq-sub/.mq-sup`，`\sum` 用 `.mq-large-operator/.mq-from/.mq-to`，两种结构都识别。）

### 4. 极限的两个输入位

`\lim_{□ \to □}` 在 MathQuill 里其实是"一个下标块里放了一个 `→`"，所以这个方框被登记成两个槽位：
光标先停在 `→` 左边填趋近条件，点「下一步」跳到 `→` 右边填趋近值。

### 5. 公式历史为什么是"渲染"而不是"显示文本"

目标用户看不懂 LaTeX，所以历史列表不能只列源码。这里复用同一套离线 MathQuill，
在弹层的 WebView 里用 `MathQuill.StaticMath` 把每条公式静态排版出来，
下面再附一行灰色小字的原始 LaTeX 供核对。

- 存储：`SharedPreferences` + 系统自带的 `org.json`，最多 50 条，去重后置顶，**不联网、不写外部存储**
- 记录时机：点「复制 LaTeX」＝这条公式真的被用上了，此时记一条（行为可预期，不会塞满垃圾）
- 交互：整行可点（载回编辑器）、右侧 ✕ 删单条、「清空」清全部
- 载回时用 `JSONObject.quote()` 转义 LaTeX 里的反斜杠，避免 JS 字符串被破坏

### 6. Material 3 迁移的一个坑

`MaterialButton` **没有公开的"带样式"构造函数**，所以不能在代码里 `MaterialButton(ctx, null, 0, styleRes)`。
这里改用 XML 模板（`key_tonal.xml` / `key_outlined.xml` / `key_text.xml`）按分组 inflate，
再统一设置字号、去掉按钮自带的上下 inset（`insetTop/insetBottom = 0`），让按键铺满格子。

另外 `MaterialButton` 默认 `textAllCaps=true`，会把 `lim` 显示成 `LIM`，模板里显式关掉了。

### 7. 彻底不让系统输入法弹出来

安卓上「点编辑区 → 系统键盘弹出来遮住半屏」是很烦的体验，而这个 App 的输入量本来很小
（字母、数字、符号都在自己键盘上）。做法是两件事：

1. 布局里用 `NoImeWebView`（`WebView` 子类）重写 `onCreateInputConnection` 返回 `null`——
   等于告诉系统"这个 View 不提供输入连接"，输入法就不会被拉起；
   MathQuill 内部那个隐藏 `textarea` 即使拿到焦点也叫不出键盘。
2. `windowSoftInputMode="stateAlwaysHidden|adjustResize"` 兜底。

字母/数字按键通过动作名 `char:x` 转到 `MQK.press()` → `mq.typedText('x')`，
和输入法打字走的是同一条路径，所以行为完全一致。插上实体键盘也照常能打字（实体键走 `onKeyDown`，与输入法无关）。

> 这里踩过一个坑：`press()` 原本会清掉"打完底自动跳进方框"的等待状态（那个机制是给输入法打字设计的）。
> 改成按键输入后，「先点上标再用字母键打底」就会两个字符都落在底上（得到 `x2^{}`）。
> 修法是只有结构/导航/编辑类动作才清除，字母与符号键保留——这个 regression 正是被自检用例抓出来的。

### 8. LaTeX 规范化

只做等价改写：`{ }` → `{}`、`x^2` → `x^{2}`、`\to\infty` → `\to \infty`、去掉 `_`/`^` 前的空格。
注意补空格的正则要写成 `(\\[a-zA-Z]+)(?=\\)`（要求后面紧跟反斜杠），
否则正则回溯会把 `\frac` 拆成 `\fra c`——这个 bug 就是被下面那套自检抓出来的。

## 自动化验收自检

`_test/` 里有两套自检脚本，用 **Edge/Chrome 无头模式**（Blink 内核，与 Android WebView 同源）
加载与 App 完全一致的页面：

```powershell
pwsh -NoProfile -File _test/run-selftest.ps1
```

| 用例页 | 项数 | 覆盖内容 |
|---|---|---|
| `acceptance.html` | 46 | 编辑区：上标 / 求和 / 极限 / 嵌套 / 全部模板 / 光标落点 / 方向键 / 撤销重做 / 系统输入法路径 / 字母数字键 / 历史载入 |
| `history-acceptance.html` | 14 | 历史弹层：渲染条数、公式确实被排版、点选回调、删除回调、空状态、深浅色 |

合计 **60 项**，全部通过。

> `editor.js` / `history.js` 末尾那段只在 URL 带 `?selftest=1` 时才生效的代码，
> 是给自检脚本暴露内部实例用的。App 加载的是不带参数的页面，正式运行时永远走不到，请勿当作死代码删除。

## 已知限制

- 只支持上文列出的模板与符号，不做任意 LaTeX 解析（面向零基础用户，有意保持界面精简）
- 编辑内核锁定 MathQuill 0.10.1（上游最后一个发布版），未跟进上游 master
- 历史只存在本机，不导出、不跨设备同步（刻意如此：应用不联网）
- 界面为中文
- 未附带 Symbola 字体，个别生僻数学符号依赖系统字体回退（见第三方声明）
- 仓库暂时没有截图，欢迎 PR 补充

## 贡献

Issue / PR 都欢迎。改代码后请顺手跑一遍 `_test/run-selftest.ps1`，确认 60 项仍然全绿。

## 许可证

- 本项目自身代码：**MIT**，见 [LICENSE](LICENSE)
- 打包的第三方库：jQuery（MIT）、MathQuill（MPL-2.0，其中 `mathquill.css` 有修改），
  详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
