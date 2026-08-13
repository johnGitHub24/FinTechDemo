#!/usr/bin/env python3
"""Static docs server with extensionless -> .html/.md fallback.

Stock `python -m http.server` 404s on /docs/md-reader (no .html).
IDE previews often strip .html; this handler maps to the real file.
"""
from __future__ import annotations

import argparse
import mimetypes
import os
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse


class DocsHandler(SimpleHTTPRequestHandler):
    def do_GET(self):  # noqa: N802
        parsed = urlparse(self.path)
        path = unquote(parsed.path)
        query = ("?" + parsed.query) if parsed.query else ""

        if path in ("/docs", "/docs/"):
            self.send_response(302)
            self.send_header("Location", "/docs/index.html" + query)
            self.end_headers()
            return

        local = self.translate_path(self.path)
        if not os.path.isfile(local) and not path.endswith("/"):
            _base, ext = os.path.splitext(local)
            if not ext:
                for candidate in (
                    local + ".html",
                    local + ".md",
                    os.path.join(local, "index.html"),
                ):
                    if os.path.isfile(candidate):
                        rel = os.path.relpath(candidate, self.directory).replace("\\", "/")
                        self.path = "/" + rel + query
                        break

        return SimpleHTTPRequestHandler.do_GET(self)

    def log_message(self, fmt: str, *args) -> None:
        import sys

        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=5500)
    parser.add_argument("--bind", default="127.0.0.1")
    parser.add_argument("--root", default=".")
    args = parser.parse_args()

    root = str(Path(args.root).resolve())
    os.chdir(root)
    mimetypes.add_type("text/markdown", ".md")
    mimetypes.add_type("application/yaml", ".yml")
    mimetypes.add_type("application/yaml", ".yaml")

    handler = partial(DocsHandler, directory=root)
    httpd = ThreadingHTTPServer((args.bind, args.port), handler)
    print(f"Serving {root} on http://{args.bind}:{args.port}/", flush=True)
    print(f"Open http://{args.bind}:{args.port}/docs/index.html", flush=True)
    print("Extensionless URLs map to .html/.md when present.", flush=True)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
