package hosei.negishi.pdrtam.view;

import hosei.negishi.pdrtam.dr_model.DeadReckoning;
import hosei.negishi.pdrtam.model.Vector3D;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * 移動経路表示用View
 * @author negishi */
public class PathView extends View{
	
	private DeadReckoning dr;
	private Paint paint;
	/** 画面スクロール位置X */
	private int eyeX;
	/** 画面スクロール位置Y */
	private int eyeY;
	/** 画面タッチジェスチャー制御 */
	private GestureDetector gestureDetector;
	/** 画面タッチ(拡大)ジェスチャー制御 */
	private ScaleGestureDetector scaleGestureDetector;

	public PathView(Context context, AttributeSet attrs) {
        super(context, attrs);
        eyeX = 0;
        eyeY = 0;
        paint = new Paint();
        gestureDetector = new GestureDetector(context, new GestureListener());
    }
	
	@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
    	float scale = 20; // スケール
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        int centerW = w / 2;
        int centerH = h / 2;
        int interval = 5; // スケール1時のグリッド間隔
        
        // 背景
        canvas.drawColor(Color.WHITE);
        // グリッド線描画
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        // 縦線
		double size = w / (float)(interval * scale) / 2;
//		Log.e("", ""+eyeX + "  size:"+size);
		for(int i = 0; i < size + 1; i++){
			float x = centerW - eyeX + (i * interval) * scale;
			canvas.drawLine(x, 0, x, h, paint);
			x = centerW - eyeX + (-i * interval) * scale;
			canvas.drawLine(x, 0, x, h, paint);
		}
        // 横線
		size = h / (float)(interval * scale) / 2;
		for(int i = 0; i < size + 1; i++){
			float y = centerH - eyeY + (i * interval) * scale;
			canvas.drawLine(0, y, w, y, paint);
			y = centerH - eyeY + (-i * interval) * scale;
			canvas.drawLine(0, y, w, y, paint);
		}
		// 外枠線描画
		paint.setColor(Color.BLUE);
		paint.setStrokeWidth(7);
		canvas.drawLine(0, 3, w, 3, paint); // 上
		canvas.drawLine(0, h-3, w, h-3, paint); // 下
		canvas.drawLine(3, 0, 3, h, paint); // 左
		canvas.drawLine(w-3, 0, w-3, h, paint); // 右
		
		// 初期位置
		paint.setColor(Color.RED);
		canvas.drawCircle(centerW - eyeX, centerH - eyeY, 10, paint);
		// 過去移動軌跡を描画
		if (dr != null) {
        	ArrayList<Vector3D> pos = dr.positions;
            for (Vector3D p : pos) {
            	canvas.drawCircle((float)(p.x * scale) + centerW - eyeX, -(float)(p.y * scale) + centerH - eyeY, 10, paint);
            }
        }
    }
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		// 複数タッチポイントの場合scaleGesture
		if(e.getPointerCount() == 1)
			gestureDetector.onTouchEvent(e);
//		else
//			scaleGestureDetector.onTouchEvent(e);
		return true;
	}
		
	public void setDR(DeadReckoning d) {
		dr = d;
	}
	
	/**
	 * ジェスチャーリスナー<br>
	 * 画面移動用
	 * @author negishi */
	private final class GestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			// 始点
			float startX = e1.getX();
			float startY = e1.getY();
			// 終点
			float endX = e2.getX();
			float endY = e2.getY();
			
			// タッチ座標に対する移動割合
			float val = 0.04f;
			eyeX += (startX - endX) * val;
			eyeY += (startY - endY) * val;
			invalidate();
			return false;
		}
		
		@Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//			// 始点
//			float startX = e1.getX();
//			float startY = e1.getY();
//			// 終点
//			float endX = e2.getX();
//			float endY = e2.getY();
//			
//			// タッチ座標に対する移動割合
//			float val = 0.7f;
//			eyeX -= (startX - endX) * val;
//			eyeY -= (startY - endY) * val;
//			invalidate();
			return false;
		}
	}
}
