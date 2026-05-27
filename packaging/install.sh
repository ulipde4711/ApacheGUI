#!/bin/sh
# Install ApacheGUI as a systemd service on Debian/Ubuntu — the scriptable
# alternative to the .deb. Run as root from the project root *after* building
# the WAR:  mvn -P dev clean package
#
#   sudo packaging/install.sh
#
# Override the WAR location with WAR=/path/to/ApacheGUI.war.
set -e

[ "$(id -u)" = 0 ] || { echo "Please run as root (e.g. sudo packaging/install.sh)."; exit 1; }

SRC_DIR="$(cd "$(dirname "$0")/.." && pwd)"
WAR="${WAR:-$SRC_DIR/target/ApacheGUI.war}"
DEST=/opt/apachegui

[ -f "$WAR" ] || { echo "WAR not found at $WAR — run 'mvn -P dev clean package' first, or set WAR=..."; exit 1; }
command -v java >/dev/null 2>&1 || echo "WARNING: 'java' not found — install openjdk-17-jre-headless (or newer)."
command -v apache2ctl >/dev/null 2>&1 || echo "WARNING: 'apache2ctl' not found — install apache2."

install -d "$DEST" "$DEST/db"
install -m 644 "$WAR" "$DEST/ApacheGUI.war"
# Seed databases only if absent (preserve existing config on re-install).
for f in apachegui-gui-database.db apachegui-history-database.db; do
  [ -f "$DEST/db/$f" ] || install -m 644 "$SRC_DIR/packaging/seed/db/$f" "$DEST/db/$f"
done
install -m 644 "$SRC_DIR/packaging/systemd/apachegui.service" /lib/systemd/system/apachegui.service

systemctl daemon-reload
systemctl enable apachegui.service
systemctl restart apachegui.service

echo "ApacheGUI installed and started."
echo "  UI:     http://localhost:8080"
echo "  Status: systemctl status apachegui"
echo "  Logs:   journalctl -u apachegui -f"
