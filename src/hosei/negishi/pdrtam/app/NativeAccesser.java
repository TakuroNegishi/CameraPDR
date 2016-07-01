package hosei.negishi.pdrtam.app;

import org.opencv.core.Mat;

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
	
	public void mainProc(Mat rgba, long nanoTime) {
		mainProcNative(rgba.getNativeObjAddr(), nanoTime);
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
	
	public float[] getPointAry() {
		float[] pointAry = new float[getPointLength()];
		getPointAryNative(pointAry.length, pointAry);
		return pointAry;
	}
	
	public int getPointLength() {
		return getPointLengthNative();
	}
	
	public void destroy() {
		// TODO
	}
	
	// nativeメソッド
	public native void initNative();
	public native void mainProcNative(long matAddrRgba, long nanoTime);
	public native void changeStateNative(boolean isSaveFrameImg);
	public native void setStopNative();
	public native void setResetNative();
	public native void getPointAryNative(int num, float[] pointAry);
	public native int getPointLengthNative();
}
