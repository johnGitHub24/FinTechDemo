/**
 * 【職責】把 demoTrace 與劇本表合成 PROCESS FLOW 步驟列。
 * 【技巧】依 service 對 hops；無 hop 的前端步視為 ok。
 * 【概念】三欄：誰／做什麼／狀態（含 hop detail）。
 */
import { SCRIPTS } from './processScripts.js';

export function mergeTrace(demoTrace) {
  if (!demoTrace || !demoTrace.action) {
    return [];
  }
  const script = SCRIPTS[demoTrace.action] || [];
  const viaGateway = !!demoTrace.viaGateway;
  const hopMap = new Map();
  (demoTrace.hops || []).forEach((h) => hopMap.set(h.service, h));

  return script
    .filter((step) => !(step.optional && step.service === 'gateway' && !viaGateway))
    .map((step) => {
      const hop = hopMap.get(step.service);
      const ok = hop ? hop.ok !== false : true;
      const detail = hop?.detail;
      return {
        service: step.service,
        title: step.title,
        purpose: step.purpose,
        stateHint: detail ? `${step.stateHint} · ${detail}` : step.stateHint,
        ok,
        active: true
      };
    });
}
