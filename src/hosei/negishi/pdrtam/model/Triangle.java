package hosei.negishi.pdrtam.model;

import org.opencv.core.Point;

public class Triangle {
	/** Double等価比較の許容範囲 */
	private static final double EPSILON = 0.000001;
	public int id1;
	public int id2;
	public int id3;
	public Point p1;
	public Point p2;
	public Point p3;
	
	public Triangle(Point point1, Point point2, Point point3, int id1, int id2, int id3) {
		p1 = new Point(point1.x, point1.y);
		p2 = new Point(point2.x, point2.y);
		p3 = new Point(point3.x, point3.y);
		this.id1 = id1;
		this.id2 = id2;
		this.id3 = id3;
	}
	
	@Override
	public boolean equals(Object obj) {
		try {
			Triangle t = (Triangle)obj;
			return((pointEquals(p1, t.p1) && pointEquals(p2, t.p2) && pointEquals(p3, t.p3)) ||
					(pointEquals(p1, t.p2) && pointEquals(p2, t.p3) && pointEquals(p3, t.p1)) ||
					(pointEquals(p1, t.p3) && pointEquals(p2, t.p1) && pointEquals(p3, t.p2)) ||
					
					(pointEquals(p1, t.p3) && pointEquals(p2, t.p2) && pointEquals(p3, t.p1)) ||
					(pointEquals(p1, t.p2) && pointEquals(p2, t.p1) && pointEquals(p3, t.p3)) ||
					(pointEquals(p1, t.p1) && pointEquals(p2, t.p3) && pointEquals(p3, t.p2)) );
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * 他の三角形と共有点を持つか  
	 * @param t
	 * @return */
	public boolean hasCommonPoints(Triangle t) {  
		return (pointEquals(p1, t.p1) || pointEquals(p1, t.p2) || pointEquals(p1, t.p3) ||  
				pointEquals(p2, t.p1) || pointEquals(p2, t.p2) || pointEquals(p2, t.p3) ||  
				pointEquals(p3, t.p1) || pointEquals(p3, t.p2) || pointEquals(p3, t.p3) );  
	}

	@Override
	public int hashCode() {
		return 0;
	}
	
	/**
	 * Point型のequal比較
	 * @param p1
	 * @param p2
	 * @return */
	private boolean pointEquals(Point p1, Point p2) {
		return (Math.abs(p1.x - p2.x) < EPSILON)
				&& (Math.abs(p1.y - p2.y) < EPSILON);
	}
}
