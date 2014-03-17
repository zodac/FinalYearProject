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

import mehdis.Entities.Key;
import mehdis.Entities.Result;
import mehdis.Entities.ResultView;
import mehdis.Entities.Settings;
import mehdis.Utils.DisplayUtilities;
import mehdis.Utils.FileUtilities;
import mehdis.Utils.MenuCreator;
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
import android.widget.ProgressBar;

public class KeyAnalyserActivity extends Activity {	
	public static ArrayList<Result> results = new ArrayList<Result>();
	public static ArrayList<Key> databaseKeys = new ArrayList<Key>();
	private static Settings settings = Settings.getSettings();
	public static Context thisContext;
	public static ResultView resultsView;
	public static Result result;
	
	private static ProgressBar progressBar;
	private static Handler progressBarHandler = new Handler();

	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		thisContext = this;
		
		((Button) findViewById(R.id.VideoIntent)).setOnClickListener(recordVideo);
		((Button) findViewById(R.id.VideoProcess)).setOnClickListener(processVideo);
		progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
		
		resultsView = new ResultView(findViewById(R.id.KeyView), findViewById(R.id.TitleView), findViewById(R.id.ValueView), findViewById(R.id.CounterView));
		registerForContextMenu(resultsView.getKeyView());
		
		generateKeyDatabase();
		
		if(!results.isEmpty()){
			showResult();
		}
	}
	
	public void onConfigurationChanged(){
		setContentView(R.layout.main);
	}
	
	public static void resetProgressBar() {
		progressBarHandler.post(new Runnable(){
			public void run(){
				progressBar.setProgress(0);
			}
		});
	}
	
	public static void updateProgressBar(final int increment) {
		progressBarHandler.post(new Runnable(){
			public void run(){
				progressBar.setProgress(progressBar.getProgress() + increment);
			}
		});
	}

	public static void saveResult() {
		String modelName = result.getModelName();
		results.add(result);
		
		int numResults = results.size();
		settings.incrementLogAndInstanceCounters();
		resultsView.showResultInView(result, numResults, numResults);
		
		DisplayUtilities.showNotification(modelName);
	}
	
	private void generateKeyDatabase(){
		if(databaseKeys.isEmpty()){
			databaseKeys.add(new Key("MS2", 	4.2, 	90));
			databaseKeys.add(new Key("UL050", 	5.5, 	87.5));
			databaseKeys.add(new Key("UNI3", 	5.75, 	97.5));
			databaseKeys.add(new Key("UL054", 	6.0, 	92.5));
		}
	}
	
	private Button.OnClickListener recordVideo = new Button.OnClickListener(){
		public void onClick(final View view){
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
				startActivityForResult(takeVideoIntent, 3);
			} else{
				DisplayUtilities.statusToast(getText(R.string.sdCardNotMounted));
			}
		}
	};
	
	private Button.OnClickListener processVideo = new Button.OnClickListener(){	
		public void onClick(final View view){
			if(FileUtilities.videoIsAvailable()){
				FileUtilities.createWorkFolders();
				new VideoProcessor(settings.getNumOfPasses()).execute();
			}
		}
	};	
	
	public boolean onCreateOptionsMenu(Menu menu){
	    getMenuInflater().inflate(R.menu.menu, menu);
	    return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo){
		super.onCreateContextMenu(menu, view, menuInfo);
		String keyModelName = results.get(settings.getLogIndex()).getModelName();
		
		if(!keyModelName.equals(String.valueOf(getText(R.string.noModelFound)))){
			menu.setHeaderTitle(getText(R.string.info) + " " + keyModelName);
			menu.add(0, view.getId(), 0, String.valueOf(getText(R.string.openLink)));

			for(Key key : databaseKeys){
				if(keyModelName.equals(key.getModelName())){
					int viewId = view.getId();
					menu.add(0, viewId, 0, String.format(Settings.getSettings().getLocale(), "%-31s%.1fcm", String.valueOf(getText(R.string.length))+":", key.getLength()));
					menu.add(0, viewId, 0, String.format(Settings.getSettings().getLocale(), "%-30s%.1f°", String.valueOf(getText(R.string.tipAngle))+":", key.getAngle()));
				}
			}
		}
	}
	
	public boolean onContextItemSelected(MenuItem item){
		if(item.getTitle().equals(String.valueOf(getText(R.string.openLink)))){
			Intent intent = new Intent(Intent.ACTION_VIEW);
	        intent.addCategory(Intent.CATEGORY_BROWSABLE);
	        intent.setData(Uri.parse(getText(R.string.silcaSite) + results.get(settings.getLogIndex()).getModelName()));
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
	
	//Hijack intent data-stream, and save video file to defined location
	private void handleCameraVideo(Intent intent){
		FileUtilities.createRootFolder();
		try{
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fileInput = videoAsset.createInputStream();
		    FileOutputStream fileOutput = new FileOutputStream(FileUtilities.VIDEO_FILE);

		    byte[] buffer = new byte[4096]; //Possible optimisation available here - need to test with recording video with smaller buffer sizes
		    int length;
		    while ((length = fileInput.read(buffer)) > 0){
		    	fileOutput.write(buffer, 0, length);
		    }
		    fileInput.close();
		    fileOutput.close();
		} catch (IOException e){
		}
	}
	
	public static void loadPreviousResult() {
		int logIndex = settings.getLogIndex();
		settings.decrementLogIndex();
		
		if(logIndex < 0){
			settings.setLogIndex(0);
		} else if(logIndex > (results.size()-1)){
			settings.setLogIndex((results.size()-1));
		}
		showResult();
	}
	
	public static void loadNextResult() {
		int logIndex = settings.getLogIndex();
		settings.incrementLogIndex();
		
		if(logIndex > results.size()-1){
			settings.setLogIndex(results.size()-1);
		} else if(logIndex < 0){
			settings.setLogIndex(0);
		}
		showResult();
	}
	
	private static void showResult() {
		int logIndex = settings.getLogIndex();
		if(logIndex >= 0 && logIndex < results.size()){
			resultsView.showResultInView(results.get(logIndex), logIndex+1, results.size());
		}
	}

	public static void loadResultsFromList(ArrayList<Result> inputResults) {
		results.clear();
		for(Result result : inputResults){
			results.add(result);
		}
		int numOfResults = results.size();
		
		settings.setInstanceCounter(numOfResults);
		settings.setLogIndex(numOfResults-1);
		resultsView.showResultInView(results.get(numOfResults-1), numOfResults, numOfResults);
	}
}