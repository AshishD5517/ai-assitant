# realtime-agent — Python voice and avatar worker

This worker connects the browser's LiveKit room to the [Spring Boot backend](../avatarDemo/readme.md) and a Synthesia interactive avatar. Spring generates the answers; Python handles speech and forwards each final transcript to Spring.

## Runtime flow

1. An unnamed LiveKit worker receives a room job automatically and joins the room.
2. `inference.STT` transcribes microphone audio with `deepgram/nova-3`, language `multi`.
3. Each non-empty final transcript is POSTed to Spring as `{"question":"..."}`.
4. Spring returns `{"answer":"..."}`; Python calls `session.say(...)` with interruptions allowed.
5. `inference.TTS` uses `cartesia/sonic-3.6`, your Cartesia voice ID, and language `hi`.
6. Synthesia publishes the speaking avatar's video/audio to the room for the browser.

No LLM is configured in the Python `AgentSession`. The answers and retrieved knowledge come from Spring. There is no automatic opening greeting; speak after the session connects.

## Prerequisites

- Python **3.12** is the version used by the supplied `.venv`. The installed LiveKit Agents package declares support for Python 3.10 through 3.14; Python 3.12 is the closest match to this environment.
- A running Spring service with a working `POST /api/avatar/ask` endpoint.
- A LiveKit Cloud project with its URL, API key, API secret, and access to LiveKit Inference for the configured Deepgram and Cartesia models.
- Synthesia API credentials with interactive-avatar access and a valid avatar ID.
- A Cartesia voice ID usable with the configured model and language.
- Network access to Spring, LiveKit, the inference service, and Synthesia.

The code uses **LiveKit Inference**, rather than directly constructing Deepgram or Cartesia plugins. The current code does not read `DEEPGRAM_API_KEY`, `CARTESIA_API_KEY`, or an OpenAI key in Python. OpenAI is configured in Spring.

## 1. Set up a virtual environment and dependencies

macOS/Linux:

```bash
cd "/Users/adityatandon/Desktop/synthesia code & Notes/realtime-agent"
python3.12 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install 'livekit-agents==1.8.3' 'livekit-plugins-synthesia==1.8.3' 'httpx==0.28.1' 'python-dotenv==1.2.3'
python -m pip check
```

Substitute your own project path on another machine. If `.venv` is already usable, skip its creation and activate it. Recreate it if copied from a different machine, OS, or Python installation.

Windows PowerShell:

```powershell
cd 'C:\path\to\realtime-agent'
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install livekit-agents==1.8.3 livekit-plugins-synthesia==1.8.3 httpx==0.28.1 python-dotenv==1.2.3
python -m pip check
```

If activation is unavailable, run commands with `.venv/bin/python` on macOS/Linux or `.\.venv\Scripts\python.exe` on Windows.

The same pinned versions are listed in `requirements.txt`, so `python -m pip install -r requirements.txt` is equivalent. A clean install of these pins was checked on Python 3.13.

## 2. Configure `.env.local`

Copy `.env.example` to `.env.local` in the same directory as `agent.py`, replacing the placeholders with your own values:

```dotenv
LIVEKIT_URL=wss://your-project.livekit.cloud
LIVEKIT_API_KEY=your-livekit-api-key
LIVEKIT_API_SECRET=your-livekit-api-secret

SYNTHESIA_API_KEY=your-synthesia-api-key
SYNTHESIA_AVATAR_ID=your-synthesia-avatar-id
CARTESIA_VOICE_ID=your-cartesia-voice-id

SPRING_AVATAR_URL=http://localhost:8080/api/avatar/ask
```

| Variable | Required? | Purpose |
| --- | --- | --- |
| `LIVEKIT_URL` | Yes | WebSocket URL for the same project configured in Spring |
| `LIVEKIT_API_KEY` | Yes | LiveKit worker and avatar room credentials |
| `LIVEKIT_API_SECRET` | Yes | LiveKit worker and avatar room credentials |
| `SYNTHESIA_API_KEY` | Yes | Read by the Synthesia plugin when starting the avatar |
| `SYNTHESIA_AVATAR_ID` | Yes | Read at module import; supplied to `AvatarConfig` |
| `CARTESIA_VOICE_ID` | Yes | Read at module import and used by TTS |
| `SPRING_AVATAR_URL` | No | Defaults to `http://localhost:8080/api/avatar/ask` |

`NEXT_PUBLIC_LIVEKIT_URL` appears in the supplied environment file but is not used by this Python code or the Spring-served frontend. The frontend gets its URL from Spring's token endpoint.

`load_dotenv(".env.local")` resolves the file relative to the **current working directory**. Launch the worker from `realtime-agent`. Existing process environment variables take precedence over `.env.local`; if an edited value seems ignored, check exported variables or restart in a clean terminal. Even `--help` requires the avatar and voice IDs because they are read before CLI parsing.

Keep `.env.local` and credentials private. The directory has no root `.gitignore`; exclude `.env.local`, `.venv/`, and `__pycache__/` before adding this project to version control.

## 3. Check Spring before starting a room

Start Spring in a separate terminal following its README, then test:

```bash
curl --fail-with-body -X POST http://localhost:8080/api/avatar/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"Explain dependency injection briefly."}'
```

Expect JSON with an `answer` field. If Spring uses another host or port, test that address and set `SPRING_AVATAR_URL` accordingly. The URL must be reachable **from the worker**; `localhost` means the worker's own machine/container. Keep the `/api/avatar/ask` path, since the worker expects JSON rather than the streaming endpoint's SSE response.

## 4. Run the worker

With `.venv` activated and `.env.local` configured:

```bash
python agent.py --help
python agent.py dev
```

`dev` runs the worker in development mode with source watching. Wait for successful worker registration with LiveKit. Keep this terminal and the Spring terminal running.

For the SDK's production worker mode without development watching:

```bash
python agent.py start
```

Run either `dev` or `start` for this demo. `python agent.py` alone does not select a worker mode. Console mode is not the browser/avatar flow described here.

The source uses `@server.rtc_session()` without an agent name, which supports automatic dispatch when the browser joins a new room. Leave `LIVEKIT_AGENT_NAME` unset for this setup. If you configure a named worker, you also need explicit agent dispatch; the existing frontend/token controller does not implement that.

## 5. Use the browser frontend

1. Open [http://localhost:8080/](http://localhost:8080/) served by Spring.
2. Click **Start Conversation** and allow microphone access.
3. Wait for the Synthesia video to appear, then ask a question.
4. Check worker logs for `USER:` (transcript) and `SPRING:` (answer). The avatar ID is also logged when the room job starts.
5. Click **Enable Audio** if browser playback is blocked. Use the microphone toggle or **End Session** as needed.
6. Stop the worker and Spring with `Ctrl+C`; use `deactivate` to leave the virtual environment.

Start the worker **before** creating the browser session. If it was started late, end the session and start a fresh conversation to create a new room.

## Files and adjustments

| File/directory | Purpose |
| --- | --- |
| `agent.py` | Environment loading, worker entry point, STT/TTS configuration, Spring HTTP call, Synthesia session |
| `.env.local` | Local service credentials and avatar/voice/backend settings |
| `requirements.txt` | Pinned dependencies; `pip install -r requirements.txt` |
| `.venv/` | Local installed Python environment; not portable source code |
| `__pycache__/`, `.idea/` | Generated Python cache and IDE metadata |

Change `SPRING_AVATAR_URL` to point at another Spring deployment. Change the avatar/voice IDs in `.env.local` and restart the worker to apply them. The STT model/language and TTS model/language are specified directly in `AgentSession(...)` in `agent.py`.

Each final transcript creates a separate asynchronous request with a 60-second HTTP timeout. Failures print `ERROR:` and speak a fallback message. There is no retry logic or per-conversation request queue; rapid final transcripts can produce overlapping backend requests. This is the behavior of the supplied demo.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `ModuleNotFoundError` or missing `AgentServer` / Synthesia APIs | Activate the correct `.venv` and install the documented versions. Check `python -m pip show livekit-agents livekit-plugins-synthesia`. |
| `KeyError: SYNTHESIA_AVATAR_ID` or `CARTESIA_VOICE_ID` | Add the variable to `.env.local` and launch from the project root. These values are required at import time. |
| Missing LiveKit credentials or worker registration failure | Check `LIVEKIT_URL`, key, secret, network access, and project permissions. |
| Browser joins but no worker receives a job | Ensure matching LiveKit projects, successful registration, and unnamed automatic dispatch; then create a fresh browser session. |
| Synthesia startup error or no avatar video | Check interactive-avatar access, API key, avatar ID, session limits, and the LiveKit credentials used by the plugin. Read the worker error details. |
| STT/TTS model, authorization, or quota error | Check LiveKit Inference access/quota and availability of the configured model and voice. Provider plugins/keys are not wired directly in this source. |
| `ERROR:` followed by the spoken fallback | Check Spring connectivity, status code, OpenAI errors, and that the response contains `answer`. Test the Spring API directly. |
| No `USER:` log when speaking | Check browser microphone permissions/mute state, secure context, room connection, and inference errors. |
| `SPRING:` is logged but nothing is heard | Check TTS/Synthesia logs and the browser's Enable Audio button and playback volume. |
| Configuration edits do not take effect | Restart the worker and check for exported environment variables overriding `.env.local`. |

## Verification scope

These instructions were checked against `agent.py`, environment-variable names, the companion Spring/frontend source, and the installed SDK/package metadata. No live rooms, inference calls, or Synthesia sessions were started during this README-only change.
