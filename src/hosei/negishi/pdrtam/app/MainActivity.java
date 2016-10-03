package hosei.negishi.pdrtam.app;

import hosei.negishi.pdrtam.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener,
		DialogInterface.OnClickListener {
	
	private SLAMEngine slam;
	public SensorAdapter sa;

	// ライブラリ初期化完了後に呼ばれるコールバック (onManagerConnected)
	// public abstract class BaseLoaderCallback implements
	// LoaderCallbackInterface
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			// 読み込みが成功したらカメラプレビューを開始
			case LoaderCallbackInterface.SUCCESS:
				slam.onResume();
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	private static Context appContext;
	private static MainActivity appActivity;
	
	public static Context getContext() {
		return appContext;
	}
	public static MainActivity getActivity() {
		return appActivity;
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// // バックライト常にON
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //
		// ステータスバー非表示
		// requestWindowFeature(Window.FEATURE_NO_TITLE); // タイトルバー非表示
	
		setContentView(R.layout.activity_main);
		appContext = getApplicationContext();
		appActivity = this;
		initTAMMode();
		initButtons();
	}

	private void initTAMMode() {
		slam = new SLAMEngine(this);
		sa = new SensorAdapter();
	}

	public void initButtons() {
		Button btn = (Button) findViewById(R.id.start_stop_btn);
		btn.setOnClickListener(this);
		btn = (Button) findViewById(R.id.save_start_stop_btn);
		btn.setOnClickListener(this);
		btn = (Button) findViewById(R.id.write_log_btn);
		btn.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// 非同期でライブラリの読み込み/初期化を行う
		// static boolean initAsync(String Version, Context AppContext,
		// LoaderCallbackInterface Callback)
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
		// OpenCVライブラリロード終了次第slam.onResume()でATAM開始
	}

	@Override
	public void onPause() {
		super.onPause();
		slam.onPause();
		sa.removeSensor(); // Listenerの登録解除
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		slam.onPause();
	}

	@Override
	public void onClick(View v) {
		if (!(v instanceof Button))
			return;
		switch (v.getId()) {
		case R.id.start_stop_btn:
			slam.changeState(false);
			Button startStopBtn = (Button) v;
			if (startStopBtn.getText().equals("Start")) {
				startStopBtn.setText("Stop");
				sa.setSensor(this); // Listenerの登録
			} else if (startStopBtn.getText().equals("Stop")) {
				startStopBtn.setText("Start");
				sa.removeSensor(); // Listenerの登録解除
			}
			break;
		case R.id.save_start_stop_btn:
			slam.changeState(true);
			Button saveStartStopBtn = (Button) v;
			if (saveStartStopBtn.getText().equals("Save Start")) {
				saveStartStopBtn.setText("Save Stop");
				sa.setSensor(this); // Listenerの登録
			} else if (saveStartStopBtn.getText().equals("Save Stop")) {
				saveStartStopBtn.setText("Save Start");
				sa.removeSensor(); // Listenerの登録解除
//				rescanSdcard(); // SDカード（端末内部）の画像をrescan
			}
			break;
		case R.id.write_log_btn:
			sa.writeLog(); // センサーデータ履歴出力
			break;
		}
	}
	
	public void rescanSdcard(){ 
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/negishi.deadreckoning/Feature Image/"))); 
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
//		if (fileChooser.isDirectory(which)) {
//			fileChooser.showFiles(which, this); // 選択したディレクトリ内のファイル表示
//		} else {
//			// 選択したファイルから点群情報を読み取る
//			String filePath = fileChooser.getFilePath(which);
//			fileChooser = null;
//			if (filePath.equals(""))
//				return;
//			float[] vertex = FileManager.readFile(filePath);
//			init3DViewer();
//			glSurfaceView.setVertex(vertex);
//			slam.onPause();
//			start3DViewer();
//		}
	}

//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		if (appMode != AppMode.MAP_VIEW)
//			return super.onTouchEvent(event);
//		glSurfaceView.onTouch(event);
//		return super.onTouchEvent(event);
//	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) { // Androidの戻るボタン押下→終了ダイアログ
			showFinishDialog();

//			switch (appMode) {
//			case TAM:
//				showFinishDialog();
//				break;
//			}
			return true;
		}
		return false;
	}

	/** アプリケーション終了確認ダイアログ表示 */
	private void showFinishDialog() {
		new AlertDialog.Builder(this)
				.setTitle("アプリケーションの終了")
				.setMessage("アプリケーションを終了しますか？")
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								MainActivity.this.finish();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

}
