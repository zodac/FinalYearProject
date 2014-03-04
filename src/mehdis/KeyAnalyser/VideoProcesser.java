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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class VideoProcesser extends AsyncTask<Void, Void, Void>{
	private static final String ROOT_LOCATION = Environment.getExternalStorageDirectory() + File.separator + "KeyAnalyser";
	private static final File VIDEO_LOCATION = new File(ROOT_LOCATION + File.separator + "video.mp4");
	
//	protected static VideoProcesser instance;
	private int firstRedColumn = 0;
	private int lastRedColumn = 0;
	private int firstRedRow = 0;
	private int lastRedRow = 0;
	Context context;
	int numOfPasses;
	
	public VideoProcesser(Context context, int numOfPasses){
		this.context = context;
		this.numOfPasses = numOfPasses;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void statusToast(String statusMessage){
		Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show();
	}
	
	private void writeImageToFile(Bitmap outputImage, String saveLocation, int imageNumber){
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		outputImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
		File imageFile = new File(ROOT_LOCATION + File.separator + saveLocation + File.separator + String.format(Locale.ENGLISH, "%03d", imageNumber) + ".png");
		try {
			imageFile.createNewFile();
			bytes.writeTo(new FileOutputStream(imageFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Result processVideo(){
		long startTime = System.currentTimeMillis();	
		Result analysisResult = null;
		
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			statusToast(String.valueOf(context.getText(R.string.sdCardNotMounted)));
		} else if(!VIDEO_LOCATION.exists()){
			statusToast(String.valueOf(context.getText(R.string.noVideoFile)));
		} else{
			createWorkFolders();
			
			MediaMetadataRetriever videoFile = new MediaMetadataRetriever();
			videoFile.setDataSource(VIDEO_LOCATION.getAbsolutePath());
			Bitmap extractedFrame = videoFile.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
			
			int i = 0;
			int imageCount = 0;
			
			double length = 0;
			double degreeAverage = 0;
			
			while(extractedFrame != null && i < numOfPasses){
				writeImageToFile(extractedFrame, String.valueOf(context.getText(R.string.rawImagesFolder)), ++imageCount);
				IplImage imageToProcess = cvLoadImage(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.rawImagesFolder)) + File.separator + String.format(Locale.ENGLISH, "%03d", imageCount) + ".png");	
				
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
			            	
			            	if(firstObjectPixelRow <= 0){
			            		firstObjectPixelRow = row;
			            	} else{
			            		lastObjectPixelRow = row;
			            	}
			            } else{
			            	cannyResultImage.setPixel(col, row, Color.argb(255, 0, 0, keyPixel)); //Background pixel
			            }
			        }
		        }
				writeImageToFile(cannyResultImage, String.valueOf(context.getText(R.string.cannyImagesFolder)), imageCount);
				
				length = (lastObjectPixelRow-firstObjectPixelRow)*(17.5/sizeOfMaxRedColumn);
				
				/**
				 * Create images for degree calculation - trying to isolate the tip of the key and some of the rows above it
				 */
				//Define area to isolate and save to file
				isolateKeyTip(imageCount, imageToProcess, cannyImage, lastObjectPixelRow);
				
				//Load image for degree calculation
				imageToProcess = cvLoadImage(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.cannyImagesFolder)) + File.separator + String.format(Locale.ENGLISH, "%03d", imageCount) + ".png");
				cannyImage = IplImage.create(imageToProcess.width(), imageToProcess.height(), IPL_DEPTH_8U, 1);
				cvCvtColor(imageToProcess, cannyImage, CV_BGR2GRAY);
			
				//Calculate angle of tip
				degreeAverage = angleCalculation(cannyImage);
				
				extractedFrame = videoFile.getFrameAtTime(100000*++i, MediaMetadataRetriever.OPTION_CLOSEST);
			}
			videoFile.release();

			analysisResult = matchKeyToDatabase(length, degreeAverage, (System.currentTimeMillis()-startTime)/1000, imageCount);
		}
		return analysisResult;
		/** End **/
	}

	private void isolateKeyTip(int imageCount, IplImage imageToProcess, IplImage cannyImage, int lastObjectPixelRow) {
		Bitmap cannyResultImage;
		int row_offset_degree = imageToProcess.height()/200;
		int source_col = firstRedColumn+(imageToProcess.width()/3);
		
		if ((lastRedColumn-(imageToProcess.width()/3))-(firstRedColumn+(imageToProcess.width()/3)) < 0){
			cannyResultImage = Bitmap.createBitmap(imageToProcess.width()/3, ((7*row_offset_degree)+1), Bitmap.Config.ARGB_8888);
		} else{
			cannyResultImage = Bitmap.createBitmap((lastRedColumn-(imageToProcess.width()/3))-(firstRedColumn+(imageToProcess.width()/3)), ((7*row_offset_degree)+1), Bitmap.Config.ARGB_8888);
		}
		
		
		for (int col = 0; col < ((lastRedColumn-(imageToProcess.width()/3)) - (firstRedColumn+(imageToProcess.width()/3))); col++, source_col++){
			int source_row = lastObjectPixelRow+1-(7*row_offset_degree);
			
			for (int row = 0; row < (7*row_offset_degree)+1; row++, source_row++){
				int keyPixel = (int) cvGet2D(cannyImage, source_row, source_col).getVal(0);
				
				if (keyPixel > 100){
					cannyResultImage.setPixel(col, row, Color.argb(255, 255, 255, keyPixel));
				} else{
					cannyResultImage.setPixel(col, row, Color.argb(255, 0, 0, keyPixel));
				}
			}
		}
		writeImageToFile(cannyResultImage, String.valueOf(context.getText(R.string.degreeImagesFolder)), imageCount);
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
				
				if(blue < 0){
					blue += 256;
				}
				if(green < 0){
					green += 256;
				}
				if(red < 0){
					red += 256;
				}
				
		        if(red > redLimit && green < greenLimit && blue < blueLimit){
		        	numOfRedPixels++;
		        }
			}
			
			if (numOfRedPixels > numOfRedPixelsInMostRedColumn){
				numOfRedPixelsInMostRedColumn = numOfRedPixels;
			}
			
			if (numOfRedPixels > 10){
				if (firstRedColumn <= 0){
					firstRedColumn = col;
				} else{
					lastRedColumn = col;
				}
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
				
				if(blue < 0){
					blue += 256;
				}
				if(green < 0){
					green += 256;
				}
				if(red < 0){
					red += 256;
				}
				
				if(red > redLimit && green < greenLimit && blue < blueLimit){
		        	numOfRedPixels++;
				}
			}
			
			if (numOfRedPixels > numOfRedPixelsInMostRedRow){
				numOfRedPixelsInMostRedRow = numOfRedPixels;
			}
			
			if (numOfRedPixels > 15){ //Red area, by start and finish rows, for Canny boundaries
				if (firstRedRow <= 0){
					firstRedRow = row;
				} else{
					lastRedRow = row;
				}
			}
		}
		return sumOfColumnsWithMaxRedPixels;
	}
	
	private void createWorkFolders(){
		new File(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.rawImagesFolder))).mkdir();
		new File(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.cannyImagesFolder))).mkdir();
		new File(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.degreeImagesFolder))).mkdir();
	}
	
	private Result matchKeyToDatabase(double length, double angle, long runTime, int passes){
		double errorThreshold = 15;
		double lengthThreshold = 2.5;
		double min = 100;
		double lengthDelta;
		double angleDelta;
		double confidence;
		Key minKey = null;
		
		if(length < lengthThreshold){
			return new Result(String.valueOf(context.getText(R.string.noModelFound)), length, angle, runTime, passes, 100-min);
		}

		for(Key key : KeyAnalyserActivity.databaseKeys){
			lengthDelta = Math.abs(key.getLength()-length)*3;
			angleDelta = Math.abs(key.getAngle()-angle)*0.75;
			confidence = lengthDelta + angleDelta;
			
			if(confidence < min){
				min = confidence;
				minKey = key;
			}
		}
		
		if(min <= errorThreshold){
			return new Result(minKey.getModelName(), length, angle, runTime, passes, 100-min);
		}
		return new Result(String.valueOf(context.getText(R.string.noModelFound)), length, angle, runTime, passes, 100-min);
	}
}