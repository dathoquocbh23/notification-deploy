#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Helper TẠM: chạy SQL trên Supabase, đọc creds từ backend/.env.
Dùng: python scripts/_dbq.py "select ... ;"
"""
import sys, re
from pathlib import Path
import psycopg2

for s in (sys.stdout, sys.stderr):
    try: s.reconfigure(encoding="utf-8")
    except Exception: pass

ENV = Path(__file__).resolve().parent.parent / "backend" / ".env"
env = {}
for line in ENV.read_text(encoding="utf-8").splitlines():
    line = line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    k, v = line.split("=", 1)
    env[k.strip()] = v.strip()

m = re.match(r"jdbc:postgresql://([^:/]+):(\d+)/(\S+)", env["DB_URL"])
host, port, db = m.group(1), m.group(2), m.group(3)

conn = psycopg2.connect(host=host, port=port, dbname=db,
                        user=env["DB_USER"], password=env["DB_PASSWORD"],
                        connect_timeout=15)
conn.autocommit = True
with conn.cursor() as cur:
    cur.execute(sys.argv[1])
    if cur.description:
        cols = [d[0] for d in cur.description]
        print(" | ".join(cols)); print("-" * 60)
        rows = cur.fetchall()
        for r in rows:
            print(" | ".join("" if v is None else str(v) for v in r))
        print(f"({len(rows)} dòng)")
    else:
        print(f"OK ({cur.rowcount} dòng ảnh hưởng)")
conn.close()
