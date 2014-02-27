/**Nee ta ma duh, tyen shee-a soy ya duh ren, doh goy-swa**/

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

import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

public class KeyAnalyserActivity extends Activity
{	
	private static final int ARRAY_SIZE = 10;
	
	//Variables that need to survive activity destruction
	int passes = 1, counter = 0, instance = 0, pointer = 0;
	double length, degree_ave;
	long time;
	boolean finished = false, exist = true;
	String keyModel;
	Key model_result = new Key("No model found", 0, 0);
	
	//Allow global access
	TextView mTextView;
	ImageView mImageView;
	Button last_result, next_result;
	
	//Info for key models
	//				  Name, Length, Degree
	Key MS_2 = new Key("MS2", 4.2, 90);
	Key UL_050 = new Key("UL050", 5.5, 85);
	Key UNI_3 = new Key("UNI3", 5.75, 105);
	Key UL_054 = new Key("UL054", 5.9, 95);
	ResultStorage[] ResultStorages = new ResultStorage[ARRAY_SIZE]; //Array to hold most recent <ARRAY_SIZE> results (then drops oldest one)
	//Ideally, I would have liked to used an ArrayList to allow for the array to, but decided that 10 results should be enough
	
	//Filepaths for video file & temp folders
	File v1 = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + "video.mp4");
	File tempFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser");
	File rawImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser" + File.separator + "1_raw_images");
	File cannyImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser" + File.separator + "2_canny_images");
	File degreeImageFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "KeyAnalyser" + File.separator + "3_degree_images");
	
	//Calls intent to record video
	private void TakeVideoIntent()
	{		
		//Confirm SD card is mounted, then call intent
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			StatusToast("SD card not mounted!");
		else
		{
			Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			startActivityForResult(takeVideoIntent, 3);
		}
	}
	
	//Main thread - processing of video
	private void ProcessVideo() throws IOException
	{		
		//Initialisation & resetting of variables
		long start = System.currentTimeMillis();
		int red_count = 0, c = 0, r = 0, min_row = 0, max_row = 0;
		int i = 0, image = 1, first_red = 0, last_red = 0, max_red = 0, max_red_col = 0;
		int first_red_col = 0, last_red_col = 0, row_ave = 0, first_red_row = 0, last_red_row = 0, degree_skip = 0;
		double rho = 0, theta = 0, degree = 0, degree_sum = 0, degree_ave = 0;
		boolean model_found = true, min_found = false;
		int first_red_col_ave = 0, last_red_col_ave = 0, first_red_row_ave = 0, last_red_row_ave = 0;
		int row_sum = 0;
		double length_result_offset = 0.025, degree_result_offset = 0.125, degree_offset = 0.1;
		
		keyModel = "No model found";
		counter = 0;
		length = 0;
		CvScalar test;
		
		//Check it SD card is mounted, and (if it is) if video.mp4 exists
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			StatusToast("SD card not mounted!");
		else if (!v1.exists())
			StatusToast("No video file to analyse!");

		//Create folders
		rawImageFolder.mkdir();
		cannyImageFolder.mkdir(); //Only used to test image files - not actually needed
		degreeImageFolder.mkdir();
		
		//Load video into MMDR
		MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
		vidFile.setDataSource(v1.getAbsolutePath());

		if (v1.exists()) //Only begin analysis if video file actually exists
		{
			for(i = 0; i < passes; i++, image++, counter++)
			{
				//Extract frames every 1/10 of a second
				Bitmap bmp = vidFile.getFrameAtTime(100000*i, MediaMetadataRetriever.OPTION_CLOSEST);
				
				if(bmp != null)
				{
					
					WriteImageToFile(bmp, "1_raw_images", image); //Save extracted frame to SD card
					//A fully completed app would not need these files at all, so it would make sense to simply not save the files, and just convert them directly to IplImages
						//However, since I was still refining the settings (for cvCanny and the thresholding for the red area), I left them in so I could check the results if things went wrong.
					
					File imagePath = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + "1_raw_images" + File.separator + String.format(Locale.ENGLISH, "%03d", image) + ".png");
					String img = imagePath.getAbsolutePath();
					bmp = BitmapFactory.decodeFile(img);
					IplImage source = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 4);
					
					source = cvLoadImage(img);
					max_red = 1;
					
					//Determine red area for pixel/cm ratio & red area boundary (for cvCanny)
					for (c = 0; c < source.width(); c++)
					{
						red_count = 0;
						
						for (r = 0; r < source.height(); r++)
						{
							//Extract RGB values from image
							test = cvGet2D(source, r, c);
							int red = (int) test.getVal(2);
				            int green = (int) test.getVal(1);
				            int blue = (int) test.getVal(0);
				            
				            //Thresholding of red area - not idea, but seems to be accurate enough; could alternatively do this through cvCanny though
				            if (red > 130 && green < 75 && blue < 75)
				            	red_count++;
						}
						
						if (red_count > max_red) //# of red pixels in column with most red pixels
							max_red = red_count;
						
						if (red_count > 10) //Red area, by start and finish columns, for boundaries - only choosing columns with at least 10 red pixels
						{
							if (first_red == 0)
								first_red = c;
							else if (first_red > 0)
								last_red = c;
						}
					}
					max_red_col = max_red_col + max_red; //Running total (NOT average - that's done later)
					
					first_red_col = first_red_col + first_red;				
					last_red_col = last_red_col + last_red;

					first_red_col_ave = first_red_col/(i+1);
					last_red_col_ave = last_red_col/(i+1);					
					
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
		
					first_red_row = first_red_row + first_red;
					last_red_row = last_red_row + last_red;
					
					first_red_row_ave = first_red_row/(i+1);
					last_red_row_ave = last_red_row/(i+1);			
					
					//Moving on to cvCanny analysis: isolating image within red area (with boundaries defined by first_red_row, last_red_row, first_red_col & last_red_col)
					
					imagePath = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + "1_raw_images" + File.separator + String.format(Locale.ENGLISH, "%03d", image) + ".png");
					img = imagePath.getAbsolutePath();
					bmp = BitmapFactory.decodeFile(img);
					min_found = false;
					
					source = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 4);
					IplImage canny = IplImage.create(bmp.getWidth(), bmp.getHeight(), IPL_DEPTH_8U, 1);
					source = cvLoadImage(img);
			        Bitmap temp = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888);
			        //Apply cvCanny
			        cvCanny(source, canny, 50, 150, 3);
			        int row_offset = bmp.getHeight()/200, col_offset = bmp.getWidth()/110; //Offsets used for red boundaries (in case video isn't straight)
			        
			        //If within these boundaries, apply cvCanny, otherwise leave empty (for .png, this results in transparent space around the image)
			        
			        for(r = first_red_row_ave+(4*row_offset); r < last_red_row_ave-(4*row_offset); r++)
			        {
			        	for(c = first_red_col_ave+(7*col_offset); c < last_red_col_ave-(7*col_offset); c++)
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
					
					//Create images for degree calculation - trying to isolate the tip of the key and some of the rows above it
					if ((last_red_col_ave-(source.width()/3))-(first_red_col_ave+(source.width()/3)) < 0)
						temp = Bitmap.createBitmap(source.width()/3, ((7*row_offset)+1), Bitmap.Config.ARGB_8888);
					
					else temp = Bitmap.createBitmap((last_red_col_ave-(source.width()/3))-(first_red_col_ave+(source.width()/3)), ((7*row_offset)+1), Bitmap.Config.ARGB_8888);
					int source_col = first_red_col_ave+(source.width()/3);
					
					for (c = 0; c < ((last_red_col_ave-(source.width()/3)) - (first_red_col_ave+(source.width()/3))); c++, source_col++)
					{
						int source_row = max_row+1-(7*row_offset);
						for (r = 0; r < (7*row_offset)+1; r++, source_row++)
						{
							test = cvGet2D(canny, source_row, source_col);
							int keyPixel = (int) test.getVal(0);
							
							if (keyPixel > 100)
								temp.setPixel(c, r, Color.argb(255, 255, 255, keyPixel));
							else temp.setPixel(c, r, Color.argb(255, 0, 0, keyPixel));
						}
					}
					WriteImageToFile(temp, "3_degree_images", image);
					
					//Calculate angle of tip of the key
					imagePath = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + "3_degree_images" + File.separator + String.format(Locale.ENGLISH, "%03d", image) +".png");
					img = imagePath.getAbsolutePath();
					source = IplImage.create(temp.getWidth(), temp.getHeight(), IPL_DEPTH_8U, 4);
					canny = IplImage.create(temp.getWidth(), temp.getHeight(), IPL_DEPTH_8U, 1);
					source = cvLoadImage(img);
					cvCvtColor(source, canny, CV_BGR2GRAY); 
					CvSeq cvlines;
					FloatPointer cvline;
	
					cvlines = cvHoughLines2(canny, CvMemStorage.create(), CV_HOUGH_STANDARD, 1, Math.PI/180, 1, 0, 0);
					
					for(int j = 0; j < cvlines.total(); j++)
					{
						cvline = new FloatPointer(cvGetSeqElem(cvlines, j));
						rho = cvline.position(0).get();
						theta = cvline.position(1).get();
					}
					
					row_sum = row_sum + (max_row-min_row);
					row_ave = row_sum/(i+1);
					
					length = row_ave*(17.5/(max_red_col/(i+1)));
					
					degree = Math.atan(Math.abs(rho)/theta)*(180/Math.PI);
					
					if (i == 0)
						degree_ave = degree;
					
					if (degree < degree_ave*(1+degree_offset) && degree > degree_ave*(1-degree_offset))
					{
						degree_sum = degree_sum + degree;
						degree_ave = degree_sum/(i+1-degree_skip);
					}
					else degree_skip++;
				}
				else if (bmp == null)
					break;
			}
		}
		
		//Release video
		vidFile.release();
		
		//Match results to "database"
		if (length > MS_2.length*(1-length_result_offset) && length < MS_2.length*(1+length_result_offset)
				&& degree_ave > MS_2.degree*(1-degree_result_offset) && degree_ave < MS_2.degree*(1+degree_result_offset))
			model_result = MS_2;
		else if (length > UL_050.length*(1-length_result_offset) && length < UL_050.length*(1+length_result_offset)
				&& degree_ave > UL_050.degree*(1-degree_result_offset) && degree_ave < UL_050.degree*(1+degree_result_offset))
			model_result = UL_050;
		else if (length > UNI_3.length*(1-length_result_offset) && length < UNI_3.length*(1+length_result_offset)
				&& degree_ave > UNI_3.degree*(1-degree_result_offset) && degree_ave < UNI_3.degree*(1+degree_result_offset))
			model_result = UNI_3;
		else if (length > UL_054.length*(1-length_result_offset) && length < UL_054.length*(1+length_result_offset)
				&& degree_ave > UL_054.degree*(1-degree_result_offset) && degree_ave < UL_054.degree*(1+degree_result_offset))
			model_result = UL_054;
		else keyModel = "No model found";
		
		keyModel = model_result.name;
		
		//Save process time
		time = ((System.currentTimeMillis()-start)/1000);
		
		//Save results in array, and set TextView to show results
		if (instance < ARRAY_SIZE)
		{
			ResultStorages[instance] = new ResultStorage(keyModel, length, degree_ave, time, counter);	
			mTextView = (TextView) findViewById(R.id.TextView);	
			ResultStorages[instance].setTextView(mTextView);
		}
		else //If array is filled, we drop the oldest result, move all other results down, and place newest result in last array slot
		{
			instance = (ARRAY_SIZE-1);
			for (i = 0; i < instance; i++)
				ResultStorages[i] = ResultStorages[i+1];
			
			ResultStorages[instance] = new ResultStorage(keyModel, length, degree_ave, time, counter);
			mTextView = (TextView) findViewById(R.id.TextView);
			ResultStorages[instance].setTextView(mTextView);
		}		
		
		//Set boolean to true so past ResultStorages can be loaded
		finished = true;
		instance++;
		pointer = instance - 1;
				
		if (model_found) //Set imageview with found key model image (and info on longpress)
		{			
			mImageView = (ImageView) findViewById(R.id.ImageView);
			SetImageView.set(mImageView, keyModel, getBaseContext().getResources(), exist); //Method in SetImageView.java class
			registerForContextMenu(mImageView);
			if (!exist)
				StatusAlert("Error", "Can't load " + keyModel + " imageview!");
		}
		else if (!model_found) //Otherwise, show info in TextView, and leave ImageView black
		{
			//TODO Implement closest match?
			mTextView.setText(
					"  Model\t\t\t\t\t\t\t\t" + "No model found" + 
					"\n\n  Length\t\t\t\t\t\t\t\t" + String.format(Locale.ENGLISH, "%.3f", length) +"cm" +
					"\n  Tip Angle\t\t\t\t\t\t" + String.format(Locale.ENGLISH, "%.3f", degree_ave) + "°" + 
					"\n\n  Process time\t\t\t\t" + time + " seconds" +
					"\n  Passes\t\t\t\t\t\t\t\t" + counter);		
		}
		
		checkResultAvailLast(last_result, mLastResult);
		checkResultAvailNext(next_result, mNextResult);
		
		RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
		/** End **/
	}	
	
	//Method to delete a file or directory (and any content it may have)
	public void DeleteFolder(File fileOrDirectory)
	{
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteFolder(child);
	    fileOrDirectory.delete();
	}
	
	//Toast pop-up
	public void StatusToast(String statusString)
	{
		Toast status = Toast.makeText(getApplicationContext(), statusString, Toast.LENGTH_SHORT);
		status.show();
	}
	
	//Dialog pop-up
	void StatusAlert(String title, String message)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE , "Done", new DialogInterface.OnClickListener()
        {
			public void onClick(DialogInterface dialog, int which) {}
		});
        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.show();
	}
	
	void StatusAlert(String title)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.show();
	}
	
	//Method to save Bitmap variable to file
	void WriteImageToFile(Bitmap source, String folder, int image_number) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.compress(Bitmap.CompressFormat.PNG, 100, bytes);
		File f = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser" + File.separator + folder + File.separator + String.format(Locale.ENGLISH, "%03d", image_number) + ".png");
		f.createNewFile();
		FileOutputStream fo = new FileOutputStream(f);
		bytes.writeTo(fo);
	}
	
	//Hijack intent data-stream, and save video file to defined location
	private void handleCameraVideo(Intent intent)
	{
		tempFolder.mkdir();
		try
		{
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fis = videoAsset.createInputStream();
		    FileOutputStream fos = new FileOutputStream(v1);

		    byte[] buf = new byte[40960];
		    int len;
		    while ((len = fis.read(buf)) > 0)
		    {
		        fos.write(buf, 0, len);
		    }       
		    fis.close();
		    fos.close();
		 }
		catch (IOException io_e)
		{
			 io_e.printStackTrace();
		}
	}

	Button.OnClickListener mTakeVideo = new Button.OnClickListener()
	{
		public void onClick(View v)
		{
			TakeVideoIntent();
		}
	};
	
	Button.OnClickListener mProcessVideo = new Button.OnClickListener()
	{	
		public void onClick(View v)
		{
				try
				{
					ProcessVideo();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
		}
	};
	
	Button.OnClickListener mLastResult = new Button.OnClickListener()
	{
		
		public void onClick(View v)
		{
			pointer--;
			if (pointer < 0)
				pointer = 0;
			
			mTextView = (TextView) findViewById(R.id.TextView);
			mImageView = (ImageView) findViewById(R.id.ImageView);
			
			//Reload ResultStorages in TextView
			ResultStorages[pointer].setTextView(mTextView);
			
			//Reload key blank in ImageView
			SetImageView.set(mImageView, keyModel, getBaseContext().getResources(), exist); //Method in SetImageView.java class
			registerForContextMenu(mImageView);
			
			if (!exist)
				StatusAlert("Error", "Can't load " + keyModel + " imageview!");
			
			checkResultAvailLast(last_result, mLastResult);
			checkResultAvailNext(next_result, mNextResult);
		}
	};
	
	Button.OnClickListener mNextResult = new Button.OnClickListener()
	{	
		public void onClick(View v)
		{
			pointer++;
			if (pointer > (ARRAY_SIZE-1))
				pointer = (ARRAY_SIZE-1);
			
			mTextView = (TextView) findViewById(R.id.TextView);
			mImageView = (ImageView) findViewById(R.id.ImageView);
			
			ResultStorages[pointer].setTextView(mTextView);
			
			SetImageView.set(mImageView, keyModel, getBaseContext().getResources(), exist); //Method in SetImageView.java class
			registerForContextMenu(mImageView);
			
			if (!exist)
				StatusAlert("Error", "Can't load " + keyModel + " imageview!");
			
			checkResultAvailLast(last_result, mLastResult);
			checkResultAvailNext(next_result, mNextResult);
		}
	};

	//Called when the activity is first created.
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		//Set view to main page
		setContentView(R.layout.main);
		
		//Initialise buttons (if intents are possible)
		Button video_intent = (Button) findViewById(R.id.VideoIntent);
		setBtnListenerOrDisable(video_intent, mTakeVideo, MediaStore.ACTION_VIDEO_CAPTURE);
		
		Button video_process = (Button) findViewById(R.id.VideoProcess);
		setBtnListenerOrDisable(video_process, mProcessVideo, MediaStore.ACTION_VIDEO_CAPTURE);
		
		last_result = (Button) findViewById(R.id.LastLog);
		checkResultAvailLast(last_result, mLastResult);
		
		next_result = (Button) findViewById(R.id.NextLog);
		checkResultAvailNext(next_result, mNextResult);
	}
	
	//Define menu
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		if (keyModel.equals("No model found"))
			StatusAlert(keyModel);
		else
		{
			menu.setHeaderTitle(keyModel + " info");
			menu.add(0, v.getId(), 0, "Open Catalogue (requires login)");
			Key tempkey = null;
			if (keyModel.equals("MS2"))
				tempkey = MS_2;
			else if (keyModel.equals("UL050"))
				tempkey = UL_050;
			else if (keyModel.equals("UNI3"))
				tempkey = UNI_3;
			else if (keyModel.equals("UL054"))
				tempkey = UL_054;
			
			if (tempkey == null){}
			else
			{
				menu.add(0, v.getId(), 0, "Length:\t\t\t" + String.format(Locale.ENGLISH, "%.3f", tempkey.length) + "cm");
				menu.add(0, v.getId(), 0, "Degree:\t\t\t" + tempkey.degree + "°");
			}
		}
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
		if(item.getTitle().equals("Open Catalogue (requires login)"))
		{
			Intent intent = new Intent();
	        intent.setAction(Intent.ACTION_VIEW);
	        intent.addCategory(Intent.CATEGORY_BROWSABLE);
	        intent.setData(Uri.parse("https://ekc.silca.biz/ricerche_stampa_chiave.php?chiave=" + keyModel));
	        startActivity(intent);
		}
		else return false;  
		
		return true;  
	}
	
    //Handle item selection
	public boolean onOptionsItemSelected(MenuItem item)
	{
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		//Switch for menu options
	    switch (item.getItemId()) {
	        case R.id.settings:
	        	if (passes == 1)
	        		alert.setTitle("Current setting: 1 frame");
	        	else alert.setTitle("Current setting: " + passes + " frames"); //Shows max as 20, but only 19 frames? Problem between "counter" & "passes"?
	        	
	        	MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
    			vidFile.setDataSource(v1.getAbsolutePath());
	        	
	        	final int max_frames = (int) ((Long.parseLong(vidFile.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))*10)/1000);
	        	
	        	alert.setMessage("Enter 1-"+ max_frames +", or 0 for max possible");

	        	// Set an EditText view to get user input 
	        	final EditText input = new EditText(this);
	        	input.setInputType(InputType.TYPE_CLASS_PHONE);
	        	alert.setView(input);
	        	
	        	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
	        	{
		        	public void onClick(DialogInterface dialog, int whichButton)
		        	{
		        		String value = input.getText().toString();
		        		
		        		try
		        		{
		        			if (value.equals("")){
			        			StatusToast("No value entered!");
			        		}
		        			else if (Integer.parseInt(value) > max_frames)
			        		{
			        			StatusToast("Entered value larger than " + max_frames + "!");
			        		}
		        			else if (Integer.parseInt(value) < 0)
		        			{
		        				StatusToast("Can't analyse negative frames!");
		        			}
			        		else if (Integer.parseInt(value) == 0)
			        		{
			        			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			        				StatusToast("SD card not mounted!");
			        			else if (!v1.exists())
			        				StatusToast("No video file to analyse!");
			        			else
			        			{
				        			passes = max_frames;
				        			StatusToast("Set to " + passes + " frames");
				        		}
			        		}
			        		else
			        		{
			        			passes = Integer.parseInt(value);
			        			if (passes == 1)
			        				StatusToast("Set to 1 frame");
			        			else StatusToast("Set to " + passes + " frames");
			        		}
		        		}
		        		catch(NumberFormatException e)
		        		{
			        		StatusToast("Number not entered!");
		        	    }
		        	}
		        });

	        	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
	        	{
	        		  public void onClick(DialogInterface dialog, int whichButton){}
	        	});
	        	alert.show();
	            return true;
	            
	        case R.id.temp_files:
	        	if (tempFolder.exists())
	        	{
	        		alert.setTitle("Would you like to delete the temp files?");
	        	
		        	alert.setPositiveButton("Yes", new DialogInterface.OnClickListener()
		        	{
			        	public void onClick(DialogInterface dialog, int whichButton)
			        	{
			        		DeleteFolder(tempFolder);
			        		StatusToast("All temp files deleted!");
			        	}
			        });
		        	
		        	alert.setNeutralButton("Images only", new DialogInterface.OnClickListener()
		        	{
			        	public void onClick(DialogInterface dialog, int whichButton)
			        	{
			        		DeleteFolder(rawImageFolder);
			        		DeleteFolder(cannyImageFolder);
			        		DeleteFolder(degreeImageFolder);
			        		StatusToast("All images deleted!");
			        	}
		        	});
	
		        	alert.setNegativeButton("No", new DialogInterface.OnClickListener()
		        	{
		        		  public void onClick(DialogInterface dialog, int whichButton){}
		        	});
		        	alert.show();
	        	}
	        	else StatusToast("No files to delete!");
	            return true;
	            
	        case R.id.clear:
	        	if (finished)
	        	{
		        	alert.setTitle("Would you like to clear the screen?");
		        	
		        	alert.setPositiveButton("Yes", new DialogInterface.OnClickListener()
		        	{
			        	public void onClick(DialogInterface dialog, int whichButton)
			        	{
			        		//Clear TextView
			        		mTextView.setText("");
			        		//Clear ImageView
			        		//mImageView.setImageDrawable(null);
			        		mImageView.setVisibility(View.INVISIBLE);
			        		//Disable button for next log (leave last log button)
			        		next_result.setClickable(false);
			    			next_result.setVisibility(View.INVISIBLE);
			    			//Reset log count  to end
			    			pointer = instance;
			    			checkResultAvailLast(last_result, mLastResult);
			        	}
			        });
	
		        	alert.setNegativeButton("No", new DialogInterface.OnClickListener()
		        	{
		        		  public void onClick(DialogInterface dialog, int whichButton){}
		        	});
		        	alert.show();  	
	        	}
	        	else StatusToast("Nothing to clear!");
	        	return true;
	        
	        case R.id.quit:
	        	if (tempFolder.exists())
	        	{
	        		alert.setTitle("Would you like to delete the temp files?");
	        	
		        	alert.setPositiveButton("Yes", new DialogInterface.OnClickListener()
		        	{
			        	public void onClick(DialogInterface dialog, int whichButton)
			        	{
			        		DeleteFolder(tempFolder);
			        		finish();
			        	}
			        });
		        	
		        	alert.setNeutralButton("Images only", new DialogInterface.OnClickListener()
		        	{
			        	public void onClick(DialogInterface dialog, int whichButton)
			        	{
			        		DeleteFolder(rawImageFolder);
			        		DeleteFolder(cannyImageFolder);
			        		DeleteFolder(degreeImageFolder);
			        		finish();
			        	}
		        	});
	
		        	alert.setNegativeButton("No", new DialogInterface.OnClickListener()
		        	{
		        		  public void onClick(DialogInterface dialog, int whichButton)
		        		  {
		        			finish();
		        		  }
		        	});
		        	alert.show();
	        	}
	        	else finish();
	        	return true;
	            
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	//Handles intent data and saves to default DCIM folder (file name handles by device)
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
			handleCameraVideo(data);
	}
	
	//Some callbacks so that the variables can survive orientation change
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBoolean("Finished", finished);
		outState.putDouble("Length", length);
		outState.putDouble("Degree", degree_ave);
		outState.putString("Model", keyModel);
		outState.putInt("Counter", counter);
		outState.putInt("Passes", passes);
		outState.putInt("Instance", instance);
		outState.putInt("Current Log", pointer);
		outState.putLong("Time", time);
	}
	
	//Restore variables on orientation change
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		finished = savedInstanceState.getBoolean("Finished");
		length = savedInstanceState.getDouble("Length");
		degree_ave = savedInstanceState.getDouble("Degree");
		keyModel = savedInstanceState.getString("Model");
		counter = savedInstanceState.getInt("Counter");
		passes = savedInstanceState.getInt("Passes");
		instance = savedInstanceState.getInt("Instance");
		pointer = savedInstanceState.getInt("Current Log");
		time = savedInstanceState.getLong("Time");
		
		mImageView = (ImageView) findViewById(R.id.ImageView);
		SetImageView.set(mImageView, keyModel, getBaseContext().getResources(), exist); //Method in SetImageView.java class
		registerForContextMenu(mImageView);
		if (!exist)
			StatusAlert("Error", "Can't load " + keyModel + " imageview!");
		
		//Check to see which buttons should be clickable on orientation change
		checkResultAvailLast(last_result, mLastResult);
		checkResultAvailNext(next_result, mNextResult);
	}
	
	
	/*** Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 */
	
	//Taken from photobyintent sample from developers.android.com
	public static boolean isIntentAvailable(Context context, String action)
	{
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	//If intent is available, set "Take Video" button as clickable (not certain it's needed, since no device has failed it yet)
	private void setBtnListenerOrDisable(Button btn, Button.OnClickListener onClickListener, String intentName)
	{
		if (isIntentAvailable(this, intentName))
			btn.setOnClickListener(onClickListener);
		else
		{
			btn.setText(getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}
	
	//Check to see if "Last Log" button should be clickable/visible
	private void checkResultAvailLast(Button btn, Button.OnClickListener onClickListener)
	{
		//Should only be shown if analysis has been run, and NOT at oldest ResultStorage
		if (!finished || pointer < 1)
		{
			btn.setClickable(false);
			btn.setVisibility(View.INVISIBLE);
		}
		else
		{
			btn.setClickable(true);
			btn.setVisibility(View.VISIBLE);
			btn.setOnClickListener(onClickListener);
		}
	}
	
	//Check to see if "Next Log" button should be clickable/visible
	private void checkResultAvailNext(Button btn, Button.OnClickListener onClickListener)
	{
		//Should only be shown if analysis has been run, and NOT at newest ResultStorage
		if (!finished || pointer == (instance-1))
		{
			btn.setClickable(false);
			btn.setVisibility(View.INVISIBLE);
		}
		else
		{
			btn.setClickable(true);
			btn.setVisibility(View.VISIBLE);
			btn.setOnClickListener(onClickListener);
		}
	}
}