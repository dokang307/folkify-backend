"""Seed dữ liệu demo cho backend chạy local (LocalDevServer + folkify_ai) — phục vụ thử tay / chụp màn hình.

Yêu cầu: backend ở API_URL, folkify_ai ở AI_URL, Postgres ở PG_PORT (LocalDevServer mặc định 55432),
Python có `pg8000 numpy soundfile`.

    python folkify_backend/scripts/dev/seed_local.py

Tài khoản tạo ra (mật khẩu = DEV_SEED_PASSWORD, mặc định bên dưới — CHỈ dùng cho môi trường local):
    free@folkify.test (FREE) · basic@folkify.test (BASIC) · pro@folkify.test (PRO) · admin@folkify.test (ADMIN, PRO)
Bản mẫu tác phẩm là giai điệu tổng hợp (không phải bản thu thật) — chỉ để demo luồng chấm điểm.
"""

from __future__ import annotations

import io
import json
import os
import sys
import urllib.error
import urllib.request
import uuid

import numpy as np
import pg8000.native
import soundfile as sf

API_URL = os.environ.get(
    "API_URL", "http://localhost:8080"
)  # scalability-ok: local dev default
AI_URL = os.environ.get(
    "AI_URL", "http://localhost:8000"
)  # scalability-ok: local dev default
PG_PORT = int(os.environ.get("PG_PORT", "55432"))
PASSWORD = os.environ.get("DEV_SEED_PASSWORD", "FolkifyLocal#2026")
SR = 22050

USERS = [
    ("Bạn Học Miễn Phí", "free@folkify.test", "FREE", "USER"),
    ("Nguyễn Văn An", "basic@folkify.test", "BASIC", "USER"),
    ("Trần Thu Hà", "pro@folkify.test", "PRO", "USER"),
    ("Quản Trị Folkify", "admin@folkify.test", "PRO", "ADMIN"),
]

# (nhạc cụ, tên, xuất xứ, gói, giai điệu MIDI) — giai điệu ngũ cung tổng hợp làm "bản mẫu"
SONGS = [
    (
        "dan-bau",
        "Trống cơm",
        "Dân ca Bắc Bộ",
        "BASIC",
        [62, 65, 67, 69, 67, 65, 62, 60, 62, 65, 67, 72, 69, 67, 65, 62],
    ),
    (
        "dan-tranh",
        "Lý cây bông",
        "Dân ca Nam Bộ",
        "BASIC",
        [67, 69, 72, 74, 76, 74, 72, 69, 72, 74, 76, 79, 76, 74, 72, 69, 67],
    ),
    (
        "sao-truc",
        "Lý con sáo",
        "Dân ca Nam Bộ",
        "BASIC",
        [72, 74, 76, 79, 81, 79, 76, 74, 72, 76, 79, 81, 84, 81, 79, 76],
    ),
    (
        "dan-nhi",
        "Bèo dạt mây trôi",
        "Dân ca Quan họ",
        "PRO",
        [64, 67, 69, 67, 64, 62, 60, 62, 64, 67, 69, 72, 69, 67, 64],
    ),
]
NOTE_SECONDS = 0.6


def tone(notes: list[float], seconds: float = NOTE_SECONDS) -> bytes:
    parts = []
    for midi in notes:
        n = int(SR * seconds)
        t = np.arange(n) / SR
        phase = 2 * np.pi * 440.0 * 2 ** ((midi - 69) / 12) * t
        env = np.minimum(1.0, t / 0.02) * np.minimum(1.0, (seconds - t) / 0.03)
        parts.append(0.3 * (np.sin(phase) + 0.4 * np.sin(2 * phase)) * env)
    buf = io.BytesIO()
    sf.write(buf, np.concatenate(parts).astype(np.float32), SR, format="WAV")
    return buf.getvalue()


def http(
    method: str,
    url: str,
    body=None,
    token: str | None = None,
    form: tuple[bytes, str] | None = None,
):
    headers = {}
    data = None
    if form is not None:
        boundary = uuid.uuid4().hex
        payload, extra = form
        chunks = [
            f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="take.wav"\r\n'
            f"Content-Type: audio/wav\r\n\r\n".encode()
            + payload
            + b"\r\n"
        ]
        for k, v in json.loads(extra).items():
            chunks.append(
                f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
            )
        chunks.append(f"--{boundary}--\r\n".encode())
        data = b"".join(chunks)
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    elif body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=300) as res:
            return res.status, json.loads(res.read() or b"null")
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b"null")


def login(email: str) -> str:
    status, body = http(
        "POST", f"{API_URL}/api/auth/login", {"email": email, "password": PASSWORD}
    )
    assert status == 200, (email, body)
    return body["result"]["accessToken"]


def main() -> int:
    for stream in (sys.stdout, sys.stderr):
        stream.reconfigure(encoding="utf-8")
    db = pg8000.native.Connection(
        "postgres",
        password="postgres",
        host="localhost",
        port=PG_PORT,
        database="postgres",
    )

    # 1. Tài khoản
    for name, email, plan, role in USERS:
        status, body = http(
            "POST",
            f"{API_URL}/api/auth/register",
            {"name": name, "email": email, "password": PASSWORD},
        )
        if status not in (200, 201, 409):
            raise SystemExit(f"Không tạo được {email}: {body}")
        expires = "NULL" if plan == "FREE" else "NOW() + INTERVAL '30 days'"
        db.run(
            f"UPDATE users SET plan = :plan, role = :role, plan_expires_at = {expires} WHERE email = :email",
            plan=plan,
            role=role,
            email=email,
        )
    print(
        f"✓ {len(USERS)} tài khoản (mật khẩu: biến DEV_SEED_PASSWORD hoặc mặc định trong script)"
    )

    # 2. Tác phẩm + bản mẫu (AI trích đường cao độ thật từ giai điệu tổng hợp)
    for slug, title, artist, plan, melody in SONGS:
        inst_id = db.run("SELECT id FROM instruments WHERE slug = :s", s=slug)[0][0]
        found = db.run(
            "SELECT id FROM songs WHERE instrument_id = :i AND title = :t",
            i=inst_id,
            t=title,
        )
        song_id = found[0][0] if found else uuid.uuid4()
        if not found:
            db.run(
                "INSERT INTO songs (id, instrument_id, title, artist, duration, order_index) VALUES (:id, :i, :t, :a, :d, -1)",
                id=song_id,
                i=inst_id,
                t=title,
                a=artist,
                d=f"0:{int(len(melody) * NOTE_SECONDS):02d}",
            )
        status, contour = http(
            "POST",
            f"{AI_URL}/extract-reference",
            form=(tone(melody), json.dumps({"instrument": slug})),
        )
        assert status == 200, contour
        db.run(
            "UPDATE songs SET required_plan = :p, reference_contour = CAST(:c AS jsonb), "
            "reference_duration_seconds = :dur, attribution = :att WHERE id = :id",
            p=plan,
            c=json.dumps(contour),
            dur=contour["durationSeconds"],
            att="Giai điệu tổng hợp để demo (thay bằng bản thu thật trong admin)",
            id=song_id,
        )
    print(f"✓ {len(SONGS)} tác phẩm có bản mẫu")

    # 3. Lượt chấm điểm thật (đoạn trích, có vài nốt lệch để thấy nhận xét)
    for email, picks in [
        ("basic@folkify.test", [0, 1]),
        ("pro@folkify.test", [1, 2, 3]),
        ("admin@folkify.test", [0]),
    ]:
        token = login(email)
        for idx in picks:
            slug, title, *_rest, melody = SONGS[idx]
            excerpt = [
                m + (0.45 if i % 5 == 2 else 0) for i, m in enumerate(melody[4:14])
            ]
            song_id = db.run(
                "SELECT s.id FROM songs s JOIN instruments i ON i.id = s.instrument_id "
                "WHERE i.slug = :s AND s.title = :t",
                s=slug,
                t=title,
            )[0][0]
            status, body = http(
                "POST",
                f"{API_URL}/api/performances",
                token=token,
                form=(tone(excerpt), json.dumps({"songId": str(song_id)})),
            )
            print(
                f"  chấm {email} · {title}: HTTP {status}",
                body["result"]["overall"] if status == 200 else body,
            )

    # 4. Làm quiz (đáp án đúng đọc từ DB) để có tiến độ, XP, streak
    token = login("pro@folkify.test")
    lessons = db.run(
        "SELECT l.slug, i.slug, l.id FROM lessons l JOIN instruments i ON i.id = l.instrument_id "
        "WHERE i.slug = 'dan-tranh' AND EXISTS (SELECT 1 FROM quiz_questions q WHERE q.lesson_id = l.id) "
        "ORDER BY l.order_index LIMIT 3"
    )
    for lesson_slug, inst_slug, lesson_id in lessons:
        answers = {}
        for (qid,) in db.run(
            "SELECT id FROM quiz_questions WHERE lesson_id = :l", l=lesson_id
        ):
            answers[str(qid)] = [
                str(o[0])
                for o in db.run(
                    "SELECT id FROM quiz_options WHERE question_id = :q AND is_correct",
                    q=qid,
                )
            ]
        status, body = http(
            "POST",
            f"{API_URL}/api/instruments/{inst_slug}/lessons/{lesson_slug}/quiz/attempts",
            {"answers": answers},
            token=token,
        )
        print(
            f"  quiz {lesson_slug}: HTTP {status}",
            body["result"]["scorePercent"] if status == 200 else body,
        )

    # 5. Giao dịch thanh toán thành công (cho trang kết quả thanh toán)
    pro_id = db.run("SELECT id FROM users WHERE email = 'pro@folkify.test'")[0][0]
    db.run(
        "INSERT INTO payment_transaction (user_id, target_plan, gateway_reference_id, amount, transfer_content, "
        "transaction_date, status) VALUES (:u, 'PRO', '1759700000000', 99000, 'FOLKIFY PRO', NOW(), 'SUCCESS') "
        "ON CONFLICT (gateway_reference_id) DO NOTHING",
        u=pro_id,
    )
    print("✓ giao dịch thanh toán mẫu")
    return 0


if __name__ == "__main__":
    sys.exit(main())
