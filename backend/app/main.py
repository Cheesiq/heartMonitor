from __future__ import annotations

import json
import os
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import List

import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

DB_PATH = Path(os.getenv("HEARTMONITOR_DB", "heartmonitor.db"))
NTFY_BASE = os.getenv("NTFY_BASE_URL", "https://ntfy.sh")

app = FastAPI(title="HeartMonitor MVP API", version="0.1.0")


class AlertIn(BaseModel):
    user_id: str = Field(min_length=1, max_length=128)
    region: str = Field(default="global", max_length=64)
    risk_score: float = Field(ge=0.0, le=1.0)
    level: int = Field(ge=0, le=4)
    rationale: List[str] = Field(default_factory=list)


class UserConfig(BaseModel):
    user_id: str = Field(min_length=1, max_length=128)
    ntfy_topic: str = Field(min_length=1, max_length=128)


def db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    with db() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                user_id TEXT PRIMARY KEY,
                ntfy_topic TEXT NOT NULL
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                region TEXT NOT NULL,
                risk_score REAL NOT NULL,
                level INTEGER NOT NULL,
                rationale_json TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """
        )


@app.on_event("startup")
def startup() -> None:
    init_db()


@app.get("/health")
def health() -> dict:
    return {"ok": True, "time": datetime.now(timezone.utc).isoformat()}


@app.post("/v1/users/register")
def register_user(config: UserConfig) -> dict:
    with db() as conn:
        conn.execute(
            """
            INSERT INTO users(user_id, ntfy_topic)
            VALUES (?, ?)
            ON CONFLICT(user_id) DO UPDATE SET ntfy_topic=excluded.ntfy_topic
            """,
            (config.user_id, config.ntfy_topic),
        )
    return {"ok": True, "user_id": config.user_id}


@app.post("/v1/alerts")
async def create_alert(alert: AlertIn) -> dict:
    with db() as conn:
        row = conn.execute(
            "SELECT ntfy_topic FROM users WHERE user_id = ?",
            (alert.user_id,),
        ).fetchone()

        if row is None:
            raise HTTPException(
                status_code=404,
                detail="user_id is not registered; call /v1/users/register first",
            )

        conn.execute(
            """
            INSERT INTO alerts(user_id, region, risk_score, level, rationale_json, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (
                alert.user_id,
                alert.region,
                alert.risk_score,
                alert.level,
                json.dumps(alert.rationale),
                datetime.now(timezone.utc).isoformat(),
            ),
        )

    title = f"HeartMonitor L{alert.level} ({alert.risk_score:.2f})"
    body = "\n".join(alert.rationale[:5]) if alert.rationale else "No rationale supplied"
    await publish_ntfy(row["ntfy_topic"], title, body)

    return {"ok": True}


@app.get("/v1/alerts/{user_id}")
def list_alerts(user_id: str) -> dict:
    with db() as conn:
        rows = conn.execute(
            """
            SELECT user_id, region, risk_score, level, rationale_json, created_at
            FROM alerts
            WHERE user_id = ?
            ORDER BY id DESC
            LIMIT 50
            """,
            (user_id,),
        ).fetchall()

    alerts = [
        {
            "user_id": row["user_id"],
            "region": row["region"],
            "risk_score": row["risk_score"],
            "level": row["level"],
            "rationale": json.loads(row["rationale_json"]),
            "created_at": row["created_at"],
        }
        for row in rows
    ]
    return {"alerts": alerts}


async def publish_ntfy(topic: str, title: str, body: str) -> None:
    url = f"{NTFY_BASE.rstrip('/')}/{topic}"
    headers = {
        "Title": title,
        "Tags": "rotating_light,heart",
        "Priority": "urgent",
    }
    async with httpx.AsyncClient(timeout=8.0) as client:
        response = await client.post(url, content=body.encode("utf-8"), headers=headers)
        response.raise_for_status()
