# 第三方组件声明 / Third-Party Notices

本仓库的 App 在 `app/src/main/assets/editor/lib/` 下**原样打包**了两个第三方前端库，
用于离线运行公式编辑器（不联网、不依赖 CDN）。它们的许可证与版权归各自作者所有，
与本项目自身的 MIT 许可证相互独立。

---

## 1. jQuery 1.12.4

- 文件：`app/src/main/assets/editor/lib/jquery.min.js`
- 许可证：**MIT**
- 版权：`(c) jQuery Foundation` — <https://jquery.org/license>
- 是否修改：**否**，原样使用（文件头的许可证声明保留）

## 2. MathQuill 0.10.1

- 文件：
  - `app/src/main/assets/editor/lib/mathquill.min.js`
  - `app/src/main/assets/editor/lib/mathquill.css`
- 许可证：**Mozilla Public License 2.0 (MPL-2.0)**
- 版权：`MathQuill v0.10.1 by Han, Jeanine, and Mary <maintainers@mathquill.com>` — <http://mathquill.com>
- 许可证全文：<https://mozilla.org/MPL/2.0/>
- 原始来源：<https://github.com/mathquill/mathquill>（v0.10.1，即 0.10.1 发布版）

### 对 MathQuill 的修改（按 MPL-2.0 要求声明）

`mathquill.min.js` **未做任何修改**，文件头的 MPL 声明完整保留。

`mathquill.css` **有修改**：删除了文件开头的 `@font-face { font-family: Symbola; ... }` 规则块。
原因与影响：

- 原规则会去加载约 1.5 MB 的 Symbola 字体文件（`font/Symbola.*`）；本 App 为了控制体积不打包该字体，
  因此这条规则只会产生一次无效请求；
- 删除后公式符号（∑ ∫ √ ∞ ∈ ⊂ ≤ ≥ 等）改为回退到 Android 系统自带字体。
  经实测，常用数学符号在系统字体中均有覆盖，显示正常；
- 除该规则块外，`mathquill.css` 其余内容与上游 0.10.1 发布版完全一致，文件头的 MPL 声明保留。

由于 MPL-2.0 是**文件级**的 copyleft：上述两个 MathQuill 文件（及其修改版）仍然受 MPL-2.0 约束，
任何人都可以获取、修改并再分发它们；本项目自身的代码（Kotlin / HTML / 自己的 JS / 资源）采用 MIT 许可证，
两者互不影响。

---

## 3. 关于 Symbola 字体

上游 MathQuill 附带 Symbola 字体，本仓库**不包含**该字体文件。
如需最佳显示效果，可自行下载 Symbola 并把 `@font-face` 规则加回 `mathquill.css`。

---

## 4. Android 与 Android SDK

App 使用系统 WebView、系统控件与 Android SDK（Apache-2.0），这些组件不随本仓库分发，
由设备与开发环境提供。
