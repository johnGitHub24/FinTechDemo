/**
 * 【職責】前端 jQuery DOM 輔助（取代 document.getElementById 選取與捲動）。
 * 【技巧】jQuery 由 index.html CDN 載入；id 用 CSS.escape 避免特殊字元。
 * 【概念】Vue 元件仍用 ref／reactive；僅跨元件 DOM（hash、面板）走 jQuery。
 */

function $() {
  const jq = window.jQuery;
  if (!jq) {
    throw new Error('jQuery 未載入：請確認 index.html 已引入 jquery.min.js');
  }
  return jq;
}

/**
 * @param {string} id 元素 id（不含 #）
 * @param {number} [offsetTop=8] 捲動頂部留白 px
 */
export function scrollToId(id, offsetTop = 8) {
  if (!id || !window.jQuery) return;
  const jq = $();
  const $el = jq(`#${CSS.escape(String(id))}`);
  if (!$el.length) return;
  const top = Math.max(0, $el.offset().top - offsetTop);
  jq('html, body').stop(true).animate({ scrollTop: top }, 400);
}

/**
 * @param {string} selector jQuery 選擇器（含 #）
 * @param {number} [offsetTop=8]
 */
export function scrollToSelector(selector, offsetTop = 8) {
  if (!window.jQuery) return;
  const jq = $();
  const $el = jq(selector);
  if (!$el.length) return;
  const top = Math.max(0, $el.offset().top - offsetTop);
  jq('html, body').stop(true).animate({ scrollTop: top }, 400);
}

export { $ };
