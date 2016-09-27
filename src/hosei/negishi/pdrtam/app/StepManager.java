package hosei.negishi.pdrtam.app;

import android.util.Log;

/***
 * 
 * @author admin
 * 
 * 初期値
 * 	private double staticThresh;
 */
public class StepManager {
	// 1つ前の3軸合成加速度
	private double oldAccl;
	private long oldTime;
	/** 極大値 */
	private double posPeakAccl;
	/** 極小値 */
	private double negPeakAccl;
	private long peakTime;
//	private int stepCnt;
	private boolean isUp;
	/** 最初の一歩を歩いたか */
	private boolean first;
	/** 今回歩行したか */
	public boolean isPeak;
	
	/** 歩行判定量閾値</br>
	 * ピークとのその一歩先との差が閾値以上なら歩行判定 */
//	private final float thresh = 0.35f;
	/** 歩行判定量閾値</br>
	 * 逆ピークとの差が一定以上ならピーク判定 */
	private final float peakThresh = 1.2f;
	/** 静止状態の閾値 */
	private double staticThresh;
	/** 歩行判定時間閾値
	 * 500ms */
//	private final long threshTime = 500 * 1000000; // ナノ秒
	private final long threshTime = 500; // ミリ秒
	
	public StepManager() {
		init();
	}
	
	/** 歩行判定 */
	public void determineWalking(double acclVal, long time) {
		isPeak = false;
		double slope = acclVal - oldAccl;
		/* 傾きが上方向かつ次で下方向へ向かう場合 */
		boolean isTimeProg = threshTime < (time - peakTime) || !first;
//		Log.e("", "isUp = " + isUp);
//		Log.e("", "slope = " + slope);
//
//		Log.e("", "threshTime = " + threshTime);
//		Log.e("", "(time - peakTime) = " + (time - peakTime));
//		Log.e("", "first = " + first);
//		
//		Log.e("", "isTimeProg = " + isTimeProg);
//		Log.e("", "peakThresh = " + peakThresh);
//		Log.e("", "acclVal = " + acclVal);
//		Log.e("", "oldAccl = " + oldAccl);
//		Log.e("", "negPeakAccl = " + negPeakAccl);
//		Log.e("", "staticThresh = " + staticThresh);
		
		if(isUp && slope < 0 && isTimeProg && peakThresh < oldAccl - negPeakAccl && staticThresh < oldAccl) {
			peakTime = oldTime;
			posPeakAccl = oldAccl;
			isUp = false;
			if (!first) first = true;
			isPeak = true;
		}
		/* 傾きが下方向かつ次で上方向へ向かう場合 */
		else if(!isUp && 0 < slope && peakThresh < posPeakAccl - oldAccl && staticThresh > oldAccl) {
			negPeakAccl = oldAccl;
			isUp = true;
			if (!first) first = true;
			isPeak = false;
		}
		oldAccl = acclVal;
		oldTime = time;
//		Log.e("", "isUp = " + isUp);
//		Log.e("", "isPeak = " + isPeak);
	}
	
	public void setInit(long time, double acclVal) {
		oldTime = time;
		oldAccl = acclVal;
		peakTime = time;
		
		/* ±ピーク&静止状態の初期値->適当...もっと良い方法がある? */
//		posPeakAccl = acclVal;
//		negPeakAccl = acclVal;
//		staticThresh = acclVal;
		
		posPeakAccl = acclVal;
		if (acclVal < 9.8)
			posPeakAccl = 9.8;
		negPeakAccl = acclVal;
		if (acclVal > 9.8)
			negPeakAccl = 9.8;
		staticThresh = 9.8;
		
//		posPeakAccl = 14;
//		negPeakAccl = 8.6;
//		staticThresh = 9.8;
	}
	
//	public int getStepCnt() {
//		return stepCnt;
//	}
	
	public double getOldVNorm() {
		return oldAccl;
	}
	
	public void init() {
		oldAccl = 0;
		oldTime = 0;
		posPeakAccl = 0;
		negPeakAccl = 0;
		peakTime = 0;
//		stepCnt = 0;
		isUp = true;
		first = false;
		isPeak = false;
	}
}
