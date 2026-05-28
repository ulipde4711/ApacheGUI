# ApacheGUI — Views: original intent & current status

What each part of the GUI was built to do (per the original developer), and how it
behaves on the modernized Spring Boot 3 build. Verified 2026-05-28 by driving the
app in the Docker dev image with a headless browser (login admin/admin).

Legend: ✅ works · ⚠️ works with caveat · ❌ broken

| View | Original intent | Status |
|------|-----------------|--------|
| Setup Wizard (Init) | First-launch flow: collect the Apache layout (server root / conf / logs / modules / bin) and create the admin account. | ✅ (fixed earlier) |
| Configuration | Browse the Apache config files under the conf dir (left tree) and edit a selected file in a syntax-highlighting editor (Editor View) with a parsed directive tree (Tree View); save writes back. | ✅ |
| Documents | A file browser/editor for arbitrary files on the server's filesystem (the "Documents" root = the OS root), e.g. to edit served content without leaving the GUI. | ✅ |
| Logs | View / search / tail / download Apache log files. | ✅ (auto-load added) |
| Control | Start/stop/restart the managed Apache and show running status (traffic light); configurable auto-refresh of process info. | ✅ |
| Global Settings | Form-based editing of common global directives without hand-editing config: Networking (Listen, KeepAlive, timeouts, server settings), MIME Types, Modules (enable/disable LoadModule). | ✅ |
| Virtual Hosts | Create and manage `<VirtualHost>` blocks; view as a directive tree or hierarchically; add a new vhost. | ✅ |
| Global Tree | One editable tree of the **entire active configuration** (all directives across all included files): ServerRoot, Listen, every LoadModule, etc. | ✅ |
| History | Record the HTTP requests Apache serves and make them searchable. | ✅ (re-ported) |
| GUISettings (Application Settings) | The app's own settings: configured Apache paths, admin credentials, theme, document encoding, auth toggle; plus Apache Info / GUI Info / re-run init ("New Server"). | ✅ |

## Notes per view

### Logs
Actions are **Search / Download / Tail**. Search runs a Java-regex over the file
(empty filter = whole file, capped server-side so large logs stay responsive — a
33 MB log returns ~359 KB). Tail follows newly-appended lines (`tail -f` semantics),
so its first response on a static file is empty by design. As of 2026-05-28 the view
auto-runs the search on open so content shows immediately instead of an empty box.

### History — original design & how it was re-ported
The feature captures an HTTP **request history**: `net.apachegui.history.History`
injects a dedicated `CustomLog` directive into the Apache config that pipes every
request (LogFormat `%h","%{User-agent}i","%r","%>s","%B`) into the SQLite
`LOGDATA` table; the History view searches/graphs that table.

It was broken on the modernized build for two reasons, both now fixed:

1. **View 400 (NPE) — fixed.** `GUIViewController.renderHistoryViewJsp` located the
   history DB via `Utilities.getTomcatInstallDirectory()`, which walks up to a folder
   literally named `tomcat` — absent under embedded Tomcat / Spring Boot / Docker, so it
   ran to the filesystem root and threw NPE. Now resolved via the `catalina.base` system
   property (the same anchor the DAOs use; Spring Boot sets it to `--server.tomcat.basedir`).

2. **Capture pipeline — re-ported.** The legacy `LogParser.jar` (`ca.apachegui.sendlog.*`)
   was a 2015 binary that bundled an old sqlite-jdbc with **no arm64 native lib** and only
   existed under `environments/*/ApacheGUI/tomcat/bin/` (not packaged). It is replaced by
   `net.apachegui.history.LogIngest` inside the app, launched from the executable WAR via
   Spring Boot's `PropertiesLauncher`, so it reuses the app's modern `sqlite-jdbc`
   (works on any architecture) and the same `LogDataDao`. `History.java` now writes a
   `CustomLog` that pipes to:
   `"|java -Dloader.main=net.apachegui.history.LogIngest -Dloader.path=WEB-INF/classes,WEB-INF/lib -cp <war> org.springframework.boot.loader.launch.PropertiesLauncher <catalina.base>"`.

Verified end-to-end in the Docker dev image: enabling History writes the CustomLog into
`apachegui.conf`, the managed Apache spawns the LogIngest pipe on start, and live requests
land in `LOGDATA` and show in the view.

> ⚠️ On a host where ApacheGUI manages a **production** Apache (e.g. mi2), enabling History
> injects this CustomLog into the live config and restarts Apache — do that deliberately.
