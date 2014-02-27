package mehdis.KeyAnalyser;
public class Point {
	
	public int x;
	public int y;
	
	public Point(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public double distanceTo(Point b){
		double result = 0;
		
		double first = this.x - b.x;
		double second = this.y - b.y;
		first *= first;
		second *= second;
		
		result = Math.sqrt(first+second);
		
		return result;
	}

}
