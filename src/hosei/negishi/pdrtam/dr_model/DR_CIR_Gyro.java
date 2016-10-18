package hosei.negishi.pdrtam.dr_model;

import hosei.negishi.pdrtam.R;
import hosei.negishi.pdrtam.app.MainActivity;
import hosei.negishi.pdrtam.app.NativeAccesser;
import hosei.negishi.pdrtam.model.SensorData;
import hosei.negishi.pdrtam.model.SensorMap;
import hosei.negishi.pdrtam.model.SensorName;
import hosei.negishi.pdrtam.model.Vector3D;
import hosei.negishi.pdrtam.utils.Filter;

import java.util.ArrayList;

import android.widget.TextView;

/**
 * 連続区間遡及型自律的経路修正手法(Continuous interval recourse)
 * 大局的な地磁気情報を使って、過去全経路を修正する手法
 * +ドリフト誤差を計算する
 * ジャイロベース
 * @author negishi
 */
public class DR_CIR_Gyro extends DeadReckoning {
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
	TextView logText;
	long prevMilliTime;
	
	Vector3D subDirection;
	
	public DR_CIR_Gyro() {
		super();
		queue = new ArrayList<SensorMap>();
		startTimeArray = new ArrayList<Long>();
		endTimeArray = new ArrayList<Long>();
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
			gravity = INIT_Z;
			sensorMap.put(SensorName.GRVT, new SensorData(0, gravity));
		}
		right = INIT_Y.exteriorProduct(gravity);
		
		/* ジャイロの回転 */
		if(!sensorMap.sensorData().containsKey(SensorName.GYRO)) return false;
		SensorData gyroData = sensorMap.sensorData().get(SensorName.GYRO);
		
		if(first){
			formerTime = gyroData.getTime();
			first = false;
		}
		
		// 1e3f はmilliからsecへの変換
		double rad = calcRadianWithRodrigues(gyroData.getVector().multCreate((gyroData.getTime() - formerTime)/1e3f), gravity.normalize());
//		double rad = calcRadianWithRodrigues(gyroData.getVector().multCreate((gyroData.getTime() - formerTime)/1e9f), gravity.normalize());
		cum_gyro += rad;
		
		/* ジャイロの積分値として保存 */
		sensorMap.put(SensorName.CUM_GYRO, new SensorData(gyroData.getTime(), new Vector3D(0, 0, cum_gyro)));
		formerTime = gyroData.getTime();
		
		/* ドリフト誤差の計算 */
		if(!sensorMap.sensorData().containsKey(SensorName.MGNT)) return false;
		calcDrift(sensorMap, gravity);
		
		/* 地磁気の平均処理 */
		meanMgnt.plus(sensorMap.vector(SensorName.MGNT).rotate(0, 0, cum_gyro - drift)); // 基準を合わせる;

//		Log.e("CUM_GYRO", "cum_gyro = " + cum_gyro);
//		Log.e("DRIFT", "drift = " + drift);
		
    	// 平均地磁気と無回転時の進行方向との角度を算出
    	meanRad = calcRadianWithGravity(right, calcEast(meanMgnt, gravity), gravity);
//    	Log.e("LOG", "meaRad = " + Math.toDegrees(meanRad) + "°");
    	
		/* 加速度初期化 */
		if(lowAccl == null) {
			lowAccl = accl.getVector(); // 初回実行時
			stepMg.setInit(accl.getTime(), accl.getVector().innerProduct(gravity)); // 初期値
		}
		
		/* 加速度フィルター */
		lowAccl = Filter.lowPass(lowAccl, accl.getVector());		
    	
    	/* 高さの推定 */
//    	if(sensorMap.sensorData().containsKey(SensorName.PRESSURE)){
//    		prss = sensorMap.sensorData().get(SensorName.PRESSURE);
//    		if(lowPrss == null) lowPrss = sensorMap.vector(SensorName.PRESSURE).clone();
//    		if(basePrss == null) basePrss = sensorMap.vector(SensorName.PRESSURE).clone();
//    		/* ローパスをかけて適用 */
//    		lowPrss = Filter.lowPass(lowPrss, sensorMap.vector(SensorName.PRESSURE), 0.01f);
//    	} else {
//    		basePrss = INIT_X;
//    		lowPrss = INIT_X;
//    	}
//		sensorMap.put(SensorName.PRESSURE, new SensorData(0, lowPrss));
	    
	    /* 鉛直成分による歩行判定 */
		stepMg.determineWalking(lowAccl.innerProduct(gravity), accl.getTime());
		
		if(stepMg.isPeak) {
			stepCnt++;
			/* 経路推定ポイントにデータを追加 */
			SensorMap copy = new SensorMap();
			copy.sensorData().putAll(sensorMap.sensorData());
			queue.add(copy);
			// 時刻保存
			posTimes.add(milliTime);
			/* 現在までの推定経路を一旦リフレッシュ */
			positions.clear();
			subPositions.clear();
			directions.clear();
			subDirections.clear();
			cp = new Vector3D();
//			positions.add(cp); // 初期位置追加
			
			// 横向き歩きの区間取得 
			if (prevMilliTime == 0) {
				prevMilliTime = milliTime;
			} else if ((milliTime - prevMilliTime) >= 2000) {
				// 2sec間隔でチェック
				long[] startEndTime = NativeAccesser.getInstance().getTimeAry();
//				LEFT_VP = -1, RIGHT_VP = 1
				if (startEndTime[0] != 0 && startEndTime[1] != 0) {
					startTimeArray.add(startEndTime[0]);
					endTimeArray.add(startEndTime[1]);
				}
				logText.setText("s=" + startEndTime[0] + ", e=" + startEndTime[1] 
						+ ", count=" + startEndTime[2] + ",sideSt=" + startEndTime[3]);
				prevMilliTime = milliTime;
			}
			// 過去の移動軌跡を大局的な地磁気とドリフト誤差から修正
			for (int i = 0; i < queue.size(); i++) {
				SensorMap data = queue.get(i);
				/* 方向推定 */
				/* -data.vector(SensorName.CUM_GYRO).z) => Mi = R(-θi)M't
				 * i歩後の地磁気ベクトルに直す */
				// 計算しておいたドリフト誤差を補正	    		
	    		
				// 横向き区間は進行方向の回転無し
				if (!isSidewaySection(posTimes.get(i))) {
					/* 暫定決定Ver */
					// i番目の進行方向を計算
					// meadRad最新の大局的な地磁気,i番目の累積回転量 - drift => driftはドリフト誤差を示す傾き
					direction = INIT_Y.rotate(0, 0, meanRad + (data.vector(SensorName.CUM_GYRO).z - drift)).normalize();
				}
				subDirection = INIT_Y.rotate(0, 0, meanRad + (data.vector(SensorName.CUM_GYRO).z - drift)).normalize();

				// 見当違いVer
//	    		direction = INIT_Y.rotate(0, 0, meanRad - data.vector(SensorName.CUM_GYRO).z - drift).normalize();
				directions.add(direction.clone());
				subDirections.add(subDirection.clone());
				/* 気圧による高度推定 */
//    			cp = new Vector3D(cp.x, cp.y, 0 - (287*290/9.81 * Math.log(data.vector(SensorName.PRESSURE).x / basePrss.x)));
				cp = new Vector3D(cp.x, cp.y, 0);
				/* 経路決定 */
				subPositions.add(cp.plusCreate(subDirection.multCreate(1.66f * 0.46f)));
				cp.plus(direction.multCreate(1.66f * 0.46f));
				positions.add(cp);
			}
			return true;
		}
		return false;
	}
	
	/** 横向き区間検出
	 * @param time 対象時刻 */
	public boolean isSidewaySection(long time) {
		if (startTimeArray.size() < 1) return false;
		
		for (int i = 0; i < startTimeArray.size(); i++) {
//			if (endTimeArray.get(i) < time) continue;
//			else if (time < startTimeArray.get(i)) return false;
			if (startTimeArray.get(i) < time && time < endTimeArray.get(i))
				return true;
		}
		return false;
	}
	
	/** ドリフト誤差の計算 */
	public void calcDrift(SensorMap sensorMap, Vector3D gravity) {
		/* (∑(i*φ) - φ0) / ∑i^2 */
		// ジャイロ回転後地磁気方位角
    	float radian = calcRadianWithGravity(right, calcEast(sensorMap.vector(SensorName.MGNT).rotate(0, 0, cum_gyro), gravity), gravity);
//    	double deg = Math.toDegrees(radian);
    	if(firstY == 0.0) {
    		firstY = radian;
        	xySum += count * radian;
        	xSumPow += Math.pow(count, 2);
         	drift = 0.0; // 初回は1データのみなのでドリフト誤差0
        	count++;
    		return;
    	}
    	// 最小二乗による傾きα(=drift)を求める
    	xySum += count * radian;
    	xSumPow += Math.pow(count, 2);
     	drift = (xySum - firstY) / xSumPow;
    	count++;
		
//		Log.e("LOG", "radian = " + radian);
//		Log.e("LOG", "drift = " + drift);
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
//		basePrss = null;
//		lowPrss = null;
		if (queue != null) queue.clear();
		if (startTimeArray != null) startTimeArray.clear();
		if (endTimeArray != null) endTimeArray.clear();
		prevMilliTime = 0;
	}
}
