# -*- coding: utf-8 -*-
"""
Convierte 04_Diseno_UML_GUI_GolIA.md a Word (.docx):
 - Texto, encabezados y tablas -> nativos de Word (python-docx).
 - Diagramas Mermaid -> PNG (render con Chrome headless) insertados como imágenes.
"""
import os, re, subprocess, pathlib, time
from docx import Document
from docx.shared import Pt, RGBColor, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH

try:
    from PIL import Image
    HAVE_PIL = True
except Exception:
    HAVE_PIL = False

DOCS = r"c:\Users\diyeb\OneDrive\Desktop\Kiro\GolIA\docs"
MMD = os.path.join(DOCS, "_mmd")
MD = os.path.join(DOCS, "04_Diseno_UML_GUI_GolIA.md")
OUT = os.path.join(DOCS, "04_Diseno_UML_GUI_GolIA.docx")
LOG = os.path.join(MMD, "_build_log.txt")
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
AZUL = RGBColor(0x1b, 0x4b, 0x91)

log = []

with open(MD, "r", encoding="utf-8") as f:
    raw = f.read()

# ---- extraer bloques ```mermaid ... ``` y reemplazarlos por marcadores ----
diagrams = []
def repl(m):
    idx = len(diagrams)
    diagrams.append(m.group(1).strip())
    return "\n@@DIAGRAM_%d@@\n" % idx
text = re.sub(r"```mermaid\s*(.*?)```", repl, raw, flags=re.DOTALL)
# eliminar cualquier otro bloque de código restante
text = text.replace("```", "")
log.append("diagramas encontrados = %d" % len(diagrams))

# ---- render de cada diagrama a PNG ----
HTML_TMPL = """<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
<script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
<style>html,body{margin:0;padding:0;background:#fff}
#wrap{display:inline-block;padding:12px;background:#fff}.mermaid{font-size:16px}</style></head>
<body><div id="wrap"><pre class="mermaid">%s</pre></div>
<script>mermaid.initialize({startOnLoad:true,theme:'default',securityLevel:'loose'});</script>
</body></html>"""

png_paths = []
for i, code in enumerate(diagrams):
    html_path = os.path.join(MMD, "d%d.html" % i)
    png_path = os.path.join(MMD, "d%d.png" % i)
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(HTML_TMPL % code)
    file_url = pathlib.Path(html_path).as_uri()
    args = [CHROME, "--headless=new", "--disable-gpu", "--no-sandbox",
            "--hide-scrollbars", "--force-device-scale-factor=2",
            "--default-background-color=FFFFFFFF",
            "--virtual-time-budget=8000",
            "--window-size=1400,2000",
            "--screenshot=" + png_path, file_url]
    try:
        subprocess.run(args, capture_output=True, text=True, timeout=90)
    except Exception as e:
        log.append("diag %d ERROR render: %r" % (i, e))
    # recortar margen blanco con Pillow
    if HAVE_PIL and os.path.isfile(png_path):
        try:
            im = Image.open(png_path).convert("RGB")
            from PIL import ImageChops
            bg = Image.new("RGB", im.size, (255, 255, 255))
            diff = ImageChops.difference(im, bg)
            bbox = diff.getbbox()
            if bbox:
                pad = 8
                l = max(0, bbox[0]-pad); t = max(0, bbox[1]-pad)
                r = min(im.width, bbox[2]+pad); b = min(im.height, bbox[3]+pad)
                im = im.crop((l, t, r, b))
                im.save(png_path)
        except Exception as e:
            log.append("diag %d crop err: %r" % (i, e))
    png_paths.append(png_path if os.path.isfile(png_path) else None)
    log.append("diag %d png=%s size=%s" % (i, os.path.isfile(png_path),
               os.path.getsize(png_path) if os.path.isfile(png_path) else 0))

# ---------------- construir DOCX ----------------
doc = Document()
doc.styles["Normal"].font.name = "Calibri"
doc.styles["Normal"].font.size = Pt(11)

def add_runs_with_bold(paragraph, s):
    for tok in re.split(r'(\*\*.+?\*\*|`.+?`)', s):
        if not tok: continue
        if tok.startswith("**") and tok.endswith("**"):
            r = paragraph.add_run(tok[2:-2]); r.bold = True
        elif tok.startswith("`") and tok.endswith("`"):
            r = paragraph.add_run(tok[1:-1]); r.font.name = "Consolas"
        else:
            paragraph.add_run(tok)

def flush_table(rows):
    if not rows: return
    ncols = max(len(r) for r in rows)
    t = doc.add_table(rows=0, cols=ncols); t.style = "Light Grid Accent 1"
    for i, row in enumerate(rows):
        cells = t.add_row().cells
        for j in range(ncols):
            cells[j].text = ""
            p = cells[j].paragraphs[0]
            add_runs_with_bold(p, (row[j] if j < len(row) else "").strip())
            if i == 0:
                for run in p.runs: run.bold = True

def insert_diagram(idx):
    p = png_paths[idx] if idx < len(png_paths) else None
    if p and os.path.isfile(p):
        # ancho máximo 16 cm
        try:
            width = Inches(6.3)
            doc.add_picture(p, width=width)
            doc.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER
        except Exception as e:
            doc.add_paragraph("[Diagrama %d no disponible: %r]" % (idx, e))
    else:
        doc.add_paragraph("[Diagrama %d no disponible]" % idx)

lines = text.split("\n")
pending = []
i = 0
while i < len(lines):
    line = lines[i].rstrip()
    s = line.strip()
    # tabla
    if s.startswith("|") and s.endswith("|"):
        if re.match(r'^\|[\s:\-\|]+\|$', s):
            i += 1; continue
        pending.append([c.strip() for c in s.strip("|").split("|")])
        i += 1; continue
    elif pending:
        flush_table(pending); pending = []

    md = re.match(r'^@@DIAGRAM_(\d+)@@$', s)
    if md:
        insert_diagram(int(md.group(1))); i += 1; continue
    if s == "" or s == "---":
        i += 1; continue
    h = re.match(r'^(#{1,6})\s+(.*)$', s)
    if h:
        doc.add_heading(h.group(2).strip(), level=min(len(h.group(1)), 4)); i += 1; continue
    if s.startswith(">"):
        p = doc.add_paragraph(style="Intense Quote"); add_runs_with_bold(p, s.lstrip(">").strip()); i += 1; continue
    if re.match(r'^[-*]\s+', s):
        p = doc.add_paragraph(style="List Bullet"); add_runs_with_bold(p, re.sub(r'^[-*]\s+', '', s)); i += 1; continue
    if re.match(r'^\d+\.\s+', s):
        p = doc.add_paragraph(style="List Number"); add_runs_with_bold(p, re.sub(r'^\d+\.\s+', '', s)); i += 1; continue
    p = doc.add_paragraph(); add_runs_with_bold(p, s); i += 1
if pending:
    flush_table(pending)

doc.save(OUT)
log.append("SAVED=%s EXISTS=%s SIZE=%d" % (OUT, os.path.isfile(OUT), os.path.getsize(OUT) if os.path.isfile(OUT) else 0))
open(LOG, "w", encoding="utf-8").write("\n".join(log))
print("done")
