package mehdis.Entities;

import java.io.IOException;
import java.io.InputStream;

import mehdis.KeyAnalyser.KeyAnalyserActivity;
import mehdis.KeyAnalyser.R;
import mehdis.Utils.SwipeTouchListener;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ResultView {
	private static final String NOT_FOUND = "-";
	private ImageView keyView;
	private TextView titleView;
	private TextView valueView;
	private TextView counterView;

	public ResultView(View keyView, View titleView, View valueView, View counterView) {
		this.keyView = (ImageView) keyView;
		this.titleView = (TextView) titleView;
		this.valueView = (TextView) valueView;
		this.counterView = (TextView) counterView;
		setGestureListener();
	}

	public void showResultInView(Result result, int currentResults, int totalResults) {
		setImageView(result.getModelName());
		setMainTextView(result);
		setCounterTextView(currentResults, totalResults);
	}
	
	public void setGestureListener(){
		this.keyView.setOnTouchListener(new SwipeTouchListener());
		this.titleView.setOnTouchListener(new SwipeTouchListener());
		this.valueView.setOnTouchListener(new SwipeTouchListener());
		this.counterView.setOnTouchListener(new SwipeTouchListener());
	}
	
	private void setMainTextView(Result result){
		String nameOutput = result.getModelName() + String.format(Settings.getSettings().getLocale(), " (%.1f%%)", result.getConfidenceLevel());
		String lengthOutput = String.format(Settings.getSettings().getLocale(), "%.3f", result.getLength()) + "cm";
		String angleOutput = String.format(Settings.getSettings().getLocale(), "%.3f", result.getAngle()) + "°";
	
		if(result.getModelName().equals(String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.noModelFound)))){
			nameOutput = NOT_FOUND;
		}
		
		if(Double.isNaN(result.getLength()) || result.getLength() == Double.POSITIVE_INFINITY || result.getLength() == Double.NEGATIVE_INFINITY){
			lengthOutput = NOT_FOUND;
		}
		
		if(Double.isNaN(result.getAngle())){
			angleOutput = NOT_FOUND;
		}
		
		titleView.setText(String.format(Settings.getSettings().getLocale(), "\t%s\n\n\t%s\n\t%s\n\n\t%s\n\t%s", KeyAnalyserActivity.thisContext.getText(R.string.model),
																				  			KeyAnalyserActivity.thisContext.getText(R.string.length),
																				  			KeyAnalyserActivity.thisContext.getText(R.string.tipAngle),
																				  			KeyAnalyserActivity.thisContext.getText(R.string.processTime),
																				  			KeyAnalyserActivity.thisContext.getText(R.string.passes)));
		
		valueView.setText(String.format(Settings.getSettings().getLocale(), "%s\n\n%s\n%s\n\n%d %s\n%d", nameOutput, lengthOutput, angleOutput, result.getRunTime(),
																					 KeyAnalyserActivity.thisContext.getText(R.string.seconds),
																					 result.getImageCount()));
	}
	
	private void setCounterTextView(int currentResults, int totalResults){
		counterView.setText(String.format(Settings.getSettings().getLocale(), "%42d %s %d", currentResults, KeyAnalyserActivity.thisContext.getText(R.string.of), totalResults));
	}
	
	private void setImageView(String keyModelName){
		if(keyModelName.equals(String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.noModelFound)))){
			keyView.setImageBitmap(BitmapFactory.decodeResource(KeyAnalyserActivity.thisContext.getResources(), R.drawable.notfound));
		}
		
		Bitmap keyImage = getBitmapFromAssets(keyModelName);
		if(keyImage != null){
			keyView.setImageBitmap(keyImage);
		}
	}
	
	private static Bitmap getBitmapFromAssets(String fileName) {
	    AssetManager assetManager = KeyAnalyserActivity.thisContext.getAssets();
	    InputStream istr;
	    Bitmap bitmap = null;
	    
		try {
			istr = assetManager.open(fileName + ".png");
			bitmap = BitmapFactory.decodeStream(istr);
		} catch (IOException e) {
		}
		
	    return bitmap;
	}
	
	public void clear(){
		keyView.setImageDrawable(null);
		counterView.setText("");
		titleView.setText("");
		valueView.setText("");
		
	}

	public ImageView getKeyView() {
		return keyView;
	}

	public void setKeyView(ImageView keyView) {
		this.keyView = keyView;
	}

	public TextView getTitleView() {
		return titleView;
	}

	public void setTitleView(TextView titleView) {
		this.titleView = titleView;
	}

	public TextView getValueView() {
		return valueView;
	}

	public void setValueView(TextView valueView) {
		this.valueView = valueView;
	}

	public TextView getCounterView() {
		return counterView;
	}

	public void setCounterView(TextView counterView) {
		this.counterView = counterView;
	}
}
