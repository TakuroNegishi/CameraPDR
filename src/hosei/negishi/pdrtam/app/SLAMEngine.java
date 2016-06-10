package hosei.negishi.pdrtam.app;

import hosei.negishi.pdrtam.R;

import org.opencv.android.CameraBridgeViewBase;

public class SLAMEngine {
	private CvCameraManager cameraManager;
//	private NativeAccesser atam;
	
	public SLAMEngine(MainActivity activity) {
//		atam = new NativeAccesser();
		NativeAccesser.getInstance(); // インスタンス生成
		cameraManager = new CvCameraManager(activity, (CameraBridgeViewBase) activity.findViewById(R.id.customizableCameraView1));
	}
	
	public void changeState(boolean isSaveFrameImg) {
		NativeAccesser.getInstance().changeState(isSaveFrameImg);
	}
	
	public void reset() {
		NativeAccesser.getInstance().setReset();
	}
	
	public void stop() {
		NativeAccesser.getInstance().setStop();
	}
	
	public void saveImage() {
		cameraManager.saveImageMat();
	}
	
//	public float[] getPoints3D() {
//		float[] pointAry = new float[NativeAccesser.getInstance().getPointLength()];
//		NativeAccesser.getInstance().getPointAry(pointAry.length, pointAry);
//		return pointAry;
//	}
	
	public void onResume() {
		cameraManager.onResume();	// カメラ開始
	}
	
	public void onPause() {
		stop();
		reset();
		cameraManager.onPause();
	}
}
