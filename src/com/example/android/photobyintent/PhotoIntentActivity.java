package com.example.android.photobyintent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
/*
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
*/
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class PhotoIntentActivity extends Activity {

	private void dispatchTakeVideoIntent() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, 3);
	}
	
	private void dispatchProcVideoIntent() throws IOException{
		
		//Variables
		Toast toast = Toast.makeText(getApplicationContext(), "Processing complete!", Toast.LENGTH_SHORT);
		int image = 1;

		File videoPath = new File(Environment.getExternalStorageDirectory(), "KeyAnalyser_tmp/video.mp4");
		String video = videoPath.getAbsolutePath();
		MediaMetadataRetriever vidFile = new MediaMetadataRetriever();
		vidFile.setDataSource(video);
		
		//if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			//If I feel like being responsible
				//IF :P
		
		//Create folder to store images		
		String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
		File newFolder = new File(extStorageDirectory + File.separator + "KeyAnalyser_tmp" + File.separator + "images_tmp");
		newFolder.mkdir();
		
		String value = vidFile.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		long vidLength = (Long.parseLong(value)/1000); //Returns milliseconds - divide by 1,000
		
		for(int i = 0; i <= 10*vidLength; i++, image++) //10*vidLength since I'm getting frames every 1/10th sec
		{
			Bitmap bmp = vidFile.getFrameAtTime(100000*i, MediaMetadataRetriever.OPTION_CLOSEST);
			
			if(bmp != null)
			{
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				bmp.compress(Bitmap.CompressFormat.PNG, 100, bytes);
				File f = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser_tmp" + File.separator + "images_tmp" + File.separator + String.format(Locale.ENGLISH, "%03d", image) + ".png");
				f.createNewFile();
				FileOutputStream fo = new FileOutputStream(f);
				bytes.writeTo(fo);

				bytes.flush();
				bytes.close();
				bytes.reset();
				fo.close();
			}
			else if (bmp == null)
				break;
		}
	
		vidFile.release();
		image = 0;
		
		//setContentView(R.layout.main2); //Switch to layout two - button with white text?
/*
		File imagePath = new File(Environment.getExternalStorageDirectory(), "KeyAnalyser/test_001.png");
		String img = imagePath.getAbsolutePath();
		
		Bitmap bmp = BitmapFactory.decodeFile(img);
		
		//int height = bmp.getHeight();
		//int width = bmp.getWidth();

        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Mat imgToProcess = null;
        Utils.bitmapToMat(bmp32, imgToProcess); 
        
        Mat imgToProcessGray = new Mat(); 
        Imgproc.cvtColor(imgToProcess, imgToProcessGray,Imgproc.COLOR_RGBA2GRAY);
        Mat imgToProcessGrayC4 = new Mat();
        Imgproc.cvtColor(imgToProcessGray, imgToProcessGrayC4,Imgproc.COLOR_GRAY2RGBA, 4);
        

        Bitmap bmpOut = Bitmap.createBitmap(bmp32.getWidth(),  bmp32.getHeight(), Bitmap.Config.ARGB_8888); 
        Utils.matToBitmap(imgToProcessGrayC4, bmpOut); 
		
		
		
/*		Mat mMat, result;
		
		
		mMat = Utils.loadResource(this, resId, Highgui.CV_LOAD_IMAGE_COLOR);
		Imgproc.cvtColor(mMat, result, Imgproc.COLOR_RGB2BGRA);
		bmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(result, bmp);
		mImageView.setImageBitmap(bmp);
		
		
		/*		
		int numPixels = bmp.getWidth()* bmp.getHeight(); 
        int[] pixels = new int[numPixels]; 

        //Get pixels.  Each int is the color values for one pixel. 
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight()); 
        //Create a Bitmap of the appropriate format. 
        Bitmap result = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Config.ARGB_8888);
        //Set RGB pixels. 
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        
        /** Working **/
        
        //Mat imgToProcess = new Mat();
        //Utils.bitmapToMat(bmp, imgToProcess);
        
        //result.cvtColor(result, result, result.COLOR_BGR2RGBA);
/*	
		Mat imgToProcess = Highgui.imread(picture);
		Mat imgToProcess = null;
		Mat imgToProcess = new Mat();
		
		Bitmap bmp = BitmapFactory.decodeFile(picture);
		Utils.bitmapToMat(bmp, imgToProcess); //App isn't crashing, but also not continuing to Intent later on - breaking out of function?
		
		Imgproc.cvtColor(imgToProcess, imgToProcess, Imgproc.COLOR_BGR2GRAY);
		Imgproc.cvtColor(imgToProcess, imgToProcess, Imgproc.COLOR_GRAY2RGBA, 4);
/*
		for(int i=0;i<imgToProcess.height();i++){
		    for(int j=0;j<imgToProcess.width();j++){
		        double y = 0.3 * imgToProcess.get(i, j)[0] + 0.59 * imgToProcess.get(i, j)[1] + 0.11 * imgToProcess.get(i, j)[2];
		        imgToProcess.put(i, j, new double[]{y, y, y, 255});
		    }
		}
		
	    Utils.matToBitmap(imgToProcess, bmp); //Again, not crashing app, but not continuing either...
*/
		toast.show(); //Confirm end of method
	}

	private void handleCameraVideo(Intent intent) {
		
		String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
		File newFolder = new File(extStorageDirectory + File.separator + "KeyAnalyser_tmp");
		newFolder.mkdir();
		
		try {
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fis = videoAsset.createInputStream();
		    File tmpFile = new File(Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser_tmp" + File.separator + "video.mp4"); 
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