package com.iisi.cmt.stationObsType.dao;

import java.util.List;
import java.util.Map;

public interface IDao {

	final static String DB = "cmt";
	final static String DB_TABLE = "StationObsType";
	
	List<Map<String, Object>> selectAll();
	
	/**
	 * @param String[] primaryKeys the primary keys of the table
	 * @param String[] columnNames array of all column
	 * @param List<Map<String, Object>> listOfValuesMap list of insert rows, row is a map which key is the column name and value is data.
	 * @return List<Map<String, Object>> If all rows are inserted successfully, return an empty list. Otherwise, return a list of not inserted map,
	 * 								keys of the map is the primary keys of the table. 
	 */
	List<Map<String, Object>> batchInsert(String[] primaryKeys, String[] columnNames, List<Map<String, Object>> listOfValuesMap);
	
	List<String> queryColumnNames();
	
}
