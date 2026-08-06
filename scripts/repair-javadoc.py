# -*- coding: utf-8 -*-
"""Repair all broken JavaDoc blocks that contain nested /** or stray ' * /'."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(r"D:\ClaudeCode\FinTechDemo")


def extract_fields(block: str) -> tuple[str, str, str]:
    duty = tip = concept = ""
    m = re.search(r"【職責】\s*([^\n*]+)", block)
    if m:
        duty = m.group(1).strip().rstrip("。") + ("。" if not m.group(1).strip().endswith("。") else "")
        # cleanup accidental trailing from ' */'
        duty = duty.replace(" */", "").strip()
        if duty and not duty.endswith("。"):
            duty += "。"
    m = re.search(r"【技巧】\s*([^\n*]+)", block)
    if m:
        tip = m.group(1).strip()
        if tip and not tip.endswith("。"):
            tip += "。"
    m = re.search(r"【概念】\s*([^\n*]+)", block)
    if m:
        concept = m.group(1).strip()
        if concept and not concept.endswith("。"):
            concept += "。"
    return duty, tip, concept


def make_doc(duty: str, tip: str, concept: str) -> str:
    if not duty:
        duty = "見類別名稱與套件職責。"
    if not tip:
        tip = "配合同套件元件使用。"
    if not concept:
        concept = "教學 Demo 以可講清邊界為優先。"
    return (
        "/**\n"
        f" * 【職責】{duty}\n"
        f" * 【技巧】{tip}\n"
        f" * 【概念】{concept}\n"
        " */"
    )


def repair_file(text: str) -> str:
    # Find javadoc-like regions before public type that look broken
    pattern = re.compile(
        r"/\*\*.*?\*/\s*(?=(?:@\w+(?:\([^;]*?\))?\s*)*public\s+(?:class|interface|enum|record)\s+)",
        re.S,
    )

    def repl(m: re.Match) -> str:
        block = m.group(0)
        if " * /**" not in block and "/** 【" not in block and " * /" not in block:
            return block
        duty, tip, concept = extract_fields(block)
        return make_doc(duty, tip, concept)

    return pattern.sub(repl, text)


def main() -> None:
    fixed = 0
    still = []
    for path in sorted(ROOT.rglob("*.java")):
        if "build" in path.parts or "test" in path.parts:
            continue
        text = path.read_text(encoding="utf-8")
        if " * /**" not in text and "/** 【" not in text and not re.search(r"\n\s*\*\s*/\s*\n", text):
            continue
        new = repair_file(text)
        if new != text:
            path.write_text(new, encoding="utf-8", newline="\n")
            fixed += 1
        t2 = path.read_text(encoding="utf-8")
        if " * /**" in t2 or "/** 【" in t2 or re.search(r"\n\s*\*\s*/\s*\n", t2):
            still.append(str(path.relative_to(ROOT)).replace("\\", "/"))
    print(f"fixed={fixed} still={len(still)}")
    for s in still:
        print("STILL", s)

    # coverage
    miss = partial = 0
    for path in sorted(ROOT.rglob("*.java")):
        if "build" in path.parts or "test" in path.parts:
            continue
        t = path.read_text(encoding="utf-8")
        if "【職責】" not in t:
            miss += 1
            print("MISS", path.relative_to(ROOT))
        elif "【技巧】" not in t or "【概念】" not in t:
            partial += 1
            print("PARTIAL", path.relative_to(ROOT))
    print(f"coverage miss={miss} partial={partial}")


if __name__ == "__main__":
    main()
