package com.iisi.cmt.stationObsType.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DaoImpl implements IDao {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	private static final Logger programLogger = LogManager.getLogger("program");
	
	@Override
	public List<Map<String, Object>> selectAll() {
		return jdbcTemplate.queryForList("SELECT * FROM " + DB_TABLE);
	}
	
	@Override
	public List<Map<String, Object>> batchInsert(String[] primaryKeys, String[] columnNames
										, List<Map<String, Object>> listOfValuesMap) {
		
		// build sql like this:
		// "insert into PERSON (FIRST_NAME, LAST_NAME, ADDRESS) values (?, ?, ?)"
		StringBuilder columnsBuilder = new StringBuilder();
		StringBuilder questionMarkBuilder = new StringBuilder();
		columnsBuilder.append("(");
		questionMarkBuilder.append("(");
		
		int firstPrimaryKeyInSqlLocation = -1;
		int secondPrimaryKeyInSqlLocation = -1;
		
		for(int i=0; i<columnNames.length; i++) {
			if(columnNames[i].equals(primaryKeys[0])) {
				firstPrimaryKeyInSqlLocation = i;
			}
			if(columnNames[i].equals(primaryKeys[1])) {
				secondPrimaryKeyInSqlLocation = i;
			}
			if(i == columnNames.length-1) {
				columnsBuilder.append(columnNames[i]).append(")");
				questionMarkBuilder.append("?) ");
			} else {
				columnsBuilder.append(columnNames[i]).append(", ");
				questionMarkBuilder.append("?, ");
			} 
		}
		
		String sql = "INSERT INTO " + DB_TABLE + " " + columnsBuilder.toString() + 
				" values " + questionMarkBuilder.toString();
		programLogger.info("BatchInsert sql: " + sql);
		
		List<Object[]> valuesList = new ArrayList<>();
		for(Map<String, Object> oneRowMap : listOfValuesMap) {
			Object[] ary = new Object[columnNames.length];
			// make the value added in the ary the order of the columnNames
			for(int i=0; i<columnNames.length; i++) {
				ary[i] = oneRowMap.get(columnNames[i]);
			}
			valuesList.add(ary);
		}
		
		// an array containing the numbers of rows affected by each update in the batch
		int[] affectedRows = jdbcTemplate.batchUpdate(sql, valuesList);
		programLogger.debug("Insert datas finished.");
		
		List<Map<String, Object>> affectedList = new ArrayList<>();	
		String firstColumn = primaryKeys[0];
		String secondColumn = primaryKeys[1];
		for(int i=0; i<affectedRows.length; i++) {
			if(affectedRows[i] > 0) {
				continue;
			}
			Map<String, Object> map = new HashMap<>();
			map.put(firstColumn, valuesList.get(i)[firstPrimaryKeyInSqlLocation]);
			map.put(secondColumn, valuesList.get(i)[secondPrimaryKeyInSqlLocation]);
			affectedList.add(map);
		}
		return affectedList;
	}

	@Override
	public List<String> queryColumnNames() {
		String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
					+ "WHERE TABLE_SCHEMA = '" + DB + "' AND TABLE_NAME = '" + DB_TABLE + "'";
		List<String> columns = jdbcTemplate.queryForList(sql, null, String.class);
		return columns;
	}

}
