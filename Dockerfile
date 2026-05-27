# ApacheGUI dev/test image.
#
# Bundles a vanilla Apache httpd 2.4 with the modernized Spring Boot 3 app
# (executable WAR on a JRE 17). ApacheGUI manages the httpd running in this
# same container, so the app is fully usable in a browser without touching
# the host. See docker/README.md.

# --- build stage: produce the executable WAR ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml build.xml ./
COPY src ./src
RUN mvn -B -DskipTests -P dev clean package

# --- runtime stage: httpd 2.4 + Temurin JRE 17 + the app ---
# Pull a self-contained Temurin 17 JRE rather than apt-installing Java, so the
# image doesn't depend on the base image's current Debian release shipping JDK 17.
FROM eclipse-temurin:17-jre AS jre

FROM httpd:2.4
# procps provides `ps`, which ApacheGUI uses to detect Apache processes.
RUN apt-get update \
 && apt-get install -y --no-install-recommends procps \
 && rm -rf /var/lib/apt/lists/*
COPY --from=jre /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

ENV APACHEGUI_HOME=/opt/apachegui
WORKDIR ${APACHEGUI_HOME}

COPY --from=build /src/target/ApacheGUI.war ./ApacheGUI.war
# Pre-seeded SQLite databases, configured for this container's httpd paths
# (/usr/local/apache2) with a BCrypt admin login (admin / admin).
COPY docker/seed/db ./db
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# 8080 = ApacheGUI web UI · 80 = the managed Apache httpd
EXPOSE 8080 80

ENTRYPOINT ["/entrypoint.sh"]
