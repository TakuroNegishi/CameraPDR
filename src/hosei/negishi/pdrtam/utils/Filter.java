package hosei.negishi.pdrtam.utils;

import hosei.negishi.pdrtam.model.Vector3D;

public class Filter {
	
	private Filter(){}
	
	/**
	 * 加重平均によるローパスフィルタ<br>
	 * 
	 * @param former 前の値
	 * @param value 入力値 */
	public static Vector3D lowPass(Vector3D former, Vector3D value){
		return Filter.lowPass(former, value, 0.1f);
	}
	
	/**
	 * 加重平均によるローパスフィルタ
	 * @param former 前の値
	 * @param value 入力値
	 * @param c フィルタレート */
	public static Vector3D lowPass(Vector3D former, Vector3D value, float c){
		Vector3D newVector = value.multCreate(c);
		newVector.plus(former.multCreate(1 - c));
		return newVector;
	}
}
