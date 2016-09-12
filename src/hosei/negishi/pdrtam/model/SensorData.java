package hosei.negishi.pdrtam.model;


/**
 * センサーデータクラス
 * @author negishi
 */
public class SensorData implements Cloneable{
	private long time;		// データ取得時間
	private Vector3D vector;	// センサ(ベクトル)データ
	
	public SensorData(long time, Vector3D vector){
		this.time = time;
		this.vector = vector;
	}
	
	public SensorData(long time, float x, float y, float z){
		this.time = time;
		this.vector = new Vector3D(x, y, z);
	}
	
	public Vector3D getVector() {
		return vector;
	}

	public long getTime() {
		return time;
	}
	
	@Override
	public String toString() {
		return time + "," + vector.toString();
	}
}
