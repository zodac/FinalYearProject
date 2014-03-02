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
		return this.modelName;
	}
	
	public void setTextView(TextView resultView, TextView counterView, int currentResult, int totalResults){
		String nameOutput = String.format(Locale.ENGLISH, "\tModel%44s (%.1f%%)", this.modelName, this.confidenceLevel);
		String lengthOutput = String.format(Locale.ENGLISH, "%.3f", this.length) + "cm";
		String angleOutput = String.format(Locale.ENGLISH, "%.3f", this.angle) + "°";
		
		if(this.modelName.equals("No model found")){
			nameOutput = String.format(Locale.ENGLISH, "%45s (%.1f%%)", "No model found", this.confidenceLevel);
		}
		if(Double.isNaN(this.length) || this.length == Double.POSITIVE_INFINITY || this.length == Double.NEGATIVE_INFINITY){
			lengthOutput = "-";
		}
		if(Double.isNaN(this.angle)){
			angleOutput = "-";
		}
		
		resultView.setText(nameOutput + 
					      String.format(Locale.ENGLISH, "\n\n\tLength%45s\n\tTip Angle%40s", lengthOutput, angleOutput) + 
					      String.format(Locale.ENGLISH, "\n\n\tProcess time%29d seconds\n\tPasses%38d", this.runTime, this.imageCount));
		
		counterView.setText(String.format(Locale.ENGLISH, "%46s", currentResult + " of " + totalResults));
	}
}