#!/bin/sh
# Launch the ApacheGUI web app in the foreground (PID 1). The managed Apache
# httpd is NOT started here — ApacheGUI starts/stops it on demand via
# apachectl, just as it would on a real host.
set -e

exec java -jar "${APACHEGUI_HOME}/ApacheGUI.war" \
  --server.port=8080 \
  --server.tomcat.basedir="${APACHEGUI_HOME}"
