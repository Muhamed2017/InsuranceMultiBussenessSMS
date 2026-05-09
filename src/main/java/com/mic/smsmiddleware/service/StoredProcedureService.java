package com.mic.smsmiddleware.service;

import com.mic.smsmiddleware.exception.StoredProcedureException;
import com.mic.smsmiddleware.properties.SpParameterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoredProcedureService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, String>> execute(String procedureName, List<SpParameterConfig> parameters) {
        String callString = buildCallString(procedureName, parameters.size());
        log.debug("Executing stored procedure: {} | parameters: {}", procedureName, parameters.size());

        try {
            return jdbcTemplate.execute(
                    (Connection conn) -> conn.prepareCall(callString),
                    (CallableStatement cs) -> {
                        bindParameters(cs, parameters);
                        boolean hasResults = cs.execute();
                        List<Map<String, String>> records = collectAllResultSets(cs, hasResults);
                        log.debug("Stored procedure '{}' returned {} record(s)", procedureName, records.size());
                        return records;
                    }
            );
        } catch (Exception ex) {
            throw new StoredProcedureException(procedureName, ex);
        }
    }

    private String buildCallString(String procedureName, int paramCount) {
        if (paramCount == 0) {
            return "{call " + procedureName + "}";
        }
        String placeholders = IntStream.range(0, paramCount)
                .mapToObj(i -> "?")
                .collect(Collectors.joining(", "));
        return "{call " + procedureName + "(" + placeholders + ")}";
    }

    private void bindParameters(CallableStatement cs, List<SpParameterConfig> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            SpParameterConfig param = parameters.get(i);
            int position = i + 1;
            switch (param.getType().toUpperCase()) {
                case "INTEGER" -> cs.setInt(position, Integer.parseInt(param.getValue()));
                case "DECIMAL" -> cs.setBigDecimal(position, new BigDecimal(param.getValue()));
                case "DATE"    -> cs.setDate(position, Date.valueOf(param.getValue()));
                default        -> cs.setString(position, param.getValue());
            }
        }
    }

    private List<Map<String, String>> collectAllResultSets(CallableStatement cs, boolean hasResults) throws SQLException {
        List<Map<String, String>> records = new ArrayList<>();
        while (true) {
            if (hasResults) {
                try (ResultSet rs = cs.getResultSet()) {
                    if (rs != null) {
                        records.addAll(mapResultSet(rs));
                    }
                }
            } else {
                if (cs.getUpdateCount() == -1) {
                    break;
                }
            }
            hasResults = cs.getMoreResults();
        }
        return records;
    }

    private List<Map<String, String>> mapResultSet(ResultSet rs) throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        while (rs.next()) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int col = 1; col <= columnCount; col++) {
                String columnName = meta.getColumnLabel(col).toLowerCase();
                String value = rs.getString(col);
                row.put(columnName, value != null ? value.trim() : "");
            }
            rows.add(row);
        }
        return rows;
    }
}
