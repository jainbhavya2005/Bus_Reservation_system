package db;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Manages database connectivity with support for external configuration via
 * db.properties or environment variables (DB_URL, DB_USER, DB_PASSWORD).
 */
public class DBConnection {

    private static String url = "jdbc:mysql://localhost:3306/tickets";
    private static String user = "root";
    private static String pass = "510315";
    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static boolean loaded = false;

    private static synchronized void loadConfig() {
        if (loaded) return;
        loaded = true;

        File f = new File("db.properties");
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                Properties p = new Properties();
                p.load(in);
                url = p.getProperty("db.url", url);
                user = p.getProperty("db.user", user);
                pass = p.getProperty("db.password", pass);
                driver = p.getProperty("db.driver", driver);
            } catch (Exception e) {
                System.err.println("Failed to read db.properties: " + e.getMessage());
            }
        }

        // Environment variable overrides
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        if (envUrl != null && !envUrl.isBlank()) url = envUrl;
        if (envUser != null && !envUser.isBlank()) user = envUser;
        if (envPass != null && !envPass.isBlank()) pass = envPass;
    }

    public static Connection getConnection() {
        loadConfig();
        try {
            Class.forName(driver);
            return DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            System.err.println("Database connection failed [" + url + ", user=" + user + "]: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
