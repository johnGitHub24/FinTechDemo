# -*- coding: utf-8 -*-
"""Batch-add 【職責】【技巧】【概念】 class JavaDocs for FinTechDemo main sources."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(r"D:\ClaudeCode\FinTechDemo")

# Exact replacements / prepends for files missing 職責
CLASS_DOCS: dict[str, str] = {
    "gateway/src/main/java/com/fintech/demo/gateway/config/ServiceUrlsProperties.java": """/**
 * 【職責】綁定下游服務基底 URL（order／risk 等），供 Gateway 路由轉發。
 * 【技巧】{@code @ConfigurationProperties(prefix = "fintech.services")} 對應 application.yml。
 * 【概念】固定 URL 路由是 Demo 敘事的簡化版；正式環境可改服務發現 {@code lb://}。
 * getter／setter：對應 YAML 欄位綁定，不另寫商業規則。
 */
""",
    "job-service/src/main/java/com/fintech/demo/job/JobServiceApplication.java": """/**
 * 【職責】job-service 啟動入口（排程觸發逾時取消）。
 * 【技巧】{@code @SpringBootApplication} 掃描本模組；實際取消邏輯透過 HTTP 打 order internal API。
 * 【概念】把排程拆成獨立進程，避免跟交易熱路徑搶執行緒。
 */
""",
    "job-service/src/main/java/com/fintech/demo/job/config/SchedulingConfig.java": """/**
 * 【職責】啟用並設定 job-service 的排程執行緒池。
 * 【技巧】{@code SchedulingConfigurer} 自訂 {@link ThreadPoolTaskScheduler}，避免預設單執行緒卡住。
 * 【概念】排程與業務觸發分離：此處只管「何時跑」，真正取消在 RemoteJobTriggerService。
 */
""",
    "job-service/src/main/java/com/fintech/demo/job/scheduler/CancelStaleOrdersJob.java": """/**
 * 【職責】依 cron 週期觸發「逾時未成交訂單取消」。
 * 【技巧】{@code @Scheduled(cron = "${fintech.job.cancel-cron}")} 把頻率外置到設定檔。
 * 【概念】Job 本身不碰 DB，只呼叫 RemoteJobTriggerService → order-service internal API。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/domain/OrderSide.java": """/**
 * 【職責】訂單買賣方向列舉。
 * 【技巧】以 enum 取代字串，避免大小寫／拼字錯誤流入交易路徑。
 * 【概念】BUY 扣現金加持倉；SELL 扣持倉加現金（見 TradingService.execute）。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/domain/Role.java": """/**
 * 【職責】JWT／授權角色（USER／ADMIN）。
 * 【技巧】種子資料先落地角色，Security 以 ROLE_ 前綴對應。
 * 【概念】RBAC：USER 做自己的單；ADMIN 可看全站審計。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/domain/OrderStatus.java": """/**
 * 【職責】訂單狀態機（精簡）：PENDING → ACCEPTED｜REJECTED｜CANCELLED。
 * 【技巧】成交／取消前都以 PENDING 做 guard，避免重入。
 * 【概念】狀態機比「隨意改欄位」更可講、更好測。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/dto/AccountResponse.java": """/**
 * 【職責】帳戶餘額查詢回應 DTO。
 * 【技巧】與 Entity 分離，避免把 JPA 欄位直接暴露給前端。
 * 【概念】Portal／Trade 畫面顯示現金用此結構。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/dto/AuditLogResponse.java": """/**
 * 【職責】審計紀錄查詢回應 DTO。
 * 【技巧】含 action／resource／detail／createdAt，對應 admin 審計表。
 * 【概念】審計是「誰對什麼做了什麼」的證據鏈，不是交易帳本本身。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/dto/LoginRequest.java": """/**
 * 【職責】登入請求：username／password。
 * 【技巧】{@code @NotBlank} 在 Controller {@code @Valid} 時擋空字串。
 * 【概念】密碼只作驗證輸入，回應不會回傳密碼。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/dto/LoginResponse.java": """/**
 * 【職責】登入成功回應：JWT、使用者與角色。
 * 【技巧】{@code tokenType} 預設 Bearer，對齊前端 Authorization 標頭。
 * 【概念】無狀態認證：之後請求靠 JWT，伺服器不需 session。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/dto/PositionResponse.java": """/**
 * 【職責】持倉查詢回應 DTO（標的／數量／均價）。
 * 【技巧】均價用 BigDecimal，避免浮點誤差。
 * 【概念】持倉是成交後的庫存觀點，與 PENDING 訂單不同。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/infrastructure/AccountRepository.java": """/**
 * 【職責】帳戶（現金）持久化存取。
 * 【技巧】Spring Data 方法名 {@code findByUserId} 自動產生查詢。
 * 【概念】Repository 只做存取，扣款規則在 TradingService。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/infrastructure/AuditLogRepository.java": """/**
 * 【職責】審計紀錄持久化與分頁查詢。
 * 【技巧】{@code findAllByOrderByCreatedAtDesc} 讓最新事件在前。
 * 【概念】審計表可追加、不改歷史，利於追溯。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/infrastructure/OrderRepository.java": """/**
 * 【職責】訂單持久化與條件分頁查詢。
 * 【技巧】JPQL 以 {@code :status IS NULL OR ...} 做可選狀態篩選；{@code clientOrderId} 冪等檢查。
 * 【概念】查詢條件外置到 Repository，Service 保持流程可讀。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/infrastructure/PositionRepository.java": """/**
 * 【職責】持倉持久化存取。
 * 【技巧】{@code findByUserIdAndSymbol} 對齊「一人一標的一列」模型。
 * 【概念】成交後 upsert 持倉；賣出前必須先有足夠數量。
 */
""",
    "order-service/src/main/java/com/fintech/demo/order/infrastructure/UserRepository.java": """/**
 * 【職責】使用者持久化存取。
 * 【技巧】{@code findByUsername} 供登入與 JWT 主體解析。
 * 【概念】使用者與帳戶一對一，交易以 userId 串起來。
 */
""",
}


def ensure_triple(doc: str, duty_hint: str, tip_hint: str, concept_hint: str) -> str:
    """Ensure a javadoc body contains 職責/技巧/概念 lines."""
    body = doc.strip()
    if "【職責】" not in body:
        body = f"【職責】{duty_hint}\n * 【技巧】{tip_hint}\n * 【概念】{concept_hint}"
    else:
        if "【技巧】" not in body:
            body = body.rstrip("。") + "\n * 【技巧】" + tip_hint
            if not body.endswith("。") and not tip_hint.endswith("。"):
                pass
        if "【概念】" not in body:
            body = body.rstrip() + "\n * 【概念】" + concept_hint
    # normalize leading stars
    lines = []
    for line in body.splitlines():
        s = line.strip()
        if s.startswith("*"):
            s = s.lstrip("*").strip()
        lines.append(" * " + s if s else " *")
    return "/**\n" + "\n".join(lines) + "\n */"


def heuristic_for(rel: str, class_name: str) -> tuple[str, str, str]:
    pkg = rel.replace("\\", "/")
    if "Controller" in class_name:
        return (
            f"{class_name}：對外 HTTP 入口，轉交 Service。",
            "只做參數／驗證／HTTP 狀態；商業規則在 Service。",
            "薄 Controller 利於測試與替換傳輸層。",
        )
    if "Service" in class_name and "Application" not in class_name:
        return (
            f"{class_name}：封裝商業流程與交易邊界。",
            "讀多用 @Transactional(readOnly=true)；寫入走預設交易。",
            "Service 是 Demo 時最常講的「流程編排」層。",
        )
    if "Repository" in class_name:
        return (
            f"{class_name}：持久化存取，不含商業規則。",
            "Spring Data 方法名／JPQL 產生查詢。",
            "資料存取與領域規則分離，避免 Repository 膨脹。",
        )
    if "Exception" in class_name:
        return (
            f"{class_name}：表達可預期業務／資源錯誤。",
            "由 GlobalExceptionHandler 映成 HTTP 狀態。",
            "用例外類型區分 404／400，比回傳 null 更明確。",
        )
    if "Config" in class_name or "Configuration" in class_name:
        return (
            f"{class_name}：組態與 Bean 組裝。",
            "以 @Configuration／Properties 外置環境差異。",
            "組態與業務分離，本機／Docker profile 才好切。",
        )
    if "Client" in class_name:
        return (
            f"{class_name}：跨服務 Feign 客戶端。",
            "url 來自設定，固定 URL 示意服務拆分。",
            "Demo 可講：下一步可換成服務發現。",
        )
    if pkg.startswith("common/"):
        return (
            f"{class_name}：跨服務共用契約（DTO／事件／常數）。",
            "放 common 模組避免各服務複製貼上欄位。",
            "契約穩定是微服務協作的前提。",
        )
    if "Entity" in class_name:
        return (
            f"{class_name}：JPA 實體，對應資料表欄位。",
            "欄位與 DB 對齊；商業流程不寫在 Entity。",
            "Entity 是持久化模型，不一定等於 API 模型。",
        )
    if "Application" in class_name:
        return (
            f"{class_name}：Spring Boot 啟動入口。",
            "@SpringBootApplication 啟動自動組態。",
            "每個微服務一個 main，對應一個可獨立部署單元。",
        )
    return (
        f"{class_name}：見類別名稱與套件職責。",
        "配合同套件 Service／Controller 使用。",
        "教學 Demo 以可講清邊界為優先。",
    )


def upgrade_first_class_javadoc(text: str, rel: str) -> str:
    m = re.search(
        r"(?P<doc>/\*\*.*?\*/)(?P<ws>\s*)(?P<ann>(?:@\w+(?:\([^)]*\))?\s*)*)(?P<decl>public\s+(?:class|interface|enum|record)\s+(?P<name>\w+))",
        text,
        re.S,
    )
    if not m:
        # no javadoc — insert before first public type
        m2 = re.search(
            r"(?P<ann>(?:@\w+(?:\([^)]*\))?\s*)*)(?P<decl>public\s+(?:class|interface|enum|record)\s+(?P<name>\w+))",
            text,
            re.S,
        )
        if not m2:
            return text
        duty, tip, concept = heuristic_for(rel, m2.group("name"))
        doc = ensure_triple("", duty, tip, concept)
        return text[: m2.start()] + doc + "\n" + text[m2.start() :]

    name = m.group("name")
    old = m.group("doc")
    if "【職責】" in old and "【技巧】" in old and "【概念】" in old:
        return text
    duty, tip, concept = heuristic_for(rel, name)
    # keep existing 職責 sentence if present
    duty_m = re.search(r"【職責】([^\n*]*)", old)
    if duty_m and duty_m.group(1).strip():
        duty = duty_m.group(1).strip()
    tip_m = re.search(r"【技巧】([^\n*]*)", old)
    if tip_m and tip_m.group(1).strip():
        tip = tip_m.group(1).strip()
    concept_m = re.search(r"【概念】([^\n*]*)", old)
    if concept_m and concept_m.group(1).strip():
        concept = concept_m.group(1).strip()
    new_doc = ensure_triple(old, duty, tip, concept)
    return text[: m.start("doc")] + new_doc + text[m.end("doc") :]


def apply_exact(rel: str, text: str, doc: str) -> str:
    # replace existing leading javadoc or insert before annotations/type
    m = re.search(
        r"(?P<doc>/\*\*.*?\*/\s*)?(?P<rest>(?:@\w+(?:\([^)]*\))?\s*)*public\s+(?:class|interface|enum|record)\s+)",
        text,
        re.S,
    )
    if not m:
        return text
    if m.group("doc"):
        return text[: m.start("doc")] + doc + text[m.end("doc") :]
    return text[: m.start("rest")] + doc + text[m.start("rest") :]


def main() -> None:
    changed = []
    # 1) exact docs for known misses / overrides
    for rel, doc in CLASS_DOCS.items():
        path = ROOT / rel.replace("/", "\\")
        if not path.exists():
            path = ROOT / rel
        text = path.read_text(encoding="utf-8")
        new = apply_exact(rel, text, doc)
        if new != text:
            path.write_text(new, encoding="utf-8", newline="\n")
            changed.append(rel)

    # 2) upgrade all main java to have triple block
    for path in sorted(ROOT.rglob("*.java")):
        if "build" in path.parts or "test" in path.parts:
            continue
        rel = str(path.relative_to(ROOT)).replace("\\", "/")
        text = path.read_text(encoding="utf-8")
        new = upgrade_first_class_javadoc(text, rel)
        if new != text:
            path.write_text(new, encoding="utf-8", newline="\n")
            if rel not in changed:
                changed.append(rel)

    # summary
    miss = []
    partial = []
    for path in sorted(ROOT.rglob("*.java")):
        if "build" in path.parts or "test" in path.parts:
            continue
        t = path.read_text(encoding="utf-8")
        rel = str(path.relative_to(ROOT)).replace("\\", "/")
        if "【職責】" not in t:
            miss.append(rel)
        elif "【技巧】" not in t or "【概念】" not in t:
            partial.append(rel)
    print(f"changed={len(changed)}")
    print(f"miss={len(miss)}")
    for m in miss:
        print("MISS", m)
    print(f"partial={len(partial)}")
    for m in partial:
        print("PARTIAL", m)


if __name__ == "__main__":
    main()
