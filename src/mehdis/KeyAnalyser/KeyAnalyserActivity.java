/**
	Sheikh Arouge Mehdi (09498389)
	4th Year B.A.I. Engineering - School of Computer Science and Statistics
	October 2012 to April 2013
	KeyAnalyser for Android 2.3.3+, to determine model number of key (using Silca model numbers)
 */

package mehdis.KeyAnalyser;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvGet2D;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_HOUGH_STANDARD;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCanny;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvHoughLines2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class KeyAnalyserActivity extends Activity{	
	int numOfPasses = 1;
	int instanceCounter = 0;
	int logPointer = 0;
	
	int firstRedColumn = 0, lastRedColumn = 0, firstRedRow = 0, lastRedRow = 0; //WEST, EAST, NORTH, SOUTH
	
	ArrayList<Result> analysisResults = new ArrayList<Result>();
	ArrayList<Key> databaseKeys = new ArrayList<Key>();
	
	String rootLocation = Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser";
	File videoLocation = new File(rootLocation + File.separator + "video.mp4");
	
	private void TakeVideoIntent(){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			startActivityForResult(takeVideoIntent, 3);
		}
		else statusToast("SD card not mounted!");
	}
	
	//Main thread - processing of video
	private void ProcessVideo(){
		long startTime = System.currentTimeMillis();	
		
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			statusToast("SD card not mounted!");
		else if(!videoLocation.exists())
			statusToast("No video file to analyse!");
		else{
			createFolders();
			
			MediaMetadataRetriever videoFile = new MediaMetadataRetriever();
			videoFile.setDataSource(videoLocation.getAbsolutePath());
			Bitmap extractedFrame = videoFile.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
			
			int i = 0;
			int imageCount = 0;
			
			double length = 0;
			double degreeAverage = 0;
			
			while(extractedFrame != null && i < numOfPasses){
				writeImageToFile(extractedFrame, "1_raw_images", ++imageCount);
				IplImage imageToProcess = cvLoadImage(rootLocation + File.separator + "1_raw_images" + File.separator + String.format(Locale.ENGLISH, "%03d", imageCount) + ".png");	
				
				/**
				 * Determine red area for pixel/cm ratio & red area boundary (for cvCanny)
				 */
				int sizeOfMaxRedColumn = redAreaCalculation(imageToProcess);
				
				/**
				 * Moving on to cvCanny analysis: isolating image within red area (with boundaries defined by first_red_row, last_red_row, first_red_col & last_red_col)
				 */
		        //Apply cvCanny
				IplImage cannyImage = IplImage.create(imageToProcess.width(), imageToProcess.height(), IPL_DEPTH_8U, 1);
		        cvCanny(imageToProcess, cannyImage, 75, 165, 3);
		       
		        int row_offset = imageToProcess.height()/200; //Offsets used for red boundaries (since usually the video isn't straight)
		        int firstObjectPixelRow = 0;
		        int lastObjectPixelRow = 0;
		        Bitmap cannyResultImage = Bitmap.createBitmap(imageToProcess.width(), imageToProcess.height(), Bitmap.Config.ARGB_8888);
		        
		        //If within these boundaries, apply cvCanny, otherwise leave empty (for .png, this results in transparent space around the image)
		        for(int row = firstRedRow+(4*row_offset); row < lastRedRow-(4*row_offset); row++){
		        	int col_offset = imageToProcess.width()/110;
		        	for(int col = firstRedColumn+(7*col_offset); col < lastRedColumn-(7*col_offset); col++){
			            int keyPixel = (int) cvGet2D(cannyImage, row, col).getVal(0);

			            if (keyPixel > 100){
			            	cannyResultImage.setPixel(col, row, Color.argb(255, 255, 255, keyPixel)); //Object pixel
			            	
			            	if(firstObjectPixelRow <= 0)
			            		firstObjectPixelRow = row;
			            	else lastObjectPixelRow = row;
			            }
			            else cannyResultImage.setPixel(col, row, Color.argb(255, 0, 0, keyPixel)); //Background pixel
			        }
		        }
				writeImageToFile(cannyResultImage, "2_canny_images", imageCount);
				
				length = (lastObjectPixelRow-firstObjectPixelRow)*(17.5/sizeOfMaxRedColumn);
				
				/**
				 * Create images for degree calculation - trying to isolate the tip of the key and some of the rows above it
				 */
				//Define area to isolate and save to file
				isolateKeyTip(imageCount, imageToProcess, cannyImage, lastObjectPixelRow);
				
				//Load image for degree calculation
				imageToProcess = cvLoadImage(rootLocation + File.separator + "2_canny_images" + File.separator + String.format(Locale.ENGLISH, "%03d", imageCount) + ".png");
				cannyImage = IplImage.create(imageToProcess.width(), imageToProcess.height(), IPL_DEPTH_8U, 1);
				cvCvtColor(imageToProcess, cannyImage, CV_BGR2GRAY);
			
				//Calculate angle of tip
				degreeAverage = angleCalculation(cannyImage);
				
				extractedFrame = videoFile.getFrameAtTime(100000*++i, MediaMetadataRetriever.OPTION_CLOSEST);
			}
			videoFile.release();
			Result analysisResult = matchKeyToDatabase(length, degreeAverage, (System.currentTimeMillis()-startTime)/1000, imageCount);
			saveResult(analysisResult);
			endingTone();
		}	
		/** End **/
	}

	private void isolateKeyTip(int imageCount, IplImage imageToProcess, IplImage cannyImage, int lastObjectPixelRow) {
		Bitmap cannyResultImage;
		int row_offset_degree = imageToProcess.height()/200;
		int source_col = firstRedColumn+(imageToProcess.width()/3);
		
		if ((lastRedColumn-(imageToProcess.width()/3))-(firstRedColumn+(imageToProcess.width()/3)) < 0)
			cannyResultImage = Bitmap.createBitmap(imageToProcess.width()/3, ((7*row_offset_degree)+1), Bitmap.Config.ARGB_8888);
		else cannyResultImage = Bitmap.createBitmap((lastRedColumn-(imageToProcess.width()/3))-(firstRedColumn+(imageToProcess.width()/3)), ((7*row_offset_degree)+1), Bitmap.Config.ARGB_8888);
		
		
		for (int col = 0; col < ((lastRedColumn-(imageToProcess.width()/3)) - (firstRedColumn+(imageToProcess.width()/3))); col++, source_col++){
			int source_row = lastObjectPixelRow+1-(7*row_offset_degree);
			
			for (int row = 0; row < (7*row_offset_degree)+1; row++, source_row++){
				int keyPixel = (int) cvGet2D(cannyImage, source_row, source_col).getVal(0);
				
				if (keyPixel > 100)
					cannyResultImage.setPixel(col, row, Color.argb(255, 255, 255, keyPixel));
				else cannyResultImage.setPixel(col, row, Color.argb(255, 0, 0, keyPixel));
			}
		}
		writeImageToFile(cannyResultImage, "3_degree_images", imageCount);
	}

	private double angleCalculation(IplImage cannyImage) {
		CvSeq cvlines = cvHoughLines2(cannyImage, CvMemStorage.create(), CV_HOUGH_STANDARD, 1, Math.PI/180, 1, 0, 0);
		FloatPointer cvline = new FloatPointer(cvGetSeqElem(cvlines, cvlines.total()-1));
		try{
			double rho = cvline.position(0).get();
			double theta = cvline.position(1).get();
		
			return Math.atan(Math.abs(rho)/theta)*(180/Math.PI);
		} catch (NullPointerException e){
			return Double.NaN;
		}
	}
	
	//Need to find these point locations programatically
//	private double angleCalculation(IplImage cannyImage){
//		Point tip = new Point(37, 27);
//		Point leftSide = new Point(27, 13);
//		Point rightSide = new Point(49, 10);
//		
//		double top = Math.pow(tip.distanceTo(leftSide), 2) + Math.pow(tip.distanceTo(rightSide), 2) - Math.pow(leftSide.distanceTo(rightSide), 2);
//		double bottom = 2*tip.distanceTo(leftSide)*tip.distanceTo(rightSide);
//		
//		return Math.acos(top/bottom)*(180/Math.PI);
//	}

	private int redAreaCalculation(IplImage imageToProcess){
		//Columns
		int numOfRedPixelsInMostRedColumn = 0;
		int sumOfColumnsWithMaxRedPixels = 0;
		
		int redLimit = 145, greenLimit = 65, blueLimit = 50;
		
		for (int col = 0; col < imageToProcess.width(); col+=2){
			int numOfRedPixels = 0;
			
			for (int row = 0; row < imageToProcess.height(); row++){
				BytePointer imageRGBExtraction = imageToProcess.imageData();
				int blue = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3);
				int green = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3 + 1);
				int red = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3 + 2);
				
				if(blue < 0)
					blue += 256;
				if(green < 0)
					green += 256;
				if(red < 0)
					red += 256;
				
		        if(red > redLimit && green < greenLimit && blue < blueLimit)
		        	numOfRedPixels++;
			}
			
			if (numOfRedPixels > numOfRedPixelsInMostRedColumn)
				numOfRedPixelsInMostRedColumn = numOfRedPixels;
			
			if (numOfRedPixels > 10){
				if (firstRedColumn <= 0)
					firstRedColumn = col;
				else lastRedColumn = col;
			}
		}
		sumOfColumnsWithMaxRedPixels += numOfRedPixelsInMostRedColumn;
		
		//Rows
		int numOfRedPixelsInMostRedRow = 1;
		for (int row = 0; row < imageToProcess.height(); row+=3){
			int numOfRedPixels = 0;
			
			for (int col = 0; col < imageToProcess.width(); col++){						
				BytePointer imageRGBExtraction = imageToProcess.imageData();
				int blue = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3);
				int green = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3 + 1);
				int red = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3 + 2);
				
				if(blue < 0)
					blue += 256;
				if(green < 0)
					green += 256;
				if(red < 0)
					red += 256;
				
				if(red > redLimit && green < greenLimit && blue < blueLimit)
		        	numOfRedPixels++;
			}
			
			if (numOfRedPixels > numOfRedPixelsInMostRedRow)
				numOfRedPixelsInMostRedRow = numOfRedPixels;
			
			if (numOfRedPixels > 15){ //Red area, by start and finish rows, for Canny boundaries
				if (firstRedRow <= 0)
					firstRedRow = row;
				else lastRedRow = row;
			}
		}
		return sumOfColumnsWithMaxRedPixels;
	}

	private void createFolders(){		
		new File(rootLocation + File.separator + "1_raw_images").mkdir();
		new File(rootLocation + File.separator + "2_canny_images").mkdir();
		new File(rootLocation + File.separator + "3_degree_images").mkdir();
	}
	
	private void deleteFolders(boolean all){
		if(all)
			deleteDirectory(rootLocation);
		else{
			deleteDirectory(rootLocation + File.separator + "1_raw_images");
    		deleteDirectory(rootLocation + File.separator + "2_canny_images");
    		deleteDirectory(rootLocation + File.separator + "3_degree_images");
		}
	}
	
	private void deleteDirectory(String inputDirectory){
		File fileOrDirectory = new File(inputDirectory);
		if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            deleteDirectory(child.toString());
	    fileOrDirectory.delete();
	}

	private Result matchKeyToDatabase(double length, double angle, long runTime, int passes){
		double threshold = 20;
		double min = 100;
		double lengthDelta, angleDelta;
		Key minKey = null;
		generateKeyDatabase();
		
		for(Key k : databaseKeys){
			lengthDelta = Math.abs(k.getLength()-length)*3;
			angleDelta = Math.abs(k.getAngle()-angle)*.75;
			
			if(lengthDelta+angleDelta < min){
				min = lengthDelta+angleDelta;
				minKey = k;
			}
		}
		
		if(min <= threshold)
			return new Result(minKey.getName(), length, angle, runTime, passes, 100-min);
		return new Result("No model found", length, angle, runTime, passes, 100-min);
	}

	private void generateKeyDatabase(){
		databaseKeys.add(new Key("MS2", 	4.2, 	90));
		databaseKeys.add(new Key("UL050", 	5.5, 	87.5));
		databaseKeys.add(new Key("UNI3", 	5.75, 	97.5));
		databaseKeys.add(new Key("UL054", 	6, 		92.5));
	}
	
	//Save results in array, and set TextView to show results
	private void saveResult(Result analysisResult) {
		analysisResults.add(analysisResult);
		analysisResults.get(instanceCounter).setTextView((TextView) findViewById(R.id.TextView));
		logPointer = instanceCounter++;
				
		SetImageView.set((ImageView) findViewById(R.id.ImageView), analysisResult.getName(), getBaseContext().getResources());
		registerForContextMenu((ImageView) findViewById(R.id.ImageView));
		
		updateResultButtons();
	}
	
	private void endingTone() {
		if(((AudioManager) getSystemService(AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_RING) == 0)
			((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
		else{
			try {
				MediaPlayer m = new MediaPlayer();
				m.setDataSource(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
				m.setAudioStreamType(AudioManager.STREAM_RING);
				m.prepare();
				m.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void statusToast(String statusMessage){
		Toast.makeText(getApplicationContext(), statusMessage, Toast.LENGTH_SHORT).show();
	}
	
	//Method to save Bitmap variable to file
	private void writeImageToFile(Bitmap outputImage, String saveLocation, int imageNumber){
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		outputImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
		File f = new File(rootLocation + File.separator + saveLocation + File.separator + String.format(Locale.ENGLISH, "%03d", imageNumber) + ".png");
		try {
			f.createNewFile();
			bytes.writeTo(new FileOutputStream(f));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Button takeVideo = (Button) findViewById(R.id.VideoIntent);
		Button processVideo = (Button) findViewById(R.id.VideoProcess);
		
		setBtnListenerOrDisable(takeVideo, mTakeVideo, MediaStore.ACTION_VIDEO_CAPTURE);
		setBtnListenerOrDisable(processVideo, mProcessVideo, MediaStore.ACTION_VIDEO_CAPTURE);
		
		updateResultButtons();
	}
	
	Button.OnClickListener mTakeVideo = new Button.OnClickListener(){
		public void onClick(View v){
			TakeVideoIntent();
		}
	};
	
	Button.OnClickListener mProcessVideo = new Button.OnClickListener(){	
		public void onClick(View v){
			ProcessVideo();			
		}
	};
	
	//Define menu
	public boolean onCreateOptionsMenu(Menu menu){
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	//Define context menu (for ImageView)
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo){
		super.onCreateContextMenu(menu, view, menuInfo);
		String keyModel = analysisResults.get(logPointer).getName();
		if (!keyModel.equals("No model found")){
			menu.setHeaderTitle(keyModel + " info");
			menu.add(0, view.getId(), 0, "Open Catalogue (requires login)");
			
			Key tempKey = new Key();
			boolean found = false;
			int i = 0;
			
			while(!found && i++ < databaseKeys.size()){
				if(keyModel.equals(databaseKeys.get(i).getName())){
					tempKey = databaseKeys.get(i);
					found = true;
				}
			}

			if(tempKey != null){
				menu.add(0, view.getId(), 0, "Length:\t\t\t" + String.format(Locale.ENGLISH, "%.3f", tempKey.getLength()) + "cm");
				menu.add(0, view.getId(), 0, "Degree:\t\t\t" + tempKey.getAngle() + "°");
			}
		}
	}
	
	public boolean onContextItemSelected(MenuItem item){
		if(item.getTitle().equals("Open Catalogue (requires login)")){
			//Use intent to create link (could have used Linkify, but this was much simpler since I only need one link in the whole app)
			Intent intent = new Intent(Intent.ACTION_VIEW);
	        intent.addCategory(Intent.CATEGORY_BROWSABLE);
	        intent.setData(Uri.parse("https://ekc.silca.biz/ricerche_stampa_chiave.php?chiave=" + analysisResults.get(logPointer).getName()));
	        startActivity(intent);
		}
		return true;  
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

	    switch (item.getItemId()) {
	        case R.id.settings:
	        	if (numOfPasses == 1)
	        		alert.setTitle("Current setting: 1 frame");
	        	else alert.setTitle("Current setting: " + numOfPasses + " frames");
	        	
	        	if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
    				statusToast("SD card not mounted!");
    			else if(!videoLocation.exists())
    				statusToast("No video file to analyse!");
    			else{
    				MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
    				vidFile.setDataSource(videoLocation.getAbsolutePath());
    				//Length of video file in seconds x10
    				final int maxFramesAvailable = (int) ((Long.parseLong(vidFile.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))*10)/1000);
    	        	alert.setMessage("Enter 1-"+ maxFramesAvailable +", or 0 for max possible");

    	        	//Set an EditText view to get user input 
    	        	final EditText input = new EditText(this);
    	        	input.setInputType(InputType.TYPE_CLASS_PHONE); //Set keyboard to phone keyboard (numerical keyboard was also an option)
    	        	alert.setView(input);
    	        	
    	        	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
    		        	public void onClick(DialogInterface dialog, int whichButton){
    		        		String inputString = input.getText().toString(); 
    		        		
    		        		try{
    		        			int value = Integer.parseInt(inputString);
    		        			
    		        			if (inputString.equals(""))
    			        			statusToast("No value entered!");
    		        			else if(value > maxFramesAvailable)
    			        			statusToast("Entered value larger than " + maxFramesAvailable + "!");
    		        			else if(value < 0)
    		        				statusToast("Can't analyse negative frames!");
    			        		else if(value == 0){
				        			numOfPasses = maxFramesAvailable;
				        			statusToast("Set to " + numOfPasses + " frames");
    			        		}
    			        		else{
    			        			numOfPasses = value;
    			        			if(numOfPasses == 1)
    			        				statusToast("Set to 1 frame");
    			        			else statusToast("Set to " + numOfPasses + " frames");
    			        		}
    		        		}
    		        		catch(NumberFormatException e){
    			        		statusToast("Number not entered!");
    		        	    }
    		        	}
    		        });

    	        	alert.setNegativeButton("Cancel", null);
    	        	alert.show();
    			}	        	
	            return true;
	            
	        case R.id.clear:
				if(analysisResults.isEmpty())
					statusToast("Nothing to clear");
				else{
	        		((TextView) findViewById(R.id.TextView)).setText("");
	        		((ImageView) findViewById(R.id.ImageView)).setVisibility(View.INVISIBLE);
	        		
	    			//Reset log count to end
	    			logPointer = instanceCounter;
	    			updateResultButtons();
	        		disableButton((Button) findViewById(R.id.NextLog));
	        	}
	        	return true;
	            
	        case R.id.temp_files:
	        	if (new File(rootLocation).exists()){
	        		deleteFolders(true);
	        		statusToast("All temp files deleted!");
	        	}
	        	else statusToast("No files to delete!");
	            return true;
	            
	        case R.id.temp_images:
	        	if (new File(rootLocation).exists()){
	        		deleteFolders(false);
	        		statusToast("All images deleted!");
	        	}
	        	else statusToast("No files to delete!");
	            return true;
	        
	        case R.id.quit:
	        	finish();
	        	return true;
	            
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	//Handles intent data and saves to default DCIM folder (file name handles by device)
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (resultCode == RESULT_OK)
			handleCameraVideo(data);
	}
	
	//Hijack intent data-stream, and save video file to defined location
	private void handleCameraVideo(Intent intent){
		new File(rootLocation).mkdir();
		try{
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fis = videoAsset.createInputStream();
		    FileOutputStream fos = new FileOutputStream(videoLocation);

		    byte[] buf = new byte[40960]; //Possible runtime optimisation available here - need to test with recording video with smaller buf sizes
		    int len;
		    while ((len = fis.read(buf)) > 0)
		        fos.write(buf, 0, len);   
		    fis.close();
		    fos.close();
		}
		catch (IOException e){
			 e.printStackTrace();
		}
	}
	
	//Some callbacks so that the variables can survive orientation change
	protected void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt("Passes", numOfPasses);
		outState.putInt("Instance", instanceCounter);
		outState.putInt("Current Log", logPointer);
	}
	
	//Restore variables on orientation change
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		numOfPasses = savedInstanceState.getInt("Passes");
		instanceCounter = savedInstanceState.getInt("Instance");
		logPointer = savedInstanceState.getInt("Current Log");
		
		String keyModel = analysisResults.get(logPointer).getName();
		SetImageView.set((ImageView) findViewById(R.id.ImageView), keyModel, getBaseContext().getResources());
		registerForContextMenu((ImageView) findViewById(R.id.ImageView));
		
		updateResultButtons();
	}
	
	//Taken from 'photobyintent' sample from developers.android.com
	public static boolean isIntentAvailable(Context context, String action){
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	private void setBtnListenerOrDisable(Button btn, Button.OnClickListener onClickListener, String intentName){
		if (isIntentAvailable(this, intentName))
			btn.setOnClickListener(onClickListener);
		else{
			btn.setText(getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}
	
	private void updateResultButtons(){		
		if(analysisResults.size() == 0 || !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			disableButton((Button) findViewById(R.id.LastLog));
			disableButton((Button) findViewById(R.id.NextLog));
		}
		else{
			if(logPointer < 1)
				disableButton((Button) findViewById(R.id.LastLog));
			else enableButton((Button) findViewById(R.id.LastLog), prevButtonListener);
			
			if(logPointer == instanceCounter-1)
				disableButton((Button) findViewById(R.id.NextLog));
			else enableButton((Button) findViewById(R.id.NextLog), nextButtonListener);
		}
	}
	
	Button.OnClickListener prevButtonListener = new Button.OnClickListener(){
		public void onClick(View v){
			logPointer--;
			if (logPointer < 0)
				logPointer = 0;
			else if (logPointer > (analysisResults.size()-1))
				logPointer = (analysisResults.size()-1);
			
			analysisResults.get(logPointer).setTextView((TextView) findViewById(R.id.TextView));
			String keyModel = analysisResults.get(logPointer).getName();
			
			SetImageView.set((ImageView) findViewById(R.id.ImageView), keyModel, getBaseContext().getResources());
			registerForContextMenu((ImageView) findViewById(R.id.ImageView));
			
			updateResultButtons();
		}
	};

	Button.OnClickListener nextButtonListener = new Button.OnClickListener(){	
		public void onClick(View v){
			logPointer++;
			if (logPointer > analysisResults.size()-1)
				logPointer = analysisResults.size()-1;
			else if (logPointer < 0)
				logPointer = 0;
			
			analysisResults.get(logPointer).setTextView((TextView) findViewById(R.id.TextView));
			String keyModel = analysisResults.get(logPointer).getName();
			
			SetImageView.set((ImageView) findViewById(R.id.ImageView), keyModel, getBaseContext().getResources());
			registerForContextMenu((ImageView) findViewById(R.id.ImageView));
			
			updateResultButtons();
		}
	};
	
	private void enableButton(Button btn, Button.OnClickListener buttonListener){
		btn.setClickable(true);
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(buttonListener);
	}
	
	private void disableButton(Button btn){
		btn.setClickable(false);
		btn.setVisibility(View.INVISIBLE);
	}
}