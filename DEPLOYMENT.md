# ApacheGUI — build & deployment

ApacheGUI is a Spring Boot 3 application (Java 17) packaged as an **executable
WAR** with an embedded Tomcat 10. It manages a **local** Apache HTTP Server, so
it must run on the same host as the Apache it controls.

## Build

```bash
mvn -P dev clean package      # fast: target/ApacheGUI.war (executable)
mvn -P prod clean package     # release: minifies the bundled JS first
```

Run it directly:

```bash
java -jar target/ApacheGUI.war --server.tomcat.basedir=<dir>
# <dir>/db must hold the SQLite databases; UI on http://localhost:8080
```

JDK 17+ is required. (Builds also run on JDK 21/25 — the bytecode targets 17.)

---

## Docker (development / testing)

A self-contained image bundles Apache httpd 2.4 + ApacheGUI, so you can exercise
the whole app — including managing a real Apache — without touching the host:

```bash
docker compose up --build
# http://localhost:8080  (login: admin / admin)
# http://localhost:8888  the managed Apache, once you start it from the UI
```

See [docker/README.md](docker/README.md). The `admin/admin` login is for local
testing only and exists only in the Docker image.

---

## Debian / Ubuntu (production)

ApacheGUI installs as a **systemd service** that manages the system `apache2`.

### Option A — .deb package

```bash
mvn -P dev,deb clean package          # builds target/apachegui_<version>_all.deb
sudo apt install ./target/apachegui_1.12.0_all.deb   # resolves JRE + apache2
```

The package:
- installs the WAR to `/opt/apachegui/`, the unit to `/lib/systemd/system/apachegui.service`;
- seeds the SQLite DB to `/opt/apachegui/db/` on first install only (upgrades keep your data);
- enables and starts `apachegui.service`.

### Option B — install script

For environments without the `.deb`:

```bash
mvn -P dev clean package
sudo packaging/install.sh
```

### Manage the service

```bash
systemctl status apachegui
journalctl -u apachegui -f
```

Open `http://<host>:8080`. On first launch the **first-time setup wizard** runs:
set the admin credentials (stored BCrypt-hashed) and confirm the Apache paths
(pre-seeded for Debian's `apache2`: ServerRoot `/etc/apache2`, bin
`/usr/sbin/apache2ctl`).

---

## Notes & current limitations

- **Authentication** is Spring Security form login with **BCrypt**. There is no
  default password in production — the setup wizard creates the first account.
- **Privileges:** the service runs as `root` because it controls the system
  Apache (edits config, runs `apache2ctl`). Harden with a dedicated user +
  sudoers rules if desired (see the unit file comment).
- **CSRF** protection is currently disabled (the legacy dojo UI sends no CSRF
  tokens) — to be re-enabled.
- ApacheGUI shells out to `ps`/`apache2ctl`; the host needs `procps` and
  `apache2` installed (the `.deb` depends on `apache2`).
