package org.sagebionetworks.table.cluster;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class TableIndexDAOImpl implements TableIndexDAO{

	private static final String FIELD = "Field";
	private static final String SQL_SHOW_COLUMNS = "SHOW COLUMNS FROM ";

	@Override
	public boolean createOrUpdateTable(SimpleJdbcTemplate connection, List<ColumnModel> newSchema, String tableId) {
		if(connection == null) throw new IllegalArgumentException("Connection cannot be null");
		// First determine if we have any columns for this table yet
		List<String> columns = getCurrentTableColumns(connection, tableId);
		// Convert the names to columnIDs
		List<String> oldSchema = SQLUtils.convertColumnNamesToColumnId(columns);
		// Build the SQL to create or update the table
		String dml = SQLUtils.creatOrAlterTableSQL(oldSchema, newSchema, tableId);
		// If there is nothing to apply then do nothing
		if(dml == null) return false;
		// Execute the DML
		connection.update(dml);
		return true;
	}

	@Override
	public boolean deleteTable(SimpleJdbcTemplate connection, String tableId) {
		if(connection == null) throw new IllegalArgumentException("Connection cannot be null");
		String dropTableDML = SQLUtils.dropTableSQL(tableId);
		try {
			connection.update(dropTableDML);
			return true;
		} catch (BadSqlGrammarException e) {
			// This is thrown when the table does not exist
			return false;
		}
	}

	@Override
	public List<String> getCurrentTableColumns(SimpleJdbcTemplate connection, String tableId) {
		String tableName = SQLUtils.getTableNameForId(tableId);
		// Bind variables do not seem to work here
		try {
			return connection.query(SQL_SHOW_COLUMNS+tableName, new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getString(FIELD);
				}
			});
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not
			return null;
		}
	}
	

}