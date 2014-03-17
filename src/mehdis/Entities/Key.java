package mehdis.Entities;

import mehdis.KeyAnalyser.KeyAnalyserActivity;
import mehdis.KeyAnalyser.R;

public class Key {
	
	private String modelName;
	private double length;
	private double angle;
	
	public Key(String modelName, double length, double angle){
		this.modelName = modelName;
		this.length = length;
		this.angle = angle;
	}
	
	public Key(){
		modelName = String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.noModelFound));
		length = 0;
		angle = 0; 
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
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
