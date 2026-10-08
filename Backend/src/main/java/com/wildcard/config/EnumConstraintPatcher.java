package com.wildcard.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ddl-auto=update adds new tables and columns but does NOT refresh the
 * CHECK constraints Hibernate creates for enum columns. When a new enum
 * value is added, the old constraint blocks it.
 *
 * Java enums are the source of truth, so these artifact constraints
 * are dropped at startup.
 */
@Slf4j
@Component
public class EnumConstraintPatcher {

    /** Columns Hibernate stores as postgres enum values. */
    private static final List<String> ENUM_TABLES = List.of(
            "notifications", "xp_log", "xp_config", "watchlist_item",
            "posts", "topics", "reactions", "reports", "users", "conversations");

    private final DataSource dataSource;

    public EnumConstraintPatcher(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * ddl-auto=update exports the schema when the EMF is created and recreates
     * the OLD constraint when an enum value is added, so we check again a bit
     * after startup and drop it.
     */
    private static final int MAX_PASSES = 40;

    private final java.util.concurrent.atomic.AtomicInteger passes =
            new java.util.concurrent.atomic.AtomicInteger();

    @Scheduled(initialDelay = 3_000, fixedDelay = 2_000)
    public void dropStaleConstraints() {
        // ddl-auto=update exports the schema several times; for the first few
        // minutes we keep checking for constraints that block new enum values,
        // then stop after a fixed number of attempts.
        if (passes.getAndIncrement() > MAX_PASSES) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            List<String> stale = findCheckConstraints(connection);
            for (String name : stale) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE " + tableOf(name)
                            + " DROP CONSTRAINT IF EXISTS " + quote(constraintOf(name)));
                    log.info("Dropped stale enum check constraint: {}", name);
                }
            }
            if (stale.isEmpty() && passes.get() > 2) {
                passes.set(MAX_PASSES + 1);
                log.info("Enum check constraints are clean");
            }
        } catch (SQLException e) {
            log.warn("Enum constraint patch skipped: {}", e.getMessage());
        }
    }

    private List<String> findCheckConstraints(Connection connection) throws SQLException {
        String sql = """
                select c.conrelid::regclass::text as tbl, c.conname
                from pg_constraint c
                where c.contype = 'c'
                  and c.connamespace = 'public'::regnamespace
                  and c.conrelid::regclass::text = any (?)
                """;
        List<String> out = new ArrayList<>();
        try (var ps = connection.prepareStatement(sql)) {
            ps.setArray(1, connection.createArrayOf("text", ENUM_TABLES.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString("tbl") + "::" + rs.getString("conname"));
                }
            }
        }
        return out;
    }

    private String tableOf(String packed) {
        return packed.substring(0, packed.indexOf("::"));
    }

    private String constraintOf(String packed) {
        return packed.substring(packed.indexOf("::") + 1);
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}