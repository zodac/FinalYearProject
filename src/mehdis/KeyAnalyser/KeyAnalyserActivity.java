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

import mehdis.Entities.Key;
import mehdis.Entities.Result;
import mehdis.Entities.Settings;
import mehdis.Utils.FlingTouchListener;
import mehdis.Utils.MenuCreator;
import mehdis.Utils.SetImageView;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class KeyAnalyserActivity extends Activity{	
	public static final String ROOT_LOCATION = Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser";
	public static final File VIDEO_LOCATION = new File(ROOT_LOCATION + File.separator + "video.mp4");
	
	private static ArrayList<Result> processResults = new ArrayList<Result>();
	private static Context thisContext;
	
	public static Settings settings = Settings.getDefaultSettings();
	public static TextView resultView;
	public static TextView counterView;
	public static ImageView keyView;
	public static Result result;
	public static ArrayList<Key> databaseKeys = new ArrayList<Key>();
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		thisContext = this;
		
		((Button) findViewById(R.id.VideoIntent)).setOnClickListener(mTakeVideo);
		((Button) findViewById(R.id.VideoProcess)).setOnClickListener(mProcessVideo);
		
		resultView = (TextView) findViewById(R.id.TextView);
		counterView = (TextView) findViewById(R.id.CounterView);
		keyView = (ImageView) findViewById(R.id.ImageView);
		
		keyView.setOnTouchListener(new FlingTouchListener());
		resultView.setOnTouchListener(new FlingTouchListener());
		registerForContextMenu(keyView);
		
		createWorkFolders();
		
		if(databaseKeys.isEmpty()){
			generateKeyDatabase();
		}
		if(!processResults.isEmpty()){
			showResult();
		}
	}
	
	private void createWorkFolders(){
		new File(ROOT_LOCATION + File.separator + String.valueOf(getText(R.string.rawImagesFolder))).mkdir();
		new File(ROOT_LOCATION + File.separator + String.valueOf(getText(R.string.cannyImagesFolder))).mkdir();
		new File(ROOT_LOCATION + File.separator + String.valueOf(getText(R.string.degreeImagesFolder))).mkdir();
	}
	
	public static void setResult(Result currentResult){
		result = currentResult;
	}
	
	private void TakeVideoIntent(){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			startActivityForResult(takeVideoIntent, 3);
		} else{
			Toast.makeText(getApplicationContext(), String.valueOf(getText(R.string.sdCardNotMounted)), Toast.LENGTH_SHORT).show();
		}
	}

	public static void saveResult() {
		processResults.add(result);
		result.setTextView(resultView, counterView, settings.getLogPointer()+1, processResults.size());
		settings.incrementLogAndInstanceCounters();
				
		SetImageView.setImageViewFromLocalFile(thisContext.getResources(), keyView, result.getModelName());
	}
	
	private void generateKeyDatabase(){
		databaseKeys.add(new Key("MS2", 	4.2, 	90));
		databaseKeys.add(new Key("UL050", 	5.5, 	87.5));
		databaseKeys.add(new Key("UNI3", 	5.75, 	97.5));
		databaseKeys.add(new Key("UL054", 	6, 		92.5));
	}
	
	private Button.OnClickListener mTakeVideo = new Button.OnClickListener(){
		public void onClick(final View view){
			TakeVideoIntent();
		}
	};
	
	private Button.OnClickListener mProcessVideo = new Button.OnClickListener(){	
		public void onClick(final View view){
			new VideoProcesser(getApplicationContext(), settings.getNumOfPasses()).execute();
		}
	};
	
	public static void showNotification(){
		Uri completionSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		Intent intent = new Intent(thisContext, KeyAnalyserActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(thisContext, 0, intent, 0);
		
        Notification notification = new NotificationCompat.Builder(thisContext)
			.setContentTitle("Analysis #" + processResults.size() + " complete")
			.setContentText(result.getModelName())
			.setSmallIcon(R.drawable.notification)
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.setSound(completionSound)
			.addAction(R.drawable.launcher, "View", pendingIntent)
			.addAction(0, "Remind", pendingIntent)
			.build();
			
        NotificationManager notificationManager = (NotificationManager) thisContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(settings.getInstanceCounter(), notification);
        
        if(isPhoneSilent()){
			((Vibrator) thisContext.getSystemService(VIBRATOR_SERVICE)).vibrate(300);
		}
	}
	
	private static boolean isPhoneSilent() {
		return ((AudioManager) thisContext.getSystemService(AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_RING) == 0;
	}
	
	
	public boolean onCreateOptionsMenu(Menu menu){
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo){
		super.onCreateContextMenu(menu, view, menuInfo);
		String keyModelName = processResults.get(settings.getLogPointer()).getModelName();
		
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
	        intent.setData(Uri.parse(getText(R.string.silcaSite) + processResults.get(settings.getLogPointer()).getModelName()));
	        startActivity(intent);
		}
		return true;  
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		return MenuCreator.generateMenu(this, getApplicationContext(), item);
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
		    FileInputStream fileInput = videoAsset.createInputStream();
		    FileOutputStream fileOutput = new FileOutputStream(VIDEO_LOCATION);

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
		} else if(logPointer > (processResults.size()-1)){
			settings.setLogPointer((processResults.size()-1));
		}
		showResult();
	}
	
	public static void loadNextResult() {
		int logPointer = settings.getLogPointer();
		settings.incrementLogPointer();
		if(logPointer > processResults.size()-1){
			settings.setLogPointer(processResults.size()-1);
		} else if(logPointer < 0){
			settings.setLogPointer(0);
		}
		showResult();
	}
	
	private static void showResult() {
		int logPointer = settings.getLogPointer();
		processResults.get(logPointer).setTextView(resultView, counterView, logPointer+1, processResults.size());
		String keyModel = processResults.get(logPointer).getModelName();
		
		SetImageView.setImageViewFromLocalFile(thisContext.getResources(), keyView, keyModel);
	}
}