#!/usr/bin/env python3
"""Generate or remove per-student Chem Lab HTML files.

The script clones `chem-lab_mm_1.html` for every student listed in
`VirtualLabStudy - study_links.csv`, customizes only the RESOURCE_URLS block,
and writes the result to `chem-lab_mm_{unique_id}.html`.

Example usage (run from repo root):

    python bazaar_server/bazaar_server_lobby/lobby/html_pages/\
        generate_chem_lab_pages.py --mode apply

To undo:

    python bazaar_server/bazaar_server_lobby/lobby/html_pages/\
        generate_chem_lab_pages.py --mode revert
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Tuple


CSV_DEFAULT = "VirtualLabStudy - study_links.csv"
TEMPLATE_DEFAULT = "chem-lab_mm_1.html"
MANIFEST_DEFAULT = "chem_lab_mm_manifest.json"

RESOURCE_BLOCK_PATTERN = re.compile(
    r"(const\s+RESOURCE_URLS\s*=\s*\{)(.*?)(\}\s*;)",
    re.DOTALL,
)


PART_BASES = {
    "virtualLab": lambda ft: "https://virtual-lab-part1.pages.dev/",
    "virtualLab2": lambda ft: (
        "https://virtual-lab-part2-1.pages.dev/"
        if ft == "A"
        else "https://virtual-lab-part2-2.pages.dev/"
    ),
    "virtualLab3": lambda ft: (
        "https://virtual-lab-part3.pages.dev/"
        if ft == "A"
        else "https://virtual-lab-part3-2.pages.dev/"
    ),
    "virtualLab4": lambda ft: "https://virtual-lab-part4.pages.dev/",
}


PART_LABELS = {
    "virtualLab": "part1",
    "virtualLab2": "part2",
    "virtualLab3": "part3",
    "virtualLab4": "part4",
}


SCRATCHPAD_COLUMN = "scratchpad link"
SCRATCHPAD_SUFFIX = "edit?tab=t.0#heading=h.tlnk9tihvaj{ROOM}?showChat=false&noColors=false"


@dataclass
class Student:
    unique_id: str
    first_test: str
    scratchpad: str
    csv_index: int


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--csv",
        default=CSV_DEFAULT,
        help="Path to the study links CSV (default: %(default)s)",
    )
    parser.add_argument(
        "--template",
        default=TEMPLATE_DEFAULT,
        help="HTML template to clone (default: %(default)s)",
    )
    parser.add_argument(
        "--output-dir",
        default=None,
        help="Directory for generated files (default: template directory)",
    )
    parser.add_argument(
        "--manifest",
        default=None,
        help=f"Manifest location (default: <output-dir>/{MANIFEST_DEFAULT})",
    )
    parser.add_argument(
        "--mode",
        choices=("apply", "revert"),
        default="apply",
        help="Create files (apply) or delete previously generated files (revert)",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Allow rewriting existing per-student files during apply",
    )
    return parser.parse_args()


def read_students(csv_path: Path) -> List[Student]:
    students: List[Student] = []
    with csv_path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        line_no = 1
        for row in reader:
            line_no = reader.line_num
            unique_id = (row.get("unique_id", "") or "").strip()
            if not unique_id:
                continue
            if any(sep in unique_id for sep in ("/", "\\")):
                raise ValueError(f"Invalid unique_id on line {line_no}: {unique_id!r}")
            first_test = (row.get("first_test", "") or "").strip().upper()
            if first_test not in {"A", "B"}:
                raise ValueError(
                    f"Row {line_no}: expected first_test to be 'A' or 'B', got {first_test!r}"
                )
            scratchpad = (row.get(SCRATCHPAD_COLUMN, "") or "").strip()
            if not scratchpad:
                raise ValueError(f"Row {line_no}: missing '{SCRATCHPAD_COLUMN}' value")
            students.append(Student(unique_id, first_test, scratchpad, line_no))
    if not students:
        raise ValueError("CSV did not provide any students with unique_id values")
    return students


def load_template(template_path: Path) -> str:
    return template_path.read_text(encoding="utf-8")


def inject_resource_urls(template: str, replacements: Dict[str, str]) -> str:
    match = RESOURCE_BLOCK_PATTERN.search(template)
    if not match:
        raise ValueError("Unable to locate RESOURCE_URLS block in template")

    block = match.group(2)
    for key, value in replacements.items():
        pattern = re.compile(rf'({re.escape(key)}\s*:\s*")([^\"]*)(")')
        block, count = pattern.subn(rf"\1{value}\3", block, count=1)
        if count != 1:
            raise ValueError(f"Failed to update RESOURCE_URLS entry for {key}")

    return template[: match.start(2)] + block + template[match.end(2) :]


def append_name_param(base_url: str, unique_id: str, part_label: str, first_test: str) -> str:
    core = base_url.split("?")[0].split("#")[0]
    return f"{core}?name={unique_id}-{part_label}-{first_test}"


def build_resource_map(student: Student) -> Dict[str, str]:
    resources = {}
    for key, part_label in PART_LABELS.items():
        base_url_builder = PART_BASES[key]
        base = base_url_builder(student.first_test)
        resources[key] = append_name_param(base, student.unique_id, part_label, student.first_test)
    scratchpad = student.scratchpad.replace("edit?usp=sharing", SCRATCHPAD_SUFFIX)
    resources["scratchpad"] = scratchpad
    return resources


def ensure_directory(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def write_student_file(
    template_text: str,
    student: Student,
    output_dir: Path,
    overwrite: bool,
) -> Path:
    replacements = build_resource_map(student)
    content = inject_resource_urls(template_text, replacements)
    filename = f"chem-lab_mm_{student.unique_id}.html"
    destination = output_dir / filename
    if destination.exists() and not overwrite:
        raise FileExistsError(
            f"Destination {destination} exists. Use --overwrite to replace it."
        )
    destination.write_text(content, encoding="utf-8")
    return destination


def write_manifest(manifest_path: Path, output_dir: Path, files: Iterable[Path]) -> None:
    relative_paths = [str(f.relative_to(output_dir)) for f in files]
    payload = {
        "output_dir": str(output_dir.resolve()),
        "files": relative_paths,
    }
    manifest_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def load_manifest(manifest_path: Path) -> Tuple[Path, List[Path]]:
    data = json.loads(manifest_path.read_text(encoding="utf-8"))
    output_dir = Path(data["output_dir"]).resolve()
    files = [output_dir / Path(rel) for rel in data.get("files", [])]
    return output_dir, files


def revert_files(output_dir: Path, manifests: List[Path]) -> None:
    missing = []
    for file_path in manifests:
        if file_path.exists():
            file_path.unlink()
            print(f"[DELETED] {file_path.relative_to(output_dir)}")
        else:
            missing.append(file_path)
    if missing:
        for path in missing:
            print(f"[SKIPPED] {path} (not found)")


def main() -> None:
    args = parse_args()
    csv_path = Path(args.csv)
    template_path = Path(args.template)
    output_dir = Path(args.output_dir) if args.output_dir else template_path.parent
    manifest_path = (
        Path(args.manifest)
        if args.manifest
        else output_dir / MANIFEST_DEFAULT
    )

    if args.mode == "revert":
        if not manifest_path.exists():
            print("Manifest not found; nothing to revert.")
            return
        recorded_dir, files = load_manifest(manifest_path)
        revert_files(recorded_dir, files)
        if manifest_path.exists():
            manifest_path.unlink()
        return

    # Mode == apply
    ensure_directory(output_dir)
    students = read_students(csv_path)
    template_text = load_template(template_path)
    generated_paths: List[Path] = []
    for student in students:
        try:
            new_path = write_student_file(
                template_text, student, output_dir, overwrite=args.overwrite
            )
        except Exception as exc:  # clean up partial progress
            for created in generated_paths:
                if created.exists():
                    created.unlink()
            raise RuntimeError(
                f"Failed while processing student {student.unique_id} (CSV line {student.csv_index}): {exc}"
            ) from exc
        else:
            generated_paths.append(new_path)
            print(f"[WRITE] {new_path.relative_to(output_dir)}")

    write_manifest(manifest_path, output_dir, generated_paths)
    print(f"Generated {len(generated_paths)} files.")


if __name__ == "__main__":
    try:
        main()
    except Exception as err:
        print(f"Error: {err}", file=sys.stderr)
        sys.exit(1)
