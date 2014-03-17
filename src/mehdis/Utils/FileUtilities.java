package mehdis.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import mehdis.Entities.Result;
import mehdis.Entities.Settings;
import mehdis.KeyAnalyser.KeyAnalyserActivity;
import mehdis.KeyAnalyser.R;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import ar.com.daidalos.afiledialog.FileChooserDialog;

public class FileUtilities {
	public static final String ROOT_LOCATION = Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser";
	public static final File VIDEO_FILE = new File(ROOT_LOCATION + File.separator + "video.mp4");
	private static final File RESULTS_LOCATION = new File(ROOT_LOCATION + File.separator + "results");
	
	private static String inputFile = null;
	
	public static boolean saveResults(){
		new File(ROOT_LOCATION).mkdir();
		RESULTS_LOCATION.mkdir();
		
		if(!KeyAnalyserActivity.results.isEmpty()){
			try {
				String fileName = new SimpleDateFormat("dd-MM-yyyy_hh.mm.ssa", Locale.ENGLISH).format(new Date()) + ".dat";
				
				ObjectOutputStream fileOut = new ObjectOutputStream(new FileOutputStream(RESULTS_LOCATION.getAbsolutePath() + File.separator + fileName));
				
				for(Result result : KeyAnalyserActivity.results){
					fileOut.writeObject(result);
				}
				
				fileOut.close();
				DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.resultsSaved) + " \"" + fileName + "\"");
			} catch (IOException e) {
			}
		} else{
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.noResults));
		}
		return true;
	}
	
	public static boolean loadResults(){
		FileChooserDialog fileChooser = new FileChooserDialog(KeyAnalyserActivity.thisContext);
		
		fileChooser.loadFolder(RESULTS_LOCATION + File.separator);
		fileChooser.addListener(new FileChooserDialog.OnFileSelectedListener() {
			public void onFileSelected(Dialog source, File file) {
				ObjectInputStream fileIn = null;
				ArrayList<Result> inputResults = new ArrayList<Result>();
				source.hide();
				inputFile = file.getAbsolutePath();
				String fileExtension = FilenameUtils.getExtension(inputFile);
				
				
				if(fileExtension.equals("dat")){
					try {
						fileIn = new ObjectInputStream(new FileInputStream(inputFile));
						
						Result inputResult = (Result) fileIn.readObject();
						
						while(inputResult != null){
							inputResults.add(inputResult);
							inputResult = (Result) fileIn.readObject();
						}
						fileIn.close();
						
						KeyAnalyserActivity.loadResultsFromList(inputResults);
					} catch (IOException e) {
						try {
							fileIn.close();
						} catch (IOException e1) {
							DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.invalidResultsFile));
						}
					} catch (ClassNotFoundException e) {
					}
					
					if(!inputResults.isEmpty()){
						KeyAnalyserActivity.loadResultsFromList(inputResults);
						DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.resultsLoaded));
					}
				} else{
					DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.invalidResultsFile));
				}
			}
			
			public void onFileSelected(Dialog source, File folder, String name) {}
		});
		fileChooser.show();
		
		return true;
	}
	
	
	public static void createRootFolder(){
		new File(ROOT_LOCATION).mkdir();
	}
	
	public static void createWorkFolders(){
		new File(ROOT_LOCATION + File.separator + KeyAnalyserActivity.thisContext.getText(R.string.rawImagesFolder)).mkdir();
		new File(ROOT_LOCATION + File.separator + KeyAnalyserActivity.thisContext.getText(R.string.cannyImagesFolder)).mkdir();
		new File(ROOT_LOCATION + File.separator + KeyAnalyserActivity.thisContext.getText(R.string.degreeImagesFolder)).mkdir();
	}
	
	public static void writeImageToFile(Bitmap outputImage, String saveLocation, int imageNumber){
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		outputImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
		File imageFile = new File(ROOT_LOCATION + File.separator + saveLocation + File.separator + String.format(Settings.getSettings().getLocale(), "%03d.png", imageNumber));
		
		try {
			imageFile.createNewFile();
			bytes.writeTo(new FileOutputStream(imageFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean tempImagesMenu() {
		if (new File(ROOT_LOCATION).exists()){
			deleteWorkFolders(false);
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.imagesDeleted));
		} else{
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.noFilesToDelete));
		}
		return true;
	}

	public static boolean tempFilesMenu() {
		if (new File(ROOT_LOCATION).exists()){
			deleteWorkFolders(true);
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.tempFilesDeleted));
		} else{
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.noFilesToDelete));
		}
		return true;
	}
	
	private static void deleteWorkFolders(boolean all){
		if(all){
			deleteDirectory(ROOT_LOCATION);
		} else{
			deleteDirectory(ROOT_LOCATION + File.separator + KeyAnalyserActivity.thisContext.getText(R.string.rawImagesFolder));
    		deleteDirectory(ROOT_LOCATION + File.separator + KeyAnalyserActivity.thisContext.getText(R.string.cannyImagesFolder));
    		deleteDirectory(ROOT_LOCATION + File.separator + KeyAnalyserActivity.thisContext.getText(R.string.degreeImagesFolder));
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
	
	public static int getVideoFramesAvailable(){
		MediaMetadataRetriever videoFile = getVideoFile();
		
		return (int) ((Long.parseLong(videoFile.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))*10)/1000);
	}
	
	public static MediaMetadataRetriever getVideoFile(){
		MediaMetadataRetriever videoFile = new MediaMetadataRetriever();
		videoFile.setDataSource(VIDEO_FILE.getAbsolutePath());
		
		return videoFile;
	}

	public static boolean videoIsAvailable() {
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.sdCardNotMounted));
			return false;
		}
		if(!VIDEO_FILE.exists()){
			DisplayUtilities.statusToast(KeyAnalyserActivity.thisContext.getText(R.string.noVideoFile));
			return false;
		}
		return true;
	}
}
