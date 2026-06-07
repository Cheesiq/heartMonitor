# HeartMonitor MVP Backend

Free, globally deployable API for the Wear OS app.

## Features

- Register users with an ntfy topic
- Receive emergency alerts from watch clients
- Persist alerts in SQLite
- Push notifications using https://ntfy.sh (free public relay)

## Run locally

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## API quickstart

1. Register a user topic:

```bash
curl -X POST http://localhost:8000/v1/users/register \
  -H 'content-type: application/json' \
  -d '{"user_id":"demo-user","ntfy_topic":"heartmonitor-demo-user"}'
```

2. Subscribe on any phone/computer:

- Open https://ntfy.sh/heartmonitor-demo-user in browser
- Or install the ntfy mobile app and subscribe to that topic

3. Send a test alert:

```bash
curl -X POST http://localhost:8000/v1/alerts \
  -H 'content-type: application/json' \
  -d '{"user_id":"demo-user","region":"global","risk_score":0.91,"level":3,"rationale":["SpO2 below 84%","No movement 6 min"]}'
```

## Deploy for free

- Railway: free trial credits, one-click Docker deploy
- Render: free web service tier (sleeps when idle)
- Fly.io: low-cost global instances (often near-free for MVP usage)

Any provider that can run Docker or Python ASGI works.

## Watch app configuration

The Wear OS app reads:

- backend URL from shared preferences key `backend_url`
- user id from key `user_id`
- region from key `region`

For production, wire these settings to a setup screen or companion app.

## Safety note

This MVP is not a medical device and should not be relied on as the sole path for emergency response.
