# Deploy backend (Railway) and link to Vercel frontend

The API is a long-running Spring Boot app. It belongs on **Railway** (or similar), not Vercel. The React site stays on Vercel and calls this API over HTTPS.

```text
Browser → Vercel (frontend)
       → Railway (Spring Boot /api/*)
```

## 1. Deploy the API on Railway

1. Sign in at [railway.app](https://railway.app) and create a new project.
2. **Deploy from GitHub** → select [olaoluwa123/TenderSprouts_backend](https://github.com/olaoluwa123/TenderSprouts_backend).
3. Railway will build with the repo `Dockerfile` (`railway.toml`).
4. Add a **Volume** mounted at `/data` (Settings → Volumes). H2 stores the database there so data survives restarts.
5. Generate a public domain (Settings → Networking → Generate Domain). Copy the HTTPS URL, e.g. `https://tendersprouts-api.up.railway.app`.

### Required Railway variables

| Variable | Example | Notes |
| --- | --- | --- |
| `FRONTEND_ORIGIN` | `https://your-app.vercel.app` | Exact Vercel origin (no trailing slash). Used for CORS. |
| `JWT_SECRET` | long random string (32+ chars) | Required in `prod` profile. |
| `SPRING_PROFILES_ACTIVE` | `prod` | Set by Dockerfile; keep it. |
| `SEED_ENABLED` | `true` then `false` | `true` only for the first boot if you want demo users; then set `false`. |
| `REQUIRE_HTTPS` | `true` | Recommended behind Railway HTTPS. |

### Optional mail variables

Same as [`.env.example`](.env.example): `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.

### Optional database overrides

Defaults use H2 at `/data/school_portal`. To point elsewhere later:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | Full JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | Credentials |
| `DB_DRIVER` | e.g. `com.mysql.cj.jdbc.Driver` |
| `HIBERNATE_DIALECT` | Matching dialect |

## 2. Link the Vercel frontend

In the **frontend** GitHub repo (or Vercel project env) set:

| Name | Value |
| --- | --- |
| `VITE_API_BASE_URL` | `https://<your-railway-host>/api` |

Include the `/api` suffix. The client builds URLs as `` `${VITE_API_BASE_URL}/auth/login` `` → `/api/auth/login`.

Then redeploy the frontend so Vite bakes the variable into the build (push to `main` or trigger the Actions workflow / Vercel redeploy).

Also set Railway `FRONTEND_ORIGIN` to your Vercel URL (and any custom domain you attach later).

## 3. Smoke test

1. Open `https://<railway-host>/actuator/health` → should be UP.
2. Open the Vercel site → **Portal login**.
3. In DevTools → Network, login should hit `https://<railway-host>/api/auth/login` with status 200 (or 401 for bad password), not a CORS error.

## CORS checklist

- Railway `FRONTEND_ORIGIN` matches the browser origin exactly (`https://….vercel.app`).
- No trailing slash on `FRONTEND_ORIGIN`.
- Frontend `VITE_API_BASE_URL` ends with `/api`.
- After changing `VITE_*` vars, you must rebuild/redeploy the frontend.
