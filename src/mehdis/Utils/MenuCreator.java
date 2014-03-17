package mehdis.Utils;

import java.text.NumberFormat;
import java.util.Locale;

import mehdis.Entities.Settings;
import mehdis.KeyAnalyser.KeyAnalyserActivity;
import mehdis.KeyAnalyser.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.NumberPicker;

public class MenuCreator {
	private static Settings settings = Settings.getSettings();
	
	public static boolean generateMenu(KeyAnalyserActivity activity, Context context, MenuItem item){
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);

	    switch (item.getItemId()) {
	        case R.id.settings:
	        	return settingsMenu(alert);
	            
	        case R.id.clearScreen:
	        	return clearScreenMenu();
	        	
	        case R.id.save:
	        	return FileUtilities.saveResults();
	        
	        case R.id.load:
	        	return FileUtilities.loadResults();
	        	
	        case R.id.clearResults:
	        	return clearResultsMenu();
	            
	        case R.id.tempFiles:
	        	return FileUtilities.tempFilesMenu();
	            
	        case R.id.tempImages:
	        	return FileUtilities.tempImagesMenu();

	        case R.id.setLanguage:
	        	return changeLanguageMenu(alert);
	        	
	        case R.id.quit:
	        	return false;
	        
	        default:
	        	return true;
	    }
	}

	private static boolean clearScreenMenu() {
		int instanceCounter = settings.getInstanceCounter();
		
		if(instanceCounter == 0){
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.nothingToClear));
			
		} else{
			KeyAnalyserActivity.resultsView.clear();
			settings.setLogIndex(instanceCounter);
		}
		return true;
	}
	
	private static boolean clearResultsMenu() {
		if(settings.getInstanceCounter() == 0){
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.nothingToClear));
		} else{
			KeyAnalyserActivity.resetProgressBar();
			KeyAnalyserActivity.results.clear();
			clearScreenMenu();
		}
		
		return true;
	}
	
	private static boolean changeLanguageMenu(AlertDialog.Builder alert){
		final Resources res = KeyAnalyserActivity.thisContext.getResources();
	    final DisplayMetrics dm = res.getDisplayMetrics();
	    final Configuration conf = res.getConfiguration();
	    
	    final CharSequence[] languageList = {KeyAnalyserActivity.thisContext.getText(R.string.english),
								    		 KeyAnalyserActivity.thisContext.getText(R.string.spanish),
								    		 KeyAnalyserActivity.thisContext.getText(R.string.french),
								    		 KeyAnalyserActivity.thisContext.getText(R.string.german),
								    		 KeyAnalyserActivity.thisContext.getText(R.string.urdu)};
	    
	    
	    alert.setSingleChoiceItems(languageList, Settings.getSettings().getLanguage(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int languageSelection) {
            	Settings.getSettings().setLanguage(languageSelection);
            	
            	if(languageSelection == 0){
            		conf.locale = Locale.ENGLISH;
            	} else if(languageSelection == 1){
            		conf.locale = new Locale("es");
            	} else if(languageSelection == 2){
            		conf.locale = Locale.FRENCH;
            	} else if(languageSelection == 3){
            		conf.locale = Locale.GERMAN;
            	} else if(languageSelection == 4){
            		conf.locale = new Locale("ur");
            	}
            	Settings.getSettings().setLocale(conf.locale);
            	res.updateConfiguration(conf, dm);
            	
            	Intent i = new Intent(KeyAnalyserActivity.thisContext, KeyAnalyserActivity.class);
            	i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            	KeyAnalyserActivity.thisContext.startActivity(i);
            	
            	DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.languageChanged));
            }
        });
        alert.show();
		
		return true;
	}

	private static boolean settingsMenu(AlertDialog.Builder alert) {
		int numOfPasses = settings.getNumOfPasses();
		
		if (numOfPasses == 1){
			alert.setTitle(KeyAnalyserActivity.thisContext.getText(R.string.singleFrameCurrent));
		} else{
			if(Settings.getSettings().getLanguage() == 4){
				String currentPasses = NumberFormat.getInstance(new Locale("ar_EG")).format(numOfPasses);
				alert.setTitle(String.format(Settings.getSettings().getLocale(), String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.multipleFramesCurrent)), currentPasses));
			} else{
				alert.setTitle(String.format(Settings.getSettings().getLocale(), String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.multipleFramesCurrent)), String.valueOf(numOfPasses)));
			}
		}
		
		if(FileUtilities.videoIsAvailable()){
			final NumberPicker frameCountChooser = new NumberPicker(KeyAnalyserActivity.thisContext);
			frameCountChooser.setMinValue(1);
			frameCountChooser.setMaxValue(FileUtilities.getVideoFramesAvailable());
			frameCountChooser.setValue(numOfPasses);
			frameCountChooser.setWrapSelectorWheel(true);
			frameCountChooser.setFormatter(new NumberPicker.Formatter() {
			    @Override
			    public String format(int value) {
			    	if(Settings.getSettings().getLanguage() == 4){
			    		return NumberFormat.getInstance(new Locale("ar_EG")).format(value);
			    	}
			        return String.valueOf(value);
			    }
			});
			frameCountChooser.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
			alert.setView(frameCountChooser);
			
			alert.setPositiveButton(KeyAnalyserActivity.thisContext.getText(R.string.ok), new DialogInterface.OnClickListener(){
		    	public void onClick(DialogInterface dialog, int whichButton){
		    		int inputNumberOfFrames = frameCountChooser.getValue();
		    		
		    		settings.setNumOfPasses(inputNumberOfFrames);
					if(inputNumberOfFrames == 1){
						DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.singleFrameConfirm));
					} else{
						if(Settings.getSettings().getLanguage() == 4){
							DisplayUtilities.statusToast(String.format(Settings.getSettings().getLocale(),
									   String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.multipleFramesConfirm)),
									   NumberFormat.getInstance(new Locale("ar_EG")).format(inputNumberOfFrames)));
						} else{
							DisplayUtilities.statusToast(String.format(Settings.getSettings().getLocale(),
																	   String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.multipleFramesConfirm)),
																	   String.valueOf(inputNumberOfFrames)));
						}
					}
		    	}
		    });

			alert.setNegativeButton(KeyAnalyserActivity.thisContext.getText(R.string.cancel), null);
			alert.show();
		}
		return true;
	}
}