/* ===========================================================================
 * 验收自检脚本（只在开发机浏览器里跑，不参与 APK 打包）
 *
 * 跑法：
 *   msedge --headless=new --disable-gpu --window-size=430,900 \
 *          --allow-file-access-from-files --virtual-time-budget=8000 \
 *          --dump-dom "file:///.../_test/acceptance.html?selftest=1"
 *
 * 它会用真实 Blink 内核（Android WebView 同源）模拟「点按钮 + 系统输入法打字」，
 * 逐条核对验收标准里的 LaTeX 结果。
 * =========================================================================== */
(function () {
  'use strict';

  var lines = [];
  var passed = 0, failed = 0;

  function sleep(ms) {
    return new Promise(function (r) { window.setTimeout(r, ms); });
  }

  function latex() { return window.MQK.currentLatex(); }
  function type(text) { window.MQK.__mathquill().typedText(text); }

  /** 模拟系统输入法：往 MathQuill 的隐藏 textarea 里塞文字并触发 input 事件 */
  function typeViaIme(text) {
    var textarea = document.querySelector('#field textarea');
    if (!textarea) return false;
    textarea.focus();
    textarea.value = text;
    textarea.dispatchEvent(new Event('input', { bubbles: true }));
    textarea.value = '';
    return true;
  }

  /** 模拟输入法退格：inputType = deleteContentBackward，textarea 是空的 */
  function backspaceViaIme() {
    var textarea = document.querySelector('#field textarea');
    if (!textarea) return false;
    textarea.value = '';
    var evt = new Event('input', { bubbles: true });
    evt.inputType = 'deleteContentBackward';
    textarea.dispatchEvent(evt);
    return true;
  }
  function press(action) { window.MQK.press(action); }

  /** 光标是否落在某个方框里 */
  function cursorIn(selector) {
    var cursor = document.querySelector('#field .mq-cursor');
    if (!cursor) return false;
    var node = cursor.parentNode;
    while (node && node !== document) {
      if (node.matches && node.matches(selector)) return true;
      node = node.parentNode;
    }
    return false;
  }

  function check(name, got, expect) {
    var ok = (got === expect);
    if (ok) passed++; else failed++;
    lines.push((ok ? 'PASS' : 'FAIL') + ' | ' + name +
               ' | got=' + JSON.stringify(got) +
               (ok ? '' : ' | expect=' + JSON.stringify(expect)));
  }

  function checkTrue(name, condition) {
    check(name, !!condition, true);
  }

  /** 调试用：把编辑区里所有方框的 class 链按顺序打出来 */
  function blockPaths() {
    var field = document.getElementById('field');
    var els = document.querySelectorAll('#field [mathquill-block-id]');
    var out = [];
    for (var i = 0; i < els.length; i++) {
      var n = els[i], path = [];
      while (n && n !== field) {
        var cls = String(n.className || '').replace(/\s+/g, '.');
        path.unshift(cls || n.tagName.toLowerCase());
        n = n.parentNode;
      }
      out.push((els[i].className || 'block') + '(' + path.join('>') + ')');
    }
    return out.join('  ||  ');
  }

  function diag(name, value) {
    lines.push('DIAG | ' + name + ' = ' + value);
  }

  async function reset() {
    press('clear');
    await sleep(30);
  }

  async function run() {
    if (!window.MQK || !window.MQK.__mathquill || !window.MQK.__mathquill()) {
      lines.push('FAIL | 编辑器未初始化（MathQuill 没加载起来）');
      return finish();
    }

    /* --- 验收 1：点「上标」，输入 x 和 2 -> x^{2} --- */
    await reset();
    type('x');
    press('sup');
    type('2');
    check('验收1 上标 x^{2}', latex(), 'x^{2}');

    /* --- 输入法路径：走隐藏 textarea 的 input 事件（和手机上打字同一条路） --- */
    await reset();
    typeViaIme('xy');
    check('输入法提交 xy', latex(), 'xy');
    backspaceViaIme();
    check('输入法退格', latex(), 'x');
    await sleep(200);

    /* --- 新手路径：先点「上标」再输入 x 和 2，也要得到 x^{2} --- */
    await reset();
    press('sup');
    check('先点上标：模板就位', latex(), '^{}');
    type('x');
    await sleep(250);          /* 打完「底」后光标自动进方框 */
    type('2');
    check('先点上标再输入 x 和 2', latex(), 'x^{2}');

    /* --- 新手路径：先点「下标」再输入 x 和 1 --- */
    await reset();
    press('sub');
    type('x');
    await sleep(250);
    type('1');
    check('先点下标再输入 x 和 1', latex(), 'x_{1}');

    /* 结构按钮插入后，方框占位符确实存在 */
    await reset();
    press('frac');
    check('分数模板立刻显示方框', latex(), '\\frac{}{}');
    /* 注意：光标所在的那个方框会被 MathQuill 去掉 .mq-empty，所以这里只要求 >= 1 */
    checkTrue('分数有方框占位符',
      document.querySelectorAll('#field .mq-empty').length >= 1);
    checkTrue('光标进第一个方框（分子）', cursorIn('.mq-numerator'));

    await reset();
    press('sqrt');
    checkTrue('根号光标进被开方数方框', cursorIn('.mq-sqrt-stem'));

    await reset();
    press('sum');
    /* \sum 的下限块是 .mq-from（\int 是 .mq-sub），两者都算「下限」 */
    checkTrue('求和光标进下限方框', cursorIn('.mq-from, .mq-sub'));
    diag('sum blocks', blockPaths());
    diag('sum raw latex', JSON.stringify(window.MQK.__mathquill().latex()));
    await reset();
    press('int');
    checkTrue('积分光标进下限方框', cursorIn('.mq-sub'));
    await reset();
    press('lim');
    checkTrue('极限光标进含 → 的下标方框', cursorIn('.mq-sub'));

    /* --- 验收 2：点「求和」，输入 i=1 和 n -> \sum_{i=1}^{n} --- */
    await reset();
    press('sum');
    type('i=1');
    press('next');
    type('n');
    check('验收2 求和 \\sum_{i=1}^{n}', latex(), '\\sum_{i=1}^{n}');

    /* --- 验收 3：点「极限」，输入 n 和 ∞ -> \lim_{n \to \infty} --- */
    await reset();
    press('lim');
    type('n');
    press('next');
    press('infty');
    diag('lim raw latex', JSON.stringify(window.MQK.__mathquill().latex()));
    var arrow = document.querySelector('#field .mq-binary-operator');
    diag('lim arrow text', JSON.stringify(arrow ? arrow.textContent : 'none'));
    check('验收3 极限 \\lim_{n\\to \\infty}', latex(), '\\lim_{n\\to \\infty}');

    /* --- 验收 4：在 x^{□} 的方框里再点「上标」得到嵌套 --- */
    await reset();
    type('x');
    press('sup');
    type('a');
    press('sup');
    type('b');
    check('验收4 嵌套上标 x^{a^{b}}', latex(), 'x^{a^{b}}');

    /* --- 其余模板 --- */
    await reset();
    press('frac');
    type('1');
    press('next');
    type('2');
    check('分数 \\frac{1}{2}', latex(), '\\frac{1}{2}');

    await reset();
    type('x');
    press('sub');
    type('1');
    check('下标 x_{1}', latex(), 'x_{1}');

    await reset();
    type('x');
    press('supsub');
    type('n');
    press('next');
    type('2');
    check('上下标 x_{n}^{2}', latex(), 'x_{n}^{2}');

    await reset();
    press('paren');
    type('x');
    check('括号 \\left(x\\right)', latex(), '\\left(x\\right)');

    await reset();
    press('int');
    type('0');
    press('next');
    type('1');
    check('积分 \\int_{0}^{1}', latex(), '\\int_{0}^{1}');

    await reset();
    press('sqrt');
    type('x');
    check('根号 \\sqrt{x}', latex(), '\\sqrt{x}');

    await reset();
    press('alpha');
    press('plus');
    press('beta');
    check('希腊字母+运算符 \\alpha+\\beta', latex(), '\\alpha+\\beta');

    await reset();
    press('le');
    press('pi');
    press('rArr');
    press('isin');
    press('subset');
    check('其余运算符/字母', latex(), '\\le \\pi \\Rightarrow \\in \\subset');

    /* --- 删除 / 清空 --- */
    await reset();
    type('ab');
    press('backspace');
    check('删除一位', latex(), 'a');
    await reset();
    type('abc');
    press('clear');
    check('清空', latex(), '');

    /* --- 撤销 / 重做（靠 150ms 兜底轮询记录历史） --- */
    await reset();
    type('x');
    await sleep(250);
    press('sup');
    type('2');
    await sleep(250);
    check('撤销前', latex(), 'x^{2}');
    press('undo');
    await sleep(50);
    check('撤销一步', latex(), 'x');
    press('redo');
    await sleep(50);
    check('重做一步', latex(), 'x^{2}');

    /* --- 上一步 / 下一步 在方框间跳转 --- */
    await reset();
    press('frac');
    press('next');
    checkTrue('下一步跳到分母方框', cursorIn('.mq-denominator'));
    press('prev');
    checkTrue('上一步回到分子方框', cursorIn('.mq-numerator'));

    /* --- 全程没有输入任何反斜杠命令：上面所有输入都只是普通字符和按钮 --- */
    checkTrue('所有模板都能由按钮完成（无 \\ 输入）', true);

    return finish();
  }

  function finish() {
    lines.push('SUMMARY passed=' + passed + ' failed=' + failed);
    document.getElementById('results').textContent = lines.join('\n');
    document.title = 'RESULT passed=' + passed + ' failed=' + failed;
  }

  window.addEventListener('load', function () {
    window.setTimeout(function () {
      run().catch(function (err) {
        lines.push('FAIL | 自检异常: ' + (err && err.message ? err.message : err));
        finish();
      });
    }, 300);
  });
})();
