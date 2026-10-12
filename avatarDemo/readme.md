# avatarDemo — Spring Boot backend and browser frontend

This project provides the AI answers and the web page for the Coder Army realtime avatar demo. It uses Spring AI with OpenAI and a local, in-memory knowledge index. The browser connects to LiveKit; the companion [Python worker](../realtime-agent/readme.md) handles speech recognition, speech synthesis, and the Synthesia avatar.

## How the two projects work together

```text
Browser microphone → LiveKit → Python worker → Spring /api/avatar/ask
                                                ↓
                                    OpenAI + retrieved knowledge
                                                ↓
Browser avatar/audio ← LiveKit ← Synthesia ← Python speech synthesis
```

You can use the Spring question APIs without starting Python. A complete voice/avatar conversation requires both processes and the external services.

## Prerequisites

- JDK 21, as configured in `pom.xml`. Check `java -version` and `javac -version`; ensure `JAVA_HOME` points to that JDK.
- Internet access for Maven dependencies, OpenAI, LiveKit, Synthesia, and the frontend's LiveKit SDK CDN.
- An OpenAI API key with access to the configured chat and embedding models.
- A LiveKit Cloud project, its WebSocket URL, API key, and API secret. Use the **same project and credentials** in the Python worker.
- For voice/avatar mode: configure the Python worker, LiveKit Inference access for its STT/TTS models, a Synthesia API key with interactive-avatar access, an avatar ID, and a Cartesia voice ID. See the Python README.
- A browser with microphone access. Use `localhost` locally and HTTPS when accessing the frontend from another machine.

The repository declares Spring Boot **4.1.1**, Spring AI **2.0.1**, and LiveKit Java SDK **0.16.0**. The Maven wrapper declares Maven **3.9.16**, so a separate Maven installation is optional. These are the versions in the supplied files; dependency resolution has not been tested as part of this documentation change.

## 1. Open the project directory

On macOS/Linux:

```bash
cd "/Users/adityatandon/Desktop/synthesia code & Notes/avatarDemo"
```

On another machine, substitute your own project path. Run the following Maven commands from this directory, which contains `pom.xml` and `mvnw`.

## 2. Configure credentials

Spring reads `src/main/resources/application.properties`, which pulls credentials from the environment variables `OPENAI_API_KEY`, `LIVEKIT_URL`, `LIVEKIT_API_KEY` and `LIVEKIT_API_SECRET`. Spring will not start until all four are set. The `SPRING_AI_OPENAI_API_KEY` form in the table below also works and takes precedence.

| Environment variable | Spring property | Purpose |
| --- | --- | --- |
| `SPRING_AI_OPENAI_API_KEY` | `spring.ai.openai.api-key` | OpenAI API credential |
| `SPRING_AI_OPENAI_CHAT_MODEL` | `spring.ai.openai.chat.model` | Optional; currently `gpt-5-mini` |
| `SPRING_AI_OPENAI_EMBEDDING_MODEL` | `spring.ai.openai.embedding.model` | Optional; currently `text-embedding-3-small` |
| `LIVEKIT_URL` | `livekit.url` | LiveKit WebSocket URL, such as `wss://your-project.livekit.cloud` |
| `LIVEKIT_API_KEY` | `livekit.api-key` | LiveKit project API key |
| `LIVEKIT_API_SECRET` | `livekit.api-secret` | LiveKit project API secret |
| `SERVER_PORT` | `server.port` | Optional; default HTTP port is `8080` |

Set variables in the terminal where you will launch Spring. Replace every placeholder:

```bash
export OPENAI_API_KEY='your-openai-api-key'
export LIVEKIT_URL='wss://your-project.livekit.cloud'
export LIVEKIT_API_KEY='your-livekit-api-key'
export LIVEKIT_API_SECRET='your-livekit-api-secret'
```

Windows PowerShell equivalent:

```powershell
$env:OPENAI_API_KEY = 'your-openai-api-key'
$env:LIVEKIT_URL = 'wss://your-project.livekit.cloud'
$env:LIVEKIT_API_KEY = 'your-livekit-api-key'
$env:LIVEKIT_API_SECRET = 'your-livekit-api-secret'
```

Spring does **not** automatically load the sibling Python project's `.env.local`. Configure each process separately.

## 3. Run Spring Boot

macOS/Linux:

```bash
./mvnw spring-boot:run
```

If the wrapper is not executable, use `sh mvnw spring-boot:run`. Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, with a compatible Maven installation, run `mvn spring-boot:run`.

Startup loads every `src/main/resources/knowledge/*.md` file, splits it into chunks, calls OpenAI for embeddings, and stores the vectors in memory. Look for `Loaded <number> knowledge chunks.` and the completed Spring startup message before sending requests. A valid OpenAI key and network access are needed **during startup**, not just when asking questions. Each restart rebuilds the index and makes embedding API calls.

No database, external vector database, Docker container, or separate frontend build is configured.

## 4. Open the frontend and start a conversation

1. In another terminal, start `realtime-agent` using its README and wait for the worker to register with LiveKit.
2. Open [http://localhost:8080/](http://localhost:8080/) or [http://localhost:8080/index.html](http://localhost:8080/index.html).
3. Click **Start Conversation** and allow microphone access.
4. Wait for the avatar to appear, then ask a question about Java, Spring, DevOps, system design, or FDE.
5. Use **Mute Microphone** / **Unmute Microphone** as needed. If the browser blocks playback, click **Enable Audio** when it appears.
6. Click **End Session** to disconnect. Stop each server with `Ctrl+C` when finished.

The frontend lives at `src/main/resources/static/index.html` and Spring serves it directly. It loads the LiveKit browser SDK from jsDelivr and calls the relative URL `/api/livekit/token`. Open it through Spring rather than double-clicking the HTML file; no npm, Node.js, or independent frontend server is needed.

Every **Start Conversation** creates a new `avatar-<UUID>` room. The unnamed Python worker uses automatic dispatch. The frontend identifies the avatar by its remote video track and plays audio from that participant.

## API checks

Run these examples from macOS/Linux with Spring running. For Windows, use an HTTP client or `curl.exe` with JSON quoting appropriate to your shell.

### Ask a text question

```bash
curl --fail-with-body -X POST http://localhost:8080/api/avatar/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What is dependency injection in Spring?"}'
```

Response shape: `{"answer":"..."}`. This checks the Spring/OpenAI/RAG path independently of LiveKit and Python.

### Stream an answer

```bash
curl --fail-with-body -N -X POST http://localhost:8080/api/avatar/ask/stream \
  -H 'Content-Type: application/json' \
  -d '{"question":"Explain JVM briefly."}'
```

Returns `text/event-stream` chunks. The current Python worker uses the non-streaming `/ask` endpoint, and the browser has no text-question form.

### Generate a LiveKit join token

```bash
curl --fail-with-body -X POST http://localhost:8080/api/livekit/token \
  -H 'Content-Type: application/json' \
  -d '{"roomName":"avatar-manual-check"}'
```

Response contains `serverUrl`, `token`, and `roomName`. An empty object `{}` generates a room name automatically. The token response is a credential: do not publish it. This endpoint creates a join token; it does not itself connect a browser or start an avatar.

## Build and test

```bash
./mvnw test
./mvnw clean package
java -jar target/avatarDemo-0.0.1-SNAPSHOT.jar
```

Run the JAR instead of the development server, or stop the server first to free port 8080. On Windows, replace `./mvnw` with `.\mvnw.cmd`.

The existing test is a `@SpringBootTest` context-load test. It starts the service and its knowledge indexing, so it needs configured credentials and OpenAI connectivity; it is not an offline unit test. Packaging runs tests too. To build without executing tests, use `./mvnw clean package -DskipTests`; application startup still needs the credentials.

## Code map and customization

| Path | Responsibility |
| --- | --- |
| `pom.xml` | Java version, Spring Boot/Spring AI dependencies, LiveKit SDK, packaging |
| `src/main/java/coderarmy/in/avatarDemo/AvatarDemoApplication.java` | Application entry point |
| `src/main/java/coderarmy/in/avatarDemo/AvatarController.java` | JSON and streaming question endpoints |
| `src/main/java/coderarmy/in/avatarDemo/AvatarService.java` | Assistant prompt, document chunking, in-memory vector store, retrieval, OpenAI calls |
| `src/main/java/coderarmy/in/avatarDemo/LiveKitTokenController.java` | Browser room-join token generation |
| `src/main/java/coderarmy/in/avatarDemo/dto/` | `question` request and `answer` response objects |
| `src/main/resources/application.properties` | Model and LiveKit configuration |
| `src/main/resources/static/index.html` | Browser interface, microphone, remote avatar video/audio |
| `src/main/resources/knowledge/` | Six Markdown knowledge files: Java, Spring, DevOps, HLD, LLD, FDE |
| `src/test/java/coderarmy/in/avatarDemo/AvatarDemoApplicationTests.java` | Context-load test |

Add or edit Markdown files under `knowledge/`, then restart Spring (rebuild first if using the JAR). Retrieval takes the top four chunks; the splitter uses a target chunk size of 400 and minimum chunk size of 100 characters. The assistant's Roman-script Hinglish style and course-specific instructions are defined in `AvatarService.SYSTEM_PROMPT`. No chat-memory advisor is configured.

If changing Spring's port, set `SERVER_PORT` and update the Python worker's `SPRING_AVATAR_URL` to the same port. Open the frontend at that port as well.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Maven reports the wrong Java version | Use JDK 21 and check `JAVA_HOME` and `./mvnw -version`. |
| Wrapper or dependencies fail to download | Check internet/proxy/repository access and the exact versions in `.mvn/wrapper/maven-wrapper.properties` and `pom.xml`. Resolve missing artifacts before starting the app. |
| Startup fails while loading knowledge | Check the OpenAI key, embedding-model access, quota, and connectivity. The index is built at startup. |
| Port 8080 is already in use | Stop the other process or set `SERVER_PORT`; update the Python backend URL too. |
| Page fails after opening the HTML directly | Open `http://localhost:8080/` through Spring. |
| `LivekitClient is not defined` | Check that the jsDelivr SDK script loads; inspect the browser console/network tab. |
| LiveKit connection fails | Check the URL/key/secret and ensure Spring and Python use the same LiveKit project. |
| Stuck at “Waiting for AI assistant...” | Start the Python worker before creating a new conversation; check its logs, dispatch configuration, and Synthesia credentials/access. |
| Microphone unavailable or no sound | Allow microphone permissions, use localhost/HTTPS, unmute, and click Enable Audio if shown. |
| Avatar speaks the fallback error message | Check Python's `ERROR:` log and Spring's logs; test `/api/avatar/ask` directly. |

## Verification scope

These instructions were checked against the supplied Java source, frontend, configuration, Maven files, and companion Python source. The application was not built or connected to paid external services during this README-only change.
