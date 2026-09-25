import subprocess
from pathlib import Path

list_cmd = [
    "docker",
    "exec",
    "tender-mysql-1",
    "mysql",
    "-uschool",
    "-pschoolpass",
    "-N",
    "-e",
    (
        "SELECT table_name FROM information_schema.tables "
        "WHERE table_schema='school_portal' AND table_type='BASE TABLE' "
        "AND table_name <> 'flyway_schema_history' ORDER BY table_name"
    ),
]
raw = subprocess.check_output(list_cmd, text=True, stderr=subprocess.STDOUT)
tables = [
    line.strip()
    for line in raw.splitlines()
    if line.strip() and "Warning" not in line and "insecure" not in line
]
if not tables:
    raise SystemExit(f"No tables found to wipe:\n{raw}")

sql_path = Path("/tmp/wipe-school-portal-data.sql")
lines = ["SET FOREIGN_KEY_CHECKS=0;"]
lines.extend(f"TRUNCATE TABLE `{name}`;" for name in tables)
lines.append("SET FOREIGN_KEY_CHECKS=1;")
sql_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"Wiping {len(tables)} tables (keeping flyway_schema_history)")

with sql_path.open("r", encoding="utf-8") as handle:
    subprocess.check_call(
        [
            "docker",
            "exec",
            "-i",
            "tender-mysql-1",
            "mysql",
            "-uschool",
            "-pschoolpass",
            "school_portal",
        ],
        stdin=handle,
    )

count_cmd = [
    "docker",
    "exec",
    "tender-mysql-1",
    "mysql",
    "-uschool",
    "-pschoolpass",
    "-N",
    "school_portal",
    "-e",
    (
        "SELECT table_name, table_rows FROM information_schema.tables "
        "WHERE table_schema='school_portal' AND table_type='BASE TABLE' "
        "ORDER BY table_name"
    ),
]
print(subprocess.check_output(count_cmd, text=True, stderr=subprocess.STDOUT))
print("done")
