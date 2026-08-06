# FinTechDemo Design

日期：2026-08-05（再修訂）  
路徑：`D:\ClaudeCode\FinTechDemo`

## 理解（使用者要求）

1. Demo 技術有**次序性**，不能當平鋪清單亂拼。  
2. 先**參考各子專案真實技巧**，再寫開發文件。  
3. 要用 **Mermaid／HTML** 說明系統架構**為什麼**長這樣。  
4. 收斂成一個展示倉庫：Frontend → Gateway → MS → Kafka → Backend。  
5. 「Kafka 先不要」＝實作順序上先 Gateway／MS，再 Kafka；Kafka 仍要。

## 已完成的文件產出

| 產物 | 作用 |
|------|------|
| `docs/技術次序與架構為什麼.md` | 次序 → 分層理由 → Phase |
| `docs/codeGraphic.html` | 四 Tab 視覺（次序／為什麼／請求流／實作上雲） |
| `docs/技術融合對照.md` | 逐專案採納技巧（含路徑級來源） |
| SPEC／architecture／README | 指向上述敘事 |

## 次序（約束）

PagingList → Security → CRUD → Gateway → MicroService → Kafka → Job → AOP → Locust → Actuator → K8s

## 下一步

使用者確認敘事後 → P0 EngineeringOS 多模組骨架實作。
