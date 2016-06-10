package hosei.negishi.pdrtam.model;


/**
 * 3次元ベクトルクラス
 */
public class Vector3D implements Cloneable{	
	public float x;
	public float y;
	public float z;
	
	/** ベクトルオブジェクトの構築(全成分0) */
	public Vector3D(){
		this.x = 0f;
		this.y = 0f;
		this.z = 0f;
	}
	
	/** ベクトルオブジェクトの構築 */
	public Vector3D(float x, float y, float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/** ベクトルオブジェクトの構築 */
	public Vector3D(Vector3D v){
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}
	
	/** 正規化ベクトルを取得する */
	public Vector3D normalize(){
		float length  = length();
		float tmpX = this.x / length;
		float tmpY = this.y / length;
		float tmpZ = this.z / length;
		return new Vector3D(tmpX, tmpY, tmpZ);
	}
	
	/** ベクトルの大きさを求める */
	public float length(){
		return (float)Math.sqrt(x * x + y * y + z * z);
	}
	
	/** スカラー倍 */
	public void mult(float d){
		x *= d;
		y *= d;
		z *= d;
	}
	
	/** スカラー倍 */
	public Vector3D multCreate(float d){
		return new Vector3D(x *d, y * d, z * d);
	}
	
	/** ベクトルの足し算 */
	public void plus(Vector3D v){
		x += v.x;
		y += v.y;
		z += v.z;
	}
	
//	/** ベクトルの足し算 */
//	public Vector3D plus(Vector3D v){
//		return new Vector3D(x + v.x, y + v.y, z + v.z);
//	}
	
	/** ベクトルの引き算 */
	public Vector3D sub(Vector3D v){
		return new Vector3D(x - v.x, y - v.y, z - v.z);
	}
	
	/** 内積を求める */
	public double innerProduct(Vector3D v){
		return x * v.x + y * v.y + z * v.z;
	}
	
	/** 外積を求める */
	public Vector3D exteriorProduct(Vector3D v){
		float tmpX = y * v.z - v.y * z;
		float tmpY = z * v.x - v.z * x;
		float tmpZ = x * v.y - v.x * y;
		return new Vector3D(tmpX, tmpY, tmpZ);
	}
	
	/** ベクトルの要素を配列として返す */
	public double[] getElements(){
		double[] elem = new double[3];
		elem[0] = x;
		elem[1] = y;
		elem[2] = z;
		return elem;
	}
	
	@Override
	public Vector3D clone(){
		Vector3D clone = null;
		try {
			clone = (Vector3D) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}
	
	@Override
	public String toString(){
		return x + ", " + y + ", " + z;
	}
	
}
