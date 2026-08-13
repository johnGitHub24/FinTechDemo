/**
 * 【職責】依 docs/catalog.yaml 產生靜態 HTML，並為 legacy 路徑寫導向 stub。
 * 【技巧】npx -p marked -p js-yaml；Mermaid 區塊轉 div.mermaid；頁內 .md 連結依 catalog 改寫。
 * 【概念】catalog 為書櫃／閱讀器／產生器唯一清單；權威 MD 在 _md/，瀏覽路徑 .md 為導向 HTML 的 stub。
 *
 * Usage (repo root):
 *   .\docs\tools\generate-docs-html.ps1
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { marked } from "marked";
import yaml from "js-yaml";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "../..");
const docs = path.join(root, "docs");
const catalogPath = path.join(docs, "catalog.yaml");

marked.setOptions({ gfm: true, breaks: false });

const STYLE = `
:root {
  --bg:#f5f7fa; --surface:#fff; --surface2:#eef2f6; --border:#d5dde6;
  --text:#1f2937; --muted:#5b6b7c; --accent:#0d7377; --warn:#b45309;
}
*, *::before, *::after { box-sizing:border-box; }
body { margin:0; font-family:"Segoe UI","Microsoft JhengHei",sans-serif; background:var(--bg); color:var(--text); line-height:1.7; }
header.top {
  border-bottom:1px solid var(--border);
  background:linear-gradient(135deg,#e6f3f2 0%,#e8eef5 55%,#f5f7fa 100%);
  padding:0.9rem 1.25rem;
}
header.top h1 { font-size:1.2rem; margin:0 0 0.25rem; }
header.top .sub { color:var(--muted); font-size:0.85rem; margin:0 0 0.45rem; }
header.top .links a { color:var(--accent); text-decoration:none; margin-right:0.85rem; font-size:0.82rem; }
main { max-width:900px; padding:1.25rem 1.5rem 3rem; margin:0 auto; }
.gate { background:var(--surface2); border-left:3px solid var(--accent); padding:0.55rem 0.85rem; font-size:0.85rem; margin-bottom:1rem; border-radius:0 6px 6px 0; }
.md-body h1 { font-size:1.55rem; margin:0 0 0.75rem; padding-bottom:0.4rem; border-bottom:2px solid var(--accent); }
.md-body h2 { font-size:1.2rem; margin:1.5rem 0 0.55rem; color:#0f4c5c; }
.md-body h3 { font-size:1.02rem; margin:1.1rem 0 0.4rem; color:var(--accent); }
.md-body p, .md-body li { color:#334155; margin-bottom:0.45rem; }
.md-body ul, .md-body ol { padding-left:1.35rem; margin:0.4rem 0 0.85rem; }
.md-body blockquote { border-left:3px solid var(--warn); background:var(--surface2); padding:0.55rem 0.9rem; margin:0.75rem 0; color:var(--muted); font-size:0.9rem; }
.md-body table { width:100%; border-collapse:collapse; font-size:0.85rem; margin:0.85rem 0; background:var(--surface); }
.md-body th, .md-body td { border:1px solid var(--border); padding:0.4rem 0.55rem; vertical-align:top; text-align:left; }
.md-body th { background:var(--surface2); color:var(--accent); }
.md-body code { background:#e8eef3; padding:0.1rem 0.35rem; border-radius:4px; font-size:0.85em; color:#0f4c5c; }
.md-body pre { background:#1e293b; color:#e2e8f0; padding:0.9rem 1rem; border-radius:8px; overflow-x:auto; font-size:0.8rem; margin:0.75rem 0; }
.md-body pre code { background:transparent; color:inherit; padding:0; }
.md-body a { color:var(--accent); }
.md-body hr { border:none; border-top:1px solid var(--border); margin:1.25rem 0; }
.md-body .mermaid { background:var(--surface); border:1px solid var(--border); border-radius:8px; padding:0.85rem; margin:0.85rem 0; }
.mermaid .note rect { fill:#fff8e6 !important; stroke:#d5dde6 !important; }
.mermaid .note text, .mermaid .note tspan { fill:#1f2937 !important; }
`.trim();

const MERMAID_INIT = `mermaid.initialize({
      startOnLoad: true,
      theme: "base",
      securityLevel: "loose",
      themeVariables: {
        primaryColor: "#e6f3f2",
        primaryTextColor: "#1f2937",
        primaryBorderColor: "#0d7377",
        lineColor: "#5b6b7c",
        secondaryColor: "#eef2f6",
        tertiaryColor: "#ffffff",
        noteBkgColor: "#fff8e6",
        noteTextColor: "#1f2937",
        noteBorderColor: "#d5dde6",
        background: "#ffffff",
        mainBkg: "#ffffff",
        actorBkg: "#e6f3f2",
        actorTextColor: "#1f2937",
        actorBorder: "#0d7377",
        signalColor: "#1f2937",
        signalTextColor: "#1f2937"
      }
    });`;

function loadCatalog() {
  const raw = fs.readFileSync(catalogPath, "utf8");
  return yaml.load(raw);
}

function relToDocs(fromHtml, toPath) {
  const fromDir = path.posix.dirname(fromHtml.replace(/\\/g, "/"));
  let rel = path.posix.relative(fromDir === "." ? "" : fromDir, toPath.replace(/\\/g, "/"));
  if (!rel || rel === "") rel = path.posix.basename(toPath);
  if (!rel.startsWith(".")) rel = "./" + rel;
  return rel;
}

/** Extra basename → html when legacy / filename ≠ catalog id（如 SPEC／CLAUDE）. */
const EXTRA_ALIASES = {
  "FinTechDemo-SPEC": "guides/spec.html",
  CLAUDE: "guides/claude.html",
  "deploy-readme": "deploy/deploy-readme.html",
  README: "deploy/deploy-readme.html",
};

/** Portal／互動頁（非 catalog.docs）的 basename → html. */
const PORTAL_ALIASES = {
  codeGraphic: "portals/codeGraphic.html",
  "learning-map": "portals/learning-map.html",
  "loop-guide": "portals/loop-guide.html",
  // stages.md → deploy/stages-doc（catalog）；互動頁用中文 legacy／stages.html
  "boot-entrypoint": "portals/boot-entrypoint.html",
  "demo-flow": "portals/demo-flow.html",
  handbook: "portals/handbook.html",
  swagger: "portals/swagger.html",
  "上線部署階段層次": "portals/stages.html",
  "學習導引地圖": "portals/learning-map.html",
  "部署跑通-LoopEngineering教學指導": "portals/loop-guide.html",
  "啟動流程-從執行到EntryPoint": "portals/boot-entrypoint.html",
  "啟動與Demo運作流程": "portals/demo-flow.html",
  "FinTechDemo-完整學習手冊": "portals/handbook.html",
};

function buildLinkMaps(entries, portals) {
  const byBasename = new Map();
  const byId = new Map();
  for (const e of entries) {
    byId.set(e.id, e.html);
    byBasename.set(path.basename(e.md, ".md"), e.html);
    byBasename.set(e.id, e.html);
    byBasename.set(path.basename(e.html, ".html"), e.html);
    for (const leg of e.legacy || []) {
      const bn = path.basename(leg);
      byBasename.set(bn.replace(/\.(md|html)$/i, ""), e.html);
    }
  }
  for (const [k, v] of Object.entries(EXTRA_ALIASES)) byBasename.set(k, v);
  for (const [k, v] of Object.entries(PORTAL_ALIASES)) byBasename.set(k, v);
  for (const p of portals || []) {
    // 勿用 id「stages」覆寫 docs 的 stages.md → stages-doc.html
    if (!byBasename.has(p.id)) byBasename.set(p.id, p.html);
    const htmlBase = path.basename(p.html, ".html");
    if (!byBasename.has(htmlBase) || htmlBase !== "stages") {
      byBasename.set(htmlBase, p.html);
    }
    for (const leg of p.legacy || []) {
      byBasename.set(path.basename(leg).replace(/\.(md|html)$/i, ""), p.html);
    }
  }
  return { byBasename, byId };
}

function decodeHrefPath(p) {
  try {
    return decodeURIComponent(p);
  } catch {
    return p;
  }
}

function resolveDocTarget(hrefPath, maps) {
  const decoded = decodeHrefPath(hrefPath).split("#")[0];
  const bare = decoded.replace(/\\/g, "/");
  // strip leading ../ and docs/ noise
  const cleaned = bare
    .replace(/^(\.\.\/)+/g, "")
    .replace(/^docs\//i, "")
    .replace(/^_md\//i, "");
  const base = path.posix.basename(cleaned).replace(/\.(md|html)$/i, "");
  return maps.byBasename.get(base) || null;
}

function rewriteDocLinks(html, fromHtml, maps) {
  return html.replace(/href="([^"]+\.(?:md|html))(#[^"]*)?"/gi, (full, p, hash) => {
    // keep absolute /docs/... as-is if already html under known tree
    const decoded = decodeHrefPath(p);
    if (/^https?:\/\//i.test(decoded)) return full;
    if (decoded.startsWith("/docs/") && decoded.endsWith(".html")) return full;
    const target = resolveDocTarget(p, maps);
    if (!target) return full;
    // already pointing at correct relative html?
    const want = relToDocs(fromHtml, target);
    const curBase = path.posix.basename(decoded).replace(/\.(md|html)$/i, "");
    const wantBase = path.posix.basename(target, ".html");
    if (decoded.endsWith(".html") && curBase === wantBase && !decoded.includes("%")) {
      // may still be wrong folder (e.g. architecture/codeGraphic.html)
      const normCur = decoded.replace(/\\/g, "/").replace(/^(\.\.\/)+/, "");
      if (normCur === target || normCur.endsWith("/" + path.posix.basename(target))) {
        /* fall through to rewrite for folder correctness */
      }
    }
    return `href="${want}${hash || ""}"`;
  });
}

function enhanceMermaidInHtml(html) {
  return html.replace(
    /<pre><code class="language-mermaid">([\s\S]*?)<\/code><\/pre>/g,
    (_, code) => {
      const decoded = code
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&amp;/g, "&")
        .replace(/&quot;/g, '"');
      return `<div class="mermaid">${decoded}</div>`;
    }
  );
}

function pageShell(title, srcRel, bodyHtml, fromHtml) {
  const indexHref = relToDocs(fromHtml, "index.html");
  const mapHref = relToDocs(fromHtml, "portals/learning-map.html");
  const readerHref = relToDocs(fromHtml, "md-reader.html");
  const swaggerHref = relToDocs(fromHtml, "portals/swagger.html");
  // 瀏覽勿開 raw _md／superpowers；僅顯示來源路徑給 IDE
  return `<!DOCTYPE html>
<html lang="zh-Hant">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>FinTechDemo — ${title}</title>
  <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
  <style>${STYLE}</style>
</head>
<body>
  <header class="top">
    <h1>${title}</h1>
    <p class="sub">來源（IDE 編輯）：<code>${srcRel}</code> · 重跑：<code>.\\scripts\\generate-docs-html.ps1</code></p>
    <p class="links">
      <a href="${indexHref}">統一學習入口</a>
      <a href="${mapHref}">學習導引地圖</a>
      <a href="${readerHref}">MD 閱讀器</a>
      <a href="${swaggerHref}">Swagger</a>
    </p>
  </header>
  <main>
    <div class="gate">請開本 HTML（或經 stub 導向）。勿依賴 raw <code>.md</code> 瀏覽。</div>
    <article class="md-body">
${bodyHtml}
    </article>
  </main>
  <script>
    ${MERMAID_INIT}
  </script>
</body>
</html>
`;
}

function stubHtml(toHref, label) {
  return `<!DOCTYPE html>
<html lang="zh-Hant">
<head>
  <meta charset="UTF-8" />
  <meta http-equiv="refresh" content="0; url=${toHref}" />
  <link rel="canonical" href="${toHref}" />
  <title>Redirect — ${label}</title>
  <script>location.replace(${JSON.stringify(toHref)} + location.search + location.hash);</script>
</head>
<body>
  <p>已改為主題目錄 HTML，請前往 <a href="${toHref}">${toHref}</a></p>
</body>
</html>
`;
}

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function writeStub(legacyRel, htmlRel) {
  const norm = legacyRel.replace(/\\/g, "/");
  const htmlNorm = htmlRel.replace(/\\/g, "/");
  if (norm === htmlNorm) return false;
  // never overwrite authoritative sources under _md/ or superpowers/
  if (norm.startsWith("_md/") || norm.startsWith("superpowers/")) return false;
  const outPath = path.join(docs, norm);
  ensureDir(outPath);
  const fromDir = path.posix.dirname(norm);
  let finalHref;
  if (fromDir === ".") finalHref = htmlNorm;
  else {
    finalHref = path.posix.relative(fromDir, htmlNorm);
    if (!finalHref.startsWith(".")) finalHref = "./" + finalHref;
  }
  fs.writeFileSync(outPath, stubHtml(finalHref, htmlNorm), "utf8");
  return true;
}

const catalog = loadCatalog();
const entries = catalog.docs || [];
const portals = catalog.portals || [];
const maps = buildLinkMaps(entries, portals);

let ok = 0;
let stubs = 0;

for (const e of entries) {
  const srcPath = path.join(docs, e.md);
  if (!fs.existsSync(srcPath)) {
    console.error("MISS", e.md);
    continue;
  }
  const md = fs.readFileSync(srcPath, "utf8");
  let html = marked.parse(md);
  html = rewriteDocLinks(html, e.html, maps);
  html = enhanceMermaidInHtml(html);
  const outPath = path.join(docs, e.html);
  ensureDir(outPath);
  fs.writeFileSync(outPath, pageShell(e.title, e.md, html, e.html), "utf8");
  console.log("OK", e.html);
  ok++;

  // public browse path: same folder as html, .md extension → stub to html
  const publicMd = e.html.replace(/\.html$/i, ".md");
  if (writeStub(publicMd, e.html)) {
    console.log("STUB", publicMd, "->", e.html);
    stubs++;
  }

  for (const leg of e.legacy || []) {
    const norm = leg.replace(/\\/g, "/");
    if (norm === e.md || norm === e.html || norm === publicMd) continue;
    if (writeStub(norm, e.html)) {
      console.log("STUB", norm, "->", e.html);
      stubs++;
    }
  }
}

for (const p of portals) {
  for (const leg of p.legacy || []) {
    const norm = leg.replace(/\\/g, "/");
    if (norm === p.html) continue;
    if (norm.startsWith("portals/") && norm === p.html) continue;
    if (writeStub(norm, p.html)) {
      console.log("STUB", norm, "->", p.html);
      stubs++;
    }
  }
}

console.log(`DONE generated=${ok} stubs=${stubs}`);
