---
name: fastapi-routers
description: FastAPI layout for this repo's math service. Use when adding or changing HTTP endpoints under services/math.
---

# FastAPI in this repo

Keep `services/math` a thin HTTP layer over the SymPy sandbox.

## Layout

- `app/main.py` mounts one `APIRouter` prefix `/v1` and `/health`.
- Routers live in `app/routers/`. Do not hang new routes on the FastAPI app object.
- Request/response models are Pydantic in `app/schemas.py`. Prefer `extra="allow"` only on ingress that must accept unknown pedagogy keys (`JobIn`).
- CORS is limited to the Next app origin (port 3000). Do not open `*`.

## Sync vs async

- Use `def` (not `async def`) for handlers that call the sandbox, grader, leak filter, or any blocking CPU work.
- `async def` is reserved for I/O that actually awaits.

## Safety

- Never import SymPy in the API process. All CAS work goes through the subprocess sandbox.
- Timeouts kill the child. Do not raise raw tracebacks to the client.
- Health payload stays `HealthOut`.

## Entrypoint

`[tool.fastapi]` / uvicorn: `app.main:app`.
