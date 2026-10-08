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
 * ddl-auto=update yeni sütun və cədvəl əlavə edir, amma Hibernate-in
 * enum sütunları üçün yaratdığı CHECK constraint-ləri YENİLƏMİR.
 * Yeni enum dəyəri əlavə olunanda köhnə constraint-i yeni dəyəri bloklayır.
 *
 * Java enum-lar həqiqi mənbə olduğu üçün bu artefakt constraint-lər
 * start-da düşürülür.
 */
@Slf4j
@Component
public class EnumConstraintPatcher {

    /** enum -> postgres dəyər kimi saxlanılan sütunlar. */
    private static final List<String> ENUM_TABLES = List.of(
            "notifications", "xp_log", "xp_config", "watchlist_item",
            "posts", "topics", "reactions", "reports", "users", "conversations");

    private final DataSource dataSource;

    public EnumConstraintPatcher(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * ddl-auto=update şemanı EMF yarandıqda ixrac edir; enum dəyəri əlavə
     * olunanda KÖHNƏ constraint-i yenidən yaradır. ona görə start-dan
     * bir az sonra yenidən yoxlayıb düşürürük.
     */
    private static final int MAX_PASSES = 40;

    private final java.util.concurrent.atomic.AtomicInteger passes =
            new java.util.concurrent.atomic.AtomicInteger();

    @Scheduled(initialDelay = 3_000, fixedDelay = 2_000)
    public void dropStaleConstraints() {
        // ddl-auto=update şemanı bir neçə dəfə ixrac edir; ilk bir neçə dəqiqə
        // təkrar-təkrar yoxlayıb yeni enum dəyərlərini bloklayan constraint-ləri
        // silirik. Sabit saydan sonra dayanır.
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

    /** "table::constraint" formatını cədvəl adını götürür. */
    private String tableOf(String packed) {
        return packed.substring(0, packed.indexOf("::"));
    }

    /** "table::constraint" formatını constraint adını götürür. */
    private String constraintOf(String packed) {
        return packed.substring(packed.indexOf("::") + 1);
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}