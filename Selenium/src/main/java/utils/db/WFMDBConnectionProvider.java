package utils.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static utils.db.DBUtils.*;

public class WFMDBConnectionProvider {
    private static WFMDBConnectionProvider instance;
    private final Connection connection;

    private WFMDBConnectionProvider() throws SQLException {
        String[] credentials = getCredentials();
        connection = DriverManager.getConnection(getDataBaseAddress(), credentials[0], credentials[1]);
    }

    public static WFMDBConnectionProvider getInstance() throws SQLException {
        if (instance == null) {
            instance = new WFMDBConnectionProvider();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

}
