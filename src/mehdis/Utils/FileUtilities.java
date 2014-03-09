package mehdis.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import mehdis.KeyAnalyser.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;

public class FileUtilities {
	
	public static final String ROOT_LOCATION = Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser";
	public static final File VIDEO_LOCATION = new File(ROOT_LOCATION + File.separator + "video.mp4");
	
	public static void createRootFolder(){
		new File(ROOT_LOCATION).mkdir();
	}
	
	public static void createWorkFolders(Context context){
		new File(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.rawImagesFolder))).mkdir();
		new File(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.cannyImagesFolder))).mkdir();
		new File(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.degreeImagesFolder))).mkdir();
	}
	
	public static void writeImageToFile(Bitmap outputImage, String saveLocation, int imageNumber){
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		outputImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
		File imageFile = new File(FileUtilities.ROOT_LOCATION + File.separator + saveLocation + File.separator + String.format(Locale.ENGLISH, "%03d.png", imageNumber));
		
		try {
			imageFile.createNewFile();
			bytes.writeTo(new FileOutputStream(imageFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean tempImagesMenu(Context context) {
		if (new File(ROOT_LOCATION).exists()){
			deleteWorkFolders(false, context);
			DisplayUtilities.statusToast(context, String.valueOf(context.getText(R.string.imagesDeleted)));
		} else{
			DisplayUtilities.statusToast(context, String.valueOf(context.getText(R.string.noFilesToDelete)));
		}
		return true;
	}

	public static boolean tempFilesMenu(Context context) {
		if (new File(ROOT_LOCATION).exists()){
			deleteWorkFolders(true, context);
			DisplayUtilities.statusToast(context, String.valueOf(context.getText(R.string.tempFilesDeleted)));
		} else{
			DisplayUtilities.statusToast(context, String.valueOf(context.getText(R.string.noFilesToDelete)));
		}
		return true;
	}
	
	private static void deleteWorkFolders(boolean all, Context context){
		if(all){
			deleteDirectory(ROOT_LOCATION);
		} else{
			deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.rawImagesFolder)));
    		deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.cannyImagesFolder)));
    		deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.degreeImagesFolder)));
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
		MediaMetadataRetriever videoFile = FileUtilities.getVideoFile();
		
		return (int) ((Long.parseLong(videoFile.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))*10)/1000);
	}
	
	public static MediaMetadataRetriever getVideoFile(){
		MediaMetadataRetriever videoFile = new MediaMetadataRetriever();
		videoFile.setDataSource(FileUtilities.VIDEO_LOCATION.getAbsolutePath());
		
		return videoFile;
	}

	public static boolean videoIsAvailable(Context context) {
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			DisplayUtilities.statusToast(context, String.valueOf(context.getText(R.string.sdCardNotMounted)));
		}
		if(!VIDEO_LOCATION.exists()){
			DisplayUtilities.statusToast(context, String.valueOf(context.getText(R.string.noVideoFile)));
		}
		return true;
	}
}
