package hosei.negishi.pdrtam.model;


/**
 * 4×4行列モデルクラス
 * @author negishi
 */
public class Matrix4D {
	
	final double[][] m;
	
	/**
	 * 行列オブジェクトの生成
	 */
	public Matrix4D(){
		m = new double[4][4];
		for(int i = 0; i< m.length; i++){
			for(int j = 0; j< m[i].length; j++){
				m[i][j] = 0;
			}
		}
	}
	
	/**
	 * 引き算
	 * @param t 引く対象
	 * @return 引き算後の行列
	 */
	public Matrix4D sub(Matrix4D t){
		Matrix4D r = new Matrix4D();
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j< m [i].length; j++){
				r.m[i][j] = m[i][j] - t.m[i][j];
			}
		}
		return r;
	}
	
	/**
	 * スカラー倍
	 * @param d スカラ量
	 * @return スカラ倍後の行列
	 */
	public Matrix4D mult(double d){
		Matrix4D r = new Matrix4D();
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j < m[i].length; j++){
				r.m[i][j] = m[i][j] * d;
			}
		}
		return r;
	}
	
	/**
	 * 行列とベクトルの積を求める
	 * @param v
	 * @return
	 */
	public Vector3D mult(Vector3D v){
		double vx = v.x, vy = v.y, vz = v.z;
		double tmpX = (m[0][0] * vx + m[0][1] * vy + m[0][2] * vz);
		double tmpY = (m[1][0] * vx + m[1][1] * vy + m[1][2] * vz);
		double tmpZ = (m[2][0] * vx + m[2][1] * vy + m[2][2] * vz);
		return new Vector3D(tmpX, tmpY, tmpZ);
	}
	
	/**
	 * 転置行列を取得する
	 * @param m
	 * @return
	 */
	public Matrix4D getTransposedMatrix(){
		Matrix4D r = new Matrix4D();
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j < m[i].length; j++){
				r.m[i][j] = m[j][i];
			}
		}
		return r;
	}
	
	public static Matrix4D identifyMatrix(){
		Matrix4D r = new Matrix4D();
		for(int i = 0; i < r.m.length; i++){
			r.m[i][i] = 1;
		}
		return r;
	}
	
	/**
	 * 半対称行列を得る  => ロドリゲスの公式で使う
	 * @param v
	 * @return
	 */
	public static Matrix4D antisymmetricMatrix(Vector3D v){
		Matrix4D r = new Matrix4D();
		double vx = v.x, vy = v.y, vz = v.z;
		r.m[0][0] = 0;		r.m[0][1] = -vz; 	r.m[0][2] = vy;
		r.m[1][0] = vz;		r.m[1][1] = 0; 		r.m[1][2] = -vx;
		r.m[2][0] = -vy;	r.m[2][1] = vx; 	r.m[2][2] = 0;
		return r;
	}
	
	/**
	 * 任意の軸周りの回転行列を取得する
	 * @param v 任意の回転軸
	 * @param m 回転角
	 * @return 回転行列
	 */
	public static Matrix4D rotateMatrix(Vector3D v, double a){
		Matrix4D r = new Matrix4D();
		double sin = Math.sin(a);
		double cos = Math.cos(a);
		double vx = v.x;
		double vy = v.y;
		double vz = v.z;
		r.m[0][0] = vx*vx*(1-cos) + cos;		r.m[0][1] = vx*vy*(1-cos) - vz*sin;	r.m[0][2] = vz*vx*(1-cos) + vy*sin;	r.m[0][3] = 0;
		r.m[1][0] = vx*vy*(1-cos) + vz*sin;		r.m[1][1] = vy*vy*(1-cos) + cos;	r.m[1][2] = vy*vz*(1-cos) - vx*sin;	r.m[1][3] = 0;
		r.m[2][0] = vz*vx*(1-cos) - vy*sin;		r.m[2][1] = vy*vz*(1-cos) + vx*sin;	r.m[2][2] = vz*vz*(1-cos) + cos;	r.m[2][3] = 0;
		r.m[3][0] = 0;							r.m[3][1] = 0;						r.m[3][2] = 0;						r.m[3][3] = 1;
		return r;
	}
	
	/**
	 * ロールピッチヨー角を使って回転
	 * @param x ピッチ
	 * @param y ロール
	 * @param z ヨー
	 * @return
	 */
	public static Matrix4D rotateMatrix(double x, double y, double z){
		Matrix4D r = new Matrix4D();
		double sinX = Math.sin(x), cosX = Math.cos(x);
		double sinY = Math.sin(y), cosY = Math.cos(y);
		double sinZ = Math.sin(z), cosZ = Math.cos(z);
		r.m[0][0] = cosZ * cosY; r.m[0][1] = cosZ * sinY * sinX - sinZ * cosX; 	r.m[0][2] = cosZ * sinY * cosX + sinZ * sinX; 	r.m[0][3] = 0;
		r.m[1][0] = sinZ * cosY; r.m[1][1] = sinZ * sinY * sinX + cosZ * cosX; 	r.m[1][2] = sinZ * sinY * cosX - cosZ * sinX; 	r.m[1][3] = 0;
		r.m[2][0] = -sinY;		 r.m[2][1] = cosY * sinX;					 	r.m[2][2] = cosY * cosX;						r.m[2][3] = 0;
		r.m[3][0] = 0;			 r.m[3][1] = 0;									r.m[3][2] = 0;									r.m[3][3] = 1;
		return r;
	}
	
	/**
	 * ロールピッチヨー角を使って回転
	 * @param x ヨー
	 * @param y ピッチ
	 * @param z ロール
	 * @return
	 */
	public static Matrix4D rotateMatrix(Vector3D vector){
		Matrix4D r = new Matrix4D();
		double sinX = Math.sin(vector.x), cosX = Math.cos(vector.x);
		double sinY = Math.sin(vector.y), cosY = Math.cos(vector.y);
		double sinZ = Math.sin(vector.z), cosZ = Math.cos(vector.z);
		r.m[0][0] = cosZ * cosY; r.m[0][1] = cosZ * sinY * sinX - sinZ * cosX; 	r.m[0][2] = cosZ * sinY * cosX + sinZ * sinX; 	r.m[0][3] = 0;
		r.m[1][0] = sinZ * cosY; r.m[1][1] = sinZ * sinY * sinX + cosZ * cosX; 	r.m[1][2] = sinZ * sinY * cosX - cosZ * sinX; 	r.m[1][3] = 0;
		r.m[2][0] = -sinY;		 r.m[2][1] = cosY * sinX;					 	r.m[2][2] = cosY * cosX;						r.m[2][3] = 0;
		r.m[3][0] = 0;			 r.m[3][1] = 0;									r.m[3][2] = 0;									r.m[3][3] = 1;
		return r;
	}
	
	public double get(int row, int col){
		return m[row][col];
	}
	
	public String toString(){
		String str = "";
		for(int i=0; i<m.length; i++){
			str += String.format("%.4f\t%.4f\t%.4f\t%.4f\n", m[i][0], m[i][1], m[i][2], m[i][3]);
		}
		return str;
		
	}
}
