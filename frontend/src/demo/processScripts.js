/**
 * 【職責】各動作的 PROCESS FLOW 劇本（誰／做什麼／狀態提示）。
 * 【技巧】optional gateway 步在 viaGateway=false 時由 merge 拿掉。
 * 【概念】文案給展演用；後端 hops 覆寫 ok／detail。
 */
export const SCRIPTS = {
  CREATE_ORDER: [
    { service: 'frontend', title: '交易前台', purpose: '送出下單表單', stateHint: 'UI → API' },
    { service: 'gateway', title: 'Gateway :8080', purpose: '統一入口轉發 /api', stateHint: '可選', optional: true },
    { service: 'order-service', title: 'Order :8081', purpose: '建立訂單並落庫', stateHint: '→ PENDING' }
  ],
  EXECUTE: [
    { service: 'frontend', title: '交易前台', purpose: '點擊成交', stateHint: 'UI → API' },
    { service: 'gateway', title: 'Gateway :8080', purpose: '統一入口轉發', stateHint: '可選', optional: true },
    { service: 'order-service', title: 'Order :8081', purpose: '執行成交流程', stateHint: 'PENDING → …' },
    {
      service: 'risk-service',
      title: 'Risk :8082',
      purpose: '名目金額風控（Feign）',
      stateHint: '通過→ACCEPTED；拒絕→REJECTED'
    }
  ],
  CANCEL: [
    { service: 'frontend', title: '交易前台', purpose: '點擊取消', stateHint: 'UI → API' },
    { service: 'gateway', title: 'Gateway :8080', purpose: '統一入口轉發', stateHint: '可選', optional: true },
    { service: 'order-service', title: 'Order :8081', purpose: '取消尚未成交訂單', stateHint: 'PENDING → CANCELLED' }
  ]
};
