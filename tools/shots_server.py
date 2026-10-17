#!/usr/bin/env python3
"""Live screenshot gallery: serves a shots folder over HTTP and refreshes as new PNGs land.

Usage: python3 tools/shots_server.py [--dir shots] [--port 8765] [--host 127.0.0.1]
Local only by default; pass --host 0.0.0.0 to open http://<this machine's LAN IP>:<port>/ from a
phone (no login: anyone on the network can see the shots). Read-only: only PNG files under --dir
are served, newest first.
"""

import argparse
import json
import mimetypes
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

PAGE = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Glass Launcher shots</title>
<style>
body { margin: 0; background: #111; color: #eee; font: 15px system-ui, sans-serif; }
header { position: sticky; top: 0; background: #1c1c1e; padding: 12px 16px; border-bottom: 1px solid #333; }
main { display: grid; grid-template-columns: repeat(auto-fill, minmax(420px, 1fr)); gap: 16px; padding: 16px; }
figure { margin: 0; background: #1c1c1e; border-radius: 10px; overflow: hidden; }
figure.new { outline: 3px solid #0a84ff; }
img { display: block; width: 100%; height: auto; cursor: zoom-in; }
figcaption { padding: 8px 10px; font-size: 13px; color: #bbb; word-break: break-all; }
#big { display: none; position: fixed; inset: 0; background: #000e; z-index: 9; }
#big img { width: 100%; height: 100%; object-fit: contain; cursor: zoom-out; }
</style></head><body>
<header><b>Glass Launcher shots</b> <span id="count"></span> (updates every 3 s)</header>
<main id="grid"></main>
<div id="big"><img alt=""></div>
<script>
let seen = new Set();
const big = document.getElementById('big');
big.onclick = () => { big.style.display = 'none'; };
async function poll() {
  try {
    const files = await (await fetch('/list.json', {cache: 'no-store'})).json();
    const grid = document.getElementById('grid');
    document.getElementById('count').textContent = files.length + ' images';
    const fresh = seen.size > 0;
    grid.replaceChildren(...files.map(f => {
      const fig = document.createElement('figure');
      if (fresh && !seen.has(f.path + f.mtime)) fig.className = 'new';
      const img = document.createElement('img');
      img.src = '/img/' + encodeURI(f.path) + '?v=' + f.mtime;
      img.alt = f.path; img.loading = 'lazy';
      img.onclick = () => { big.querySelector('img').src = img.src; big.style.display = 'block'; };
      const cap = document.createElement('figcaption');
      cap.textContent = f.path + '  ' + new Date(f.mtime * 1000).toLocaleTimeString();
      fig.append(img, cap);
      return fig;
    }));
    seen = new Set(files.map(f => f.path + f.mtime));
  } catch (e) { /* server restarting; try again next tick */ }
}
poll(); setInterval(poll, 3000);
</script></body></html>
"""


def make_handler(root: Path) -> type[SimpleHTTPRequestHandler]:
    class Handler(SimpleHTTPRequestHandler):
        def log_message(self, format: str, *args: object) -> None:
            pass

        def send_body(self, body: bytes, content_type: str) -> None:
            self.send_response(200)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)

        def do_GET(self) -> None:
            path = self.path.split("?", 1)[0]
            if path == "/":
                self.send_body(PAGE.encode(), "text/html; charset=utf-8")
            elif path == "/list.json":
                files = sorted(root.rglob("*.png"), key=lambda p: p.stat().st_mtime, reverse=True)
                items = [{"path": p.relative_to(root).as_posix(), "mtime": int(p.stat().st_mtime)} for p in files]
                self.send_body(json.dumps(items).encode(), "application/json")
            elif path.startswith("/img/"):
                from urllib.parse import unquote

                target = (root / unquote(path[len("/img/"):])).resolve()
                # Only PNGs inside the served folder; no traversal out of it.
                if target.suffix != ".png" or root not in target.parents or not target.is_file():
                    self.send_error(404)
                    return
                self.send_body(target.read_bytes(), mimetypes.types_map[".png"])
            else:
                self.send_error(404)

    return Handler


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--dir", default="shots")
    ap.add_argument("--port", type=int, default=8765)
    ap.add_argument("--host", default="127.0.0.1")
    args = ap.parse_args()
    root = Path(args.dir).resolve()
    root.mkdir(parents=True, exist_ok=True)
    print(f"serving {root} on {args.host}:{args.port}", flush=True)
    ThreadingHTTPServer((args.host, args.port), make_handler(root)).serve_forever()


if __name__ == "__main__":
    main()
