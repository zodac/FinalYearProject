/**
	Sheikh Arouge Mehdi (09498389)
	4th Year B.A.I. Engineering - School of Computer Science and Statistics
	October 2012 to April 2013
	KeyAnalyser for Android 2.3.3+, to determine model number of key (using Silca model numbers)
 */

package mehdis.KeyAnalyser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import mehdis.Entities.Key;
import mehdis.Entities.Result;
import mehdis.Entities.Settings;
import mehdis.Utils.DisplayUtilities;
import mehdis.Utils.FileUtilities;
import mehdis.Utils.FlingTouchListener;
import mehdis.Utils.MenuCreator;
import mehdis.Utils.SetImageView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class KeyAnalyserActivity extends Activity{	
	private static ArrayList<Result> results = new ArrayList<Result>();
	private static Context thisContext;
	
	public static Settings settings = Settings.getSettings();
	public static TextView resultView;
	public static TextView counterView;
	public static ImageView keyView;
	public ProgressBar progressBar;
	public static Result result;
	public static ArrayList<Key> databaseKeys = new ArrayList<Key>();
	private Handler mHandler = new Handler();
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		thisContext = this;
		
		((Button) findViewById(R.id.VideoIntent)).setOnClickListener(recordVideo);
		((Button) findViewById(R.id.VideoProcess)).setOnClickListener(processVideo);
		progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
		
		resultView = (TextView) findViewById(R.id.TextView);
		counterView = (TextView) findViewById(R.id.CounterView);
		keyView = (ImageView) findViewById(R.id.ImageView);
		
		keyView.setOnTouchListener(new FlingTouchListener());
		resultView.setOnTouchListener(new FlingTouchListener());
		registerForContextMenu(keyView);
		
		FileUtilities.createWorkFolders(this);
		generateKeyDatabase();
		
		if(!results.isEmpty()){
			showResult();
		}
	}

	public static void saveResult() {
		String modelName = result.getModelName();
		results.add(result);
		int numResults = results.size();
		
		result.setTextView(resultView, counterView, numResults, numResults);
		settings.incrementLogAndInstanceCounters();
		SetImageView.setImageViewFromLocalFile(thisContext.getResources(), keyView, modelName);
		
		DisplayUtilities.showNotification(thisContext, modelName);
	}
	
	private void generateKeyDatabase(){
		if(databaseKeys.isEmpty()){
			databaseKeys.add(new Key("MS2", 	4.2, 	90));
			databaseKeys.add(new Key("UL050", 	5.5, 	87.5));
			databaseKeys.add(new Key("UNI3", 	5.75, 	97.5));
			databaseKeys.add(new Key("UL054", 	6, 		92.5));
		}
	}
	
	private Button.OnClickListener recordVideo = new Button.OnClickListener(){
		public void onClick(final View view){
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
				startActivityForResult(takeVideoIntent, 3);
			} else{
				DisplayUtilities.statusToast(getApplicationContext(), String.valueOf(getText(R.string.sdCardNotMounted)));
			}
		}
	};
	
	private Button.OnClickListener processVideo = new Button.OnClickListener(){	
		public void onClick(final View view){
			if(FileUtilities.videoIsAvailable(thisContext)){
				new VideoProcesser(getApplicationContext(), settings.getNumOfPasses(), progressBar, mHandler).execute();
			}
		}
	};	
	
	public boolean onCreateOptionsMenu(Menu menu){
	    getMenuInflater().inflate(R.menu.menu, menu);
	    return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo){
		super.onCreateContextMenu(menu, view, menuInfo);
		String keyModelName = results.get(settings.getLogPointer()).getModelName();
		
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
	        intent.setData(Uri.parse(getText(R.string.silcaSite) + results.get(settings.getLogPointer()).getModelName()));
	        startActivity(intent);
		}
		return true;  
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		if(!MenuCreator.generateMenu(this, getApplicationContext(), item)){
			finish();
		}
		return true;
	}

	//Handles intent data and saves to default DCIM folder (file name handles by device)
	protected void onActivityResult(int requestCode, int resultCode, Intent intent){
		if (resultCode == RESULT_OK){
			handleCameraVideo(intent);
		}
	}
	
	//Kills process and (hopefully) its Asynctasks
	public void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }   
	
	//Hijack intent data-stream, and save video file to defined location
	private void handleCameraVideo(Intent intent){
		FileUtilities.createRootFolder();
		try{
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fileInput = videoAsset.createInputStream();
		    FileOutputStream fileOutput = new FileOutputStream(FileUtilities.VIDEO_LOCATION);

		    byte[] buf = new byte[4096]; //Possible optimisation available here - need to test with recording video with smaller buf sizes
		    int len;
		    while ((len = fileInput.read(buf)) > 0){
		    	fileOutput.write(buf, 0, len);
		    }
		    fileInput.close();
		    fileOutput.close();
		} catch (IOException e){
			 e.printStackTrace();
		}
	}
	
	public static void loadPreviousResult() {
		int logPointer = settings.getLogPointer();
		settings.decrementLogPointer();
		
		if(logPointer < 0){
			settings.setLogPointer(0);
		} else if(logPointer > (results.size()-1)){
			settings.setLogPointer((results.size()-1));
		}
		showResult();
	}
	
	public static void loadNextResult() {
		int logPointer = settings.getLogPointer();
		settings.incrementLogPointer();
		
		if(logPointer > results.size()-1){
			settings.setLogPointer(results.size()-1);
		} else if(logPointer < 0){
			settings.setLogPointer(0);
		}
		showResult();
	}
	
	private static void showResult() {
		int logPointer = settings.getLogPointer();
		if(logPointer >= 0 && logPointer < results.size()){
			results.get(logPointer).setTextView(resultView, counterView, settings.getLogPointer()+1, results.size());
			String keyModel = results.get(logPointer).getModelName();
			
			SetImageView.setImageViewFromLocalFile(thisContext.getResources(), keyView, keyModel);
		}
	}
}