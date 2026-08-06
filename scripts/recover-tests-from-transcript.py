# -*- coding: utf-8 -*-
import json
import re
from pathlib import Path

TRANS = Path(r"C:\Users\johnpeng\.cursor\projects\d-ClaudeCode-Trading-System-MVP\agent-transcripts\6b207706-258d-4ccd-935e-1ecf3c5f592f\6b207706-258d-4ccd-935e-1ecf3c5f592f.jsonl")
OUT = Path(r"D:\ClaudeCode\FinTechDemo\.tmp-test-recover")
OUT.mkdir(exist_ok=True)

classes = [
    "RiskServiceTest",
    "TradingServiceTest",
    "AuthServiceTest",
    "JwtTokenProviderTest",
    "StaleOrderServiceTest",
    "OrderServiceApplicationTest",
    "RiskServiceApplicationTest",
    "GatewayApplicationTest",
    "JobServiceApplicationTest",
    "AccountServiceApplicationTest",
    "AccountLedgerServiceTest",
    "AccountApiIntegrationTest",
    "RiskApiIntegrationTest",
    "TradingFlowIntegrationTest",
]

text = TRANS.read_text(encoding="utf-8", errors="ignore")
# Also search nested agent transcripts
for p in Path(r"C:\Users\johnpeng\.cursor\projects\d-ClaudeCode-Trading-System-MVP\agent-transcripts").rglob("*.jsonl"):
    if p == TRANS:
        continue
    try:
        chunk = p.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        continue
    if "class RiskServiceTest" in chunk or "buyWithinCashAndLimit" in chunk:
        text += "\n" + chunk

for cls in classes:
    # Prefer complete file with package + class + methods
    pattern = re.compile(
        rf"(package com\.fintech\.demo[^\n]*;\r?\n(?:import[\s\S]*?)?(?:@[\w.()\"'\s]+)*\s*(?:/\*\*[\s\S]*?\*/\s*)?(?:public\s+)?class {cls}\b[\s\S]*?\n\}})",
        re.M,
    )
    best = ""
    for m in pattern.finditer(text):
        body = m.group(1)
        # unescape json if needed
        if "\\n" in body and "\n" not in body[:80]:
            try:
                body = json.loads('"' + body.replace('"', '\\"') + '"')
            except Exception:
                body = body.encode("utf-8").decode("unicode_escape")
        if "void " in body and len(body) > len(best):
            # require not obviously truncated mid-method only
            if body.count("{") <= body.count("}") + 2:
                best = body
    print(cls, "len", len(best))
    if best:
        (OUT / f"{cls}.java").write_text(best, encoding="utf-8")
