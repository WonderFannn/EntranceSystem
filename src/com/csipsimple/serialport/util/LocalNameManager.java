package com.csipsimple.serialport.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.os.Environment;

public class LocalNameManager {

	private final static String PATH = "/sdcard/EntranceSystem";
	private final static String FILENAME = "/LocalName.txt";
	private final static String DEFAULTNAME = "德商国际4栋2单元";
	
	public static void writeFile() {
		try {
			if (Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				File path = new File(PATH);
				File f = new File(PATH + FILENAME);
				if (!path.exists()) {
					path.mkdirs();
				}
				if (!f.exists()) {
					f.createNewFile();
					OutputStreamWriter osw = new OutputStreamWriter(
							new FileOutputStream(f), "UTF-8");
					osw.write(DEFAULTNAME);
					osw.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String readFile() {
		String fileContent = "";
		try {
			File f = new File(PATH + FILENAME);
			if (f.isFile() && f.exists()) {
				InputStreamReader read = new InputStreamReader(
						new FileInputStream(f), "UTF-8");
				BufferedReader reader = new BufferedReader(read);
				String line;
				while ((line = reader.readLine()) != null) {
					fileContent += line;
				}
				read.close();
			}
		} catch (Exception e) {
			System.out.println("读取文件内容操作出错");
			e.printStackTrace();
		}
		if (!fileContent.isEmpty()) {
			return fileContent;
		}
		return DEFAULTNAME;
	}
}
