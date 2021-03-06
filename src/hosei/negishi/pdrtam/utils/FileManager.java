package hosei.negishi.pdrtam.utils;

import hosei.negishi.pdrtam.app.MainActivity;
import hosei.negishi.pdrtam.model.SensorData;
import hosei.negishi.pdrtam.model.Vector3D;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.opencv.core.Core.MinMaxLocResult;

import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class FileManager {

	private static OnScanCompletedListener mScanCompletedListener = new OnScanCompletedListener() {
	    @Override
	    public void onScanCompleted(String path, Uri uri) {
	        Log.e("OnScanCompletedListener", "Scan completed: path = " + path);
	        if (uri == null) {
		        Log.e("OnScanCompletedListener", "scan uri =NULL");
	        } else {
		        Log.e("OnScanCompletedListener", "scan uri = " + uri);
	        }
	    }
	};
	
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
		File file = new File(Config.APP_DATA_DIR_PATH + "Feature Image/" + fileName);
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
	
	/**
	 * ログファイルを移動
	 */
	public static void moveLogFiles() {
		File srcDir = new File(Config.APP_DATA_DIR_PATH + "Feature Image/");
		File movedDir = new File(Config.APP_DATA_DIR_PATH);
		File[] files = srcDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			File movedFile = new File(movedDir.getAbsolutePath() + "/" + files[i].getName());
			if (!file.renameTo(movedFile)) {
				Log.e("", "failed move log file\n"
						+ file.getAbsolutePath() + "\n"
						+ movedFile.getAbsolutePath());
			} else {
				Log.e("", "success move log file\n"
						+ file.getAbsolutePath() + "\n"
						+ movedFile.getAbsolutePath());
			}
		}
	}
	
	/**
	 * ログファイルをスキャン(PC上などで見えるように)
	 */
	public static void scanLogFiles() {
		File srcDir = new File(Config.APP_DATA_DIR_PATH + "Feature Image/");
		String[] paths = srcDir.list();
		String[] mimeType = new String[paths.length];
		for (int i = 0; i < mimeType.length; i++) {
			String suffix = getSuffix(paths[i]);
			if (suffix.equals("csv")) {
				mimeType[i] = "text/csv";
			} else if (suffix.equals("txt")) {
				mimeType[i] = "text/plain";
			} else if (suffix.equals("dat")) {
				mimeType[i] = "text/dat";
			} else {
				mimeType[i] = null;
			}
		}
		MediaScannerConnection.scanFile(MainActivity.getContext(), paths, mimeType, mScanCompletedListener);
	}
	
	/**
	 * ファイル名から拡張子を返します。
	 * @param fileName ファイル名
	 * @return ファイルの拡張子
	 */
	private static String getSuffix(String fileName) {
		if (fileName == null) return null;
		int point = fileName.lastIndexOf(".");
		if (point != -1) return fileName.substring(point + 1);
		return fileName;
	}
	
	/**
	 * SensorData形式のリストを書き込む
	 * @param fileName 書き込むファイル名
	 * @param dataList センサーデータList
	 * @return 書き込み成功:true */
	public static boolean writeListData(String fileName, List<SensorData> dataList) {
		StringBuilder sb = new StringBuilder();
		sb.append("time,x,y,z\n");	// header
		
		for(int i = 0; i < dataList.size(); i++)
			sb.append(dataList.get(i).toString() + "\n");
		if (FileManager.write(fileName + ".csv", sb.toString()) && FileManager.write(fileName + ".dat", FileManager.convertSDToBytes(dataList))) {
			Log.e("", "Success Write => " + fileName);
//			Toast.makeText(MainActivity.getContext(), "Success Write => " + fileName, Toast.LENGTH_SHORT).show();
			return true;
		} else {
			Log.e("", "Failed Write => " + fileName);
			return false;
		}
	}
	
	/**
	 * Vector3D+Long形式のリストを書き込む
	 * @param fileName 書き込むファイル名
	 * @param dataList Vector3DデータList
	 * @param timeList longtimeList
	 * @return 書き込み成功:true */
	public static boolean writeListData(String fileName, List<Vector3D> dataList, List<Long> timeList) {
		ArrayList<SensorData> vectorTimeList = new ArrayList<SensorData>(dataList.size());
		StringBuilder sb = new StringBuilder();
		sb.append("time,x,y,z\n");	// header
		
		for(int i = 0; i < dataList.size(); i++) {
			sb.append(timeList.get(i) + "," + dataList.get(i).toString() + "\n");
			vectorTimeList.add(new SensorData(timeList.get(i), dataList.get(i)));
		}
		if (FileManager.write(fileName + ".csv", sb.toString()) && FileManager.write(fileName + ".dat", FileManager.convertSDToBytes(vectorTimeList))) {
			Log.e("", "Success Write => " + fileName);
//			Toast.makeText(MainActivity.getContext(), "Success Write => " + fileName, Toast.LENGTH_SHORT).show();
			return true;
		} else {
			Log.e("", "Failed Write => " + fileName);
			return false;
		}
	}
	
	/** UTF-8 上書き書き込み
	 * @return 書き込み成功ならtrue */
	public static boolean write(String fileName, String text) {
		PrintWriter writer = null;
		boolean success = true;
        File file = new File(Config.APP_DATA_DIR_PATH + "Feature Image/" + fileName);
        file.getParentFile().mkdir();
		try {
			FileOutputStream out = new FileOutputStream(file, false); // 上書き
			writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
			writer.append(text);
		} catch (FileNotFoundException fnfe) {
			Log.e("FileManager#write", fnfe.getMessage() );
			Toast.makeText(MainActivity.getContext(), "failed write!\n" + fnfe.getMessage(), Toast.LENGTH_SHORT).show();
			success = false;
		} catch (IOException ioe) {
			Log.e("FileManager#write", ioe.getMessage() );
			Toast.makeText(MainActivity.getContext(), "failed write!\n" + ioe.getMessage(), Toast.LENGTH_SHORT).show();
			success = false;
		} finally {
			if(writer != null)
				writer.close();
		}
		return success;
	}
	
	public static boolean write(String fileName, byte[][] bytess) {
		BufferedOutputStream fis = null;
		boolean success = true;

		try {
			// 出力先ファイル
			File file = new File(Config.APP_DATA_DIR_PATH + "Feature Image/" + fileName);
			
			fis = new BufferedOutputStream(new FileOutputStream(file));
			for (byte[] bytes : bytess) {
				fis.write(bytes);
			}
		} catch (IOException e) {
			Log.e("FileManager#write", e.getMessage() );
			Toast.makeText(MainActivity.getContext(), "failed write!\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
			success = false;
		} finally {
			try {
				if (fis != null) fis.close();
			} catch (IOException e) {
				Log.e("FileManager#write", e.getMessage() );
				Toast.makeText(MainActivity.getContext(), "failed write!\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
				success = false;
			}
		}
		return success;
	}
	
	public static byte[][] convertSDToBytes(List<SensorData> sd) {
		int dataNum = 4; // time, x, y, z で4つ
		int byteArraySize = sd.size() * dataNum;
		byte[][] byteArys = new byte[byteArraySize][];
		ByteBuffer buf;
		for (int i = 0; i < sd.size(); i++) {
			SensorData data = sd.get(i);
			long time = data.getTime();
			Vector3D v = data.getVector();
			// time
			buf = ByteBuffer.allocate(8);
			buf.putDouble(time);
			byteArys[i * dataNum] = buf.array();
			// x
			buf = ByteBuffer.allocate(8);
			buf.putDouble(v.x);
			byteArys[(i * dataNum) + 1] = buf.array();
			// y
			buf = ByteBuffer.allocate(8);
			buf.putDouble(v.y);
			byteArys[(i * dataNum) + 2] = buf.array();
			// z
			buf = ByteBuffer.allocate(8);
			buf.putDouble(v.z);
			byteArys[(i * dataNum) + 3] = buf.array();
		}
//		for (int i = 0; i < byteArys.length; i++) {
//			buf = ByteBuffer.allocate(8);
//			buf.putDouble(dAry[i]);
//			byteArys[i] = buf.array();
//		}
		return byteArys;
	}

}
