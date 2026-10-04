/*!
 * 公式转LaTeX —— 编辑区核心脚本（完全离线，无任何网络请求）
 *
 * 依赖：assets/editor/lib/jquery.min.js、mathquill.min.js
 *
 * 对外接口（Android 通过 evaluateJavascript 调用 window.MQK）：
 *   MQK.press(action)      按下某个按钮，action 见下面 TEMPLATES / SYMBOLS
 *   MQK.currentLatex()     取当前 LaTeX（已规范化）
 *   MQK.focus()            让编辑区拿到焦点
 *
 * 对 Android 的回调（window.Android，由 MainActivity 注入）：
 *   Android.onLatexChanged(latex)  公式每次变化都会推一次
 *   Android.onEditorReady()        编辑器就绪
 *   Android.onError(message)       出错时上报一句，便于排查
 */
(function () {
  'use strict';

  /* ======================================================================
   * 一、模板与符号表
   * ==================================================================== */

  /* 结构模板：{} 会被 MathQuill 渲染成方框占位符，
     插入后光标会自动落进第一个方框（见 focusFirstNewBlock）。 */
  var TEMPLATES = {
    frac:   '\\frac{}{}',    /* 分数   \frac{□}{□}   */
    sup:    '^{}',           /* 上标   x^{□}         */
    sub:    '_{}',           /* 下标   x_{□}         */
    supsub: '_{}^{}',        /* 上下标 x_{□}^{□}     */
    sum:    '\\sum_{}^{}',   /* 求和   \sum_{□}^{□}  */
    int:    '\\int_{}^{}',   /* 积分   \int_{□}^{□}  */
    sqrt:   '\\sqrt{}'       /* 根号   \sqrt{□}      */
  };

  /* 希腊字母与运算符：直接交给 MathQuill 的命令接口插入 */
  var SYMBOLS = {
    alpha: '\\alpha',  beta: '\\beta',   gamma: '\\gamma', theta: '\\theta',
    pi:    '\\pi',     lambda: '\\lambda', mu: '\\mu',     sigma: '\\sigma',
    omega: '\\omega',  infty: '\\infty',
    plus:  '+',        minus: '-',       times: '\\times', div: '\\div',
    eq:    '=',        ne: '\\ne',       le: '\\le',       ge: '\\ge',
    to:    '\\to',     rArr: '\\Rightarrow', isin: '\\in', subset: '\\subset'
  };

  /* MathQuill 给每个块打的 DOM 标记，用来反查方框 */
  var BLOCK_ATTR = 'mathquill-block-id';

  /* ======================================================================
   * 二、模块状态
   * ==================================================================== */

  var mq = null;            /* MathQuill 实例 */
  var history = [];         /* 撤销栈：LaTeX 快照 */
  var hIndex = -1;          /* 当前快照下标 */
  var replaying = false;    /* 正在回放历史（回放时不再记录） */
  var curSlot = null;       /* 光标当前所在槽位 { el, dir } */
  var limitSlotEl = null;   /* 极限模板那个「含 → 的下标方框」，它有两个输入位 */
  var lastReported = null;  /* 上一次已同步的 LaTeX，用来判断「公式变了没有」 */
  var pendingSlot = null;   /* 等着「打完底就自动跳进去」的方框（见 insertTemplate） */

  /* ======================================================================
   * 三、工具函数
   * ==================================================================== */

  /** 上报错误给 Android（没有桥时静默忽略） */
  function notifyError(err) {
    try {
      if (window.Android && window.Android.onError) {
        window.Android.onError(String((err && err.message) || err));
      }
    } catch (e) { /* 忽略 */ }
  }

  /** 把最新 LaTeX 推给 Android，用于下方只读文本区 */
  function reportLatex() {
    if (!mq) return;
    var latex = normalize(mq.latex());
    try {
      if (window.Android && window.Android.onLatexChanged) {
        window.Android.onLatexChanged(latex);
      }
    } catch (e) { /* 忽略 */ }
  }

  /**
   * LaTeX 规范化，只做「不改变含义」的整理：
   *   { }        -> {}          MathQuill 用「花括号+空格」表示空块
   *   \to }      -> \to}        右花括号前不留空格
   *   \sum _{..} -> \sum_{..}   _ 和 ^ 前不留空格
   *   x^2        -> x^{2}       单字符上下标补上花括号（等价写法，读起来更规整）
   *   \to\infty  -> \to \infty  两个控制词之间补空格（等价写法，粘贴到别处更保险）
   * 注意：控制词后面的空格本身不能删（\to \infty 与 \to\infty 等价，但后者可读性差）
   */
  function normalize(latex) {
    var s = String(latex == null ? '' : latex);
    s = s.replace(/\{\s*\}/g, '{}');
    s = s.replace(/\s+\}/g, '}');
    s = s.replace(/\s+([_^])/g, '$1');
    s = s.replace(/([_^])([^{}\s\\])/g, '$1{$2}');
    /* 只在「后面紧跟另一个反斜杠命令」时补空格，避免把 \frac 拆成 \fra c */
    s = s.replace(/(\\[a-zA-Z]+)(?=\\)/g, '$1 ');
    s = s.replace(/[ \t]{2,}/g, ' ');
    return s.replace(/^\s+|\s+$/g, '');
  }

  /* ======================================================================
   * 四、方框（可编辑块）的收集、排序与定位
   * ==================================================================== */

  /** 判断某个方框是不是空的（子元素里既没有块也没有命令，也没有文字） */
  function isEmptyBlock(el) {
    var text = (el.textContent || '').replace(/[\s\u200b\u00a0]/g, '');
    if (text) return false;
    return !el.querySelector('[' + BLOCK_ATTR + '], [mathquill-command-id]');
  }

  /**
   * 判断方框落在哪个「上下限容器」的哪一支：
   * 同一个容器里，下标要排在上标前面（MathQuill 的 DOM 里上标在前）。
   * 两种排版结构都要认：
   *   \int  —— <span class="mq-supsub"><span class="mq-sup">…</span><span class="mq-sub">…</span>
   *   \sum  —— <span class="mq-large-operator"><span class="mq-to">…</span>…<span class="mq-from">…</span>
   */
  function supsubInfo(el) {
    var node = el, branch = 0;
    while (node && node.nodeType === 1) {
      var cls = ' ' + (node.className || '') + ' ';
      if (cls.indexOf(' mq-supsub ') > -1) return { box: node, branch: branch };
      if (cls.indexOf(' mq-large-operator ') > -1) return { box: node, branch: branch };
      if (cls.indexOf(' mq-to ') > -1) branch = 1;          /* 求和的「上限」 */
      else if (cls.indexOf(' mq-from ') > -1) branch = 0;   /* 求和的「下限」 */
      else if (cls.indexOf(' mq-sup ') > -1) branch = 1;
      else if (cls.indexOf(' mq-sub ') > -1) branch = 0;
      node = node.parentNode;
    }
    return null;
  }

  /** 方框排序：同一下标/上标容器内「先下标后上标」，其余按文档顺序 */
  function compareBlocks(a, b) {
    if (a === b) return 0;
    var ia = supsubInfo(a), ib = supsubInfo(b);
    if (ia && ib && ia.box === ib.box && ia.branch !== ib.branch) {
      return ia.branch - ib.branch;
    }
    var pos = a.compareDocumentPosition(b);
    if (pos & 4 /* FOLLOWING */) return -1;
    if (pos & 2 /* PRECEDING */) return 1;
    return 0;
  }

  /** 收集当前公式中所有可编辑方框（不含根块），已按输入顺序排好 */
  function collectBlockEls() {
    if (!mq) return [];
    var all = mq.el().querySelectorAll('[' + BLOCK_ATTR + ']');
    var els = [];
    for (var i = 0; i < all.length; i++) {
      var cls = ' ' + (all[i].className || '') + ' ';
      if (cls.indexOf(' mq-root-block ') > -1) continue;   /* 根块不算方框 */
      els.push(all[i]);
    }
    els.sort(compareBlocks);
    return els;
  }

  /**
   * 生成槽位列表（「上一步 / 下一步」的跳转目标）：
   *   普通方框   —— 一个槽位（非空方框落在内容末尾，空方框落在框内）
   *   极限的方框 —— 两个槽位（→ 左侧、→ 右侧），方便分别输入条件和极限值
   */
  function slotList() {
    var els = collectBlockEls(), out = [];
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      if (el === limitSlotEl) {
        out.push({ el: el, dir: 'left' });
        out.push({ el: el, dir: 'right' });
      } else {
        out.push({ el: el, dir: isEmptyBlock(el) ? 'left' : 'right' });
      }
    }
    return out;
  }

  /** 在槽位列表里找到某个槽位的下标（先精确匹配方向，再退化为只匹配方框） */
  function indexOfSlot(list, slot) {
    if (!slot) return -1;
    var byEl = -1;
    for (var i = 0; i < list.length; i++) {
      if (list[i].el === slot.el) {
        if (list[i].dir === slot.dir) return i;
        if (byEl < 0) byEl = i;
      }
    }
    return byEl;
  }

  /**
   * 把光标放进指定槽位。
   * 用 MathQuill 公开的 clickAt(clientX, clientY, target)：
   * 传入方框元素和它左/右边缘外一点的坐标，即可精确落到该方框的左端或右端。
   */
  function focusSlot(slot) {
    if (!slot || !slot.el || !slot.el.getBoundingClientRect) return;
    var rect = slot.el.getBoundingClientRect();
    var x = (slot.dir === 'left') ? (rect.left - 2) : (rect.right + 2);
    var y = rect.top + (rect.height > 0 ? rect.height / 2 : 0);
    try {
      mq.clickAt(x, y, slot.el);
      mq.focus();
      curSlot = slot;
    } catch (e) {
      notifyError(e);
    }
  }

  /** 光标所在的方框元素（找不到则返回 null，说明光标在根块里） */
  function cursorBlockEl() {
    if (!mq) return null;
    var el = mq.el().querySelector('.mq-cursor');
    while (el) {
      if (el.getAttribute && el.getAttribute(BLOCK_ATTR)) return el;
      el = el.parentNode;
    }
    return null;
  }

  /** 记录光标当前所在槽位；仍在同一个方框里就保持不变（避免方向信息丢失） */
  function noteCursorSlot() {
    var el = cursorBlockEl();
    if (!el) { curSlot = null; return; }
    if (curSlot && curSlot.el === el) return;
    curSlot = { el: el, dir: isEmptyBlock(el) ? 'left' : 'right' };
  }

  /** 插入模板后，把光标放到「新出现的第一个方框」里 */
  function focusFirstNewBlock(beforeEls) {
    var list = slotList();
    for (var i = 0; i < list.length; i++) {
      if (beforeEls.indexOf(list[i].el) < 0) {
        focusSlot(list[i]);
        return list[i];
      }
    }
    noteCursorSlot();
    return null;
  }

  /* ======================================================================
   * 五、编辑历史（撤销 / 重做）
   * ==================================================================== */

  function pushHistory() {
    if (replaying || !mq) return;
    var latex = mq.latex();
    if (hIndex >= 0 && history[hIndex] === latex) return;
    history.length = hIndex + 1;             /* 丢弃 redo 分支 */
    history.push(latex);
    hIndex = history.length - 1;
    if (history.length > 120) { history.shift(); hIndex--; }
  }

  function applyHistory(index) {
    if (index < 0 || index >= history.length || !mq) return;
    replaying = true;
    hIndex = index;
    mq.latex(history[index]);
    mq.moveToRightEnd();
    replaying = false;
    curSlot = null;
    limitSlotEl = null;
    pendingSlot = null;
    lastReported = mq.latex();
    reportLatex();
  }

  function undo() { if (hIndex > 0) applyHistory(hIndex - 1); }
  function redo() { if (hIndex < history.length - 1) applyHistory(hIndex + 1); }

  /* ======================================================================
   * 六、按钮动作
   * ==================================================================== */

  /** 所有写操作结束后统一走这里：记录历史 + 通知 Android */
  function scheduleSync() {
    window.setTimeout(syncNow, 0);
  }

  /**
   * 同步一次状态：公式变了才记历史、才上报。
   * MathQuill 的 typedText（系统输入法打字走的就是这条路）不一定会回调 edit，
   * 所以除了 edit 回调，还有一个定时轮询兜底，保证下方 LaTeX 始终是实时的。
   */
  function syncNow() {
    if (!mq) return;
    var latex = mq.latex();
    if (latex === lastReported) return;
    lastReported = latex;
    noteCursorSlot();
    pushHistory();
    reportLatex();
    applyPendingSlot();     /* 用户刚打完「底」，把光标送进方框 */
  }

  /** 兜底轮询：150ms 检查一次，代价极低 */
  function startWatcher() {
    window.setInterval(syncNow, 150);
  }

  /** 插入模板（可嵌套：在方框里再点模板就套在里面） */
  function insertTemplate(name) {
    var before = collectBlockEls();

    /* 公式还空着的时候点「上标/下标」，先不急着进方框：
       把光标留在命令左边等用户先打「底」，等第一个字符进来再自动跳进方框。
       这样两种顺序都能得到 x^{2}：
         先打 x 再点「上标」再打 2
         先点「上标」再打 x（自动进方框）再打 2                                        */
    var needBase = (name === 'sup' || name === 'sub' || name === 'supsub') && isFormulaEmpty();

    mq.focus();
    mq.write(TEMPLATES[name]);
    var slot = focusFirstNewBlock(before);
    if (needBase && slot) {
      pendingSlot = slot;
      focusRootStart();
    }
    scheduleSync();
  }

  /** 公式是不是还空着（只用于上面的兜底判断） */
  function isFormulaEmpty() {
    return mq.latex().replace(/[\s{}]/g, '') === '';
  }

  /** 把光标放到整条公式的最左端 */
  function focusRootStart() {
    var root = mq.el().querySelector('.mq-root-block');
    if (!root) return;
    var rect = root.getBoundingClientRect();
    mq.clickAt(rect.left - 2, rect.top + rect.height / 2, root);
  }

  /** 打完「底」了，把光标送进等着它的方框 */
  function applyPendingSlot() {
    if (!pendingSlot) return;
    var slot = pendingSlot;
    pendingSlot = null;
    focusSlot(slot);
  }

  /** 极限：\lim + 下标方框，方框里预置 →，两个输入位在 → 左右 */
  function insertLimit() {
    var before = collectBlockEls();
    mq.focus();
    mq.write('\\lim_{}');            /* 先得到 lim 和一个空方框 */
    var slot = focusFirstNewBlock(before);
    mq.write('\\to ');               /* 在方框里写入 → */
    if (slot) {
      limitSlotEl = slot.el;
      focusSlot({ el: slot.el, dir: 'left' });   /* 光标停在 → 左边，等待输入条件 */
    }
    scheduleSync();
  }

  /** 括号：先插左括号（MathQuill 会带出成对方括号和内部方框），再补右括号 */
  function insertParen() {
    var before = collectBlockEls();
    mq.focus();
    mq.cmd('(');
    mq.cmd(')');
    focusFirstNewBlock(before);
    scheduleSync();
  }

  /** 希腊字母 / 运算符 */
  function insertSymbol(name) {
    mq.focus();
    mq.cmd(SYMBOLS[name]);
    noteCursorSlot();
    scheduleSync();
  }

  /** 删除：等价于按一次退格 */
  function backspace() {
    mq.focus();
    mq.keystroke('Backspace');
    scheduleSync();
  }

  /** 清空 */
  function clearAll() {
    mq.latex('');
    mq.moveToRightEnd();
    curSlot = null;
    limitSlotEl = null;
    pendingSlot = null;
    lastReported = '';
    pushHistory();
    reportLatex();
  }

  /** 上一步 / 下一步：在方框之间跳转 */
  function step(direction) {
    var list = slotList();
    if (!list.length) return;
    var index = indexOfSlot(list, curSlot);
    if (index < 0) index = (direction > 0) ? -1 : list.length;
    var target = index + direction;
    if (target < 0) target = 0;
    if (target > list.length - 1) target = list.length - 1;
    focusSlot(list[target]);
  }

  /* 按钮总入口：Android 端只传一个动作名 */
  function press(action) {
    if (!mq) return;
    pendingSlot = null;     /* 换动作了就取消「打完底自动跳」的等待 */
    try {
      switch (action) {
        case 'undo':      return undo();
        case 'redo':      return redo();
        case 'clear':     return clearAll();
        case 'backspace': return backspace();
        case 'prev':      return step(-1);
        case 'next':      return step(1);
        case 'paren':     return insertParen();
        case 'lim':       return insertLimit();
        default: break;
      }
      if (Object.prototype.hasOwnProperty.call(TEMPLATES, action)) {
        return insertTemplate(action);
      }
      if (Object.prototype.hasOwnProperty.call(SYMBOLS, action)) {
        return insertSymbol(action);
      }
    } catch (err) {
      notifyError(err);
    }
  }

  /* ======================================================================
   * 七、初始化
   * ==================================================================== */

  function showFallback() {
    var el = document.getElementById('fallback');
    if (el) el.style.display = 'block';
  }

  /** 点到编辑区空白处时，把光标放到公式末尾 */
  function bindBlankAreaClick() {
    var wrap = document.getElementById('wrap');
    if (!wrap) return;
    wrap.addEventListener('mousedown', function (e) {
      if (!mq) return;
      var root = mq.el().querySelector('.mq-root-block');
      if (root && root.contains(e.target)) return;   /* 交给 MathQuill 自己处理 */
      mq.focus();
      mq.moveToRightEnd();
      curSlot = null;
    }, false);
  }

  /**
   * 输入桥（安卓上就打不了字，这一步很关键）：
   *
   * MathQuill 0.10.1 只监听 keydown / keypress —— 它的 keydown 处理里
   * default 分支直接 return，可打印字符全部靠 keypress 之后去读隐藏 textarea；
   * 而安卓输入法（Gboard 等）提交文字走的是 composition + input 事件，
   * keydown 只有 keyCode 229，也不会补 keypress，于是文字会卡在 textarea 里进不了公式。
   *
   * 所以这里自己监听 input：
   *   有文字  —— 交给 mq.typedText 送进公式（桌面浏览器上 MathQuill 会先一步清空
   *              textarea，这里读到空串自然让开，不会重复输入）
   *   删除    —— 输入法的退格不会产生按键事件，这里补一次 Backspace
   */
  function bindInputBridge() {
    var textarea = mq.el().querySelector('textarea');
    if (!textarea) return;

    var lastDeleteKeyTime = 0;   /* 真实退格键的时间戳，避免被删两次 */

    textarea.addEventListener('keydown', function (e) {
      var key = e.key || '';
      if (key === 'Backspace' || key === 'Delete' || e.keyCode === 8 || e.keyCode === 46) {
        lastDeleteKeyTime = Date.now();
      }
    }, true);

    textarea.addEventListener('input', function (e) {
      var text = textarea.value;
      var inputType = (e && e.inputType) || '';

      if (text) {
        textarea.value = '';
        if (document.activeElement !== textarea) mq.focus();
        mq.typedText(text);
      } else if (/^delete/.test(inputType)) {
        if (Date.now() - lastDeleteKeyTime > 150) {
          if (document.activeElement !== textarea) mq.focus();
          mq.keystroke('Backspace');
        }
      }
      syncNow();
    }, false);
  }

  function init() {
    if (!window.jQuery || !window.MathQuill) {
      showFallback();
      return;
    }
    try {
      mq = window.MathQuill.MathField(document.getElementById('field'), {
        spaceBehavesLikeTab: false,        /* 空格就是空格 */
        restrictMismatchedBrackets: true,  /* 括号不匹配时不做奇怪的事 */
        handlers: {
          /* 用户用系统键盘输入时也会走到这里（延后一拍，等公式改完再上报） */
          edit: function () { scheduleSync(); }
        }
      });
      bindBlankAreaClick();
      bindInputBridge();
      pushHistory();
      lastReported = mq.latex();
      reportLatex();
      startWatcher();
      if (window.Android && window.Android.onEditorReady) {
        window.Android.onEditorReady();
      }
    } catch (err) {
      notifyError(err);
      showFallback();
    }
  }

  /* Android 调用的入口 */
  window.MQK = {
    press: press,
    currentLatex: function () { return mq ? normalize(mq.latex()) : ''; },
    focus: function () { if (mq) mq.focus(); }
  };

  /* 自检入口：只有页面带 ?selftest=1 时才暴露内部实例，App 里永远走不到这里。
     作用是让「验收脚本」可以在真实浏览器里模拟按键与打字（见项目根目录 _test/）。 */
  if (window.location.search.indexOf('selftest=1') > -1) {
    window.MQK.__mathquill = function () { return mq; };
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, false);
  } else {
    window.setTimeout(init, 0);
  }
})();
