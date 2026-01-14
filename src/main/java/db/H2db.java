package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class H2db {
    private static final String URL = "jdbc:h2:./data/appdb;DATABASE_TO_UPPER=false";
    private static final String USER = "sa";
    private static final String PASS = "";


    private H2db() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    
}
