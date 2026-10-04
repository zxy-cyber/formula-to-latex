# 实现笔记

这里记录开发过程中真正花了时间的地方，供后续维护者参考。
README 只留结论，细节放在这里。

## 1. 不弹系统输入法

安卓上「点编辑区 → 系统键盘弹出来遮住半屏」很影响使用，而这个应用本身输入量很小
（字母、数字、符号都在自带键盘上）。做法是两件事：

1. 编辑区用 `NoImeWebView`（`WebView` 子类）重写 `onCreateInputConnection` 返回 `null`——
   等于告诉系统"这个 View 不提供输入连接"，输入法就不会被拉起。
   MathQuill 内部那个隐藏 `textarea` 即使拿到焦点，也叫不出键盘。
2. `windowSoftInputMode="stateAlwaysHidden|adjustResize"` 兜底。

字母与数字键的动作名形如 `char:x`，在 `editor.js` 里转成 `mq.typedText('x')`，
和输入法打字走的是同一条路径，行为完全一致。插上实体键盘照常能打字（实体键走 `onKeyDown`，与输入法无关）。

**踩过的坑**：`press()` 原本会清掉「打完底自动跳进方框」的等待状态（那个机制是给输入法打字设计的）。
改成按键输入后，「先点『上标』再用字母键打底」就会两个字符都落在底上（得到 `x2^{}` 而不是 `x^{2}`）。
修法是只有结构 / 导航 / 编辑类动作才清除，字母与符号键保留。这个 regression 是被 `_test` 里的用例抓出来的。

## 2. 安卓输入法输入桥（保留给实体键盘或其它运行环境）

MathQuill 0.10.1 只监听 `keydown` / `keypress`：它的 `keydown` 处理里可打印字符分支直接 `return`，
真正的文字靠 `keypress` 之后回头去读隐藏 `textarea` 的值。
而安卓输入法提交文字走的是 composition + `input` 事件（`keydown` 只有 `keyCode 229`，也不会补 `keypress`），
结果文字会卡在 `textarea` 里进不了公式。

所以 `editor.js` 里有一座**输入桥**（`bindInputBridge`）：

- 监听 `input`，把提交的文字交给 `mq.typedText()`；
- 输入法退格只有 `inputType: deleteContentBackward`、没有按键事件，补一次 `Backspace`；
- 用"最近是否发生过真实按键"判断，避免在桌面 / 外接键盘上把同一个字符输入两次。

现在应用自带键盘后这条路径平时用不到，但保留它可以让实体键盘、以及将来可能的输入方式照常工作。

## 3. 光标与方框

- **方向键**：`mq.keystroke('Left' / 'Right' / 'Up' / 'Down')` 就是真实键盘方向键走的那条路。
  ↑ ↓ 之所以能在分子/分母、求和下限/上限之间跳，是因为 `Fraction.finalizeTree` /
  `SummationNotation.finalizeTree` 定义了这些上下关系。
- **方框跳转**（`□← □→`）：用公开 API `mq.clickAt(x, y, 方框元素)`——
  传入方框元素和它左（右）缘外一点的坐标，光标就精确落在该方框的左端 / 右端。
  方框按 DOM 顺序枚举，并做了一处修正：`\sum` 的上限在 DOM 里排在下限前面，
  而用户习惯先填下限，所以排序时把下限提前。
  （`\int` 用 `.mq-supsub/.mq-sub/.mq-sup`，`\sum` 用 `.mq-large-operator/.mq-from/.mq-to`，两种结构都识别。）
- **为什么两类箭头都要有**：极限 `\lim_{□ \to □}` 在 MathQuill 里是"一个下标块里放了一个 `→`"，
  两个输入位在**同一个块内部**，逐字符移动要按很多下还会迷路；`□← □→` 一步到位。

## 4. 「上标」的两种操作顺序都能用

- 先打底再点「上标」→ 光标直接进方框；
- 公式还空着时先点「上标」→ 光标先留在命令左边等用户打底，打完第一个字符自动跳进方框。

## 5. 公式历史为什么是"渲染"而不是"显示文本"

目标用户看不懂 LaTeX，所以历史列表不能只列源码。这里复用同一套离线 MathQuill，
在弹层的 WebView 里用 `MathQuill.StaticMath` 把每条公式静态排版出来，下面再附一行灰色小字的原始 LaTeX。

- 存储：`SharedPreferences` + 系统自带 `org.json`，最多 50 条，去重后置顶，不联网、不写外部存储
- 记录时机：点「复制」＝这条公式真的被用上了，此时记一条（行为可预期，不会塞满垃圾）
- 载回时用 `JSONObject.quote()` 转义 LaTeX 里的反斜杠，避免 JS 字符串被破坏

## 6. Material 3 迁移的两个坑

- `MaterialButton` **没有公开的"带样式"构造函数**，所以不能在代码里 `MaterialButton(ctx, null, 0, styleRes)`。
  改用 XML 模板（`key_tonal.xml` / `key_outlined.xml` / `key_text.xml`）按分组 inflate，
  再统一设置字号、去掉按钮自带的上下 inset（`insetTop/insetBottom = 0`），让按键铺满格子。
- `MaterialButton` 默认 `textAllCaps=true`，会把 `lim` 显示成 `LIM`，模板里显式关掉。

另外：`MathQuill.StaticMath(el)` 是把 `mq-math-mode` 类加到传入元素**自身**上（不是子元素），
写选择器时要注意。

## 7. LaTeX 规范化

只做等价改写：`{ }` → `{}`、`x^2` → `x^{2}`、`\to\infty` → `\to \infty`、去掉 `_`/`^` 前的空格。

**踩过的坑**：补空格的正则要写成 `(\\[a-zA-Z]+)(?=\\)`（要求后面紧跟反斜杠）。
如果写成只看下一个字符是不是字母/反斜杠，正则回溯会把 `\frac` 拆成 `\fra c`。

## 8. 撤销 / 重做 / 实时输出

MathQuill 没有撤销功能，这里自己维护 LaTeX 快照栈。
另外因为打字不会触发 MathQuill 的 `edit` 回调，除了回调还加了 150ms 兜底轮询 + 输入桥里的即时同步，
保证下方 LaTeX 始终是最新的。
