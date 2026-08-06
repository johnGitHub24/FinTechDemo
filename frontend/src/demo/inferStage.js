/**
 * 【職責】依拓撲服務燈推斷 S1／S2／S3（與後端公式一致，備援用）。
 */
export function inferStage(services) {
  const up = (id) => (services || []).some((s) => s.id === id && s.up);
  const order = up('order');
  const risk = up('risk');
  const gateway = up('gateway');
  const account = up('account');
  if (order && risk && (gateway || account)) return 'S3';
  if (order && risk) return 'S2';
  if (order) return 'S1';
  return 'S0';
}
