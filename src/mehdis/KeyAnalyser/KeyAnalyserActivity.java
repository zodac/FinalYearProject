/**
	Sheikh Arouge Mehdi (09498389)
	4th Year B.A.I. Engineering - School of Computer Science and Statistics
	October 2012 to April 2013
	KeyAnalyser for Android 2.3.3+, to determine model number of key (using Silca model numbers)
 */

package mehdis.KeyAnalyser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class KeyAnalyserActivity extends Activity{	
	private static final String ROOT_LOCATION = Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser";
	private static final File VIDEO_LOCATION = new File(ROOT_LOCATION + File.separator + "video.mp4");
	
	private int numOfPasses = 1;
	public int instanceCounter = 0;
	public int logPointer = 0;
	private float prevX = -1;
	private boolean onTouchBlocked = false;
	private Handler onTouchHandler = new Handler();
	private static ArrayList<Result> analysisResults = new ArrayList<Result>();
	private TextView resultView;
	private TextView counterView;
	private ImageView keyView;
	
	public static ArrayList<Key> databaseKeys = new ArrayList<Key>();
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		((Button) findViewById(R.id.VideoIntent)).setOnClickListener(mTakeVideo);
		((Button) findViewById(R.id.VideoProcess)).setOnClickListener(mProcessVideo);
		
		resultView = (TextView) findViewById(R.id.TextView);
		counterView = (TextView) findViewById(R.id.CounterView);
		keyView = (ImageView) findViewById(R.id.ImageView);
		
		keyView.setOnTouchListener(new MyTouchListener());
		resultView.setOnTouchListener(new MyTouchListener());
		registerForContextMenu(keyView);
		generateKeyDatabase();
	}
	
	private void processVideo(){
		VideoProcesser vP = new VideoProcesser(getApplicationContext(), numOfPasses);
		Result analysisResult = vP.processVideo();
		saveResult(analysisResult);
		endingTone();
	}
	
	private void TakeVideoIntent(){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			startActivityForResult(takeVideoIntent, 3);
		} else{
			statusToast(String.valueOf(getText(R.string.sdCardNotMounted)));
		}
	}

	private void saveResult(Result analysisResult) {
		analysisResults.add(analysisResult);
		analysisResults.get(instanceCounter).setTextView(resultView, counterView, logPointer+1, analysisResults.size());
		logPointer = instanceCounter++;
				
		SetImageView.setImageViewFromLocalFile(keyView, analysisResult.getModelName());
	}
	
	
	private void deleteWorkFolders(boolean all){
		if(all){
			deleteDirectory(ROOT_LOCATION);
		} else{
			deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(getText(R.string.rawImagesFolder)));
    		deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(getText(R.string.cannyImagesFolder)));
    		deleteDirectory(ROOT_LOCATION + File.separator + String.valueOf(getText(R.string.degreeImagesFolder)));
		}
	}
	
	private void deleteDirectory(String inputDirectory){
		File fileOrDirectory = new File(inputDirectory);
		if (fileOrDirectory.isDirectory()){
	        for (File child : fileOrDirectory.listFiles())
	            deleteDirectory(child.toString());
		}
	    fileOrDirectory.delete();
	}

	

	private void generateKeyDatabase(){
		databaseKeys.add(new Key("MS2", 	4.2, 	90));
		databaseKeys.add(new Key("UL050", 	5.5, 	87.5));
		databaseKeys.add(new Key("UNI3", 	5.75, 	97.5));
		databaseKeys.add(new Key("UL054", 	6, 		92.5));
	}
	
	
	
	private void endingTone() {
		if(isPhoneSilent()){
			((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
		} else{
			try {
				MediaPlayer completedNotification = new MediaPlayer();
				completedNotification.setDataSource(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
				completedNotification.setAudioStreamType(AudioManager.STREAM_RING);
				completedNotification.prepare();
				completedNotification.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean isPhoneSilent() {
		return ((AudioManager) getSystemService(AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_RING) == 0;
	}
	
	private void statusToast(String statusMessage){
		Toast.makeText(getApplicationContext(), statusMessage, Toast.LENGTH_SHORT).show();
	}
	
	private Button.OnClickListener mTakeVideo = new Button.OnClickListener(){
		public void onClick(final View view){
			TakeVideoIntent();
		}
	};
	
	private Button.OnClickListener mProcessVideo = new Button.OnClickListener(){	
		public void onClick(final View view){
			processVideo();			
		}
	};
	
	public boolean onCreateOptionsMenu(Menu menu){
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo){
		super.onCreateContextMenu(menu, view, menuInfo);
		String keyModelName = analysisResults.get(logPointer).getModelName();
		
		if(!keyModelName.equals(String.valueOf(getText(R.string.noModelFound)))){
			menu.setHeaderTitle(keyModelName + " info");
			menu.add(0, view.getId(), 0, String.valueOf(getText(R.string.openCatalogue)));

			for(Key key : databaseKeys){
				if(keyModelName.equals(key.getModelName())){
					int viewId = view.getId();
					menu.add(0, viewId, 0, String.format(Locale.ENGLISH, "%-15s%.1fcm", "Length:", key.getLength()));
					menu.add(0, viewId, 0, String.format(Locale.ENGLISH, "%-15s%.1f°", "Degree:", key.getAngle()));
				}
			}
		}
	}
	
	public boolean onContextItemSelected(MenuItem item){
		if(item.getTitle().equals(String.valueOf(getText(R.string.openCatalogue)))){
			Intent intent = new Intent(Intent.ACTION_VIEW);
	        intent.addCategory(Intent.CATEGORY_BROWSABLE);
	        intent.setData(Uri.parse(getText(R.string.silcaSite) + analysisResults.get(logPointer).getModelName()));
	        startActivity(intent);
		}
		return true;  
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

	    switch (item.getItemId()) {
	        case R.id.settings:
	        	if (numOfPasses == 1){
	        		alert.setTitle("Current setting: 1 frame");
	        	} else{
	        		alert.setTitle("Current setting: " + numOfPasses + " frames");
	        	}
	        	
	        	if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
    				statusToast(String.valueOf(getText(R.string.sdCardNotMounted)));
	        	} else if(!VIDEO_LOCATION.exists()){
    				statusToast(String.valueOf(getText(R.string.noVideoFile)));
	        	} else{
    				MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
    				vidFile.setDataSource(VIDEO_LOCATION.getAbsolutePath());
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
    		        			
    		        			if (inputString.equals("")){
    			        			statusToast("No value entered!");
    		        			} else if(value > maxFramesAvailable){
    			        			statusToast("Entered value larger than " + maxFramesAvailable + "!");
    		        			} else if(value < 0) {
    		        				statusToast("Can't analyse negative frames!");
    		        			} else if(value == 0){
				        			numOfPasses = maxFramesAvailable;
				        			statusToast("Set to " + numOfPasses + " frames");
    			        		} else{
    			        			numOfPasses = value;
    			        			if(numOfPasses == 1){
    			        				statusToast("Set to 1 frame");
    			        			} else{
    			        				statusToast("Set to " + numOfPasses + " frames");
    			        			}
    			        		}
    		        		} catch(NumberFormatException e){
    			        		statusToast("Number not entered!");
    		        	    }
    		        	}
    		        });

    	        	alert.setNegativeButton("Cancel", null);
    	        	alert.show();
    			}	        	
	            return true;
	            
	        case R.id.clear:
				if(analysisResults.isEmpty()){
					statusToast(String.valueOf(getText(R.string.nothingToClear)));
				} else{
	        		resultView.setText("");
	        		counterView.setText("");
	        		keyView.setImageDrawable(null);
	        		
	    			logPointer = instanceCounter;
	        	}
	        	return true;
	            
	        case R.id.temp_files:
	        	if (new File(ROOT_LOCATION).exists()){
	        		deleteWorkFolders(true);
	        		statusToast(String.valueOf(getText(R.string.tempFilesDeleted)));
	        	} else{
	        		statusToast(String.valueOf(getText(R.string.noFilesToDelete)));
	        	}
	            return true;
	            
	        case R.id.temp_images:
	        	if (new File(ROOT_LOCATION).exists()){
	        		deleteWorkFolders(false);
	        		statusToast(String.valueOf(getText(R.string.imagesDeleted)));
	        	} else{
	        		statusToast(String.valueOf(getText(R.string.noFilesToDelete)));
	        	}
	            return true;
	        
	        case R.id.quit:
	        	finish();
	        	return true;
	            
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	//Handles intent data and saves to default DCIM folder (file name handles by device)
	protected void onActivityResult(int requestCode, int resultCode, Intent intent){
		if (resultCode == RESULT_OK){
			handleCameraVideo(intent);
		}
	}
	
	//Hijack intent data-stream, and save video file to defined location
	private void handleCameraVideo(Intent intent){
		new File(ROOT_LOCATION).mkdir();
		try{
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fis = videoAsset.createInputStream();
		    FileOutputStream fos = new FileOutputStream(VIDEO_LOCATION);

		    byte[] buf = new byte[4096]; //Possible optimisation available here - need to test with recording video with smaller buf sizes
		    int len;
		    while ((len = fis.read(buf)) > 0){
		    	fos.write(buf, 0, len);
		    }
		    fis.close();
		    fos.close();
		} catch (IOException e){
			 e.printStackTrace();
		}
	}
	
	public void loadPreviousResult() {
		logPointer--;
		if(logPointer < 0){
			logPointer = 0;
		} else if(logPointer > (analysisResults.size()-1)){
			logPointer = (analysisResults.size()-1);
		}
		showResult();
	}
	
	public void loadNextResult() {
		logPointer++;
		if(logPointer > analysisResults.size()-1){
			logPointer = analysisResults.size()-1;
		} else if(logPointer < 0){
			logPointer = 0;
		}
		showResult();
	}
	
	public void showResult() {
		analysisResults.get(logPointer).setTextView(resultView, counterView, logPointer+1, analysisResults.size());
		String keyModel = analysisResults.get(logPointer).getModelName();
		
		SetImageView.setImageViewFromLocalFile(keyView, keyModel);
	}
	
	private class MyTouchListener implements OnTouchListener{
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			final float currentX = event.getX();
            
            if(!onTouchBlocked){
            	onTouchBlocked = true;
            	onTouchHandler.postDelayed(new Runnable() {
            		@Override
					public void run() {
						onTouchBlocked = false;
						if(prevX == -1){
			            	prevX = currentX;
			            }
						float delta = prevX-currentX; 
						
			            if(delta < 0){
			            	loadNextResult();
			            } else if(delta > 0){
			            	loadPreviousResult();
			            }
					}
				}, 500);
            }          
            return false;
		}
	}
}