package com.example.android.photobyintent;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvGet2D;
//import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class PhotoIntentActivity extends Activity {	
	
	int image;
	File tempFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser_tmp");
	File rawImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser_tmp" + File.separator + "images_tmp");
	File procImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser_tmp" + File.separator + "Procimages_tmp");
	File lengthImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser_tmp" + File.separator + "Lengthimages_tmp");
	File v1 = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser_tmp" + File.separator + "video.mp4"); 	
	
	private void dispatchTakeVideoIntent() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, 3);
	}
	
	private void dispatchProcVideoIntent() throws IOException{
		
		//Variables
		long start = System.currentTimeMillis();
		int counter, c = 0, min_row = 0, max_row = 0, i, j;
		int first_red = 0, last_red = 0, max_red = 0, max_red_sum = 0, first_red_ave = 0, last_red_ave = 0, row_ave = 0;
		double pixel_cm = 0, length = 0;
		boolean min_found = false;
		CvScalar test;
		
		image = 1;
		File videoPath = v1;
		String video = videoPath.getAbsolutePath();
		MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
		vidFile.setDataSource(video);

		//Create folder to store images
		rawImageFolder.mkdir();
		
		//String value = vidFile.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		//long vidLength = (Long.parseLong(value)/1000); //Returns milliseconds - divide by 1,000

		//for(i = 0; i <= 10*vidLength ; i++, image++) //10*vidLength since I'm getting frames every 1/10th sec
		for(i = 0; i < 3; i++, image++)
		{
			Bitmap bmp = vidFile.getFrameAtTime(100000*i, MediaMetadataRetriever.OPTION_CLOSEST);
					
			if(bmp != null)
				WriteImageToFile(bmp, "images_tmp", image);
			else if (bmp == null)
				break;
		}
		
		vidFile.release();
		counter = image;
		image = 1;
		//StatusToast("Frames Extracted");
		
		/** Red area **/
		
		for (j = 0; j <= counter; j++, image++){
			
			File imagePath = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser_tmp" + File.separator + "images_tmp" + File.separator + String.format(Locale.ENGLISH, "%03d", image) + ".png");
			String img = imagePath.getAbsolutePath();
			Bitmap bmp = BitmapFactory.decodeFile(img);
			max_red = 1;
			
			if (bmp != null)
	        {
				IplImage source = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 4);
				source = cvLoadImage(img);
				
				for (c = 0; c < source.width(); c++)
				{
					int red_count = 0;
					
					for (int r = 0; r < source.height(); r++)
					{
						test = cvGet2D(source, r, c);
						int red = (int) test.getVal(2);
			            int green = (int) test.getVal(1);
			            int blue = (int) test.getVal(0);
			            
			            if (red > 140 && green < 75 && blue < 75)
			            	red_count++;
					}
					
					if (red_count > max_red) //# of red pixels in column with most red pixels
						max_red = red_count;
					
					if (red_count > 10) //Red area, by start and finish columns
					{
						if (first_red == 0)
							first_red = c;
						else if (first_red > 0){
							last_red = c;
						}
					}
				}

				max_red_sum = max_red_sum + max_red; //Running total (NOT average)			
	
				first_red_ave = first_red_ave + first_red;
				last_red_ave = last_red_ave + last_red;
				
				if (j > 0)
				{
					first_red_ave = first_red_ave/2;
					last_red_ave = last_red_ave/2;					
				}
	        }
			else if (bmp == null)
				break;
		}
		//StatusToast("Red area analysed");
		
		/** BT of key **/
		
		image = 1;
		procImageFolder.mkdir();
	
		for(j = 0; j <= counter; j++, image++)
		{
			File imagePath = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser_tmp" + File.separator + "images_tmp" + File.separator + String.format(Locale.ENGLISH, "%03d", image) + ".png");
			String img = imagePath.getAbsolutePath();
			Bitmap bmp = BitmapFactory.decodeFile(img);
			c = first_red_ave;
			min_found = false;
			
			if (bmp != null)
	        {
				IplImage source = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 4);
				source = cvLoadImage(img);
		        Bitmap temp = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888);
				//IplImage canny = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 1);
		        //cvCvtColor(source, canny, CV_BGR2GRAY); 
		        //cvCanny(source, canny, 50, 100, 3);
		        
		        for(int r = 0; r < source.height(); r++) {
		        	for(c = first_red_ave; c <= last_red_ave; c++)
			        {
			            test = cvGet2D(source, r, c);
			
			            int red = (int) test.getVal(2);
			            int green = (int) test.getVal(1);
			            int blue = (int) test.getVal(0);
			            
			           /* test = cvGet2D(canny, r, c);
			            int red1 = (int) test.getVal(2);
			            int green1 = (int) test.getVal(1);
			            int blue1 = (int) test.getVal(0);

			            if (blue1 > 100) {
			            	temp.setPixel(c,  r, Color.argb(255, 255, 255, blue1)); //Object pixel
			            	/*if (!min_found)
			            	{
			            		min_found = true;
			            		min_row = r;
			            	}
			            	else if (min_found == true)
			            		max_row = r;
			            }
			            else temp.setPixel(c,  r, Color.argb(255, red1, green1, blue1)); //Background pixel
			            */
			            if (red > 90 && green > 175 && blue > 175){}
			            if (red > 90 && green < red*1.25 && blue < red*1.25){}
			            else{
			            	temp.setPixel(c, r, Color.argb(255, 255, 255, 255));
			            	
			            	if (!min_found)
			            	{
			            		min_found = true;
			            		min_row = r;
			            	}
			            	else if (min_found == true)
			            		max_row = r;
			            }
			        }
			    }			 
		        
				WriteImageToFile(temp, "Procimages_tmp", image);
				row_ave = row_ave + (max_row-min_row);
				
				if (j > 0 && max_row-min_row < row_ave*1.25 && max_row-min_row > row_ave*0.75)
					row_ave = (row_ave)/2;				
	        }			
			else if (bmp == null)
				break;
		}
		//StatusToast("Length calculated");

        //DeleteFolder(tempFolder); //Delete all temp files
		DeleteFolder(rawImageFolder);
		DeleteFolder(procImageFolder);
		
		row_ave = row_ave/i;
		max_red_sum = max_red_sum/i;
		pixel_cm = 17.5/max_red_sum;
		length = row_ave*pixel_cm;
		String keyModel = "No model found";
		
		if (length > 5.2 && length < 5.4)
			keyModel = "UL 050";
		
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
        //MediaPlayer mPlayer = MediaPlayer.create(getBaseContext(), R.raw.op);
        //mPlayer.start()
        
        long end = System.currentTimeMillis();
		StatusAlert(keyModel,
					"Length                 " + String.format(Locale.ENGLISH, "%.3f", length) +"cm\n" +
					"\nProcess time       " + ((end-start)/1000) + " seconds" + 
					"\nRow Delt              " + row_ave + 
					"\nRatio pixel/cm    " + String.format(Locale.ENGLISH, "%.3f", pixel_cm) +
					"\nFirst Red Col       " + first_red_ave + 
					"\nLast Red Col       " + last_red_ave + 
					"\nPasses                " + i);
	}	
	
	//Method to delete a file or directory (and any content it may have)
	void DeleteFolder(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteFolder(child);
	    fileOrDirectory.delete();
	}
	
	void StatusToast(String statusString){
		Toast status = Toast.makeText(getApplicationContext(), statusString, Toast.LENGTH_LONG);
		status.show();
	}
	
	void StatusAlert(String title, String message){
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE , "Done", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.show();
		
	}
	
	void WriteImageToFile(Bitmap source, String location, int image_number) throws IOException{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.compress(Bitmap.CompressFormat.PNG, 100, bytes);
		File f = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser_tmp" + File.separator + location + File.separator + String.format(Locale.ENGLISH, "%03d", image_number) + ".png");
		f.createNewFile();
		FileOutputStream fo = new FileOutputStream(f);
		bytes.writeTo(fo);
	}
	
	private void handleCameraVideo(Intent intent) {
		tempFolder.mkdir();
		
		try {
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fis = videoAsset.createInputStream();
		    File tmpFile = v1; 
		    FileOutputStream fos = new FileOutputStream(tmpFile);

		    byte[] buf = new byte[40960];
		    int len;
		    while ((len = fis.read(buf)) > 0) {
		        fos.write(buf, 0, len);
		    }       
		    fis.close();
		    fos.close();
		 } catch (IOException io_e){
			 io_e.printStackTrace();
		 }
	}

	Button.OnClickListener mTakeVidOnClickListener = 
		new Button.OnClickListener() {
		public void onClick(View v) {
			dispatchTakeVideoIntent();
		}
	};
	
	Button.OnClickListener mProcVidOnClickListener = 
			new Button.OnClickListener() {
			
		public void onClick(View v) {
				try{
					dispatchProcVideoIntent();
				}
				catch (IOException e){
					e.printStackTrace();
				}
		}
	};

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);

		Button vidBtn = (Button) findViewById(R.id.btnIntendV);
		setBtnListenerOrDisable( 
				vidBtn, 
				mTakeVidOnClickListener,
				MediaStore.ACTION_VIDEO_CAPTURE
		);
		
		Button vidProc = (Button) findViewById(R.id.btnProc);
		setBtnListenerOrDisable(
				vidProc,
				mProcVidOnClickListener,
				MediaStore.ACTION_VIDEO_CAPTURE
		);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {

		case 3: {
			if (resultCode == RESULT_OK) {
				handleCameraVideo(data);
			}
			break;
		} // ACTION_TAKE_VIDEO
		} // switch
	}
	
	// Some lifecycle callbacks so that the image can survive orientation change
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	/*** Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 */
	
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	private void setBtnListenerOrDisable( 
			Button btn, 
			Button.OnClickListener onClickListener,
			String intentName
	) {
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);        	
		} else {
			btn.setText( 
				getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}
}