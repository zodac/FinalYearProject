package com.example.android.photobyintent;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvGet2D;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCanny;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

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
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class PhotoIntentActivity extends Activity {	
	
	int passes = 1, counter = 0;
	String keyModel = "No model found"; //Default output in case of no matches
	double length, degree;
	long time;
	boolean finished = false;
	
	double MS_2 = 4.2, UL_050 = 5.5, UNI_3 = 5.75, UL_054 = 5.9;
	int MS_2d = 90, UL_050d = 78, UNI_3d = 105, UL_054d = 95;
	File v1 = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + "video.mp4");
	File tempFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser");
	File rawImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser" + File.separator + "1_raw_images");
	File cannyImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser" + File.separator + "2_canny_images");
	File degreeImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser" + File.separator + "3_degree_images");	
	
	private void dispatchTakeVideoIntent() {
		
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			StatusToast("SD card not mounted!");
		else
			startActivityForResult(takeVideoIntent, 3);
	}
	
	private void dispatchProcVideoIntent() throws IOException, Exception{
		
		/** Initialisation **/
		
		long start = System.currentTimeMillis();
		int red_count, c = 0, r = 0, min_row = 0, max_row = 0, i, image = 1, first_red = 0, last_red = 0, max_red = 0, max_red_col = 0, max_red_row = 0;
		int first_red_col = 0, last_red_col = 0, row_ave = 0, first_red_row = 0, last_red_row = 0;
		double rho = 0, theta = 0, rho_ave = 0, theta_ave = 0;
		
		CvScalar test;

		//Check it SD card is mounted, and (if it is) if video.mp4 exists
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			StatusToast("SD card not mounted!");
		else if (!v1.exists())
			StatusToast("No video file to analyse!");

		//Create folders
		rawImageFolder.mkdir();
		cannyImageFolder.mkdir(); //Only needed for debugging - left in for now
		degreeImageFolder.mkdir();
		
		MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
		vidFile.setDataSource(v1.getAbsolutePath());

		for(i = 0; i < passes; i++, image++, counter++)
		{
			Bitmap bmp = vidFile.getFrameAtTime(100000*i, MediaMetadataRetriever.OPTION_CLOSEST);
					
			if(bmp != null)
				WriteImageToFile(bmp, "1_raw_images", image);
			else if (bmp == null)
				break;
		}
		
		vidFile.release();
        
		/** Calculate pixel:cm ratio using red area **/
		
		for (i = 0, image = 1; i < counter; i++, image++){
			
			File imagePath = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + "1_raw_images" + File.separator + String.format(Locale.ENGLISH, "%03d", image) + ".png");
			String img = imagePath.getAbsolutePath();
			Bitmap bmp = BitmapFactory.decodeFile(img);
			max_red = 1;
			
			if (bmp != null)
	        {
				IplImage source = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 4);
				source = cvLoadImage(img);
				
				for (c = 0; c < source.width(); c++)
				{
					red_count = 0;
					
					for (r = 0; r < source.height(); r++)
					{
						test = cvGet2D(source, r, c);
						int red = (int) test.getVal(2);
			            int green = (int) test.getVal(1);
			            int blue = (int) test.getVal(0);
			            
			            if (red > 130 && green < 75 && blue < 75)
			            	red_count++;
					}
					
					if (red_count > max_red) //# of red pixels in column with most red pixels
						max_red = red_count;
					
					if (red_count > 10) //Red area, by start and finish columns, for Canny/BT boundaries
					{
						if (first_red == 0)
							first_red = c;
						else if (first_red > 0)
							last_red = c;
					}
				}
				max_red_col = max_red_col + max_red; //Running total (NOT average)			
	
				first_red_col = first_red_col + first_red;
				last_red_col = last_red_col + last_red;
				
				if (i > 0)
				{
					first_red_col = first_red_col/2;
					last_red_col = last_red_col/2;					
				}
	
				max_red = 1;
				first_red = last_red = 0;
				
				for (r = 0; r < source.height(); r++)
				{
					red_count = 0;
					
					for (c = 0; c < source.width(); c++)
					{
						test = cvGet2D(source, r, c);
						int red = (int) test.getVal(2);
			            int green = (int) test.getVal(1);
			            int blue = (int) test.getVal(0);
			            
			            if (red > 130 && green < 75 && blue < 75)
			            	red_count++;
					}
					
					if (red_count > max_red) //# of red pixels in row with most red pixels
						max_red = red_count;
					
					if (red_count > 15) //Red area, by start and finish rows, for Canny boundaries
					{
						if (first_red == 0)
							first_red = r;
						else if (first_red > 0)
							last_red = r;
					}
				}
				max_red_row = max_red_row + max_red; //Running total (NOT average)			
	
				first_red_row = first_red_row + first_red;
				last_red_row = last_red_row + last_red;
				
				if (i > 0)
				{
					first_red_row = first_red_row/2;
					last_red_row = last_red_row/2;					
				}	
	        }
			else if (bmp == null)
				break;
		}
		
		//Red area bound by first_red_col, last_red_col & first_red_row, last_red_row - set limits accordingly for Canny
		
		/** Canny & angle of profile**/
		
		for(i = 0, image = 1; i < counter; i++, image++)
		{
			File imagePath = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + "1_raw_images" + File.separator + String.format(Locale.ENGLISH, "%03d", image) + ".png");
			String img = imagePath.getAbsolutePath();
			Bitmap bmp = BitmapFactory.decodeFile(img);
			boolean min_found = false;
			
			if (bmp != null)
	        {
				IplImage source = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 4);
				IplImage canny = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 1);
				source = cvLoadImage(img);
		        Bitmap temp = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888);
		        cvCanny(source, canny, 50, 150, 3);
		        int row_offset = bmp.getHeight()/200, col_offset = bmp.getWidth()/110;
		        
		        for(r = first_red_row+(4*row_offset); r < last_red_row-(4*row_offset); r++) {
		        	for(c = first_red_col+(7*col_offset); c < last_red_col-(7*col_offset); c++)
			        {
		        		test = cvGet2D(canny, r, c);
			            int keyPixel = (int) test.getVal(0);

			            if (keyPixel > 100){
			            	temp.setPixel(c,  r, Color.argb(255, 255, 255, keyPixel)); //Object pixel
			            	if (!min_found)
			            	{
			            		min_found = true;
			            		min_row = r;
			            	}
			            	else if (min_found == true)
			            		max_row = r;
			            }
			            else temp.setPixel(c,  r, Color.argb(255, 0, 0, keyPixel)); //Background pixel
			        }
		        }
				WriteImageToFile(temp, "2_canny_images", image);
				
				temp = Bitmap.createBitmap((last_red_col-(source.width()/3))-(first_red_col+(source.width()/3)), (7*row_offset), Bitmap.Config.ARGB_8888);
				
				int source_col = first_red_col+(source.width()/3);
				
				for (c = 0; c < ((last_red_col-(source.width()/3)) - (first_red_col+(source.width()/3))); c++, source_col++)
				{
					int source_row = max_row-(6*row_offset);
					for (r = 0; r < (7*row_offset); r++, source_row++)
					{
						test = cvGet2D(canny, source_row, source_col);
						int keyPixel = (int) test.getVal(0);
						
						if (keyPixel > 100)
							temp.setPixel(c, r, Color.argb(255, 255, 255, keyPixel));
						else temp.setPixel(c, r, Color.argb(255, 0, 0, keyPixel));
					}
				}
				
				WriteImageToFile(temp, "3_degree_images", image);
				imagePath = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + "3_degree_images" + File.separator + String.format(Locale.ENGLISH, "%03d", image) +".png");
				img = imagePath.getAbsolutePath();
				source = IplImage.create(temp.getWidth(), temp.getHeight(), IPL_DEPTH_8U, 4);
				canny = IplImage.create(temp.getWidth(), temp.getHeight(), IPL_DEPTH_8U, 1);
				source = cvLoadImage(img);
				cvCvtColor(source, canny, CV_BGR2GRAY); 
				CvSeq cvlines;
				FloatPointer cvline;

				cvlines = cvHoughLines2(canny, CvMemStorage.create(), CV_HOUGH_STANDARD, 1, Math.PI/180, 1, 0, 0);
				
				for(int j = 0; j < cvlines.total(); j++){
					cvline = new FloatPointer(cvGetSeqElem(cvlines, j));
					rho = cvline.position(0).get();
					theta = cvline.position(1).get();
				}
	        	
				if (rho < 0)
					rho = rho*-1;
				rho_ave = rho_ave + rho;
				theta_ave = theta_ave + theta;
				row_ave = row_ave + (max_row-min_row);
				
				if (i > 0)
				{
					if ((max_row-min_row) < row_ave*1.25 && (max_row-min_row) > row_ave*.75)
						row_ave = row_ave/2;
					if (rho < rho_ave*1.25 && rho > rho_ave*.75)
						rho_ave = rho_ave/2;
					if (theta < theta_ave*1.25 && theta > theta_ave*.75)
						theta_ave = theta_ave/2;
				}
	        }

			else if (bmp == null)
				break;
		}
		
		double raw_theta = theta_ave;
		if (rho_ave < 0)
			rho_ave = rho_ave*-1;
		
        /** theta modifiers 
         Emperical values, determined through testing keys of various angles, in multiple orientations
        **/

		if (raw_theta > 0 && raw_theta < 0.2)
			theta_ave = theta_ave*3;
		if (raw_theta > 0.6 && raw_theta < 0.9)
			theta_ave = theta_ave/2;
		if (raw_theta > 0.9 && raw_theta < 1)
			theta_ave = theta_ave*8;
		if (raw_theta > 1 && raw_theta < 2)
			theta_ave = theta_ave/2;
		if (raw_theta > 5 && raw_theta < 6)
			theta_ave = 1.05/theta_ave;
		if (raw_theta > 17)
			theta_ave = theta_ave/6;
	
		/** Determine key model **/
		
		degree = (rho_ave/theta_ave);
		
		if (row_ave == 0 || max_red_col == 0 || counter == 0)
			length = 0;
		else length = (row_ave/counter)*(17.5/(max_red_col/counter));
		
		if (length > MS_2*.975 && length < MS_2*1.025 && degree > MS_2d*.925 && degree < MS_2d*1.075)
			keyModel = "MS 2";
		else if (length > UL_050*.975 && length < UL_050*1.025 && degree > UL_050d*.925 && degree < UL_050d*1.075)
			keyModel = "UL 050";
		else if (length > UNI_3*.975 && length < UNI_3*1.025 && degree > UNI_3d*.925 && degree < UNI_3d*1.075)
			keyModel = "UNI 3";
		else if (length > UL_054*.975 && length < UL_054*1.025 && degree > UL_054d*.925 && degree < UL_054d*1.075)
			keyModel = "UL 054";
		
		/** Display results **/
		
		RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
        //MediaPlayer.create(getBaseContext(), R.raw.op).start()
		time = ((System.currentTimeMillis() - start)/1000);
	
		StatusAlert(keyModel,
					"Length                 " + String.format(Locale.ENGLISH, "%.3f", length) +"cm" + 
					"\nTip Angle             " + String.format(Locale.ENGLISH, "%.1f", degree) + "°" +
					"\nProcess time      " + time + " seconds"
					+
					/* + "\n"
					+ "\nRatio pixel/cm    " + String.format(Locale.ENGLISH, "%.3f", (17.5/(max_red_col/counter))) +
					"\nFirst Col               " + first_red_col +
					"\nLast Col               " + last_red_col +
					"\nFirst Red Row      " + first_red_row +
					"\nLast Red Row      " + last_red_row +
					"\nKey First Row      " + min_row +
					"\nKey Last Row      " + max_row +
					"\nRow Ave               " + (row_ave/counter) +*/
					"\nRho 			       	      " + String.format(Locale.ENGLISH, "%.0f", rho_ave) +
					"\nTheta		               " + String.format(Locale.ENGLISH, "%.3f", theta_ave) +
					"\nTheta (raw)         " + String.format(Locale.ENGLISH, "%.3f", raw_theta) +
					"\nPasses                " + counter
					);
		
		finished = true;
					
		/** End **/
	}	
	
	//Method to delete a file or directory (and any content it may have)
	public void DeleteFolder(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteFolder(child);
	    fileOrDirectory.delete();
	}
	
	public void StatusToast(String statusString){
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
	
	void WriteImageToFile(Bitmap source, String folder, int image_number) throws IOException{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.compress(Bitmap.CompressFormat.PNG, 100, bytes);
		File f = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + folder + File.separator + String.format(Locale.ENGLISH, "%03d", image_number) + ".png");
		f.createNewFile();
		FileOutputStream fo = new FileOutputStream(f);
		bytes.writeTo(fo);
	}
	
	
	private void handleCameraVideo(Intent intent) {
		tempFolder.mkdir();
		try {
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fis = videoAsset.createInputStream();
		    FileOutputStream fos = new FileOutputStream(v1);

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
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	};

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		//setVolumeControlStream(AudioManager.STREAM_MUSIC); //Use media volume, not ringtone volume
		setVolumeControlStream(AudioManager.STREAM_RING);
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
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
	    switch (item.getItemId()) {
	        case R.id.settings:
	        	if (passes == 1)
	        		alert.setTitle("Current setting: 1 frame");
	        	else alert.setTitle("Current setting: " + passes + " frames");
	        	alert.setMessage("Enter 1-15, or 0 for max possible");

	        	// Set an EditText view to get user input 
	        	final EditText input = new EditText(this);
	        	input.setInputType(InputType.TYPE_CLASS_PHONE);
	        	alert.setView(input);
	        	
	        	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		        	public void onClick(DialogInterface dialog, int whichButton) {
		        		String value = input.getText().toString();
		        		
		        		try{
		        			if (value.equals("")){
			        			//value = "1";
			        			StatusToast("No value entered!");
			        		}
		        			else if (Integer.parseInt(value) > 15)
			        		{
			        			//value = "1";
			        			StatusToast("Entered value larger than 15!");
			        		}
		        			else if (Integer.parseInt(value) < 0)
		        			{
		        				StatusToast("Can't analyse negative frames!");
		        			}
			        		else if (Integer.parseInt(value) == 0)
			        		{
			        			//Load video.mp4 as MMDR file for frame extraction
			        			MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
			        			vidFile.setDataSource(v1.getAbsolutePath());
			        			
			        			//Determine video.mp4 length (returns milliseconds - divide by 1,000)
			        			String x = vidFile.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			        			long vidLength = (Long.parseLong(x)/1000);
			        			passes = (int) (vidLength*10);
			        		}
			        		else passes = Integer.parseInt(value);
		        		} catch(NumberFormatException e) {
		        			//value = "1";
			        		StatusToast("Number not entered!");
		        	    }
		        	}
		        });

	        	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        		  public void onClick(DialogInterface dialog, int whichButton) {
	        		    // Canceled.
	        		  }
	        	});
	        	alert.show();
	            return true;
	            
	        case R.id.log:
	        	if (finished)
	        	{
		        	StatusAlert(keyModel + " (last result)",
							"Length                 " + String.format(Locale.ENGLISH, "%.3f", length) +"cm" + 
							"\nTip Angle             " + String.format(Locale.ENGLISH, "%.1f", degree) + "°" +
							"\nProcess time      " + time + " seconds" + 
							"\nPasses                " + counter
							);
	        	}
	        	else StatusToast("No analysis done!");
	            return true;
	            
	        case R.id.temp_files:
	        	if (tempFolder.exists())
	        	{
		        	alert.setTitle("Really?");
		        	alert.setMessage("Are you sure?");
		        	
		        	alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			        	public void onClick(DialogInterface dialog, int whichButton) {
			        		DeleteFolder(tempFolder);
			        		StatusToast("All files deleted!");
			        	}
			        });

		        	alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
		        		  public void onClick(DialogInterface dialog, int whichButton) {
		        		    // Canceled.
		        		  }
		        	});
		        	alert.show();	
	        	}
	        	else StatusToast("No files to delete!");
	            return true;
	            
	        case R.id.temp_images:
	        	if (rawImageFolder.exists())
	        	{
	        		alert.setTitle("Really?");
		        	alert.setMessage("Are you sure?");
		        	
		        	alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			        	public void onClick(DialogInterface dialog, int whichButton) {
			        		DeleteFolder(rawImageFolder);
			        		DeleteFolder(cannyImageFolder);
			        		DeleteFolder(degreeImageFolder);
			        		StatusToast("Image files deleted!");
			        	}
			        });

		        	alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
		        		  public void onClick(DialogInterface dialog, int whichButton) {
		        		    // Canceled.
		        		  }
		        	});
		        	alert.show();
	        	}
	        	else StatusToast("No images to delete!");
	            return true;
	        
	        case R.id.quit:
	        	alert.setTitle("Would you like to delete the temp files?");
	        	//alert.setMessage("Are you sure?");
	        	
	        	alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		        	public void onClick(DialogInterface dialog, int whichButton) {
		        		DeleteFolder(tempFolder);
		        		finish();
		        	}
		        });
	        	
	        	alert.setNeutralButton("Images only", new DialogInterface.OnClickListener() {
		        	public void onClick(DialogInterface dialog, int whichButton) {
		        		DeleteFolder(rawImageFolder);
		        		DeleteFolder(cannyImageFolder);
		        		DeleteFolder(degreeImageFolder);
		        		finish();
		        	}
	        	});

	        	alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
	        		  public void onClick(DialogInterface dialog, int whichButton) {
	        			finish();
	        		  }
	        	});
	        	alert.show();
	        	return true;
	            
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK)
			handleCameraVideo(data);
	}
	
	// Some lifecycle callbacks so that the image can survive orientation change
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("Finished", finished);
		outState.putDouble("Degree", degree);
		outState.putDouble("Length", length);
		outState.putString("Model", keyModel);
		outState.putInt("Counter", counter);
		outState.putInt("Passes", passes);
		outState.putLong("Time", time);
	}
	
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		finished = savedInstanceState.getBoolean("Finished");
		degree = savedInstanceState.getDouble("Degree");
		length = savedInstanceState.getDouble("Length");
		keyModel = savedInstanceState.getString("Model");
		counter = savedInstanceState.getInt("Counter");
		passes = savedInstanceState.getInt("Passes");
		time = savedInstanceState.getLong("Time");
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