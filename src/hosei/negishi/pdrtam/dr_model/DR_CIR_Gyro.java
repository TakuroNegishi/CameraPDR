package hosei.negishi.pdrtam.dr_model;

import hosei.negishi.pdrtam.model.SensorData;
import hosei.negishi.pdrtam.model.SensorMap;
import hosei.negishi.pdrtam.model.SensorName;
import hosei.negishi.pdrtam.model.Vector3D;
import hosei.negishi.pdrtam.utils.Filter;

import java.util.LinkedList;

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
	public LinkedList<SensorMap> queue;

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

	
	public DR_CIR_Gyro() {
		super();
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
		
		double rad = calcRadianWithRodrigues(gyroData.getVector().multCreate((gyroData.getTime() - formerTime)/1e9f), gravity.normalize());
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
	    	directions.clear();
	    	cp = new Vector3D();
	    	positions.add(cp);
	    	for(SensorMap data : queue){
	    		/* 方向推定 */
	    		/* -data.vector(SensorName.CUM_GYRO).z) => Mi = R(-θi)M't
	    		 * i歩後の地磁気ベクトルに直す */
	    		// 計算しておいたドリフト誤差を補正	    		
	    		
	    		// 暫定決定Ver
	    		direction = INIT_Y.rotate(0, 0, meanRad + (data.vector(SensorName.CUM_GYRO).z - drift)).normalize();
	    		
	    		// 見当違いVer
//	    		direction = INIT_Y.rotate(0, 0, meanRad - data.vector(SensorName.CUM_GYRO).z - drift).normalize();
	    		directions.add(direction.clone());
	    		/* 気圧による高度推定 */
//    			cp = new Vector3D(cp.x, cp.y, 0 - (287*290/9.81 * Math.log(data.vector(SensorName.PRESSURE).x / basePrss.x)));
	    		cp = new Vector3D(cp.x, cp.y, 0);
	    		/* 経路決定 */
        		cp.plus(direction.multCreate(1.66f * 0.46f));
    	    	positions.add(cp);
	    	}
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
    	// 最小二乗による傾きαを求める(=drift)
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
		
		queue = new LinkedList<SensorMap>();
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
		queue.clear();
	}
}
