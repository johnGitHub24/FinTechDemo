/**
 * 【職責】本機 kind／FinTechDemo K8s 精簡驗證指令（對齊 SPEC §3.1）。
 * 【技巧】一鍵複製短腳本；畫面只留必要 4～5 條。
 * 【概念】瀏覽器開 API＝403；請用 kubectl。
 */

export const K8S_VERIFY_SCRIPT = `# K8s 精簡驗證（Docker 已開）
docker info
kubectl get --raw=/readyz
kubectl get nodes
kubectl get all -n fintech-demo
`;

export const k8sVerifyGroups = [
  {
    title: '三步確認',
    items: [
      { label: 'Docker', cmd: 'docker info', expect: 'Server Version' },
      { label: 'API', cmd: 'kubectl get --raw=/readyz', expect: 'ok' },
      { label: '節點', cmd: 'kubectl get nodes', expect: 'Ready' },
      { label: '本專案', cmd: 'kubectl get all -n fintech-demo', expect: '四服務 Running' }
    ]
  }
];

export const K8S_BROWSER_NOTE =
  '瀏覽器開 API 根路徑會 403（anonymous）屬正常；用上面 kubectl。';
