package com.project200.undabang.exercise.sync;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseEditClockMigrationTest {
    @Test
    void additive_upgrade_preserves_legacy_rows_photos_and_receipts_without_inventing_a_clock() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE exercises (exercise_id BIGINT PRIMARY KEY, exercise_title VARCHAR(255), "
                        + "exercise_detail TEXT, version BIGINT NOT NULL, exercise_created_at TIMESTAMP, exercise_deleted_at TIMESTAMP)");
                statement.execute("CREATE TABLE exercise_pictures (exercise_picture_id BIGINT PRIMARY KEY, exercise_id BIGINT, picture_id BIGINT)");
                statement.execute("CREATE TABLE exercise_receipts (exercise_receipt_id BIGINT PRIMARY KEY, request_id VARCHAR(36), "
                        + "fingerprint VARCHAR(64), response_json TEXT)");
                statement.execute("INSERT INTO exercises VALUES (1, 'active', 'original body', 7, '2025-01-01 12:00:00', NULL), "
                        + "(2, 'deleted', NULL, 9, '2025-01-02 12:00:00', '2025-01-03 12:00:00')");
                statement.execute("INSERT INTO exercise_pictures VALUES (10, 1, 100)");
                statement.execute("INSERT INTO exercise_receipts VALUES (20, '11111111-1111-1111-1111-111111111111', "
                        + "'legacy-fingerprint', '{\"exerciseId\":1,\"earnedPoints\":3,\"version\":0}')");
            }
            List<List<String>> originalRows = rows(connection, "SELECT * FROM exercises ORDER BY exercise_id");
            List<List<String>> originalPictures = rows(connection, "SELECT * FROM exercise_pictures");
            List<List<String>> originalReceipts = rows(connection, "SELECT * FROM exercise_receipts");

            ScriptUtils.executeSqlScript(connection, new FileSystemResource(
                    "documents/Undabang SQL/exercise_edit_clock_upgrade.sql"));

            assertThat(rows(connection, "SELECT exercise_id, exercise_title, exercise_detail, version, "
                    + "exercise_created_at, exercise_deleted_at FROM exercises ORDER BY exercise_id")).isEqualTo(originalRows);
            assertThat(rows(connection, "SELECT * FROM exercise_pictures")).isEqualTo(originalPictures);
            assertThat(rows(connection, "SELECT * FROM exercise_receipts")).isEqualTo(originalReceipts);
            try (var columns = connection.getMetaData().getColumns(null, null, "EXERCISES", "EDITED_AT")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BIGINT);
                assertThat(columns.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.columnNullable);
                assertThat(columns.getString("COLUMN_DEF")).isNull();
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM exercises WHERE edited_at IS NULL")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(2);
            }
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO exercises (exercise_id, exercise_title, version) VALUES (3, 'legacy new row', 0)");
                statement.executeUpdate("UPDATE exercises SET edited_at = 1793869597994 WHERE exercise_id = 1");
            }
            assertThat(rows(connection, "SELECT exercise_id, edited_at FROM exercises ORDER BY exercise_id"))
                    .containsExactly(java.util.Arrays.asList("1", "1793869597994"),
                            java.util.Arrays.asList("2", null), java.util.Arrays.asList("3", null));
        }
    }

    private List<List<String>> rows(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            List<List<String>> rows = new ArrayList<>();
            while (result.next()) {
                List<String> row = new ArrayList<>();
                for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
                    row.add(result.getString(column));
                }
                rows.add(row);
            }
            return rows;
        }
    }
}
