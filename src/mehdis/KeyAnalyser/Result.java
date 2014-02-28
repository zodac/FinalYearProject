package mehdis.KeyAnalyser;

import java.util.Locale;

import android.widget.TextView;

public class Result{
	private String modelName;
	private double length;
	private double angle;
	private long runTime;
	private int imageCount;
	private double confidenceLevel;
	
	public Result(String modelName, double length, double angle, long runtime, int imageCount, double confidenceLevel){
		this.modelName = modelName;
		this.length = length;
		this.angle = angle;
		this.runTime = runtime;
		this.imageCount = imageCount;
		this.confidenceLevel = confidenceLevel;
	}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
	
	public String getModelName(){
		return modelName;
	}
	
	public void setTextView(TextView mTextView){

		String nameOutput = "\tModel\t\t\t\t\t\t\t\t" + modelName + " (" + String.format(Locale.ENGLISH, "%.1f", confidenceLevel) + "%)";
		String lengthOutput = String.format(Locale.ENGLISH, "%.3f", length) + "cm";
		String angleOutput = String.format(Locale.ENGLISH, "%.3f", angle) + "°";
		
		if(modelName.equals("No model found")){
			nameOutput = "\t\t\t\t\tNo model found (" + String.format(Locale.ENGLISH, "%.1f", confidenceLevel) + "%)";
		}
		if(Double.isNaN(length) || length == Double.POSITIVE_INFINITY || length == Double.NEGATIVE_INFINITY){
			lengthOutput = "-";
		}
		if(Double.isNaN(angle)){
			angleOutput = "-";
		}
		
		mTextView.setText(nameOutput + 
					      "\n\n\tLength\t\t\t\t\t\t\t" + lengthOutput +
					      "\n\tTip Angle\t\t\t\t\t\t" + angleOutput + 
					      "\n\n\tProcess time\t\t\t\t" + runTime + " seconds" +
					      "\n\tPasses\t\t\t\t\t\t\t" + imageCount);
	}
}