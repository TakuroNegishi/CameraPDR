package hosei.negishi.pdrtam.app;

import org.opencv.core.Mat;
import android.util.Log;


/**
 * Naitiveアクセス用クラス
 * */
public class NativeAccesser {
	private static final NativeAccesser instance;
	
	static {
//		System.loadLibrary("gnustl_shared");
//		System.loadLibrary("stlport_shared");
		System.loadLibrary("ATAMNative");
		instance = new NativeAccesser();
	}
	
	
	public static NativeAccesser getInstance() {
        return instance;
    }
	
	private NativeAccesser() {
		init();
	}
	
	public void init() {
		initNative();
	}
	
	public void mainProc(Mat rgba, long milliTime) {
//		Log.e("camera", milliTime + " msec");
		mainProcNative(rgba.getNativeObjAddr(), milliTime);
	}
	
	public void changeState(boolean isSaveFrameImg) {
		changeStateNative(isSaveFrameImg);
	}
	
	public void setStop() {
		setStopNative();
	}
	
	public void setReset() {
		setResetNative();
	}
	
	public long[] getTimeAry() {
		long[] timeAry = new long[8];
		getTimeAryNative(timeAry);
		return timeAry;
	}
	
	public int getPointLength() {
		return getPointLengthNative();
	}
	
	public void destroy() {
		// TODO
	}
	
	// nativeメソッド
	public native void initNative();
	public native void mainProcNative(long matAddrRgba, long milliTime);
	public native void changeStateNative(boolean isSaveFrameImg);
	public native void setStopNative();
	public native void setResetNative();
	public native void getTimeAryNative(long[] timeAry);
	public native int getPointLengthNative();
}
