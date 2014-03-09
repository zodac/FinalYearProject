package mehdis.Utils;

import mehdis.Entities.Settings;
import mehdis.KeyAnalyser.KeyAnalyserActivity;
import mehdis.KeyAnalyser.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.MenuItem;
import android.widget.NumberPicker;

public class MenuCreator {
	private static Settings settings = Settings.getSettings();
	private static Context thisContext;
	
	public static boolean generateMenu(KeyAnalyserActivity activity, Context context, MenuItem item){
		thisContext = context;
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);

	    switch (item.getItemId()) {
	        case R.id.settings:
	        	return settingsMenu(alert);
	            
	        case R.id.clear:
	        	return clearMenu();
	            
	        case R.id.temp_files:
	        	return FileUtilities.tempFilesMenu(thisContext);
	            
	        case R.id.temp_images:
	        	return FileUtilities.tempImagesMenu(thisContext);
	        
	        case R.id.quit:
	        	return false;
	        
	        default:
	        	return true;
	    }
	}

	private static boolean clearMenu() {
		int instanceCounter = settings.getInstanceCounter();
		
		if(instanceCounter == 0){
			DisplayUtilities.statusToast(thisContext, String.valueOf(thisContext.getText(R.string.nothingToClear)));
		} else{
			KeyAnalyserActivity.resultView.setText("");
			KeyAnalyserActivity.counterView.setText("");
			KeyAnalyserActivity.keyView.setImageDrawable(null);
			
			settings.setLogPointer(instanceCounter);
		}
		return true;
	}

	private static boolean settingsMenu(AlertDialog.Builder alert) {
		int numOfPasses = settings.getNumOfPasses();
		
		if (numOfPasses == 1){
			alert.setTitle("Current setting: 1 frame");
		} else{
			alert.setTitle("Current setting: " + numOfPasses + " frames");
		}
		
		if(FileUtilities.videoIsAvailable(thisContext)){
			final NumberPicker frameCountChooser = new NumberPicker(thisContext);
			frameCountChooser.setMinValue(1);
			frameCountChooser.setMaxValue(FileUtilities.getVideoFramesAvailable());
			frameCountChooser.setValue(numOfPasses);
			frameCountChooser.setWrapSelectorWheel(true);
			frameCountChooser.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
			alert.setView(frameCountChooser);
			
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
		    	public void onClick(DialogInterface dialog, int whichButton){
		    		int inputNumberOfFrames = frameCountChooser.getValue();
		    		
		    		settings.setNumOfPasses(inputNumberOfFrames);
					if(inputNumberOfFrames == 1){

						DisplayUtilities.statusToast(thisContext, String.valueOf(thisContext.getText(R.string.singleFrame)));
					} else{
						DisplayUtilities.statusToast(thisContext, "Set to " + inputNumberOfFrames + " frames");
					}
		    	}
		    });

			alert.setNegativeButton("Cancel", null);
			alert.show();
		}
		return true;
	}
}