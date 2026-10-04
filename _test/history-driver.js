/* ===========================================================================
 * 公式历史弹层 的验收自检（只在开发机的浏览器里跑，不参与 APK 打包）
 *
 * 验证点：
 *   - 列表渲染条数、空状态切换
 *   - 每条公式确实被 MathQuill 静态渲染出来（不是只显示原始 LaTeX 文本）
 *   - 点条目回调 onPick(正确索引)、点 ✕ 回调 onDelete(正确索引) 且不误触 onPick
 *   - 深浅色切换
 * =========================================================================== */
(function () {
  'use strict';

  var lines = [];
  var passed = 0, failed = 0;

  function check(name, got, expect) {
    var ok = (got === expect);
    if (ok) passed++; else failed++;
    lines.push((ok ? 'PASS' : 'FAIL') + ' | ' + name +
               ' | got=' + JSON.stringify(got) +
               (ok ? '' : ' | expect=' + JSON.stringify(expect)));
  }

  function checkTrue(name, cond) { check(name, !!cond, true); }

  function run() {
    if (!window.MQHistory) {
      lines.push('FAIL | 历史页面未初始化（history.js 没跑起来）');
      return finish();
    }

    var picked = -1, deleted = -1;
    window.Android = {
      onReady: function () { },
      onPick: function (i) { picked = i; },
      onDelete: function (i) { deleted = i; }
    };

    var items = ['\\frac{1}{2}', 'x^{2}', '\\sum_{i=1}^{n}'];
    window.MQHistory.render(items, 'light');

    var rows = document.querySelectorAll('#list .item');
    check('渲染出 3 条历史', rows.length, 3);
    check('列表为空时空状态隐藏', document.getElementById('empty').style.display, 'none');

    // MathQuill.StaticMath 是把 mq-math-mode 加到 .formula 元素自身，所以用复合选择器
    var rendered = document.querySelectorAll('#list .item .formula.mq-math-mode');
    check('每条公式都用 MathQuill 渲染', rendered.length, 3);
    check('分数被渲染成分数结构',
      document.querySelectorAll('#list .item .formula .mq-fraction').length, 1);
    check('求和被渲染成大算符',
      document.querySelectorAll('#list .item .formula .mq-large-operator').length, 1);
    check('下面附了原始 LaTeX',
      document.querySelector('#list .item .latex').textContent, '\\frac{1}{2}');

    rows[1].click();
    check('点第 2 条 -> onPick(1)', picked, 1);

    picked = -1;
    rows[2].querySelector('.del').click();
    check('点第 3 条的 ✕ -> onDelete(2)', deleted, 2);
    check('点删除不会误触发 onPick', picked, -1);

    window.MQHistory.render([], 'light');
    check('清空后列表为空', document.querySelectorAll('#list .item').length, 0);
    check('清空后显示空状态', document.getElementById('empty').style.display, 'block');
    checkTrue('空状态有提示文案', document.getElementById('empty').textContent.length > 0);

    window.MQHistory.setTheme('dark');
    checkTrue('切深色：html.dark 生效', document.documentElement.classList.contains('dark'));
    window.MQHistory.setTheme('light');
    checkTrue('切回浅色：html.dark 移除', !document.documentElement.classList.contains('dark'));

    return finish();
  }

  function finish() {
    lines.push('SUMMARY passed=' + passed + ' failed=' + failed);
    document.getElementById('results').textContent = lines.join('\n');
    document.title = 'RESULT passed=' + passed + ' failed=' + failed;
  }

  window.addEventListener('load', function () {
    window.setTimeout(function () {
      try { run(); } catch (err) {
        lines.push('FAIL | 自检异常: ' + (err && err.message ? err.message : err));
        finish();
      }
    }, 300);
  });
})();
