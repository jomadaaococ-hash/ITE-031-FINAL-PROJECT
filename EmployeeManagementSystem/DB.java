package sugbomacciato;

import java.sql.*;

/** Database connection (XAMPP MySQL defaults: user root, empty password). */
public class DB {
    private static final String URL = "jdbc:mysql://localhost:3306/sugbomacciato_db";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
