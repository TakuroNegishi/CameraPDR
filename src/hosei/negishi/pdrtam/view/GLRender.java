package hosei.negishi.pdrtam.view;

import hosei.negishi.pdrtam.model.Triangle;
import hosei.negishi.pdrtam.model.Vector3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;

/**
 * OpenGL用のRender
 */
public class GLRender implements GLSurfaceView.Renderer {
	private static final int MOVE_THRESHOLD = 70;
	private static final int PINCH_RATIO = 22;
	private static final float ROTATE_RATIO = 0.6f;
	
	/** アスペクト比 */
	float aspect = 0.0f;
	/** カメラ座標 */
	Vector3D eye;
	/** カメラ注視座標 */
	Vector3D center;
	/** 3軸回転量 */
	Vector3D rotate;

	float prevTouchX = 0.0f;
	float prevTouchY = 0.0f;
	private float[] originalVertex;
	private List<Triangle> triangleList;
	float scale = 1.0f;
	Bitmap textureImage;
	int texWidth;
	int texHeight;
	// private Resources res;
	private int textureID;
	int screenW;
	int screenH;

	public GLRender() {
		originalVertex = new float[0];
//		eye = new Vector3D(30.0f, 30.0f, 30.0f);
		eye = new Vector3D(0.0f, 0.0f, 30.0f);
		center = new Vector3D(0.0f, 0.0f, 0.0f);
		rotate = new Vector3D(0.0f, 0.0f, 0.0f);
//		rotAxis = new Vector3D(0.0f, 0.0f, 0.0f);
	}

	public void setVertex(float[] vertex) {
		this.originalVertex = vertex;
		centering();
	}

	public void setTextureImage(Bitmap img) {
		this.textureImage = img;
	}

	public void setTriangleList(List<Triangle> triangleList) {
		this.triangleList = triangleList;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

	}

	/** 画面サイズ変更時 */
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// 左下が(0,0)
		// width, height(画面いっぱいまで表示に使う)
		this.screenW = width;
		this.screenH = height;
		gl.glViewport(0, 0, width, height);
		aspect = (float) width / (float) height;

		// ! 深度バッファを有効にする
		gl.glEnable(GL10.GL_DEPTH_TEST);
		/*
		 * 
		 * texWidth = textureImage.getWidth(); texHeight =
		 * textureImage.getHeight();
		 * 
		 * //テクスチャバッファの成分を有効にする。 gl.glEnable(GL10.GL_TEXTURE_2D);
		 * //テクスチャ用メモリを指定数確保（ここではテクスチャ１枚） int[] buffers = new int[1];
		 * //１．一度に確保するテクスチャの枚数を指定 //２．確保したテクスチャの管理番号を書き込む為の配列を指定。
		 * //３．オフセット値：配列の何番目からに書き込むかの指定 gl.glGenTextures(1, buffers, 0);
		 * //テクスチャ管理番号を保存する textureID = buffers[0]; //テクスチャ情報の設定
		 * //１．GL_TEXTURE_2D　を指定。 //２．テクスチャ管理番号
		 * gl.glBindTexture(GL10.GL_TEXTURE_2D, textureID);
		 * //ビットマップのメモリからテクスチャへ転送。 //１．GL_TEXTURE_2D　を指定。
		 * //２．ミップマップレベル（２Dには関係なし） //３．画像を格納したbitmapを指定 //４．テクスチャ境界（常に０で良い）
		 * GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, textureImage, 0); //フィルタ
		 * //画像の引き伸ばしの方法を指定する必要がある（デフォルトで指定されていない） //１．GL_TEXTURE_2D：２Dのテクスチャ
		 * //２．拡大時：GL_TEXTURE_MAG_FILTER　縮小時：GL_TEXTURE_MIN_FILTER
		 * //３．GL_NEAREST：処理対象に最も近いピクセルの色を参照する
		 * gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
		 * GL10.GL_NEAREST); gl.glTexParameterf(GL10.GL_TEXTURE_2D,
		 * GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST); //bitmapを破棄
		 * textureImage.recycle();
		 */
	}

	@Override
	public void onDrawFrame(GL10 gl) {
//		 ATAMから点群を取得
//		NativeAccesser.getInstance().getPointAry();
		
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		// カメラ転送
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluPerspective(gl, 45.0f, aspect, 0.01f, 200.0f);
		// カメラの位置, 注視位置, 上方向ベクトル
		GLU.gluLookAt(gl, eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f,
				1.0f, 0.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		// 3軸描画
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY); // テクスチャ無効化
		float[] c = { 0.0f, 0.0f, 1.0f, 1.0f };
		drawAxis(gl, c);
		
		// 指定行列を生成
//		rotateX += rotateVX;
//		rotateY += rotateVY;
//		rotateZ += rotateVZ;
		gl.glRotatef(rotate.x, 1, 0, 0);
		gl.glRotatef(rotate.y, 0, 1, 0);
		gl.glRotatef(rotate.z, 0, 0, 1);
		
//		gl.glRotatef(0.1f, rotAxis.x, rotAxis.y, rotAxis.z);

		// 3軸描画
		c[0] = 0.0f; c[1] = 0.0f; c[2] = 1.0f; c[3] = 1.0f;
		drawAxis(gl, c);

		// 点群描画
		// float[] scaledVertex = getScaledVertex(originalVertex);
		c[0] = 1.0f;
		c[1] = 1.0f;
		c[2] = 1.0f;
		drawPoints(gl, originalVertex, c);

		// 最初の1点
		// float[] firstVertex = {scaledVertex[0], scaledVertex[1],
		// scaledVertex[2]};
		// c[0] = 1.0f; c[1] = 0.0f; c[2] = 0.0f;
		// drawPoints(gl, firstVertex, c);
		//
		// // 2点目
		// float[] secondVertex = {scaledVertex[3], scaledVertex[4],
		// scaledVertex[5]};
		// c[0] = 0.0f; c[1] = 1.0f; c[2] = 0.0f;
		// drawPoints(gl, secondVertex, c);
		//
		// // 3点目
		// float[] thirdVertex = {scaledVertex[6], scaledVertex[7],
		// scaledVertex[8]};
		// c[0] = 0.0f; c[1] = 0.0f; c[2] = 1.0f;
		// drawPoints(gl, thirdVertex, c);

		// テクスチャ描画
		// float[] vertices = new float[12];
		// for (int i = 0; i < scaledVertex.length; i+=3) {
		// // 3次元×4点=12
		// if (i + 12 >= scaledVertex.length) break;
		// for (int j = 0; j < vertices.length; j++) {
		// vertices[j] = scaledVertex[i+j];
		// }
		// drawTexture(gl, vertices, textureID, 0.0f, 0.0f, 1.0f, 1.0f);
		// }

		// テクスチャテスト
		// 左下, 右下, 左上, 右上
		// float[] vertices = {-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f,
		// 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
		// drawTexture(gl, vertices, textureID, 0.0f, 0.0f, 1.0f, 1.0f);
		// gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		// drawTriangleTexture(gl, scaledVertex);
	}
	
	/** 3軸描画 */
	public void drawAxis(GL10 gl, float[] c) {
		float one = 25.0f;
		float[] v = { -one, 0.0f, 0.0f, one, 0.0f, 0.0f };
		drawLine(gl, v, c); // 青:X軸
		v[0] = 0.0f;
		v[3] = 0.0f;
		v[1] = -one;
		v[4] = one;
		c[2] = 0.0f;
		c[1] = 1.0f;
		drawLine(gl, v, c); // 緑:Y軸
		v[1] = 0.0f;
		v[4] = 0.0f;
		v[2] = -one;
		v[5] = one;
		c[1] = 0.0f;
		c[0] = 1.0f;
		drawLine(gl, v, c); // 赤:Z軸
	}

	public void drawTriangleTexture(GL10 gl, float[] scaledVertex) {
		gl.glEnable(GL10.GL_TEXTURE_2D); // テクスチャ機能の有効化
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureID); // テクスチャオブジェクトの指定
		// 　ポリゴンの頂点の色
		// float[] colors = {
		// 1.0f, 1.0f, 1.0f, 1.0f,
		// 1.0f, 1.0f, 1.0f, 1.0f,
		// 1.0f, 1.0f, 1.0f, 1.0f
		// };

		for (int i = 0; i < triangleList.size(); i++) {
			Triangle triangle = triangleList.get(i);
			if (triangle.id1 * 3 + 2 >= scaledVertex.length
					|| triangle.id2 * 3 + 2 >= scaledVertex.length
					|| triangle.id3 * 3 + 2 >= scaledVertex.length)
				continue;
			// 3点(x1, y1, z1)(x2, y2, z2)(x3, y3, z3)の順で入っている
			// (i=0) [0] [1] [2] [3] [4] [5] [6] [7] [8]
			// trianggle.id1=三角形の1つ目の点のid
			// scaledVertex, originalVertexのインデックスに対応
			float[] vertices = { scaledVertex[triangle.id1 * 3],
					scaledVertex[triangle.id1 * 3 + 1],
					scaledVertex[triangle.id1 * 3 + 2],
					scaledVertex[triangle.id2 * 3],
					scaledVertex[triangle.id2 * 3 + 1],
					scaledVertex[triangle.id2 * 3 + 2],
					scaledVertex[triangle.id3 * 3],
					scaledVertex[triangle.id3 * 3 + 1],
					scaledVertex[triangle.id3 * 3 + 2] };
			// マッピング座標
			float[] coords = { (float) (triangle.p1.x / texWidth),
					(float) (triangle.p1.y / texHeight),
					(float) (triangle.p2.x / texWidth),
					(float) (triangle.p2.y / texHeight),
					(float) (triangle.p3.x / texWidth),
					(float) (triangle.p3.y / texHeight) };
			// Log.e("x1", ""+coords[0]);
			// Log.e("y1", ""+coords[1]);
			// Log.e("x2", ""+coords[2]);
			// Log.e("y2", ""+coords[3]);
			// Log.e("x3", ""+coords[4]);
			// Log.e("y3", ""+coords[5]);
			FloatBuffer polygonVertices = makeFloatBuffer(vertices);
			// FloatBuffer polygonColors = makeFloatBuffer(colors);
			FloatBuffer texCoords = makeFloatBuffer(coords);

			// glVertexPointer(1頂点あたりのデータ数, データ型, オフセット, 頂点配列)
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, polygonVertices); // 確保したメモリをOpenGLに渡す
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY); // ポリゴン頂点座標のバッファをセットしたことをOpenGLに伝える
			// gl.glColorPointer(4, GL10.GL_FLOAT, 0, polygonColors); //
			// 確保したメモリをOpenGLに渡す
			// gl.glEnableClientState(GL10.GL_COLOR_ARRAY); //
			// ポリゴン頂点色のバッファをセットしたことをOpenGLに伝える
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texCoords); // 確保したメモリをOpenGLに渡す
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY); // マッピング座標のバッファをセットしたことをOpenGLに伝える

			// ポリゴンの描画には幾つか種類があり、引数で指定(GL10.GL_TRIANGLE_STRIP)
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 3); // ポリゴンの描画
		}
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY); // 描画が終わったら、テクスチャマッピング用のバッファをリセット
		gl.glDisable(GL10.GL_TEXTURE_2D); // テクスチャ機能の無効
	}

	// テクスチャを描画するためのメソッド
	public void drawTexture(GL10 gl, float[] vertices, int textureID, float u,
			float v, float tex_w, float tex_h) {
		// マッピング座標
		float[] coords = { u, v + tex_h, // 左下
				u + tex_w, v + tex_h, // 右下
				u, v, // 左上
				u + tex_w, v // 右上
		};

		// 　ポリゴンの頂点の色
		float[] colors = { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

		// OpenGLではVM上に確保したメモリ領域にアクセスできないため、
		// 作成した配列をシステムメモリに転送する必要がある。
		FloatBuffer polygonVertices = makeFloatBuffer(vertices);
		FloatBuffer polygonColors = makeFloatBuffer(colors);
		FloatBuffer texCoords = makeFloatBuffer(coords);

		gl.glEnable(GL10.GL_TEXTURE_2D); // テクスチャ機能の有効化
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureID); // テクスチャオブジェクトの指定(引数で取得する)

		// glVertexPointer(1頂点あたりのデータ数, データ型, オフセット, 頂点配列)
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, polygonVertices); // 確保したメモリをOpenGLに渡す
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY); // ポリゴン頂点座標のバッファをセットしたことをOpenGLに伝える
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, polygonColors); // 確保したメモリをOpenGLに渡す
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY); // ポリゴン頂点色のバッファをセットしたことをOpenGLに伝える
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texCoords); // 確保したメモリをOpenGLに渡す
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY); // マッピング座標のバッファをセットしたことをOpenGLに伝える

		// ポリゴンの描画には幾つか種類があり、引数で指定(GL10.GL_TRIANGLE_STRIP)
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4); // ポリゴンの描画

		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY); // 描画が終わったら、テクスチャマッピング用のバッファをリセット
		gl.glDisable(GL10.GL_TEXTURE_2D); // テクスチャ機能の無効化
	}

	public void setTextureArea(GL10 gl, int x, int y, int w, int h) {
		// float left = ((float)x / (float)textureImage.getWidth());
		// float top = ((float)y / (float)textureImage.getHeight());
		// float right = left + ((float)w / (float)textureImage.getWidth());
		// float bottom = top + ((float)h / (float)textureImage.getHeight());
		float uv[] = { 0.0f, 0.0f,// !< 左上
				0.0f, 1.0f,// !< 左下
				1.0f, 0.0f,// !< 右上
				1.0f, 1.0f,// !< 右下
		};

		FloatBuffer fb = makeFloatBuffer(uv);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, fb);
	}

	public void drawQuad(GL10 gl10, float leftTopX, float leftTopY,
			float leftTopZ, float leftBtmX, float leftBtmY, float leftBtmZ,
			float rightTopX, float rightTopY, float rightTopZ, float rightBtmX,
			float rightBtmY, float rightBtmZ) {
		// ! 位置情報
		float positions[] = {
				// ! x y z
				leftTopX, leftTopY, leftTopZ, // !< 左上
				leftBtmX, leftBtmY, leftBtmZ, // !< 左下
				rightTopX, rightTopY, rightTopZ, // !< 右上
				rightBtmX, rightBtmY, rightBtmZ, // !< 右下
		};

		// ! OpenGLはビッグエンディアンではなく、CPUごとのネイティブエンディアンで数値を伝える必要がある。
		// ! そのためJavaヒープを直接的には扱えず、java.nio配下のクラスへ一度値を格納する必要がある。
		FloatBuffer fb = makeFloatBuffer(positions);

		gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, fb);
		gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
	}

	/** 　線描画 */
	public void drawLine(GL10 gl, float[] vertex, float[] color) {
		FloatBuffer fb = makeFloatBuffer(vertex);
		gl.glLineWidth(5);
		gl.glColor4f(color[0], color[1], color[2], color[3]);
		// 頂点座標を有効に
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		// 3=(x, y, z)のこと。2次元図形で(x, y)しか定義していない場合は、2
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, fb);
		// gl.glColorPointer(4, GL10.GL_UNSIGNED_BYTE, 0, color); //　カラーのセット
		gl.glDrawArrays(GL10.GL_LINES, 0, 2); // 　pointNumだけ描画する
	}

	/** 　点描画 */
	public void drawPoints(GL10 gl, float[] vertex, float[] color) {
		FloatBuffer fb = makeFloatBuffer(vertex);
		gl.glPointSize(15);
		gl.glColor4f(color[0], color[1], color[2], color[3]);
		// 頂点座標を有効
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		// 3=(x, y, z)のこと。2次元図形で(x, y)しか定義していない場合は、2
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, fb);
		gl.glDrawArrays(GL10.GL_POINTS, 0, vertex.length / 3); // 　pointNumだけ描画する
	}

	// システム上のメモリ領域を確保するためのメソッド
	public static final FloatBuffer makeFloatBuffer(float[] arr) {
		// float型の配列を入れるためのデータ領域を用意し、そこにデータを転送している。
		// 配列のサイズは[4 * 配列のサイズ]バイトとなっている。(float型=4byte)
		ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(arr);
		fb.position(0);
		return fb;
	}

	/** 点群が原点に来るように移動 */
	public void centering() {
		// 中心を求める
		float avgX = 0.0f;
		float avgY = 0.0f;
		float avgZ = 0.0f;
		for (int i = 0; i < originalVertex.length / 3; i++) {
			avgX += originalVertex[i * 3 + 0];
			avgY += originalVertex[i * 3 + 1];
			avgZ += originalVertex[i * 3 + 2];
		}
		int pointNum = originalVertex.length / 3;
		avgX /= pointNum;
		avgY /= pointNum;
		avgZ /= pointNum;

		// 点群が原点付近に来るように移動
		for (int i = 0; i < originalVertex.length / 3; i++) {
			originalVertex[i * 3 + 0] -= avgX;
			originalVertex[i * 3 + 1] -= avgY;
			originalVertex[i * 3 + 2] -= avgZ;
		}
	}

	/** 画面タッチ時 */
	public void onTouchDown(float touchX, float touchY) {
		// スケール
		// if (touchX > (screenW / 2)) {
		// scale += 0.02f;
		// } else {
		// scale -= 0.02f;
		// }
		// Log.e("scale=", ""+scale);

		// Log.e("width", "" + width);
		// Log.e("height", "" + height);
		// Log.e("x", "" + touchX);
		// Log.e("y", "" + touchY);
	}

	/** タッチしながら移動 */
	public void onTouchMove(float touchX, float touchY, long startTime, long nowTime) {
		// if (nowTime - startTime > 2000) {
		// if (touchX > (screenW / 2)) scale += 0.02f;
		// else scale -= 0.02f;
		// Log.e("scale=", ""+scale);
		// }
		
		// カメラ位置までのベクトルA(0, 0, z)と, タッチ位置までのベクトルB(touchX, touchY, 0)の外積が回転軸
//		float x = eyeY * 0 - eyeZ * touchY;
//		float y = eyeZ * touchX - eyeX * 0;
//		float z = eyeX * touchY - eyeY * touchX;
//		rotateVec = norm(x, y, z);
		
		float diffX = touchX - prevTouchX;
		float diffY = touchY - prevTouchY;
		if (diffX < MOVE_THRESHOLD && diffX > -MOVE_THRESHOLD) { // 瞬間移動を防ぐ
//			if (diffX < 1) diffX = 0;
			rotate.y += diffX * ROTATE_RATIO;
			Log.e("", "diffX = " + diffX);
		}
		if (diffY < MOVE_THRESHOLD && diffY > -MOVE_THRESHOLD) { // 瞬間移動を防ぐ
//			if (diffY < 1) diffY = 0;
			rotate.x -= diffY * ROTATE_RATIO;
			Log.e("", "diffY = " + diffY);
		}
		
		prevTouchX = touchX;
		prevTouchY = touchY;
	}

	/** タッチ離れた時 */
	public void onTouchUp(float touchX, float touchY) {
		prevTouchX = 0.0f;
		prevTouchY = 0.0f;
	}

	/** ピンチイン/アウト操作でカメラを前後に移動(ズーム) */
	public void onScale(float scale) {
		Vector3D dirVec = eye.normalize();
		if (scale > 1) {
			// 拡大はカメラを(0,0,0)に寄せるからマイナス方向
			dirVec.mult(-1);
		} else if (scale == 1.0f) { // 何もなし
			return;
		} // 縮小はカメラを(0,0,0)から遠ざけるからプラス方向=そのまま

		// ピンチイン/アウトの速度が速いとscaleが1よりも, 拡大なら大きく, 縮小なら小さくなる
		float rate = Math.abs(1.0f - scale) * PINCH_RATIO;
		dirVec.mult(rate);
		
		eye.plus(dirVec);
	}

//	/** スケール倍した頂点座標値を返す */
//	public float[] getScaledVertex(float[] v) {
//		float[] scaledVertex = new float[v.length];
//		for (int i = 0; i < v.length; i++) {
//			scaledVertex[i] = v[i] * scale;
//		}
//		return scaledVertex;
//	}
}
