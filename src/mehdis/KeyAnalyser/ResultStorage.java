package mehdis.KeyAnalyser;

import java.util.Locale;

import android.widget.TextView;


//Class to store results from KeyAnalyserActivity for callback
public class ResultStorage {
	
	private String model;
	private double length, angle;
	private long process_time;
	private int frames;
	public boolean set; //Check if variable is empty or not
	
	//Constructor
	public ResultStorage(String _model, double _length, double _angle, long _process_time, int _frames)
	{
		model = _model;
		length = _length;
		angle = _angle;
		process_time = _process_time;
		frames = _frames;
		set = true;
	}

	public void setTextView(TextView mTextView) {
		
		if (model.equals("No model found"))
		{
			if (Double.isNaN(angle))
			{
			mTextView.setText(
				"\t\t\t\t\t\tNo model found" +
				"\n\n  Length\t\t\t\t\t\t\t\t" + String.format(Locale.ENGLISH, "%.3f", length) +"cm" +
				"\n  Tip Angle\t\t\t\t\t\t-" +
				"\n\n  Process time\t\t\t\t" + process_time + " seconds" +
				"\n  Passes\t\t\t\t\t\t\t\t" + frames);
			}
			else mTextView.setText(
					"\t\t\t\t\t\tNo model found" +
							"\n\n  Length\t\t\t\t\t\t\t\t" + String.format(Locale.ENGLISH, "%.3f", length) +"cm" +
							"\n  Tip Angle\t\t\t\t\t\t" + String.format(Locale.ENGLISH, "%.3f", angle) + "°" + 
							"\n\n  Process time\t\t\t\t" + process_time + " seconds" +
							"\n  Passes\t\t\t\t\t\t\t\t" + frames);
		}
		else mTextView.setText(
				"  Model\t\t\t\t\t\t\t\t" + model + 
				"\n\n  Length\t\t\t\t\t\t\t\t" + String.format(Locale.ENGLISH, "%.3f", length) +"cm" +
				"\n  Tip Angle\t\t\t\t\t\t" + String.format(Locale.ENGLISH, "%.3f", angle) + "°" + 
				"\n\n  Process time\t\t\t\t" + process_time + " seconds" +
				"\n  Passes\t\t\t\t\t\t\t\t" + frames);		
	}
}
