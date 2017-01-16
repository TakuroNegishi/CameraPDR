package hosei.negishi.pdrtam.dr_model;

import hosei.negishi.pdrtam.R;
import hosei.negishi.pdrtam.app.MainActivity;
import hosei.negishi.pdrtam.app.NativeAccesser;
import hosei.negishi.pdrtam.model.SensorData;
import hosei.negishi.pdrtam.model.SensorMap;
import hosei.negishi.pdrtam.model.SensorName;
import hosei.negishi.pdrtam.model.Vector3D;
import hosei.negishi.pdrtam.model.Vector3D.VectorTYPE;
import hosei.negishi.pdrtam.utils.Filter;

import java.util.ArrayList;

import android.util.Log;
import android.widget.TextView;

/**
 * 連続区間遡及型自律的経路修正手法(Continuous interval recourse)
 * 大局的な地磁気情報を使って、過去全経路を修正する手法
 * +ドリフト誤差を計算する
 * ジャイロベース
 * 端末横向き固定
 * @author negishi
 */
public class DR_CIR_Gyro extends DeadReckoning {
	/** ドリフト誤差の適用させる,加速度センサの計測回数 */
	private static final int DRIFT_ADAPT_COUNT = 30;
	private static final boolean IS_DRIFT_ADAPT = true;
	/** Native側にアクセスする間隔(msec) */
	private static final int NATIVE_ACCESS_TIME_SPAN = 2000;
	/** 偏角(西偏)(東京) */
//	private static final double MAGNETIC_DECLINATION = 7.31; // 度(°)
	/** 大局的な地磁気 */
	Vector3D meanMgnt;
	/** 更新待ち位置のデータ */
	public ArrayList<SensorMap> queue;

	/** ジャイロ積分値 */
	double cum_gyro;
	long formerTime;
	boolean first;
	/** ドリフト誤差 */
	double drift;
	int count;
	double firstY;
	double xySum;
	double xSum;
	double ySum;
	double xSumPow;
	/** 無回転時進行方向と地磁気との角度 */
	double meanRad;
	/** 横向き回転 */
	ArrayList<Long> startTimeArray;
	ArrayList<Long> endTimeArray;
	ArrayList<Long> sideLookStartTimeArray;
	ArrayList<Long> sideLookEndTimeArray;
	ArrayList<Long> walkStatusArray;
	
	TextView logText;
	long prevMilliTime;
	Vector3D subDirection;
	/** フィルター適応後の気圧値 */
	Vector3D lowPrss;
	/** 計測開始時の気圧値 */
	Vector3D basePrss;
	
	public DR_CIR_Gyro() {
		super();
		queue = new ArrayList<SensorMap>();
		startTimeArray = new ArrayList<Long>();
		endTimeArray = new ArrayList<Long>();
		sideLookStartTimeArray = new ArrayList<Long>();
		sideLookEndTimeArray = new ArrayList<Long>();
		walkStatusArray = new ArrayList<Long>();
		
		logText = (TextView)(MainActivity.getActivity().findViewById(R.id.log_text));
		subDirection = new Vector3D();

		init();
//		this.window = window;
//		drName = "組み合わせ平均法" + window*20 +"ミリ秒窓(半自由)";
	}
	
	@Override
	public String toString() {
		return "連続区間遡及型-ジャイロベース-(D)";
	}
	
	@Override
	public boolean process(SensorMap sensorMap, long milliTime){
		/** 正規化した重力ベクトル */
		Vector3D gravity;
		SensorData accl; // 加速度センサデータ

		/* 加速度がそもそもセンサとして無かったら */
		if(sensorMap.sensorData().containsKey(SensorName.ACCL)) {
			accl = sensorMap.sensorData().get(SensorName.ACCL);
		} else {
			return false;
		}
		
		/* 重力と端末向きを決め打ちする */
		if(sensorMap.sensorData().containsKey(SensorName.GRVT)){
			gravity = sensorMap.vector(SensorName.GRVT).normalize();
		} else {
			// 横向き
			gravity = INIT_X;
			sensorMap.put(SensorName.GRVT, new SensorData(0, gravity));
//			gravity = INIT_Y;
//			sensorMap.put(SensorName.GRVT, new SensorData(0, gravity));
		}
		right = INIT_Z.exteriorProduct(gravity);
		
//		if (gravity.getMax() == VectorTYPE.X) {
//			// X方向に重力(横向き)
//			right = INIT_Z.exteriorProduct(gravity);
//		} else {
//			// Z方向に重力(水平)
//			right = INIT_Y.multCreate(-1).exteriorProduct(gravity);
//		}
		
		/* ジャイロの回転 */
		if(!sensorMap.sensorData().containsKey(SensorName.GYRO)) return false;
		SensorData gyroData = sensorMap.sensorData().get(SensorName.GYRO);
		
		if(first){
			formerTime = gyroData.getTime();
			posTimes.add(milliTime);
			first = false;
		}
		
		double rad = calcRadianWithRodrigues(gyroData.getVector().multCreate((gyroData.getTime() - formerTime)/1e9f), gravity.normalize());
		cum_gyro += rad;
//    	Log.e("CUM_GYRO", Math.toDegrees(cum_gyro) + "°");
		
		/* ジャイロの積分値として保存 */
		sensorMap.put(SensorName.CUM_GYRO, new SensorData(gyroData.getTime(), new Vector3D(0, 0, cum_gyro)));
		formerTime = gyroData.getTime();
		
		/* ドリフト誤差の計算 */
		if(!sensorMap.sensorData().containsKey(SensorName.MGNT)) return false;
		// +-方向補正(加速度の軸と合わせる)&偏角分回転
		Vector3D mgnt = sensorMap.vector(SensorName.MGNT).multCreate(-1);
		calcDrift(mgnt, gravity);
//    	Log.e("DRIFT", Math.toDegrees(drift) + "°");
		
		/* 地磁気の平均処理 */
    	mgnt = mgnt.rotate(cum_gyro - drift, 0, 0);
//    	Log.e("地磁気方向", mgnt.toString());
		meanMgnt = meanMgnt.plusCreate(mgnt); // 基準を合わせる;
		
    	// 平均地磁気と無回転時の進行方向との角度を算出
		Vector3D east = calcEast(meanMgnt, gravity);
    	meanRad = calcRadianWithGravity(right, east, gravity);
//    	Log.e("", "right" + right.toString());
//    	Log.e("", "gravity" + gravity.toString());
//    	Log.e("", "east" + east.toString());
//    	Log.e("", "meadRad" + Math.toDegrees(meanRad) + "°");
    	
		/* 加速度初期化 */
		if (lowAccl == null) {
			lowAccl = accl.getVector(); // 初回実行時
			stepMg.setInit(accl.getTime(), accl.getVector().innerProduct(gravity)); // 初期値
		}
		
		/* 加速度フィルター */
		lowAccl = Filter.lowPass(lowAccl, accl.getVector());		
    	
    	/* 高さの推定 */
//		Log.e("PRESSURE", sensorMap.sensorData().get(SensorName.PRESSURE).toString());
    	if(sensorMap.sensorData().containsKey(SensorName.PRESSURE)){
//    		prss = sensorMap.sensorData().get(SensorName.PRESSURE);
    		if(lowPrss == null) lowPrss = sensorMap.vector(SensorName.PRESSURE).clone();
    		if(basePrss == null) basePrss = sensorMap.vector(SensorName.PRESSURE).clone();
    		/* ローパスをかけて適用 */
    		lowPrss = Filter.lowPass(lowPrss, sensorMap.vector(SensorName.PRESSURE), 0.01f);
    	} else {
    		basePrss = INIT_X;
    		lowPrss = INIT_X;
    	}
		sensorMap.put(SensorName.PRESSURE, new SensorData(0, lowPrss));
	    
	    /* 鉛直成分による歩行判定 */
		stepMg.determineWalking(lowAccl.innerProduct(gravity), accl.getTime());
		
		if(stepMg.isPeak) {
			stepCnt++;
			/* 経路推定ポイントにデータを追加 */
			SensorMap copy = new SensorMap();
			copy.sensorData().putAll(sensorMap.sensorData());
			queue.add(copy);
			
			posTimes.add(milliTime); // 時刻保存
			/* 現在までの推定経路を一旦リフレッシュ */
			positions.clear();
			subPositions.clear();
			directions.clear();
			subDirections.clear();
			cp = new Vector3D();
			subCP = new Vector3D();
			// 初期位置追加
			positions.add(cp);
			subPositions.add(subCP);
			
			// 横向き歩きの区間取得 
			if (prevMilliTime == 0) {
				prevMilliTime = milliTime;
			} else if ((milliTime - prevMilliTime) >= NATIVE_ACCESS_TIME_SPAN) {
				// 2sec間隔でチェック
				long[] startEndTime = NativeAccesser.getInstance().getTimeAry();
//				LEFT_VP = -1, RIGHT_VP = 1
				if (startEndTime[0] != 0 && startEndTime[1] != 0) {
					startTimeArray.add(startEndTime[0]);
					endTimeArray.add(startEndTime[1]);
					sideLookStartTimeArray.add(startEndTime[5]);
					sideLookEndTimeArray.add(startEndTime[6]);
					walkStatusArray.add(startEndTime[7]);
				}
				logText.setText("s=" + startEndTime[0] + ", e=" + startEndTime[1] + "\n"
						+ ", count=" + startEndTime[2] + ",startVP=" + startEndTime[3]
						+ ",sideSt=" + startEndTime[4] + ",sideS=" + startEndTime[5] + "\n"
						+ ",sideE=" + startEndTime[6] + ",walkSt=" + startEndTime[7]);
				prevMilliTime = milliTime;
			}
			// 過去の移動軌跡を大局的な地磁気とドリフト誤差から修正
			for (int i = 0; i < queue.size(); i++) {
				SensorMap data = queue.get(i);
				/* 方向推定 */
				/* -data.vector(SensorName.CUM_GYRO).z) => Mi = R(-θi)M't
				 * i歩後の地磁気ベクトルに直す */
				// 計算しておいたドリフト誤差を補正	    		
	    		
				double angleRad = meanRad + (data.vector(SensorName.CUM_GYRO).z - drift);
//				Log.e("angleDeg", Math.toDegrees(angleRad)+"");
				subDirection = INIT_Y.rotate(0, 0, angleRad).normalize();
				// 横向き区間は進行方向の回転無し
				if (!isSidewaySection(posTimes.get(i))) {
					/* 暫定決定Ver */
					// i番目の進行方向を計算
					// meadRad最新の大局的な地磁気,i番目の累積回転量 - drift => driftはドリフト誤差を示す傾き
					direction = subDirection.clone();
				}

				// 見当違いVer
//	    		direction = INIT_Y.rotate(0, 0, meanRad - data.vector(SensorName.CUM_GYRO).z - drift).normalize();
				directions.add(direction.clone());
				subDirections.add(subDirection.clone());
				/* 気圧による高度推定 */
    			cp = new Vector3D(cp.x, cp.y, 287*290/9.81 * Math.log(data.vector(SensorName.PRESSURE).x / basePrss.x));
    			Log.e("", cp.z + " m");
				cp = cp.plusCreate(direction.multCreate(1.66f * 0.46f));
				subCP = subCP.plusCreate(subDirection.multCreate(1.66f * 0.46f));
				/* 経路決定 */
				positions.add(cp);
				subPositions.add(subCP);
			}
//			for (int k = 1; k < directions.size(); k++) {
//				double inner = directions.get(k).innerProduct(directions.get(k - 1));
//				double cos = inner / (directions.get(k).length() * directions.get(k - 1).length());
//				Log.e("", "" + Math.toDegrees(Math.acos(cos)));
//			}
			
			// TODO ここでdirectionsの履歴を見て横向いている間に回転しているかチェック
			// sideLookStartTimeArray, sideLookEndTimeArray
			for (int i = sideLookStartTimeArray.size() - 1; i >= 0; i--) {
				long start = sideLookStartTimeArray.get(i);
				long end = sideLookEndTimeArray.get(i);
				if (walkStatusArray.get(i) != 2) continue; // FRONT状態のみ角度判定
				for (int k = 1; k < directions.size(); k++) {
					if (posTimes.get(k) > start && posTimes.get(k) < end) {
						double inner = directions.get(k).innerProduct(directions.get(k - 1));
						double cos = inner / (directions.get(k).length() * directions.get(k - 1).length());
//						Log.e("", "" + Math.toDegrees(Math.acos(cos)));
						if (Math.toDegrees(Math.acos(cos)) < 5) { // 5°未満
							// 毎ステップ5度未満なら普通のカーブ
							startTimeArray.remove(i);
							endTimeArray.remove(i);
							sideLookStartTimeArray.remove(i);
							sideLookEndTimeArray.remove(i);
							walkStatusArray.remove(i);
							break;
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	/** 横向き区間検出
	 * @param time 対象時刻 */
	public boolean isSidewaySection(long time) {
		for (int i = 0; i < startTimeArray.size(); i++) {
			if (startTimeArray.get(i) < time && time < endTimeArray.get(i)) {
				return true;
			}
		}
		return false;
	}
	
	/** ドリフト誤差の計算 */
	public void calcDrift(Vector3D mgnt, Vector3D gravity) {
		/* (∑(i*φ) - φ0) / ∑i^2 */
		// ジャイロ回転後地磁気方位角
    	float radian = calcRadianWithGravity(right, calcEast(mgnt.rotate(cum_gyro, 0, 0), gravity), gravity);
//    	double deg = Math.toDegrees(radian);
    	if(firstY == 0.0) {
    		firstY = radian;
        	xySum += count * radian;
        	xSumPow += count * count;
         	drift = 0.0; // 初回は1データのみなのでドリフト誤差0
        	count++;
    		return;
    	}
    	// 最小二乗による傾きα(=drift)を求める
    	xySum += count * radian;
    	xSumPow += count * count;
    	// 長時間計測の場合のみ, ドリフト誤差を反映
    	if (IS_DRIFT_ADAPT && count > DRIFT_ADAPT_COUNT) drift = -1 * (xySum - firstY) / xSumPow;
    	else drift = 0.0;
    	count++;
	}
	
	@Override
	public void init(){
		super.init();
//		super.init(threshTime, threshValue);
//		buff = new LinkedList<SensorMap>();
		
		cum_gyro = 0;
		drift = 0;
		count = 0;
		firstY = 0;
		xySum = 0;
		xSum = 0;
		ySum = 0;
		xSumPow = 0;
		meanRad = 0.0;
		meanMgnt = new Vector3D();
		first = true;
		basePrss = null;
		lowPrss = null;
		if (queue != null) queue.clear();
		if (startTimeArray != null) startTimeArray.clear();
		if (endTimeArray != null) endTimeArray.clear();
		if (sideLookStartTimeArray != null) sideLookStartTimeArray.clear();
		if (sideLookEndTimeArray != null) sideLookEndTimeArray.clear();
		prevMilliTime = 0;
	}
}
