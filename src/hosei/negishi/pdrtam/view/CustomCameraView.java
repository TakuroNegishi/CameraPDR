package hosei.negishi.pdrtam.view;

import hosei.negishi.pdrtam.utils.Config;

import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.AttributeSet;
import android.util.Log;

/**
 * JavaCameraViewの拡張クラス
 */
public class CustomCameraView extends JavaCameraView {


	public CustomCameraView(Context context, int cameraId) {
		super(context, cameraId);
	}
	
	public CustomCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	/**
	 * カメラプレビューのFPSを設定
	 * @param min 最低FPS
	 * @param max 最大FPS
	 */
	public void setPreviewFPS(double min, double max){
		if (mCamera != null) {
			Parameters params = mCamera.getParameters();
			params.setPreviewFpsRange((int)(min * 1000), (int)(max * 1000));
			mCamera.setParameters(params);
		}
	}
	
	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT);
	}
	
	public void check() {
		Parameters params = mCamera.getParameters();
		List<int[]> a = params.getSupportedPreviewFpsRange();
		for (int[] is : a) {
			StringBuilder sb = new StringBuilder();
			for (int i : is) {
				sb.append(i + ",");
			}
			Log.e("", "[" + sb.toString() + "]");
		}		
	}
}
