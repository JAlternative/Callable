package utils.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static utils.db.DBUtils.getCredentials;
import static utils.db.DBUtils.getIntegrationDatabaseAddress;

public class IntDBConnectionProvider {
    private static IntDBConnectionProvider instance;
    private final Connection connection;

    private IntDBConnectionProvider() throws SQLException {
        String[] credentials = getCredentials();
        connection = DriverManager.getConnection(getIntegrationDatabaseAddress(), credentials[0], credentials[1]);
    }

    public static IntDBConnectionProvider getInstance() throws SQLException {
        if (instance == null) {
            instance = new IntDBConnectionProvider();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

}
