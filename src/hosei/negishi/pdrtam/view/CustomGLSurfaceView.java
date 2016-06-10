package hosei.negishi.pdrtam.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

public class CustomGLSurfaceView extends GLSurfaceView implements
		OnScaleGestureListener {
	
	private ScaleGestureDetector mScaleDetector;
	private GLRender glRender;

	public CustomGLSurfaceView(Context context) {
		super(context);
		mScaleDetector = new ScaleGestureDetector(context, this);
		glRender = new GLRender();
		setRenderer(glRender);
	}
	
	public CustomGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mScaleDetector = new ScaleGestureDetector(context, this);
		glRender = new GLRender();
		setRenderer(glRender);
	}

	public void setVertex(float[] vertex) {
		glRender.setVertex(vertex);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() < 2) {
			float x = event.getX();
			float y = event.getY();
			
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				glRender.onTouchDown(x, y);
				break;
			case MotionEvent.ACTION_MOVE:
				glRender.onTouchMove(x, y, event.getDownTime(), event.getEventTime());
				break;
			case MotionEvent.ACTION_UP:
				glRender.onTouchUp(x, y);
				performClick(); // どっかでperformClick()を呼ばないとworningが出る
				break;
			}
		}

		// 受けたMotionEventをそのままScaleGestureDetector#onTouchEvent()に渡す
		return mScaleDetector.onTouchEvent(event);
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		glRender.onScale(detector.getScaleFactor());
//		Log.e("", "onScale() " + detector.getScaleFactor());
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
//		Log.e("", "onScaleBegin() " + detector.getScaleFactor());
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
	}

	@Override
	public boolean performClick() {
		super.performClick();
		return true;
	}
	
	public void dispose() {
//		glRender.dispose();
		glRender = null;
	}
}
