/*!
 * 公式历史列表（完全离线）
 *
 * Android 侧通过 window.MQHistory 调用：
 *   render(items, mode)   渲染整个列表，mode 为 'dark' / 'light'
 *   setTheme(mode)        只切深浅色
 * 点某一条 -> Android.onPick(index)
 * 点右侧 ✕ -> Android.onDelete(index)
 *
 * 用 MathQuill 的静态排版把公式画出来，这样不懂 LaTeX 的人也能一眼认出用过哪条。
 */
(function () {
  'use strict';

  var listEl = null;
  var emptyEl = null;

  function setTheme(mode) {
    var root = document.documentElement;
    if (mode === 'dark') root.classList.add('dark');
    else root.classList.remove('dark');
  }

  function render(items, mode) {
    if (mode) setTheme(mode);
    if (!listEl) return;

    items = items || [];
    listEl.innerHTML = '';
    emptyEl.textContent = '还没有历史记录\n点「复制 LaTeX」的公式会自动存到这里';
    emptyEl.style.display = items.length ? 'none' : 'block';

    items.forEach(function (latex, index) {
      var row = document.createElement('div');
      row.className = 'item';

      var formula = document.createElement('div');
      formula.className = 'formula';

      var latexText = document.createElement('div');
      latexText.className = 'latex';
      latexText.textContent = latex;

      var del = document.createElement('button');
      del.className = 'del';
      del.type = 'button';
      del.textContent = '✕';
      del.addEventListener('click', function (event) {
        event.stopPropagation();
        if (window.Android && window.Android.onDelete) window.Android.onDelete(index);
      });

      row.appendChild(formula);
      row.appendChild(latexText);
      row.appendChild(del);
      row.addEventListener('click', function () {
        if (window.Android && window.Android.onPick) window.Android.onPick(index);
      });
      listEl.appendChild(row);

      // 静态渲染公式；万一这条 LaTeX 渲染不了，就退回显示原始文本
      try {
        window.MathQuill.StaticMath(formula).latex(latex);
      } catch (err) {
        formula.textContent = latex;
      }
    });
  }

  window.MQHistory = { render: render, setTheme: setTheme };

  function init() {
    listEl = document.getElementById('list');
    emptyEl = document.getElementById('empty');
    if (window.Android && window.Android.onReady) window.Android.onReady();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, false);
  } else {
    window.setTimeout(init, 0);
  }
})();
