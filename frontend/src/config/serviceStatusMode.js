/**
 * 【職責】服務狀態面板：本機 Demo vs K8s 兩套埠位／燈號規則（單一來源）。
 * 【概念】K8s 只部署 4 Pod；本機入口是 port-forward :18080，不是 :8080。
 */
import { isK8sFrontendMode } from './demoLinks';

const env = import.meta.env || {};

/** 與 demo/platform-run.properties K8S_GATEWAY_PF_LOCAL 對齊 */
export const K8S_GATEWAY_PF_PORT = 18080;

export const VITE_DEV_PORT = 5173;

const LOCAL_ROWS = [
  { id: 'gateway', label: 'Gateway', port: 8080 },
  { id: 'order', label: 'Order', port: 8081 },
  { id: 'risk', label: 'Risk', port: 8082 },
  { id: 'job', label: 'Job', port: 8083 },
  { id: 'account', label: 'Account', port: 8084 }
];

export function isK8sServiceStatusMode() {
  return isK8sFrontendMode();
}

export function getK8sGatewayPfPort() {
  const target = String(env.VITE_API_TARGET || '');
  const m = target.match(/:(\d+)\s*$/);
  if (m) return Number(m[1]);
  return K8S_GATEWAY_PF_PORT;
}

/**
 * 本機 Demo：五服務皆應在本機 :8080～:8084 亮起。
 */
export function buildLocalStatusRows(upById) {
  return LOCAL_ROWS.map((row) => ({
    ...row,
    section: 'local',
    required: row.id !== 'account',
    up: Boolean(upById[row.id]),
    na: false
  }));
}

/**
 * K8s：本機只看 port-forward；叢集內看 topology；Job 標未部署。
 */
export function buildK8sStatusView({ gatewayPfUp, topologyServices }) {
  const topo = Object.fromEntries((topologyServices || []).map((s) => [s.id, s]));

  const localRows = [
    {
      id: 'gateway-pf',
      label: 'Gateway (port-forward)',
      port: getK8sGatewayPfPort(),
      section: 'local-entry',
      required: true,
      up: Boolean(gatewayPfUp),
      na: false,
      hint: '本機打叢集入口'
    },
    {
      id: 'vite',
      label: 'Vue 前端',
      port: VITE_DEV_PORT,
      section: 'local-entry',
      required: true,
      up: true,
      na: false,
      hint: '本頁使用中'
    }
  ];

  const clusterRows = [
    {
      id: 'order',
      label: 'Order',
      port: 8081,
      section: 'cluster',
      required: true,
      up: Boolean(topo.order?.up),
      na: false,
      hint: '叢集內 Pod'
    },
    {
      id: 'risk',
      label: 'Risk',
      port: 8082,
      section: 'cluster',
      required: true,
      up: Boolean(topo.risk?.up),
      na: false,
      hint: '叢集內 Pod'
    },
    {
      id: 'account',
      label: 'Account',
      port: 8084,
      section: 'cluster',
      required: false,
      up: Boolean(topo.account?.up),
      na: false,
      hint: '叢集內 Pod'
    },
    {
      id: 'job',
      label: 'Job',
      port: 8083,
      section: 'cluster',
      required: false,
      up: false,
      na: true,
      hint: 'K8s manifest 未部署'
    }
  ];

  const ignoredRows = [
    {
      id: 'gateway-local',
      label: 'Gateway（本機埠）',
      port: 8080,
      section: 'ignored',
      required: false,
      up: false,
      na: true,
      hint: 'K8s 模式請改看 :18080'
    },
    {
      id: 'job-local',
      label: 'Job（本機埠）',
      port: 8083,
      section: 'ignored',
      required: false,
      up: false,
      na: true,
      hint: 'K8s 未部署 job-service'
    }
  ];

  return { localRows, clusterRows, ignoredRows };
}

export function summarizeK8sReady(rows) {
  const required = rows.filter((r) => r.required && !r.na);
  const up = required.filter((r) => r.up).length;
  return { up, total: required.length, ready: up === required.length && required.length > 0 };
}

export function summarizeLocalReady(rows) {
  const order = rows.find((r) => r.id === 'order');
  const risk = rows.find((r) => r.id === 'risk');
  const allUp = rows.every((r) => r.up);
  const demoReady = Boolean(order?.up && risk?.up);
  return {
    up: rows.filter((r) => r.up).length,
    total: rows.length,
    allUp,
    demoReady
  };
}
