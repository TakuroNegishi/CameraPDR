package hosei.negishi.pdrtam.app;

import hosei.negishi.pdrtam.utils.Config;
import hosei.negishi.pdrtam.view.CustomCameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.widget.Toast;

public class CvCameraManager implements CvCameraViewListener2 {
	
	/** カメラビューのインスタンス
	 * CameraBridgeViewBase は JavaCameraView/NativeCameraView のスーパークラス */
	private CameraBridgeViewBase cameraView;
	
	private Mat mRgba;
	private Context context;
//	private NativeAccesser mAtam;
	
	private final double MIN_FPS = 30.0;
	private final double MAX_FPS = 30.0;
	
	public CvCameraManager(Context context, CameraBridgeViewBase camera) {
		this.context = context;
		cameraView = camera;
		cameraView.setMaxFrameSize(Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT);
		cameraView.setCvCameraViewListener(this);
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// TODO ここでカメラ画像取得時間を文字列保存
		long milliTime = System.currentTimeMillis();
		mRgba = inputFrame.rgba();
		NativeAccesser.getInstance().mainProc(mRgba, milliTime);
		return mRgba;
	}
	
	@Override
	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		Log.e("", width + "x" + height);
		((CustomCameraView)cameraView).setPreviewFPS(MIN_FPS, MAX_FPS);;
	}

	@Override
	public void onCameraViewStopped() {
		mRgba.release();
	}
	
	public void onResume() {
		cameraView.enableView();
	}
	
	public void onPause() {
		if (cameraView != null) cameraView.disableView();
	}
	
	public void saveImageMat() {
		Mat m;
//		synchronized (Global.sharedResource) {
			m = new Mat(mRgba.height(), mRgba.width(), CvType.CV_8UC4);
			mRgba.copyTo(m);
//		}
		
		Bitmap dst = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(m, dst);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPANESE);
		String fileName = sdf.format(new Date()) + ".jpg";
		File file = new File(Environment.getExternalStorageDirectory() + "/" + "negishi.deadreckoning/" + fileName);
		if (!file.getParentFile().exists()) file.getParentFile().mkdir();

		try {
			FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
			dst.compress(CompressFormat.JPEG, 100, out);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			Log.e("", "" + e.getLocalizedMessage());
		} catch (IOException e) {
			Log.e("", "" + e.getLocalizedMessage());
		}
		Toast.makeText(context, fileName + "saved.", Toast.LENGTH_SHORT).show();
		
		// saveIndex
		ContentValues values = new ContentValues();
		ContentResolver contentResolver = context.getContentResolver();
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.TITLE, fileName);
		values.put("_data", file.getAbsolutePath());
		contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	}
}
