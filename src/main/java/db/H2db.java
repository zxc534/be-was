package db;

import org.h2.tools.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class H2db {
    private static final String URL = "jdbc:h2:./data/appdb;DATABASE_TO_UPPER=false";
    private static final String USER = "sa";
    private static final String PASS = "";


    private H2db() {
        init();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void init() {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                  user_id VARCHAR(50) PRIMARY KEY,
                  password VARCHAR(100) NOT NULL,
                  name VARCHAR(50),
                  email VARCHAR(100)
                );
                """;

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {
            st.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // h2 console (Web UI)
        try {
            Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
