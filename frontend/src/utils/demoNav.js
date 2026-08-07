/**
 * 【職責】頂欄／快捷共用：探測外部 URL、SPA+hash 導向、展開 Demo 面板。
 * 【技巧】外部連結：先同步 window.open('about:blank') 保住 user gesture，
 *         再 await 探測；失敗關窗並回提示。勿在 await 後才 open（會被擋彈窗＝看起來沒反應）。
 * 【概念】loop-engineering：導向失敗要有可執行下一步，不是 silent fail。
 */
import { nextTick } from 'vue';

/**
 * 【目的】探測 URL 是否可連（同源用 fetch；跨源只要不是 network error 也算可試開）。
 */
export async function probeUrl(url) {
  if (!url) return false;
  try {
    const ctrl = new AbortController();
    const t = setTimeout(() => ctrl.abort(), 2500);
    await fetch(url, { method: 'GET', mode: 'no-cors', signal: ctrl.signal, cache: 'no-store' });
    clearTimeout(t);
    // no-cors → opaque；能完成請求通常表示埠有在聽
    return true;
  } catch {
    try {
      const ctrl = new AbortController();
      const t = setTimeout(() => ctrl.abort(), 2500);
      const res = await fetch(url, { method: 'GET', signal: ctrl.signal, cache: 'no-store' });
      clearTimeout(t);
      return res.ok || res.type === 'opaque';
    } catch {
      return false;
    }
  }
}

/**
 * 【目的】開外部連結；失敗回傳錯誤訊息字串，成功回傳 null。
 * 【技巧】必須在 click 同步階段先 open，否則 Chrome 擋彈窗且無提示。
 */
export async function openExternal(href, probe, startHint) {
  if (!href) return '缺少連結網址';

  // 同步開空白頁（勿加 noopener：否則回傳 null，無法改 location）
  const win = window.open('about:blank', '_blank');
  if (!win) {
    return `瀏覽器擋下彈出視窗。請允許本站彈出，或手動開：${href}`;
  }

  const check = probe || href;
  try {
    const ok = await probeUrl(check);
    if (!ok) {
      win.close();
      return `連不上 ${check}。請先啟動：${startHint || '對應服務'}，再開一次。`;
    }
    win.location.href = href;
    try {
      win.opener = null;
    } catch {
      /* ignore */
    }
    return null;
  } catch (e) {
    try {
      win.close();
    } catch {
      /* ignore */
    }
    return `開啟失敗：${e?.message || e}。可手動開：${href}`;
  }
}

/**
 * 【目的】路由到 path 並滾到 hash 元素。
 */
export async function goSpa(router, path, hash) {
  const target = hash ? `${path}#${hash}` : path;
  if (router.currentRoute.value.fullPath !== target && router.currentRoute.value.path !== path) {
    await router.push(hash ? { path, hash: `#${hash}` } : path);
  } else if (hash && router.currentRoute.value.path === path) {
    await router.replace({ path, hash: `#${hash}` });
  } else {
    await router.push(path);
  }
  await nextTick();
  await new Promise((r) => setTimeout(r, 50));
  if (hash) {
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

/**
 * 【目的】展開並捲到 Demo 快捷面板。
 */
export function openDemoPanel(panelRef) {
  const el = document.getElementById('demo-shortcuts');
  if (panelRef?.value?.expand) panelRef.value.expand();
  el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
