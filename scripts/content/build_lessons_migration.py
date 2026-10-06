"""Sinh Flyway migration seed bài học YouTube + quiz từ các file JSON nội dung.

Cách dùng (chạy từ thư mục gốc repo hoặc bất kỳ đâu):
    python folkify_backend/scripts/content/build_lessons_migration.py \
        --content docs/content --out folkify_backend/src/main/resources/db/migration/V16__seed_youtube_lessons_and_quiz.sql
    python folkify_backend/scripts/content/build_lessons_migration.py --content docs/content --verify-only

- Kiểm tra mỗi video còn sống + cho phép embed qua YouTube oEmbed (HTTP 200).
- Kiểm tra quiz: SINGLE đúng 1 đáp án đúng, MULTI >= 1, mỗi câu >= 2 lựa chọn.
- UUID sinh bằng uuid5 từ slug → chạy lại cho ra cùng SQL.
Chỉ dùng thư viện chuẩn.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

OEMBED_URL = "https://www.youtube.com/oembed?format=json&url="
WATCH_URL = "https://www.youtube.com/watch?v="
EMBED_URL = "https://www.youtube.com/embed/"
NAMESPACE = uuid.UUID(
    "6f1c3a52-6d0e-4b8f-9a51-2f0b7c4e9d10"
)  # cố định để UUID ổn định giữa các lần chạy
FREE_LESSONS_PER_INSTRUMENT = 2
LEVELS = {"Beginner", "Intermediate", "Advanced"}
REQUEST_TIMEOUT_S = 15
REQUEST_RETRIES = 2


def required_plan(order_index: int, level: str) -> str:
    if order_index < FREE_LESSONS_PER_INSTRUMENT:
        return "FREE"
    return "PRO" if level == "Advanced" else "BASIC"


def oembed_ok(video_id: str) -> bool:
    url = OEMBED_URL + urllib.parse.quote(WATCH_URL + video_id, safe="")
    for attempt in range(REQUEST_RETRIES + 1):
        try:
            with urllib.request.urlopen(url, timeout=REQUEST_TIMEOUT_S) as resp:
                return resp.status == 200
        except urllib.error.HTTPError:
            return False
        except urllib.error.URLError:
            if attempt == REQUEST_RETRIES:
                raise
            time.sleep(1 + attempt)
    return False


def validate(data: dict, path: Path) -> list[str]:
    errors = []
    slugs = set()
    for li, lesson in enumerate(data.get("lessons", [])):
        where = f"{path.name} lesson[{li}] {lesson.get('slug')}"
        for field in (
            "slug",
            "title",
            "level",
            "youtubeVideoId",
            "channelName",
            "sourceUrl",
        ):
            if not lesson.get(field):
                errors.append(f"{where}: thiếu {field}")
        if lesson.get("level") not in LEVELS:
            errors.append(f"{where}: level không hợp lệ {lesson.get('level')}")
        if lesson.get("slug") in slugs:
            errors.append(f"{where}: trùng slug")
        slugs.add(lesson.get("slug"))
        if len(lesson.get("youtubeVideoId", "")) != 11:
            errors.append(f"{where}: youtubeVideoId phải 11 ký tự")
        for qi, q in enumerate(lesson.get("quiz", [])):
            qwhere = f"{where} quiz[{qi}]"
            options = q.get("options", [])
            correct = sum(1 for o in options if o.get("correct"))
            if len(options) < 2:
                errors.append(f"{qwhere}: cần >= 2 lựa chọn")
            if q.get("type") == "SINGLE" and correct != 1:
                errors.append(
                    f"{qwhere}: SINGLE phải có đúng 1 đáp án đúng (đang {correct})"
                )
            if q.get("type") == "MULTI" and correct < 1:
                errors.append(f"{qwhere}: MULTI cần >= 1 đáp án đúng")
            if q.get("type") not in ("SINGLE", "MULTI"):
                errors.append(f"{qwhere}: type không hợp lệ")
    return errors


def sql(value) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, int):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def lesson_id_expr(slug: str) -> str:
    return f"(SELECT id FROM lessons WHERE slug = {sql(slug)})"


def build_sql(contents: list[dict]) -> str:
    out = [
        "-- SINH TỰ ĐỘNG bởi folkify_backend/scripts/content/build_lessons_migration.py — không sửa tay.",
        "-- Nguồn: docs/content/*.json. Video chỉ EMBED (không tải về); đã kiểm tra oEmbed lúc sinh.",
        "-- LƯU Ý: nội dung quiz cần giảng viên review trước khi phát hành chính thức.",
        "",
    ]
    for content in contents:
        inst = content["instrumentSlug"]
        out.append(f"-- ==================== {inst} ====================")
        for order, lesson in enumerate(content["lessons"]):
            slug = lesson["slug"]
            vid = lesson["youtubeVideoId"]
            lid = uuid.uuid5(NAMESPACE, f"lesson:{slug}")
            plan = required_plan(order, lesson["level"])
            cols = {
                "title": lesson["title"],
                "duration": lesson.get("duration"),
                "level": lesson["level"],
                "description": lesson.get("description"),
                "xp": int(lesson.get("xp", 50)),
                "youtube_url": EMBED_URL + vid,
                "order_index": order,
                "required_plan": plan,
                "youtube_video_id": vid,
                "channel_name": lesson["channelName"].strip(),
                "source_url": lesson["sourceUrl"],
            }
            names = ", ".join(cols)
            values = ", ".join(sql(v) for v in cols.values())
            updates = ", ".join(f"{k} = EXCLUDED.{k}" for k in cols)
            out.append(
                f"INSERT INTO lessons (id, instrument_id, slug, {names})\n"
                f"SELECT {sql(str(lid))}, i.id, {sql(slug)}, {values} FROM instruments i WHERE i.slug = {sql(inst)}\n"
                f"ON CONFLICT (slug) DO UPDATE SET {updates}, updated_at = NOW();"
            )
            lref = lesson_id_expr(slug)
            for table, column, items in (
                ("lesson_steps", "step", lesson.get("steps", [])),
                ("lesson_tips", "tip", lesson.get("tips", [])),
            ):
                out.append(f"DELETE FROM {table} WHERE lesson_id = {lref};")
                for idx, item in enumerate(items):
                    out.append(
                        f"INSERT INTO {table} (lesson_id, {column}, order_index) VALUES ({lref}, {sql(item)}, {idx});"
                    )
            out.append(f"DELETE FROM quiz_questions WHERE lesson_id = {lref};")
            for qi, q in enumerate(lesson.get("quiz", [])):
                qid = uuid.uuid5(NAMESPACE, f"question:{slug}:{qi}")
                out.append(
                    "INSERT INTO quiz_questions (id, lesson_id, question, type, explanation, order_index) "
                    f"VALUES ({sql(str(qid))}, {lref}, {sql(q['question'])}, {sql(q['type'])}, {sql(q.get('explanation'))}, {qi});"
                )
                for oi, o in enumerate(q["options"]):
                    oid = uuid.uuid5(NAMESPACE, f"option:{slug}:{qi}:{oi}")
                    out.append(
                        "INSERT INTO quiz_options (id, question_id, text, is_correct, order_index) "
                        f"VALUES ({sql(str(oid))}, {sql(str(qid))}, {sql(o['text'])}, {sql(bool(o.get('correct')))}, {oi});"
                    )
            out.append("")
    return "\n".join(out) + "\n"


def main() -> int:
    for stream in (sys.stdout, sys.stderr):  # console Windows mặc định cp1252
        stream.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument(
        "--content", required=True, type=Path, help="thư mục chứa *.json nội dung"
    )
    parser.add_argument("--out", type=Path, help="file migration .sql đầu ra")
    parser.add_argument(
        "--verify-only", action="store_true", help="chỉ kiểm tra JSON + oEmbed"
    )
    parser.add_argument(
        "--skip-oembed", action="store_true", help="bỏ qua kiểm tra mạng (CI offline)"
    )
    args = parser.parse_args()

    files = sorted(args.content.glob("*.json"))
    if not files:
        print(f"Không có file JSON trong {args.content}", file=sys.stderr)
        return 2
    contents, errors = [], []
    for f in files:
        data = json.loads(f.read_text(encoding="utf-8"))
        errors += validate(data, f)
        contents.append(data)

    if not args.skip_oembed:
        for data in contents:
            for lesson in data["lessons"]:
                if not oembed_ok(lesson["youtubeVideoId"]):
                    errors.append(
                        f"{data['instrumentSlug']} {lesson['slug']}: video {lesson['youtubeVideoId']} không embed được"
                    )

    total = sum(len(d["lessons"]) for d in contents)
    quizzes = sum(len(l.get("quiz", [])) for d in contents for l in d["lessons"])
    print(f"{len(contents)} nhạc cụ, {total} bài học, {quizzes} câu hỏi")
    if errors:
        print("LỖI:", *errors, sep="\n  ", file=sys.stderr)
        return 1
    if args.verify_only:
        print("OK")
        return 0
    if not args.out:
        parser.error("--out bắt buộc khi không dùng --verify-only")
    args.out.write_text(build_sql(contents), encoding="utf-8", newline="\n")
    print(f"Đã ghi {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
