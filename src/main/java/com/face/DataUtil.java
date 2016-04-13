package com.face;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DataUtil {
	private static final DataUtil dataUtil = new DataUtil();
	
	public final static DataUtil getInstance() { return dataUtil; }
	
	public List<String> readJsonArray(String json, String field)
			throws ParseException {
		List<String> list = new ArrayList<>();

		JSONParser parser = new JSONParser();
		JSONObject jsonObj = (JSONObject)parser.parse(json);
		JSONArray array = (JSONArray) jsonObj.get("data");
		Iterator<JSONObject> ite = array.iterator();
		while (ite.hasNext()) {
			JSONObject obj = (JSONObject)ite.next();
			list.add(obj.get(field).toString());
		}

		return list;
	}

	public String readJsonObject(String json, String field) throws ParseException {
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(json);
		JSONObject object = (JSONObject) obj;

		return object.get(field).toString();

	}

	public List<String> readFile(String file) {
		List<String> list = new ArrayList<String>();
		try (Stream<String> stream = Files.lines(Paths.get(file))) {
			list = stream.collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public String readByte(String file) {
		String read = null;
		try {
			read = new String(Files.readAllBytes(Paths.get(file)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return read;
	}
	
	/* write */ 
	public void writeText(String text, File file, boolean append)
			throws Exception {
		if (!file.exists())
			file.createNewFile();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file,
				append))) {
			writer.write(text);
			writer.newLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
