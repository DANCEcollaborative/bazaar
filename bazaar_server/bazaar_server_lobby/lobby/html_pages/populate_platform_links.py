#!/usr/bin/env python3
"""Populate or revert the `platform_link` column in the study CSV.

Each row receives a Bazaar login URL of the form:

    https://bazaar.lti.cs.cmu.edu/bazaar/login?
        roomName=<room name>&roomId=<group id>&
        id=<1|2 depending on first_test>&username=Student<student_number>&
        html=chem-lab_mm_<unique_id>

Consecutive rows that share the same scratchpad link reuse the same
roomId; the id value is 1 when first_test == 'A' and 2 otherwise.
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path
from typing import Dict, List, Tuple


CSV_DEFAULT = "VirtualLabStudy - study_links.csv"
MANIFEST_DEFAULT = "platform_links_manifest.json"
SCRATCHPAD_COLUMN = "scratchpad link"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--csv",
        default=CSV_DEFAULT,
        help="CSV to modify (default: %(default)s)",
    )
    parser.add_argument(
        "--mode",
        choices=("apply", "revert"),
        default="apply",
        help="Apply changes or revert to the last manifest (default: %(default)s)",
    )
    parser.add_argument(
        "--manifest",
        default=None,
        help=f"Manifest file (default: <csv-dir>/{MANIFEST_DEFAULT})",
    )
    parser.add_argument(
        "--room-name",
        default="labassistant",
        help="roomName parameter (default: %(default)s)",
    )
    parser.add_argument(
        "--start-room-id",
        type=int,
        default=1001,
        help="roomId assigned to the first scratchpad group (default: %(default)d)",
    )
    return parser.parse_args()


def read_rows(csv_path: Path) -> Tuple[List[Dict[str, str]], List[str]]:
    with csv_path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames is None:
            raise ValueError("CSV file is missing a header row")
        rows = [row for row in reader]
    return rows, reader.fieldnames


def write_rows(csv_path: Path, fieldnames: List[str], rows: List[Dict[str, str]]) -> None:
    with csv_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def sanitize_first_test(value: str, row_index: int) -> str:
    normalized = (value or "").strip().upper()
    if normalized not in {"A", "B"}:
        raise ValueError(f"Row {row_index + 2}: first_test must be 'A' or 'B', got {value!r}")
    return normalized


def ensure_column(fieldnames: List[str], column: str) -> None:
    if column not in fieldnames:
        raise ValueError(f"CSV is missing expected column '{column}'")


def build_link(
    *,
    room_name: str,
    room_id: int,
    first_test: str,
    student_number: str,
    unique_id: str,
) -> str:
    id_param = "1" if first_test == "A" else "2"
    username = f"{unique_id}_{first_test}"
    html_target = f"chem-lab_mm_{unique_id}"
    return (
        "https://bazaar.lti.cs.cmu.edu/bazaar/login?"
        f"roomName={room_name}&roomId={room_id}&id={id_param}"
        f"&username={username}&html={html_target}"
    )


def assign_room_ids(rows: List[Dict[str, str]], start_room_id: int) -> List[int]:
    ids: List[int] = []
    current_id = start_room_id
    previous_scratchpad = None
    for row in rows:
        scratchpad = (row.get(SCRATCHPAD_COLUMN, "") or "").strip()
        if previous_scratchpad is None:
            pass
        elif scratchpad != previous_scratchpad:
            current_id += 1
        ids.append(current_id)
        previous_scratchpad = scratchpad
    return ids


def apply(csv_path: Path, manifest_path: Path, room_name: str, start_room_id: int) -> None:
    rows, fieldnames = read_rows(csv_path)
    ensure_column(fieldnames, "first_test")
    ensure_column(fieldnames, "unique_id")
    ensure_column(fieldnames, SCRATCHPAD_COLUMN)
    ensure_column(fieldnames, "platform_link")

    room_ids = assign_room_ids(rows, start_room_id)
    manifest_rows = []

    for idx, (row, room_id) in enumerate(zip(rows, room_ids)):
        first_test = sanitize_first_test(row.get("first_test", ""), idx)
        unique_id = (row.get("unique_id", "") or "").strip()
        if not unique_id:
            raise ValueError(f"Row {idx + 2}: missing unique_id")
        student_number = (row.get("student_number", "") or "").strip()
        old_value = row.get("platform_link", "")
        manifest_rows.append(
            {
                "index": idx,
                "unique_id": unique_id,
                "platform_link": old_value,
            }
        )
        row["platform_link"] = build_link(
            room_name=room_name,
            room_id=room_id,
            first_test=first_test,
            student_number=student_number,
            unique_id=unique_id,
        )

    write_rows(csv_path, fieldnames, rows)
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "csv": str(csv_path.resolve()),
        "rows": manifest_rows,
    }
    manifest_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(f"Updated platform_link for {len(rows)} rows. Manifest written to {manifest_path}.")


def revert(csv_path: Path, manifest_path: Path) -> None:
    if not manifest_path.exists():
        print("Manifest not found; nothing to revert.")
        return
    payload = json.loads(manifest_path.read_text(encoding="utf-8"))
    recorded_csv = Path(payload.get("csv", "")).resolve()
    if recorded_csv != csv_path.resolve():
        raise ValueError("Manifest does not correspond to the provided CSV path")

    rows, fieldnames = read_rows(csv_path)
    ensure_column(fieldnames, "platform_link")

    for entry in payload.get("rows", []):
        idx = entry["index"]
        if idx >= len(rows):
            raise IndexError(f"Manifest row index {idx} is out of range")
        if (
            entry.get("unique_id")
            and rows[idx].get("unique_id", "").strip() != entry["unique_id"]
        ):
            raise ValueError(
                f"Manifest unique_id mismatch at row {idx + 2}: "
                f"expected {entry['unique_id']}, found {rows[idx].get('unique_id')}"
            )
        rows[idx]["platform_link"] = entry.get("platform_link", "")

    write_rows(csv_path, fieldnames, rows)
    manifest_path.unlink()
    print("Reverted platform_link column and removed manifest.")


def main() -> None:
    args = parse_args()
    csv_path = Path(args.csv)
    manifest_path = (
        Path(args.manifest)
        if args.manifest
        else csv_path.parent / MANIFEST_DEFAULT
    )

    try:
        if args.mode == "revert":
            revert(csv_path, manifest_path)
        else:
            apply(csv_path, manifest_path, args.room_name, args.start_room_id)
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
