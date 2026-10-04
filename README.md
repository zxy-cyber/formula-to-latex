# 公式转LaTeX · FormulaToLatex

> 一个只有一屏的安卓小工具：**上面可视化编辑公式，下面实时显示 LaTeX 源码，底部一键复制。**
> 面向完全不懂 LaTeX 的人——全程只用中文按钮和系统键盘打字，**不需要输入任何反斜杠命令**。

*An Android app with a single screen that turns button taps into LaTeX.
Visual formula editor (WebView + MathQuill, fully offline) on top, live LaTeX source below,
one-tap copy at the bottom. UI is in Chinese; no LaTeX knowledge required.*

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![minSdk](https://img.shields.io/badge/minSdk-26-green.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-35-green.svg)
![APK](https://img.shields.io/badge/release%20APK-~100%20KB-orange.svg)
![Permissions](https://img.shields.io/badge/permissions-0-brightgreen.svg)
![Dependencies](https://img.shields.io/badge/third--party%20deps-none-lightgrey.svg)

---

## 界面

```
┌────────────────────────────────────┐
│                                    │
│        x² + 1      ∑ᵢ₌₁ⁿ   √x      │  ← ① 可视化编辑区（所见即所得，空位是方框 □）
│                                    │
├────────────────────────────────────┤
│ 分数 上标 下标 上下标 求和 极限     │  ← ② 结构按钮：点一下就插入骨架，
│ 积分 根号 括号                      │     光标自动跳进第一个方框
│ α β γ θ π λ μ σ ω ∞                │  ← 希腊字母
│ + − × ÷ = ≠ ≤ ≥ → ⇒ ∈ ⊂            │  ← 运算符
│ 上一步 下一步 删除 清空 撤销 重做    │  ← 编辑操作
├────────────────────────────────────┤
│ \sum_{i=1}^{n}                     │  ← ③ 实时 LaTeX 源码（只读）
├────────────────────────────────────┤
│           [ 复制 LaTeX ]           │  ← ④ 一键复制
└────────────────────────────────────┘
```

## 特性

- **不懂 LaTeX 也能用**：9 个中文结构按钮 + 10 个希腊字母 + 12 个运算符，全程零命令输入
- **所见即所得**：编辑区就是排好版的公式本身，空位显示为方框占位符
- **可嵌套**：在方框里继续点结构按钮就套进去，例如 `x^{a^{b}}`
- **方框间跳转**：「上一步 / 下一步」在分子、被开方数、求和上下限之间移动，专门为填上下限设计
- **完全离线**：编辑内核打包在 `assets` 里，**Manifest 一个权限都没申请**（没有网络权限）
- **极其轻量**：release 包约 **100 KB**；无 AndroidX、无 Material、无第三方依赖、无原生库、无 PNG 图标
- **撤销 / 重做 / 清空 / 删除**，以及实时 LaTeX 输出

## 环境与版本

本仓库验证过的组合（都能正常构建）：

| 组件 | 版本 | 说明 |
|---|---|---|
| Android Gradle Plugin | 9.4.1 | **AGP 9 已内置 Kotlin 编译**，不要再加 `org.jetbrains.kotlin.android` 插件，否则报 `Cannot add extension with name 'kotlin'` |
| Gradle | 9.8.0 | 见 `gradle/wrapper/gradle-wrapper.properties` |
| JDK | 17+（实测 25 可用） | 用 Android Studio 自带的 JBR 即可 |
| compileSdk / targetSdk / minSdk | 37 / 35 / 26 | |
| 编辑内核 | MathQuill 0.10.1 + jQuery 1.12.4 | 见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) |

## 构建

### Android Studio

1. `File → Open` 选择本仓库根目录（**不是** `app` 子目录）
2. 等 Gradle Sync 完成（首次会下载 Gradle 与 AGP 依赖）
3. 打包：
   - `Build → Build App Bundle(s) / APK(s) → Build APK(s)` → 产出 `app/build/outputs/apk/debug/app-debug.apk`
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

   这类路径在 aapt2 / R8 / 增量构建等环节历史上有过"某些机器莫名失败"的问题，所以 AGP 默认拦掉。
   救急可以在 `gradle.properties` 里加 `android.overridePathCheck=true`，但**更推荐把目录改成纯英文**。

2. **国内网络**。Maven Central 直连经常超时，`settings.gradle.kts` 里已经把阿里云镜像放在官方仓库前面；
   Gradle 发行版地址也指向了腾讯云镜像（SHA-256 与官方一致）。
   有代理或能直连的话，把这些镜像行删掉即可。

## 自己生成签名（仓库里没有 keystore）

本仓库**不包含**任何签名文件（`keystore/`、`keystore.properties` 都在 `.gitignore` 里）。
要打可安装的 release 包，自己生成一份即可：

```bash
keytool -genkeypair -v -keystore keystore/my-release.jks \
  -alias mykey -keyalg RSA -keysize 2048 -validity 10000
```

然后在仓库根目录建 `keystore.properties`：

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
│   ├── build.gradle.kts                 # 无任何 dependencies
│   ├── proguard-rules.pro               # 保住 @JavascriptInterface（否则 release 包按钮失灵）
│   └── src/main/
│       ├── AndroidManifest.xml          # 一个权限都没申请
│       ├── java/com/formulalatex/
│       │   ├── MainActivity.kt          # 唯一 Activity：WebView + 键盘 + 复制
│       │   └── KeypadSpec.kt            # 所有按键的定义（界面与数据分离）
│       ├── res/                         # 布局、配色、矢量图标（无 PNG）
│       └── assets/editor/
│           ├── index.html
│           ├── editor.css               # 方框占位符样式
│           ├── editor.js                # ★ 核心：模板插入 / 方框跳转 / 历史 / 输入桥
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

`_test` 里的「输入法提交 / 输入法退格」两项就是专门回归这条路径的。

### 2. 方框跳转不依赖私有 API

用 MathQuill 公开的 `mq.clickAt(x, y, 元素)`：传入方框元素 + 其左（右）缘外一点的坐标，
光标就精确落在该方框的左端 / 右端。方框按 DOM 顺序枚举，并做了一处修正：
`\sum` 的上限在 DOM 里排在下限前面，而用户习惯先填下限，所以排序时把下限提前。
（`\int` 用 `.mq-supsub/.mq-sub/.mq-sup`，`\sum` 用 `.mq-large-operator/.mq-from/.mq-to`，两种结构都识别。）

### 3. 极限的两个输入位

`\lim_{□ \to □}` 在 MathQuill 里其实是"一个下标块里放了一个 `→`"，所以这个方框被登记成两个槽位：
光标先停在 `→` 左边填趋近条件，点「下一步」跳到 `→` 右边填趋近值。

### 4. 「上标」的两种操作顺序都能用

- 先打底再点「上标」→ 光标直接进方框；
- 公式还空着时先点「上标」→ 光标先留在命令左边等用户打底，打完第一个字符自动跳进方框。

两种顺序都能得到 `x^{2}`。

### 5. LaTeX 规范化

只做等价改写：`{ }` → `{}`、`x^2` → `x^{2}`、`\to\infty` → `\to \infty`、去掉 `_`/`^` 前的空格。
注意补空格的正则要写成 `(\\[a-zA-Z]+)(?=\\)`（要求后面紧跟反斜杠），
否则正则回溯会把 `\frac` 拆成 `\fra c`——这个 bug 就是被下面那套自检抓出来的。

### 6. 撤销 / 重做 / 实时输出

MathQuill 没有撤销功能，这里自己维护 LaTeX 快照栈。
另外因为打字不会触发 MathQuill 的 `edit` 回调，除了回调还加了 150ms 兜底轮询 + 输入桥里的即时同步，
保证下方 LaTeX 始终是最新的。

## 自动化验收自检

`_test/` 里有一套 32 项的验收脚本：用 **Edge/Chrome 无头模式**（Blink 内核，与 Android WebView 同源）
加载与 App 完全一致的页面，模拟「点按钮 + 输入法打字」，逐条核对 LaTeX 结果。

```powershell
pwsh -NoProfile -File _test/run-selftest.ps1
```

覆盖内容：

| 验收点 | 期望结果 |
|---|---|
| 点「上标」，输入 x 和 2 | `x^{2}` |
| 点「求和」，输入 i=1 和 n | `\sum_{i=1}^{n}` |
| 点「极限」，输入 n 和 ∞ | `\lim_{n\to \infty}` |
| 在 `x^{□}` 的方框里再点「上标」 | `x^{a^{b}}` |
| 分数 / 下标 / 上下标 / 根号 / 括号 / 积分 | `\frac{1}{2}`、`x_{1}`、`x_{n}^{2}`、`\sqrt{x}`、`\left(x\right)`、`\int_{0}^{1}` |
| 希腊字母与全部运算符 | `\alpha+\beta`、`\le \pi \Rightarrow \in \subset` |
| 光标落点 | 分子 / 被开方数 / 下限方框 |
| 上一步 / 下一步、删除 / 清空 / 撤销 / 重做 | 通过 |
| 系统输入法路径 | 提交文字、退格 |

> `editor.js` 末尾有一段只在 URL 带 `?selftest=1` 时才生效的代码，它把内部 MathQuill 实例
> 暴露给自检脚本。App 加载的是不带参数的 `index.html`，所以正式运行时永远不会执行，请勿当作死代码删除。

## 已知限制

- 目前只支持上文列出的模板与符号，不做任意 LaTeX 解析（有意为之：面向零基础用户，界面越少越好）
- 编辑内核锁定 MathQuill 0.10.1（上游最后一个发布版），未跟进上游 master
- 界面为中文；若需要其它语言，欢迎 PR
- 未附带 Symbola 字体，个别生僻数学符号可能依赖系统字体回退（见第三方声明）
- 仓库暂时没有截图，欢迎 PR 补充

## 贡献

Issue / PR 都欢迎。改代码后请顺手跑一遍 `_test/run-selftest.ps1`，确认 32 项仍然全绿。

## 许可证

- 本项目自身代码：**MIT**，见 [LICENSE](LICENSE)
- 打包的第三方库：jQuery（MIT）、MathQuill（MPL-2.0，其中 `mathquill.css` 有修改），
  详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
