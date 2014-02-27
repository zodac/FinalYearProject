package com.example.android.photobyintent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

public class PhotoIntentActivity extends Activity {
	
	private String appName = "KeyAnalyser";
	private String storageFolder = "/" + appName + "_tmp"; // /KeyAnalyser_tmp
	
	private static final int ACTION_TAKE_PHOTO_B = 1;
	private static final int ACTION_TAKE_PHOTO_S = 2;
	private static final int ACTION_TAKE_VIDEO = 3;

	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
	private ImageView mImageView;
	private Bitmap mImageBitmap;

	private static final String VIDEO_STORAGE_KEY = "viewvideo";
	private static final String VIDEOVIEW_VISIBILITY_STORAGE_KEY = "videoviewvisibility";
	private VideoView mVideoView;
	private Uri mVideoUri;

	private String mCurrentPhotoPath;

	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

	
	/* Photo album for this application */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
			
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}
	@SuppressLint("SimpleDateFormat")
	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}
	private File setUpPhotoFile() throws IOException {
		
		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();
		
		return f;
	}
	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = mImageView.getWidth();
		int targetH = mImageView.getHeight();

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;
		
		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		
		/* Associate the Bitmap to the ImageView */
		mImageView.setImageBitmap(bitmap);
		mVideoUri = null;
		mImageView.setVisibility(View.VISIBLE);
		mVideoView.setVisibility(View.INVISIBLE);
	}
	private void galleryAddPic() {
		    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			File f = new File(mCurrentPhotoPath);
		    Uri contentUri = Uri.fromFile(f);
		    mediaScanIntent.setData(contentUri);
		    this.sendBroadcast(mediaScanIntent);
	}
	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch(actionCode) {
		case ACTION_TAKE_PHOTO_B:
			File f = null;
			
			try {
				f = setUpPhotoFile();
				mCurrentPhotoPath = f.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			} catch (IOException e) {
				e.printStackTrace();
				f = null;
				mCurrentPhotoPath = null;
			}
			break;

		default:
			break;			
		} // switch

		startActivityForResult(takePictureIntent, actionCode);
	}
	private void dispatchTakeVideoIntent() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
	}
	
	private void dispatchProcVideoIntent() throws IOException{
		
		//Activity.requestWindowFeature(Window.FEATURE_PROGRESS); 
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
		File newFolder = new File(extStorageDirectory + storageFolder + File.separator + "tmp_images");
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
				File f = new File(Environment.getExternalStorageDirectory() + storageFolder + File.separator + "tmp_images" + File.separator + String.format(Locale.ENGLISH, "%03d", image) + ".png");
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

	private void handleSmallCameraPhoto(Intent intent) {
		Bundle extras = intent.getExtras();
		mImageBitmap = (Bitmap) extras.get("data");
		mImageView.setImageBitmap(mImageBitmap);
		mVideoUri = null;
		mImageView.setVisibility(View.VISIBLE);
		mVideoView.setVisibility(View.INVISIBLE);
	}
	private void handleBigCameraPhoto() {

		if (mCurrentPhotoPath != null) {
			setPic();
			galleryAddPic();
			mCurrentPhotoPath = null;
		}

	}
	private void handleCameraVideo(Intent intent) {
		//Method to change storage location of video file
			//Good luck
		
		String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
		File newFolder = new File(extStorageDirectory + storageFolder);
		newFolder.mkdir();
		
		try {
		    AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
		    FileInputStream fis = videoAsset.createInputStream();
		    //File f = new File(Environment.getExternalStorageDirectory() + storageFolder + File.separator + "test_" + String.format(Locale.ENGLISH, "%03d", image) + ".png");
		    File tmpFile = new File(Environment.getExternalStorageDirectory() + storageFolder + File.separator + "video.mp4"); //TODO: change filename to application name 
		    FileOutputStream fos = new FileOutputStream(tmpFile);

		    byte[] buf = new byte[40960];
		    int len;
		    while ((len = fis.read(buf)) > 0) {
		        fos.write(buf, 0, len);
		    }       
		    fis.close();
		    fos.close();
		 } catch (IOException io_e) {
		    // Handle error?
		  }
		
		mVideoUri = intent.getData();
		mVideoView.setVideoURI(mVideoUri);
		mImageBitmap = null;
		mVideoView.setVisibility(View.VISIBLE);
		mImageView.setVisibility(View.INVISIBLE);
	}
	Button.OnClickListener mTakePicOnClickListener = 
		new Button.OnClickListener() {
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
		}
	};
	Button.OnClickListener mTakePicSOnClickListener = 
		new Button.OnClickListener() {
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
		}
	};
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
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mImageView = (ImageView) findViewById(R.id.imageView1);
		mVideoView = (VideoView) findViewById(R.id.videoView1);
		mImageBitmap = null;
		mVideoUri = null;

		Button picBtn = (Button) findViewById(R.id.btnIntend);
		setBtnListenerOrDisable( 
				picBtn, 
				mTakePicOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);

		Button picSBtn = (Button) findViewById(R.id.btnIntendS);
		setBtnListenerOrDisable( 
				picSBtn, 
				mTakePicSOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);

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
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO_B: {
			if (resultCode == RESULT_OK) {
				handleBigCameraPhoto();
			}
			break;
		} // ACTION_TAKE_PHOTO_B

		case ACTION_TAKE_PHOTO_S: {
			if (resultCode == RESULT_OK) {
				handleSmallCameraPhoto(data);
			}
			break;
		} // ACTION_TAKE_PHOTO_S

		case ACTION_TAKE_VIDEO: {
			if (resultCode == RESULT_OK) {
				handleCameraVideo(data);
			}
			break;
		} // ACTION_TAKE_VIDEO
		} // switch
	}
	/*
	public void VideoProcess() {
        // Do something in response to button
		
		//char* filename = "/mnt/sdcard/DCIM/01.jpg";
		//cv::Mat image = imread(filename, 1);
		
		String filename = "/mnt/sdcard/DCIM/01.jpg";		
		Bitmap bmap = BitmapFactory.decodeFile(filename);
		Mat imgToProcess = null;
		
		Utils.bitmapToMat(bmap, imgToProcess);
		
		Imgproc.cvtColor(imgToProcess, imgToProcess, Imgproc.COLOR_BGR2GRAY);
		Imgproc.cvtColor(imgToProcess, imgToProcess, Imgproc.COLOR_GRAY2RGBA, 4);
		
	    Utils.matToBitmap(imgToProcess, bmap);
    }
	*/
	
	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putParcelable(VIDEO_STORAGE_KEY, mVideoUri);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null) );
		outState.putBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY, (mVideoUri != null) );
		super.onSaveInstanceState(outState);
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		mVideoUri = savedInstanceState.getParcelable(VIDEO_STORAGE_KEY);
		mImageView.setImageBitmap(mImageBitmap);
		mImageView.setVisibility(
				savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ? 
						ImageView.VISIBLE : ImageView.INVISIBLE
		);
		mVideoView.setVideoURI(mVideoUri);
		mVideoView.setVisibility(
				savedInstanceState.getBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY) ? 
						ImageView.VISIBLE : ImageView.INVISIBLE
		);
	}
	
	/*** Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
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