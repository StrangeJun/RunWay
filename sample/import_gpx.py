#!/usr/bin/env python3
"""
GPX 파일을 RunWay 백엔드에 임포트하는 스크립트.
Usage: python3 import_gpx.py <gpx_file> [--email EMAIL] [--password PASSWORD]
"""

import argparse
import json
import math
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

try:
    import urllib.request as urlreq
    import urllib.error as urlerr
except ImportError:
    print("Python 3 표준 라이브러리를 사용합니다.")

BASE_URL = "http://40.233.98.156:8080"
NS = {
    "gpx": "http://www.topografix.com/GPX/1/1",
    "gpxtpx": "http://www.garmin.com/xmlschemas/TrackPointExtension/v1",
}
BATCH_SIZE = 20


# ── HTTP helpers ──────────────────────────────────────────────────────────────

def post(path: str, body: dict, token: str | None = None) -> dict:
    url = BASE_URL + path
    data = json.dumps(body).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urlreq.Request(url, data=data, headers=headers, method="POST")
    try:
        with urlreq.urlopen(req) as resp:
            return json.loads(resp.read())
    except urlerr.HTTPError as e:
        body = json.loads(e.read())
        print(f"  HTTP {e.code}: {body.get('message', e.reason)}")
        sys.exit(1)


# ── GPX parser ────────────────────────────────────────────────────────────────

def parse_gpx(path: str):
    tree = ET.parse(path)
    root = tree.getroot()

    points = []
    for trkpt in root.findall(".//gpx:trkpt", NS):
        lat = float(trkpt.attrib["lat"])
        lon = float(trkpt.attrib["lon"])
        ele_el = trkpt.find("gpx:ele", NS)
        time_el = trkpt.find("gpx:time", NS)
        hr_el = trkpt.find(".//gpxtpx:hr", NS)

        altitude = float(ele_el.text) if ele_el is not None else None
        recorded_at = time_el.text if time_el is not None else None
        hr = int(hr_el.text) if hr_el is not None else None

        points.append({
            "latitude": lat,
            "longitude": lon,
            "altitudeMeters": altitude,
            "speedMps": None,
            "recordedAt": recorded_at,
            "hr": hr,
        })

    return points


def haversine_m(lat1, lon1, lat2, lon2) -> float:
    R = 6_371_000.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlam = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlam / 2) ** 2
    return 2 * R * math.asin(math.sqrt(a))


def calc_stats(points: list) -> dict:
    total_m = sum(
        haversine_m(points[i]["latitude"], points[i]["longitude"],
                    points[i + 1]["latitude"], points[i + 1]["longitude"])
        for i in range(len(points) - 1)
    )

    t0 = datetime.fromisoformat(points[0]["recordedAt"].replace("Z", "+00:00"))
    t1 = datetime.fromisoformat(points[-1]["recordedAt"].replace("Z", "+00:00"))
    duration_s = int((t1 - t0).total_seconds())

    pace = int(duration_s / (total_m / 1000)) if total_m > 0 else 0

    hr_values = [p["hr"] for p in points if p["hr"] is not None]
    avg_hr = int(sum(hr_values) / len(hr_values)) if hr_values else None

    calories = int(total_m / 1000 * 72)

    return {
        "distanceMeters": round(total_m, 2),
        "durationSeconds": duration_s,
        "avgPaceSecondsPerKm": pace,
        "caloriesBurned": calories,
        "avgHeartRateBpm": avg_hr,
    }


# ── main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="GPX → RunWay importer")
    parser.add_argument("gpx_file", help="GPX 파일 경로")
    parser.add_argument("--email", default="test@example.com")
    parser.add_argument("--password", default="password123")
    args = parser.parse_args()

    # 1. GPX 파싱
    print(f"[1/5] GPX 파싱: {args.gpx_file}")
    points = parse_gpx(args.gpx_file)
    stats = calc_stats(points)
    print(f"      포인트 수: {len(points)}")
    print(f"      거리: {stats['distanceMeters'] / 1000:.2f} km")
    print(f"      시간: {stats['durationSeconds'] // 60}분 {stats['durationSeconds'] % 60}초")
    if stats["avgHeartRateBpm"]:
        print(f"      평균 심박수: {stats['avgHeartRateBpm']} bpm")

    # 2. 로그인
    print(f"\n[2/5] 로그인: {args.email}")
    resp = post("/api/auth/login", {"email": args.email, "password": args.password})
    token = resp["data"]["accessToken"]
    print(f"      토큰 획득 완료")

    # 3. 런 시작
    print(f"\n[3/5] 런 시작")
    resp = post("/api/runs/start", {"startedAt": points[0]["recordedAt"]}, token)
    run_id = resp["data"]["runId"]
    print(f"      runId: {run_id}")

    # 4. 포인트 배치 전송
    print(f"\n[4/5] 포인트 전송 ({len(points)}개, 배치 {BATCH_SIZE}개)")
    batches = [points[i:i + BATCH_SIZE] for i in range(0, len(points), BATCH_SIZE)]
    for idx, batch in enumerate(batches):
        payload = [
            {
                "sequence": points.index(p) + 1,
                "latitude": p["latitude"],
                "longitude": p["longitude"],
                "altitudeMeters": p["altitudeMeters"],
                "speedMps": p["speedMps"],
                "recordedAt": p["recordedAt"],
            }
            for p in batch
        ]
        post(f"/api/runs/{run_id}/points", {"points": payload}, token)
        progress = min((idx + 1) * BATCH_SIZE, len(points))
        print(f"      {progress}/{len(points)}", end="\r")
    print(f"      완료{' ' * 20}")

    # 5. 런 완료
    print(f"\n[5/5] 런 완료")
    finish_body = {
        "endedAt": points[-1]["recordedAt"],
        "distanceMeters": stats["distanceMeters"],
        "durationSeconds": stats["durationSeconds"],
        "avgPaceSecondsPerKm": stats["avgPaceSecondsPerKm"],
        "caloriesBurned": stats["caloriesBurned"],
        "avgHeartRateBpm": stats["avgHeartRateBpm"],
    }
    resp = post(f"/api/runs/{run_id}/finish", finish_body, token)
    print(f"      상태: {resp['data']['status']}")

    print(f"\n완료! runId: {run_id}")
    print(f"앱에서 My Runs → 해당 기록을 열면 지도가 표시됩니다.")


if __name__ == "__main__":
    main()
