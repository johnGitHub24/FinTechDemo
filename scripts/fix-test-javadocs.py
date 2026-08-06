# -*- coding: utf-8 -*-
"""Rewrite garbled test JavaDocs to proper UTF-8 Traditional Chinese."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(r"D:\ClaudeCode\FinTechDemo")

CLASS_DOCS = {
    "AccountApiIntegrationTest": (
        "覆蓋 Account HTTP API 整合層（JWT + 帳戶／持倉查詢）。",
        "以 @SpringBootTest 打真實端點驗證安全與查詢契約。",
        "整合測試驗證「安全濾器 → Controller → Service」整條鏈。",
    ),
    "AccountServiceApplicationTest": (
        "覆蓋 AccountServiceApplication 能否正常啟動 Spring context。",
        "Smoke test：contextLoads 代表組態與 Bean 沒有互撞。",
        "啟動測試是微服務最小安全網，改組態前先綠燈。",
    ),
    "AccountLedgerServiceTest": (
        "覆蓋 AccountLedgerService 帳本買賣與持倉更新。",
        "單元測試聚焦加減現金／持倉，不啟動完整 Web。",
        "帳本正確性是成交入帳的最後一道防線。",
    ),
    "GatewayApplicationTest": (
        "覆蓋 GatewayApplication 啟動與基本組態。",
        "Smoke test 確認路由 Bean 可載入。",
        "Gateway 掛了整站入口就掛，先保證能起來。",
    ),
    "JobServiceApplicationTest": (
        "覆蓋 JobServiceApplication 與排程組態啟動。",
        "Smoke test 確認 Scheduling 相關 Bean 可載入。",
        "Job 服務獨立部署，啟動失敗要最早發現。",
    ),
    "AuthServiceTest": (
        "覆蓋 AuthService 登入與 JWT 回應組裝。",
        "Mock Repository／JwtTokenProvider，驗證成功路徑。",
        "登入是所有受保護 API 的前置條件。",
    ),
    "StaleOrderServiceTest": (
        "覆蓋 StaleOrderService 逾時 PENDING 取消邏輯。",
        "單元測試用固定時間邊界驗證取消條件。",
        "對應 job-service 遠端觸發的內部取消能力。",
    ),
    "TradingServiceTest": (
        "覆蓋 TradingService 建單、冪等與風控成交。",
        "Mockito 隔離 RiskClient／Repository／Kafka publisher。",
        "訂單核心單元測試：happy path 與 reject 路徑都在這。",
    ),
    "OrderServiceApplicationTest": (
        "覆蓋 OrderServiceApplication 啟動。",
        "Smoke test：主入口 context 必須能起來。",
        "Order 是 Demo 主服務，啟動失敗等於 Demo 全滅。",
    ),
    "JwtTokenProviderTest": (
        "覆蓋 JwtTokenProvider 簽發、驗證與 Claim 解析。",
        "不啟動 Web，直接測 token 字串往返。",
        "JWT 錯了會全站 401，必須有獨立單元測試。",
    ),
    "TradingFlowIntegrationTest": (
        "覆蓋下單→風控→成交的端對端整合流程。",
        "@SpringBootTest 打真實 API，驗證狀態與餘額變化。",
        "整合測試是前後台 Demo 劇本的自動化版。",
    ),
    "RiskServiceTest": (
        "覆蓋 RiskService 現金與名義金額風控規則。",
        "純單元：構造 RiskCheckRequest 驗證 allow／reject。",
        "風控規則改動時，這裡是最快的迴歸網。",
    ),
    "RiskApiIntegrationTest": (
        "覆蓋 risk-service HTTP /api/risk/check 整合。",
        "啟動 Web 環境，驗證 JSON 契約。",
        "對齊 order-service Feign 呼叫的真實協定。",
    ),
    "RiskServiceApplicationTest": (
        "覆蓋 RiskServiceApplication 啟動。",
        "Smoke test 確認風控服務 context 正常。",
        "成交依賴 :8082，服務起不來會 Connection refused。",
    ),
}

METHOD_DOCS = {
    "create_shouldPersistPendingOrder": "CASE-TRADING-001：Given 合法下單 When create Then 回 PENDING 且 symbol 大寫。",
    "create_duplicateClientOrderId_shouldFail": "CASE-TRADING-002：Given 重複 clientOrderId When create Then 拋 BusinessException。",
    "execute_whenRiskAllows_shouldAcceptAndDeductCash": "CASE-TRADING-003：Given 風控通過 When execute Then ACCEPTED 且扣現金。",
    "execute_whenRiskRejects_shouldMarkRejected": "CASE-TRADING-004：Given 風控拒絕 When execute Then REJECTED 且現金不變。",
    "buyWithinCashAndLimit_shouldAllow": "CASE-RISK-001：Given 買入名義金額小於現金與上限 When check Then allowed。",
    "buyOverCash_shouldReject": "CASE-RISK-002：Given 買入超過現金 When check Then reject（insufficient cash）。",
    "overMaxNotional_shouldReject": "CASE-RISK-003：Given 名義金額超過上限 When check Then reject（max）。",
    "contextLoads": "CASE-SMOKE-001：Given 應用組態 When 載入 context Then 不拋例外。",
}

METHOD_FALLBACK = [
    (r"login|authenticate", "CASE-AUTH：Given 正確帳密 When 登入 Then 回 JWT／角色。"),
    (r"cancel|stale", "CASE-STALE：Given 逾時 PENDING When 取消 Then 狀態變 CANCELLED。"),
    (r"token|jwt|parse|validate|generate", "CASE-JWT：Given token When 簽發／驗證 Then Claim 正確或拒絕無效票。"),
    (r"buy|sell|ledger|apply|position", "CASE-LEDGER：Given 成交事件 When 入帳 Then 現金／持倉正確。"),
    (r"account|me|balance", "CASE-ACCT：Given 已登入 JWT When 查帳戶 Then 回餘額。"),
    (r"risk|check|allow|reject", "CASE-RISK：Given 風控請求 When check Then allow 或 reject。"),
    (r"order|trade|flow|execute|create|fill", "CASE-FLOW：Given Demo 交易步驟 When 呼叫 API Then 狀態符合劇本。"),
]


def class_doc(name: str) -> str:
    duty, tip, concept = CLASS_DOCS.get(
        name,
        (
            f"覆蓋 {name} 對應元件／層的行為。",
            "用單元或整合測試鎖定契約。",
            "測試是學習系統行為的可執行說明書。",
        ),
    )
    return (
        "/**\n"
        f" * 【職責】{duty}\n"
        f" * 【技巧】{tip}\n"
        f" * 【概念】{concept}\n"
        " */"
    )


def method_doc_line(name: str) -> str:
    if name in METHOD_DOCS:
        return METHOD_DOCS[name]
    for pat, text in METHOD_FALLBACK:
        if re.search(pat, name, re.I):
            return text
    return f"CASE：驗證 {name} 的預期行為（Given／When／Then 見方法名稱）。"


def needs_rewrite(doc: str) -> bool:
    if not doc:
        return True
    if "?" in doc or "\ufffd" in doc:
        return True
    has_cjk = any("\u4e00" <= c <= "\u9fff" for c in doc)
    return not has_cjk


def fix_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    original = text

    m = re.search(
        r"(?P<doc>/\*\*.*?\*/)(?P<ws>\s*)(?P<decl>(?:public\s+)?class\s+(?P<name>\w+))",
        text,
        re.S,
    )
    if m:
        if needs_rewrite(m.group("doc")) or "【職責】" not in m.group("doc"):
            text = text[: m.start("doc")] + class_doc(m.group("name")) + text[m.end("doc") :]
    else:
        m2 = re.search(r"(?P<decl>(?:public\s+)?class\s+(?P<name>\w+))", text)
        if m2:
            text = text[: m2.start("decl")] + class_doc(m2.group("name")) + "\n" + text[m2.start("decl") :]

    pattern = re.compile(
        r"(?P<doc>/\*\*.*?\*/\s*)?(?P<ann>@Test\s+)(?P<sig>void\s+(?P<name>\w+)\s*\()",
        re.S,
    )
    out = []
    last = 0
    for mm in pattern.finditer(text):
        out.append(text[last : mm.start()])
        name = mm.group("name")
        doc = mm.group("doc") or ""
        if needs_rewrite(doc) or "CASE-" not in doc:
            line = method_doc_line(name)
            block = f"    /**\n     * {line}\n     */\n    @Test\n    void {name}("
            out.append(block)
        else:
            out.append(mm.group(0))
        last = mm.end()
    text = "".join(out) + text[last:]

    if text != original:
        path.write_text(text, encoding="utf-8", newline="\n")
        return True
    return False


def main() -> None:
    changed = 0
    for path in sorted(ROOT.rglob("*.java")):
        if "build" in path.parts or "test" not in path.parts:
            continue
        if fix_file(path):
            changed += 1
            print("fixed", path.relative_to(ROOT))

    bad = []
    for path in sorted(ROOT.rglob("*.java")):
        if "build" in path.parts or "test" not in path.parts:
            continue
        t = path.read_text(encoding="utf-8")
        rel = str(path.relative_to(ROOT))
        if "【職責】" not in t:
            bad.append(("no-duty", rel))
        if re.search(r"\*\s*\?{3,}", t):
            bad.append(("qmarks", rel))
        tests = len(re.findall(r"@Test\b", t))
        cases = len(re.findall(r"CASE-", t))
        if cases < tests:
            bad.append((f"cases {cases}/{tests}", rel))
        if not any("\u4e00" <= c <= "\u9fff" for c in t):
            bad.append(("no-cjk", rel))
    print("changed", changed, "bad", len(bad))
    for b in bad:
        print(b)


if __name__ == "__main__":
    main()
