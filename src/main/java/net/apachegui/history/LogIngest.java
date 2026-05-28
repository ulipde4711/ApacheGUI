package net.apachegui.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.apachegui.db.LogData;
import net.apachegui.db.LogDataDao;
import net.apachegui.db.SettingsDao;
import net.apachegui.db.Timestamp;

/**
 * Replacement for the legacy {@code LogParser.jar} (ca.apachegui.sendlog.*),
 * which bundled a 2015 sqlite-jdbc with no arm64 native lib and resolved paths
 * via the obsolete standalone-Tomcat layout.
 *
 * <p>Apache pipes each request to this process's stdin via the History
 * {@code CustomLog} as {@code host","userAgent","request","status","bytes}. We
 * parse and batch those lines into the SQLite history DB through the same DAOs
 * the app uses. It is launched from the executable WAR (Spring Boot
 * PropertiesLauncher), so it reuses the app's modern sqlite-jdbc and works on
 * any architecture without a separate packaged jar.
 *
 * @param args arg[0] = the Tomcat base dir (exposed as {@code catalina.base});
 *             its {@code db/} folder holds the SQLite databases.
 */
public class LogIngest {

    private static final String HISTORY_BUFFER_SETTING = "historyBuffer";

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0] == null) {
            System.err.println("LogIngest: base directory not provided");
            return;
        }
        File base = new File(args[0]);
        if (!base.exists()) {
            System.err.println("LogIngest: base directory does not exist: " + args[0]);
            return;
        }
        // The DAOs locate the SQLite databases under catalina.base/db.
        System.setProperty("catalina.base", base.getAbsolutePath());

        List<LogData> batch = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                LogData entry = parse(line);
                if (entry == null) {
                    continue;
                }
                batch.add(entry);
                if (batch.size() >= bufferSize()) {
                    LogDataDao.getInstance().commitLogData(batch.toArray(new LogData[0]));
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            LogDataDao.getInstance().commitLogData(batch.toArray(new LogData[0]));
        }
    }

    private static LogData parse(String line) throws Exception {
        // Matches the History LogFormat: %h","%{User-agent}i","%r","%>s","%B
        String[] p = line.split("\",\"", -1);
        if (p.length < 5) {
            return null;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return new LogData(now, p[0], p[1], p[2], p[3], p[4]);
    }

    private static int bufferSize() {
        try {
            String hb = SettingsDao.getInstance().getSetting(HISTORY_BUFFER_SETTING);
            return hb == null ? 1 : Math.max(1, Integer.parseInt(hb));
        } catch (Exception e) {
            return 1;
        }
    }
}
