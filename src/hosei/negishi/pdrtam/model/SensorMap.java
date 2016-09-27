package hosei.negishi.pdrtam.model;

import java.util.HashMap;

/**
 * ある時点でのセンサーデータ集
 * @author negishi
 */
public class SensorMap{
	
	private HashMap<SensorName, SensorData> sensorData;
	
	public SensorMap(){
		sensorData = new HashMap<SensorName, SensorData>();
	}
	
	/** key で指定したセンサ値の時間を取得する */
	public long time(SensorName key){
		return sensorData.get(key).getTime();
	}
	
	/** key で指定したデータのベクトル値を取得する */
	public Vector3D vector(SensorName key){
		return sensorData.get(key).getVector();
	}
	
	/** ベクトル値を持つセンサデータ値を取得する */
	public HashMap<SensorName, SensorData> sensorData(){
		return sensorData;
	}
	
	/** データを格納する */
	public void put(SensorName name, SensorData vector){
		sensorData.put(name, vector);
	}	
}
