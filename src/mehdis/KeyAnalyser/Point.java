package mehdis.KeyAnalyser;
public class Point {
	
	private int x;
	private int y;
	
	public Point(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public double distanceTo(Point b){
		double first = this.x - b.x;
		double second = this.y - b.y;
		first *= first;
		second *= second;
		
		return Math.sqrt(first+second);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
}
