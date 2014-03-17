package mehdis.Entities;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Result implements Serializable {
	private String modelName;
	private double length;
	private double angle;
	private long runTime;
	private int imageCount;
	private double confidenceLevel;
	
	public Result(String modelName, double length, double angle, long runtime, int imageCount, double confidenceLevel){
		this.modelName = modelName;
		this.setLength(length);
		this.setAngle(angle);
		this.setRunTime(runtime);
		this.setImageCount(imageCount);
		this.setConfidenceLevel(confidenceLevel);
	}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
	
	public String getModelName(){
		return this.modelName;
	}

	public double getConfidenceLevel() {
		return confidenceLevel;
	}

	public void setConfidenceLevel(double confidenceLevel) {
		this.confidenceLevel = confidenceLevel;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public int getImageCount() {
		return imageCount;
	}

	public void setImageCount(int imageCount) {
		this.imageCount = imageCount;
	}

	public long getRunTime() {
		return runTime;
	}

	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}
}