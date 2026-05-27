# ApacheGUI in Docker (dev / test)

A single container that bundles a vanilla **Apache httpd 2.4** with the
modernized **Spring Boot 3** ApacheGUI (executable WAR on a JRE 17).
ApacheGUI manages the httpd running *inside the same container*, so you can
exercise the whole app in a browser without installing anything on the host.

## Run

```bash
docker compose up --build
```

Then open:

| URL                      | What                                            |
|--------------------------|-------------------------------------------------|
| http://localhost:8080    | ApacheGUI web UI — login **admin / admin**      |
| http://localhost:8888    | the Apache httpd ApacheGUI manages (once started)|

From the UI you can start/stop and configure the in-container Apache. Its
paths are pre-configured for the official httpd image layout
(`/usr/local/apache2`).

## Notes

- The image is multi-stage: it builds the WAR with Maven (JDK 17) and copies
  it onto an `httpd:2.4` + JRE 17 runtime image. First build downloads Maven
  dependencies and may take a few minutes.
- Login uses **BCrypt**; the pre-seeded `admin/admin` is for local testing
  only. The production `.deb` ships *without* a default password (first-time
  init sets it) — do not reuse `admin/admin` outside of dev.
- The SQLite databases live at `/opt/apachegui/db` inside the container.
  To persist changes across `docker compose down`, mount a volume there.
- Stop with `docker compose down`.
