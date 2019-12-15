package com.iisi.cmt.stationObsType.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.iisi.cmt.stationObsType.dao.IDao;

@Service
public class WriteProcess {
	
	@Autowired
	ReadCsv csvReader;
	
	@Autowired
	IDao dbReader;
	
	private static final Logger resultLogger = LogManager.getLogger("result");
	private static final Logger programLogger = LogManager.getLogger("program");
	private boolean allCsvWriteIn = true;
	private static final String PRIMARY_KEY_TYPE = "Type";
	private static final String PRIMARY_KEY_DATE = "Date";
	private List<String> csvHeaders = null;
	private List<String> dbColumns = null;
	
	public void run(String filePath) {

		csvReader.setFilePath(filePath);		
		final List<Map<String, Object>> csvDatas;
		
		// 1. read csv and get header
		try {
			programLogger.debug("Read csv headers...");
			csvHeaders = csvReader.readCsvHeader();			
			programLogger.info("Csv Headers: \n" + csvHeaders);	
		} catch (IOException e) {
			resultLogger.error("Error in reading csv files, please try again.");
			programLogger.error(e);
			allCsvWriteIn = false;
			return;
		}	
			
		// 2. get db table columns
		try {
			programLogger.debug("Query DB columns...");
			dbColumns = dbReader.queryColumnNames();
			programLogger.info("Db columns: \n" + dbColumns);
		} catch (DataAccessException e) {
			resultLogger.error("Error in access database data, please check db username, password, and network connection, and try again.");
			programLogger.error(e);
			allCsvWriteIn = false;
			return;
		}
		
		// 3. comparing header and columns, if there are difference, log to file and return false
		if(csvHeaders.size() != dbColumns.size()) {
			resultLogger.warn("Csv and Db columns size doesn't match." 
						+ " Csv headers size: " + csvHeaders.size()
						+ ". Db columns size: " + dbColumns.size() + ".");
			allCsvWriteIn = false;
		}
		if( !csvHeaders.containsAll(dbColumns) || !dbColumns.containsAll(csvHeaders)) {
			resultLogger.warn("Csv and Db have some columns not match.");
			List<String> dbMore = new ArrayList<>();
			for(String db : dbColumns) {
				if(csvHeaders.contains(db)) {
					continue;
				} else {
					dbMore.add(db);
				}
			}
			
			List<String> csvMore = new ArrayList<>();
			for(String csv : csvHeaders) {
				if(dbColumns.contains(csv)) {
					continue;
				} else {
					csvMore.add(csv);
				}
			}
			
			resultLogger.warn("Csv has more following headers: " + csvMore);
			resultLogger.warn("DB has more following columns: " + dbMore);
			allCsvWriteIn = false;
			return;
		}
		
		// 4. get csv value and check values, if there are any invalid situations, log to file and return false 
		try {	
			programLogger.debug("Read csv data...");
			csvDatas = csvReader.readCsv();
			
			programLogger.debug("Check if csv has invalid data..."); 
			Set<Map<String, Object>> invalidCsvMaps = new HashSet<>();
			
			// only 0 and 1 is valid
			for(int i=0; i<csvDatas.size(); i++) {
				Map<String, Object> map = csvDatas.get(i);
				for(String key : csvHeaders) {
					if(key.equals(PRIMARY_KEY_TYPE) || key.equals(PRIMARY_KEY_DATE)) {
						continue;
					}
					if(map.get(key) == null  || 
							( !(map.get(key).equals("0")) && !(map.get(key).equals("1"))) ) {
						invalidCsvMaps.add(csvDatas.get(i));
						resultLogger.warn(map.get(PRIMARY_KEY_TYPE) + ", " + map.get(PRIMARY_KEY_DATE) + ", " + key  
							+ " has invalid values. (All values should be either 0 or 1. It can not be null or empty.)");
						break;
					}
				}
			}
			if(invalidCsvMaps.size() > 0) {
				allCsvWriteIn = false;
				return;
			}
				
			// check if there are duplicate primary key in csv
			List<Map<String, String>> duplicatePrimaryKey = this.checkDuplicatePrimaryKey(csvDatas, csvDatas, true);
			if(duplicatePrimaryKey.size() > 0) {
				for(Map<String, String> map :duplicatePrimaryKey) {
					for(Entry<String, String> entry : map.entrySet()) {
						resultLogger.warn("There are duplicate " + entry.getKey() + ", " + entry.getValue() + " in csv file. Please modify csv file." );
					}
				}
				allCsvWriteIn = false;
				return;
			}
			
			// check if there are same values in csv excluding of primary key
			List<String> duplicateValues = this.checkDuplicateValues(csvDatas, csvDatas);
			if(duplicateValues.size() > 0) {
				String i;
				String j;
				for(String s : duplicateValues) {
					i = s.split("=")[0];
					j = s.split("=")[1];
					resultLogger.warn(i + " and " + j + " have same values in csv. Please modify csv file." );
				}
				allCsvWriteIn = false;
				return;
			}
		} catch (IOException ioE) {
			resultLogger.error("Error in reading csv files, please try again.");
			programLogger.error(ioE);
			allCsvWriteIn = false;
			return;
		}	
		
		// 5. get db datas and check values, if there are any invalid situations, log to file and return false
		try {
			programLogger.debug("Query all db datas...");
			final List<Map<String, Object>> dbDatas = dbReader.selectAll();
				
			// check if there are duplicate primary key in csv
			List<Map<String, String>> duplicatePrimaryKey = this.checkDuplicatePrimaryKey(csvDatas, dbDatas, false);
			if(duplicatePrimaryKey.size() > 0) {
				for(Map<String, String> map :duplicatePrimaryKey) {
					for(Entry<String, String> entry : map.entrySet()) {
						resultLogger.warn("There are conlict primary key " + entry.getKey() + ", " + entry.getValue() + " in csv file and database. "
								+ " Please modify csv file." );
					}
				}
				allCsvWriteIn = false;
				return;
			}
			
			// check if there are same values in csv and database exclusive of primary key
			List<String> duplicateValues = this.checkDuplicateValues(csvDatas, dbDatas);
			if(duplicateValues.size() > 0) {
				String i;
				String j;
				for(String s : duplicateValues) {
					i = s.split("=")[0];
					j = s.split("=")[1];
					resultLogger.warn("csv's " + i + " and database's " + j + " have same values. Please modify csv file." );
				}
				allCsvWriteIn = false;
				return;
			}
						
			// 5. write csv rows into db
			programLogger.debug("Write to db...");
			List<Map<String, Object>> notInsertedDatas = dbReader.batchInsert(new String[] {PRIMARY_KEY_TYPE, PRIMARY_KEY_DATE}
													, csvHeaders.toArray(new String[csvHeaders.size()]), csvDatas);
					
			// 6. if there are some rows not correctly written to db, log to file
			if(notInsertedDatas.size() > 0) {
				for(Map<String, Object> map : notInsertedDatas) {
					resultLogger.error(map.get(PRIMARY_KEY_TYPE)+ ", " +map.get(PRIMARY_KEY_DATE)+ " not write to db.");
				}
				allCsvWriteIn = false;
			}

		} catch (DataAccessException daE) {
			resultLogger.error("Error in access database data or wirte to db, please check db username and password, and try again.");
			programLogger.error(daE);
			allCsvWriteIn = false;
			return;
		}
	}
		
	private List<Map<String, String>> checkDuplicatePrimaryKey(List<Map<String, Object>> firstList, List<Map<String, Object>> secondList, boolean sameSource){
		List<Map<String, String>> duplicatePrimaryKey = new ArrayList<>();
		Map<String, Object> iMap = null;
		Map<String, Object> jMap = null;
		String iType = null;
		String iDate = null;
		String jType = null;
		String jDate = null;
		for(int i=0; i<firstList.size(); i++) {
			for(int j=sameSource?i+1:0; j<secondList.size(); j++) {
				iMap = firstList.get(i);
				jMap = secondList.get(j);
				iType = Objects.toString(iMap.get(PRIMARY_KEY_TYPE));
				iDate = Objects.toString(iMap.get(PRIMARY_KEY_DATE));
				jType = Objects.toString(jMap.get(PRIMARY_KEY_TYPE));
				jDate = Objects.toString(jMap.get(PRIMARY_KEY_DATE));
				// duplicate primary key
				if(iType.equals(jType) && iDate.equals(jDate) ) {
					Map<String, String> map = new HashMap<>();
					map.put(iType, iDate);
					duplicatePrimaryKey.add(map);
				}
			}
		}
		return duplicatePrimaryKey;
	}
	
	private List<String> checkDuplicateValues(List<Map<String, Object>> firstList, List<Map<String, Object>> secondList){
		Map<Map<String, String>, Integer> sameValueGroup = new HashMap<>();
		Map<String, Object> iMap = null;
		Map<String, Object> jMap = null;
		String iType = null;
		String iDate = null;
		String jType = null;
		String jDate = null;
		for(int i=0; i<firstList.size(); i++) {
			iMap = firstList.get(i);
			iType = Objects.toString(iMap.get(PRIMARY_KEY_TYPE));
			iDate = Objects.toString(iMap.get(PRIMARY_KEY_DATE));
			columnValueCheck:
			for(int j=0; j<secondList.size(); j++) {
				jMap = secondList.get(j);
				jType = Objects.toString(jMap.get(PRIMARY_KEY_TYPE));
				jDate = Objects.toString(jMap.get(PRIMARY_KEY_DATE));
				// they are same row
				if(iType.equals(jType) && iDate.equals(jDate)) {
					continue;
				}
				// only comparing same Date rows
				if(!iDate.equals(jDate)) {
					continue;
				}
				for(String key : csvHeaders) {
					if(key.equals(PRIMARY_KEY_TYPE) || key.equals(PRIMARY_KEY_DATE)) {
						continue;
					}
					if(!Objects.toString(iMap.get(key)).equals(Objects.toString(jMap.get(key)))) {
						continue columnValueCheck;
					}
				}
				// all values are the same
				Map<String, String> sameType = new HashMap<>();
				sameType.put(iType, jType);
				if(sameValueGroup.containsKey(sameType)) {
					int times = sameValueGroup.get(sameType).intValue();
					sameValueGroup.put(sameType, times+1);
				} else {
					sameValueGroup.put(sameType, 1);
				}	
			}
		}
		
		List<String> result = new ArrayList<>();
		if(sameValueGroup.containsValue(3)) {
			for(Entry<Map<String, String>, Integer> entry :sameValueGroup.entrySet()) {	
				if(entry.getValue().intValue() >= 3) {
					String i = (String)entry.getKey().keySet().toArray()[0];
					String j = (String)entry.getKey().values().toArray()[0];
					result.add(i+"="+j);
				}
			}
		}
		return result;
	}

	public boolean isAllCsvWriteIn() {
		return allCsvWriteIn;
	}

}
