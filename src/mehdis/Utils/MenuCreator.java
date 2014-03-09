package mehdis.Utils;

import java.io.File;

import mehdis.KeyAnalyser.KeyAnalyserActivity;
import mehdis.KeyAnalyser.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.Toast;

public class MenuCreator {
	private static final String ROOT_LOCATION = KeyAnalyserActivity.ROOT_LOCATION;
	private static final File VIDEO_LOCATION = KeyAnalyserActivity.VIDEO_LOCATION;
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
	        	return tempFilesMenu();
	            
	        case R.id.temp_images:
	        	return tempImagesMenu();
	        
	        case R.id.quit:
	        	return false;
	        
	        default:
	        	return true;
	    }
	}
	
	private static boolean tempImagesMenu() {
		if (new File(ROOT_LOCATION).exists()){
			deleteWorkFolders(false);
			statusToast(String.valueOf(thisContext.getText(R.string.imagesDeleted)));
		} else{
			statusToast(String.valueOf(thisContext.getText(R.string.noFilesToDelete)));
		}
		return true;
	}

	private static boolean tempFilesMenu() {
		if (new File(ROOT_LOCATION).exists()){
			deleteWorkFolders(true);
			statusToast(String.valueOf(thisContext.getText(R.string.tempFilesDeleted)));
		} else{
			statusToast(String.valueOf(thisContext.getText(R.string.noFilesToDelete)));
		}
		return true;
	}

	private static boolean clearMenu() {
		int instanceCounter = KeyAnalyserActivity.settings.getInstanceCounter();
		
		if(instanceCounter == 0){
			statusToast(String.valueOf(thisContext.getText(R.string.nothingToClear)));
		} else{
			KeyAnalyserActivity.resultView.setText("");
			KeyAnalyserActivity.counterView.setText("");
			KeyAnalyserActivity.keyView.setImageDrawable(null);
			
			KeyAnalyserActivity.settings.setLogPointer(instanceCounter);
		}
		return true;
	}

	private static boolean settingsMenu(AlertDialog.Builder alert) {
		int numOfPasses = KeyAnalyserActivity.settings.getNumOfPasses();
		
		if (numOfPasses == 1){
			alert.setTitle("Current setting: 1 frame");
		} else{
			alert.setTitle("Current setting: " + numOfPasses + " frames");
		}
		
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			statusToast(String.valueOf(thisContext.getText(R.string.sdCardNotMounted)));
		} else if(!VIDEO_LOCATION.exists()){
			statusToast(String.valueOf(thisContext.getText(R.string.noVideoFile)));
		} else{
			MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
			vidFile.setDataSource(VIDEO_LOCATION.getAbsolutePath());
			//Length of video file in seconds x10
			final int maxFramesAvailable = (int) ((Long.parseLong(vidFile.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))*10)/1000);
			
			final NumberPicker frameCountChooser = new NumberPicker(thisContext);
			frameCountChooser.setMinValue(1);
			frameCountChooser.setMaxValue(maxFramesAvailable);
			frameCountChooser.setValue(numOfPasses);
			frameCountChooser.setWrapSelectorWheel(true);
			frameCountChooser.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
			alert.setView(frameCountChooser);
			
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
		    	public void onClick(DialogInterface dialog, int whichButton){
		    		int inputNumberOfFrames = frameCountChooser.getValue();
		    		
		    		KeyAnalyserActivity.settings.setNumOfPasses(inputNumberOfFrames);
					if(inputNumberOfFrames == 1){
						statusToast(String.valueOf(thisContext.getText(R.string.singleFrame)));
					} else{
						statusToast("Set to " + inputNumberOfFrames + " frames");
					}
		    	}
		    });

			alert.setNegativeButton("Cancel", null);
			alert.show();
		}
		return true;
	}
	
	private static void statusToast(String statusMessage){
		Toast.makeText(thisContext, statusMessage, Toast.LENGTH_SHORT).show();
	}
	
	private static void deleteWorkFolders(boolean all){
		if(all){
			deleteDirectory(ROOT_LOCATION);
		} else{
			deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(thisContext.getText(R.string.rawImagesFolder)));
    		deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(thisContext.getText(R.string.cannyImagesFolder)));
    		deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(thisContext.getText(R.string.degreeImagesFolder)));
		}
	}
	
	private static void deleteDirectory(String inputDirectory){
		File fileOrDirectory = new File(inputDirectory);
		if (fileOrDirectory.isDirectory()){
	        for (File child : fileOrDirectory.listFiles())
	            deleteDirectory(child.toString());
		}
	    fileOrDirectory.delete();
	}
}
