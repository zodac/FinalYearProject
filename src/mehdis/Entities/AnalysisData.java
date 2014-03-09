package mehdis.Entities;

public class AnalysisData {
	private double length;
	private double angle;
	
	public AnalysisData(){
		this.length = 0;
		this.angle = 0;
	}
	
	public AnalysisData(double length, double angle/*, long time*/){
		this.length = length;
		this.angle = angle;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}
}
