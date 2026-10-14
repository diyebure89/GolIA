# -*- coding: utf-8 -*-
import importlib
OUT = r"c:\Users\diyeb\OneDrive\Desktop\Kiro\GolIA\docs\_mmd\_check.txt"
res = []
for m in ["PIL", "docx"]:
    try:
        importlib.import_module(m); res.append(m+"=OK")
    except Exception as e:
        res.append("%s=NO (%s)" % (m, type(e).__name__))
open(OUT,"w",encoding="utf-8").write("\n".join(res))
print("done")
