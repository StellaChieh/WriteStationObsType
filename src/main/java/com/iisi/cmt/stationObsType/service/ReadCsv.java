package com.iisi.cmt.stationObsType.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

@Component
public class ReadCsv {

	private String filePath;
	
	private List<String> csvHeaders = new ArrayList<>();
	
	public List<String> readCsvHeader() throws IOException {
		String line = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			line = br.readLine();
			String[] headerAry = line.split(",");
			for(String header : headerAry) {
				csvHeaders.add(header);
			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
		return csvHeaders;
	}
	
	private CellProcessor[] getCellProcessor() {
		CellProcessor[] processors = new CellProcessor[csvHeaders.size()];
		for(int i=0; i<csvHeaders.size(); i++) {
			processors[i] = new Optional();
		} 
		return processors;
	} 
	
	public List<Map<String, Object>> readCsv() throws IOException {
		ICsvMapReader mapReader = null;
		List<Map<String, Object>> result = new ArrayList<>();
		try {
			mapReader = new CsvMapReader(new FileReader(filePath), CsvPreference.STANDARD_PREFERENCE);
			// the header columns are used as the keys to the Map
			final String[] header = mapReader.getHeader(true);
			final CellProcessor[] processors = getCellProcessor();
			
			Map<String, Object> lineMap;
			while( (lineMap = mapReader.read(header, processors)) != null) {
				result.add(lineMap);	
			}
		} finally {
			if (mapReader != null) {
				mapReader.close();
			}
		}
		return result;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	
}