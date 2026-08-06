# FinTechDemo — Lombok / Java record 去樣板設計

**日期**：2026-08-06  
**狀態**：已核准並落地（註解已寫入對應原始檔）

## 目標

消除 DTO／Entity 手寫 getter／setter／建構子，對齊 TradingSpringBoot／TradingCRUD／APIGatewayMQ。

## 決策規則（開檔即見）

| 類型 | 寫法 | 為何 |
|------|------|------|
| Properties／API 回應／Kafka 事件／跨服務契約 | Java `record` | 不可變；編譯器產 `foo()`／equals／hashCode；啟動或組裝後不應改寫 |
| 需逐步 `setXxx` 的 Request、Jackson 可變綁定 | class + `@Data` | 組裝階段可變 |
| JPA Entity | class + `@Getter`/`@Setter`（勿 `@Data`） | 生命週期／dirty checking 必須可變 |

## Gateway 範例（已註解於原始碼）

- `ServiceUrlsProperties`：record + `@ConfigurationProperties`
- `GatewayRouteConfig`：注入後用 `urls.orderUrl()`／`urls.accountUrl()`
- `gateway/.../application.yml`：註明鍵 ↔ record 元件對應

## 驗證

`.\gradlew.bat check`；Gateway bootRun 曾驗證 record Properties 綁定。
