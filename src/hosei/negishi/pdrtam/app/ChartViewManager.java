package hosei.negishi.pdrtam.app;

import hosei.negishi.pdrtam.R;
import hosei.negishi.pdrtam.dr_model.DeadReckoning;
import hosei.negishi.pdrtam.view.PathView;
import android.app.Activity;
import android.widget.TextView;

/**
 * グラフビュー制御クラス
 * @author 
 */
public class ChartViewManager {
	
//	public DeadReckoning deadReckoning;
	TextView stepText;
	TextView drNameText;
	PathView views;
	
	public ChartViewManager() {
		MainActivity activity = MainActivity.getActivity();
		stepText = (TextView) activity.findViewById(R.id.step_text);
//		drNameText = (TextView) mainActivity.findViewById(R.id.drText);
		views = (PathView) activity.findViewById(R.id.pathView);
		init();
	}
	
	public void init() {
		// DR名の表示
//		drNameText.setText(mainActivity.sa.dr.toString());
		updateWindow(null);
	}
	
	// 画面更新
	public void updateWindow(DeadReckoning dr) {
		// グラフ描画
		views.setDR(dr);
		views.invalidate();

		// TextView更新
		if (dr != null)
			stepText.setText(dr.getStepCount() + " step");
	}
}
