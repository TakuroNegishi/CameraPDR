package hosei.negishi.pdrtam.app;

import hosei.negishi.pdrtam.model.SensorData;
import hosei.negishi.pdrtam.model.Vector3D;
import hosei.negishi.pdrtam.utils.FileManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.R;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.widget.Toast;


/**
 * 内蔵センサ用アダプター
 * @author negishi */
public class SensorAdapter implements SensorEventListener {
	
//	MainActivity mainActivity;
	private SensorManager manager;
	
	// センサーデータ格納用
	private ArrayList<SensorData> accelerometerList;	// 加速度
	private ArrayList<SensorData> gyroscopeList;		// ジャイロ
	
	public SensorAdapter () {
//		this.mainActivity = mainActivity;
		init();
	}
	
	public void init() {
		accelerometerList = new ArrayList<SensorData>();
		gyroscopeList = new ArrayList<SensorData>();
	}
	
	/** センサー起動 */
	public void setSensor(Activity activity) {
		/* センサーマネージャーのインスタンスを取得 */
		manager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
		/* センサーの登録 */
		List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ALL);
		for(Sensor sensor : sensors){
			Log.v("RESEARCH", "保有センサ" + sensor.getName());
			switch(sensor.getType()){
			case Sensor.TYPE_GYROSCOPE:				// ジャイロセンサー
//			case Sensor.TYPE_GRAVITY:				// 重力センサー
//			case Sensor.TYPE_LINEAR_ACCELERATION:	// 直線加速度センサー(加速度-重力=直線加速度)
			case Sensor.TYPE_ACCELEROMETER:			// 加速度センサー
//			case Sensor.TYPE_PRESSURE:				// 気圧センサー
//			case Sensor.TYPE_AMBIENT_TEMPERATURE:	// 温度センサー
//			case Sensor.TYPE_MAGNETIC_FIELD:		// 地磁気センサ
				/* 謎のセンサ登録阻止 */
				if(sensor.getVendor().startsWith("Google")) break;
				if(sensor.getName().contains("Secondary")) break;
				Log.e("RESEARCH", "SENSOR_NAME = (" + sensor.getName() + ") BENDER_NAME = (" + sensor.getVendor() + ")");
				/* SensorManager.SENSOR_DELAY_FASTEST	0ms
				 * SensorManager.SENSOR_DELAY_GAME		20ms
				 * SensorManager.SENSOR_DELAY_UI		60ms
				 * SensorManager.SENSOR_DELAY_NORMAL	200ms */
				manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
				break;
			default:
				break;
			}
		}
		
		Toast.makeText(MainActivity.getContext(), "Sensor Start", Toast.LENGTH_SHORT).show();
	}
	
	/** センサーリスナーの解除 */
	public void removeSensor() {
		if(manager != null){
			manager.unregisterListener(this);
			manager = null;
			Toast.makeText(MainActivity.getContext(), "Sensor Stop", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Vector3D elem = new Vector3D(event.values[0], event.values[1], event.values[2]);
		// 現在の時刻(ナノ秒)を記録
		SensorData sensorData = new SensorData(event.timestamp, elem);
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {	// 加速度センサ
			accelerometerList.add(sensorData);
//			Log.e("onSensorChanged()", sensorData.toString());
		} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {	// ジャイロセンサ
			gyroscopeList.add(sensorData);
		}
	}
	
	public void writeLog() {
		boolean success = true;
		Date now = new Date();
		SimpleDateFormat s = new SimpleDateFormat("yyyy'-'MM'-'dd' 'HH';'mm';'ss", Locale.JAPAN);
		String header = s.format(now);
		success &= FileManager.writeListData(header + "_acce", accelerometerList);
		success &= FileManager.writeListData(header + "_gyro", gyroscopeList);
		if (success)
			Toast.makeText(MainActivity.getContext(), "Success Write All Log", Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(MainActivity.getContext(), "Failed Write Any Log", Toast.LENGTH_SHORT).show();
	}
}
