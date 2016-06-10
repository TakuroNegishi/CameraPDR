package hosei.negishi.pdrtam.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

public class FileManager {

	private FileManager() {
	}

	public static float[] readFile(String filePath) {
		File file = new File(filePath);
		BufferedReader br = null;
		ArrayList<Float> floatAry = new ArrayList<Float>();
		try {
			br = new BufferedReader(new FileReader(file));
			String str;
			String[] strAry;
			while ((str = br.readLine()) != null) {
				strAry = str.split(",");
				for (String string : strAry) {
					floatAry.add(Float.parseFloat(string));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		float[] fAry = new float[floatAry.size()];
		for (int i = 0; i < fAry.length; i++) {
			fAry[i] = floatAry.get(i);
		}
		return fAry;
	}

	/**
	 * UTF-8 上書き書き込み
	 * 
	 * @return 書き込み成功ならtrue
	 */
	public static boolean write(String fileName, float[] vertex) {
		PrintWriter writer = null;
		boolean success = true;
		File file = new File(Environment.getExternalStorageDirectory() + "/"
				+ "negishi.deadreckoning/" + fileName);
		file.getParentFile().mkdir();
		StringBuilder sb = new StringBuilder();
		sb.append("x,y,z\n");
		for (int i = 0; i < vertex.length; i++) {
			if (i % 3 == 2)
				sb.append(vertex[i] + "\n");
			else
				sb.append(vertex[i] + ",");
		}
		try {
			FileOutputStream out = new FileOutputStream(file, false); // 上書き
			writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
			writer.append(sb.toString());
		} catch (FileNotFoundException fnfe) {
			Log.e("FileManager#write", fnfe.getMessage());
			// Toast.makeText(MainActivity.getContext(), "failed write!\n" +
			// fnfe.getMessage(), Toast.LENGTH_SHORT).show();
			success = false;
		} catch (IOException ioe) {
			Log.e("FileManager#write", ioe.getMessage());
			// Toast.makeText(MainActivity.getContext(), "failed write!\n" +
			// ioe.getMessage(), Toast.LENGTH_SHORT).show();
			success = false;
		} finally {
			if (writer != null)
				writer.close();
		}
		return success;
	}
}
