package hosei.negishi.pdrtam.dr_model;

import hosei.negishi.pdrtam.R;
import hosei.negishi.pdrtam.app.MainActivity;
import hosei.negishi.pdrtam.app.StepManager;
import hosei.negishi.pdrtam.model.Matrix4D;
import hosei.negishi.pdrtam.model.SensorData;
import hosei.negishi.pdrtam.model.SensorMap;
import hosei.negishi.pdrtam.model.SensorName;
import hosei.negishi.pdrtam.model.Vector3D;
import hosei.negishi.pdrtam.utils.Filter;

import java.util.ArrayList;

import android.util.Log;
import android.widget.TextView;

public class DeadReckoning {
	public static final Vector3D INIT_X = new Vector3D(1, 0, 0);
	public static final Vector3D INIT_Y = new Vector3D(0, 1, 0);
	public static final Vector3D INIT_Z = new Vector3D(0, 0, 1);
	
	/** 端末右ベクトル */
	Vector3D right;
	/** 歩行向きベクトル */
	public Vector3D direction;
	/** 端末回転量 */
	double rot;
	/** ジャイロセンサ直前の計測時間 */
	long formerTime;
	/** 平滑処理後加速度 */
	Vector3D lowAccl;
	/** 歩行判定器 */
	public StepManager stepMg;
	/** 歩行回数 */
	protected int stepCnt;
	/** 進行距離 */
	double distance;
	/** 移動経路 */
	public ArrayList<Vector3D> positions;
	/** 移動時刻 */
	public ArrayList<Long> posTimes;
	/** 現在位置 */
	Vector3D cp;
	/** 進行方向ログ */
	public ArrayList<Vector3D> directions;
	
	public DeadReckoning() {
		init();
	}
	
	@Override
	public String toString() {
		return "地磁気のみ即値利用";
	}
	
	public void init() {
		right = new Vector3D();
		direction = new Vector3D();
		rot = 0;
		formerTime = -1;
		stepMg = new StepManager();
		distance = 0;
		lowAccl = null;
		cp = new Vector3D(); // 初期位置(0,0,0)
		positions = new ArrayList<Vector3D>();
		posTimes = new ArrayList<Long>();
		// TODO 厳密には開始ボタンをおした時の時刻が入るべき?
		/* 履歴出力時にDR開始時の時刻を入力する */
//		positions.add(cp);
		directions = new ArrayList<Vector3D>();
	}
	
//	public void setTime(long startTime, long endTime) {
//		// 最初に挿入
//		posTimes.add(0, startTime);
//		
//		// 終了時刻( position=(0, 0, 0) )を挿入
//		positions.add(new Vector3D(0, 0, 0));
//		posTimes.add(endTime);
//	}
	
	/** DRメインプロセス */
	public boolean process(SensorMap sensorMap) {
		/** 正規化した重力ベクトル */
		Vector3D gravity;
		SensorData accl; // 加速度センサデータ
		
		// 加速度センサが無ければ中断 >> そもそもココに来るときは加速度が計測された時のみなので,この処理は不要?
		if(sensorMap.sensorData().containsKey(SensorName.ACCL))
			accl = sensorMap.sensorData().get(SensorName.ACCL);
		else return false;
		
		// 重力と端末向きの決定
		if(sensorMap.sensorData().containsKey(SensorName.GRVT)){
			gravity = sensorMap.vector(SensorName.GRVT).normalize();
		} else {
			gravity = INIT_X; // ジャイロセンサが効いて無い時,端末左(X軸)向きのベクトルを重力加速度ベクトルに
		}
		right = INIT_Z.exteriorProduct(gravity);
				
		// 回転量の計算
		if(sensorMap.sensorData().containsKey(SensorName.GYRO)){
			if(formerTime < 0) formerTime = sensorMap.time(SensorName.GYRO);
			rot = calcRotation(sensorMap.vector(SensorName.GYRO), gravity, rot, sensorMap.time(SensorName.GYRO), formerTime);
			formerTime = sensorMap.time(SensorName.GYRO);
		}
		/* 回転量rotは積み重ね(合計)値になる? */
		sensorMap.put(SensorName.CUM_GYRO, new SensorData(formerTime, new Vector3D(rot, 0, 0))); // ジャイロの積分値として保存
		
		/* 初期化 */
		if(lowAccl == null) {
			lowAccl = accl.getVector(); // 初回実行時
			stepMg.setInit(accl.getTime(), accl.getVector().x); // 初期値
		}
		
		/* 加速度フィルター */
		lowAccl = Filter.lowPass(lowAccl, accl.getVector());
		// 進行方向の推定
    	direction = calcDirection(gravity, right, sensorMap);
    	
    	// 歩行判定
		stepMg.determineWalking(lowAccl.x, accl.getTime());
//		stepMg.determineWalking(lowAccl.innerProduct(gravity), accl.getTime());
//    	peakAg.calcPeak(lowAccl.innerProduct(gravity), accl.getTime()); // 加速度重力成分の極値検出
		
    	if(stepMg.isPeak){
    		// 現在位置更新
    		stepCnt++;
    		TextView stepView = (TextView) MainActivity.getActivity().findViewById(R.id.step_text);
    		stepView.setText(stepCnt + " step");
    		cp.plus(direction.multCreate(1.66f * 0.46f));
    		// TODO Z軸(高さ)方向の位置推定も考える必要アリ
			cp = new Vector3D(cp.x, cp.y, 0);
			positions.add(cp);
			posTimes.add(System.currentTimeMillis());
	    	directions.add(direction.clone());
			return true;
    	}
    	
    	return false;
	}
	
	/*========= 端末回転量の計算関係 =========*/
	
	/**
	 * 鉛直軸周りの回転量を取得する
	 * @param ang_v 角速度
	 * @param gravity 重力
	 * @param cum 前回回転量
	 * @param cur_time 現在時刻
	 * @param pre_time 前回測定時刻
	 * @return 回転量
	 */
	public double calcRotation(Vector3D ang_v, Vector3D gravity, double cum, long cur_time, long pre_time) {
		// 単位時間(sec)辺りの積分
		// (cur_time - pre_time) >> 積分範囲		1e6f はmilliからsecへの変換
		double rad = calcRadianWithRodrigues(ang_v.multCreate((cur_time - pre_time) / 1e6f), gravity.normalize());
		
//    	Log.e("進行方向角度G", "" + (int)(Math.toDegrees(cum + rad)) + " °");
    	
		return cum + rad; // 前回までの回転量と加算
	}
	
	/**
	 * ロドリゲスの公式を用いて回転角計算
	 * @param rotation 回転を表すベクトル ex:ジャイロ等(というかジャイロしかない)
	 * @param gravity 重力ベクトル
	 * @return
	 */
	public double calcRadianWithRodrigues(Vector3D rotation, Vector3D gravity){
		double rad;
    	
		/* ロドリゲスの右辺構築 */
		// (R - R転置) / 2
		Matrix4D r = Matrix4D.rotateMatrix(rotation.x, rotation.y, rotation.z);
		Matrix4D rt = r.getTransposedMatrix();
		Matrix4D rod = r.sub(rt).mult(1/2.0);
		
		/* ロドリゲスの公式による回転軸の計算 */
		// 回転軸ベクトルr=(rx, ry, rz)
		Vector3D axis = new Vector3D(rod.get(2,1), rod.get(0,2), rod.get(1,0)).normalize();
		
		/* 回転量の計算(わかっていない) */
		rad = Math.asin(rod.get(2, 1) / axis.normalize().x);
		if(!Double.isNaN(rad)) return rad * axis.innerProduct(gravity);
		rad = Math.asin(rod.get(0, 2) / axis.normalize().y);
		if(!Double.isNaN(rad)) return rad * axis.innerProduct(gravity);
		rad = Math.asin(rod.get(1, 0) / axis.normalize().z);
		if(!Double.isNaN(rad)) return rad * axis.innerProduct(gravity);
		
		/* 全部NANの場合そもそも回転していない */
		return 0;
	}

	/*========= 進行方向の推定関係 =========*/

	/** 進行方向の推定 */
	public Vector3D calcDirection(Vector3D gravity, Vector3D right, SensorMap sensorMap) {
		/******** 地磁気単独 ********/
		if(!sensorMap.sensorData().containsKey(SensorName.MGNT)) return INIT_Z;
    	/* 地磁気の進行方向ベクトルの計算  */
		Vector3D east = calcEast(sensorMap.vector(SensorName.MGNT), gravity);
		// rad = Z軸に対する回転角度
    	float rad = calcRadianWithGravity(right, east, gravity);
    	
//    	Log.e("進行方向角度", "" + Math.toDegrees(rad) + " °");
    	
    	return INIT_Z.rotate(rad, 0.f, 0.f).normalize(); // TODO 後で偏角を追加
//    	return INIT_Z; // TODO 後で偏角を追加
	}
	
	/**
	 * 東ベクトルを計算する
	 * @param mgnt 地磁気ベクトル
	 * @param gravity 重力ベクトル
	 * @return 東ベクトル
	 */
	public Vector3D calcEast(Vector3D mgnt, Vector3D gravity){
		return mgnt.exteriorProduct(gravity).normalize();
	}
	
	/**
	 * 重力方向を考慮してベクトルとベクトルのなす角を求める</br>
	 * 重力方向時計回りに正の回転となる
	 * @param v ベクトル
	 * @param base 基準ベクトル
	 * @param gravity 重力方向
	 * @return 角度
	 */
	public float calcRadianWithGravity(Vector3D v, Vector3D base, Vector3D gravity){
    	float rad = (float)Math.acos(v.innerProduct(base));
    	// 東ベクトルと右ベクトルの外積
		Vector3D rotate = base.exteriorProduct(v).normalize();
		if(Double.isNaN(rad) || Double.isNaN(rotate.x) || Double.isNaN(rotate.y) || Double.isNaN(rotate.z)){
    		return 0;
		} else {
			if(rotate.innerProduct(gravity) < 0) rad = -rad;
	    	return rad;
		}
	}
	
	public int getStepCount() {
		return stepCnt;
	}
}
