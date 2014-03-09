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

import mehdis.Entities.AnalysisData;
import mehdis.Entities.Boundary;
import mehdis.Entities.Key;
import mehdis.Entities.Result;
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
	private static final String ROOT_LOCATION = KeyAnalyserActivity.ROOT_LOCATION;
	private static final File VIDEO_LOCATION = KeyAnalyserActivity.VIDEO_LOCATION;
	
	private int numOfPasses;
	private Boundary redAreaBoundary = new Boundary();
	private Context context;
	private Result result;
	
	public VideoProcesser(Context context, int numOfPasses){
		this.context = context;
		this.numOfPasses = numOfPasses;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		result = processVideo();
		KeyAnalyserActivity.setResult(result);
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		KeyAnalyserActivity.saveResult();
		KeyAnalyserActivity.showNotification();
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
			MediaMetadataRetriever videoFile = new MediaMetadataRetriever();
			videoFile.setDataSource(VIDEO_LOCATION.getAbsolutePath());
			Bitmap extractedFrame = videoFile.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
			
			int frameCounter = 0;
			int imageCounter = 0;
			AnalysisData analysisData = new AnalysisData(0, 0);
			
			while(extractedFrame != null && frameCounter < numOfPasses){
				writeImageToFile(extractedFrame, String.valueOf(context.getText(R.string.rawImagesFolder)), ++imageCounter);
				IplImage imageToProcess = cvLoadImage(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.rawImagesFolder)) + File.separator + String.format(Locale.ENGLISH, "%03d", imageCounter) + ".png");	
				
				/**
				 * Determine red area for pixel/cm ratio & red area boundary (for cvCanny)
				 */
				int sizeOfMaxRedColumn = redAreaCalculation(imageToProcess);
				
				/**
				 * Moving on to cvCanny analysis: isolating image within red area (with boundaries defined by first_red_row, last_red_row, first_red_col & last_red_col)
				 */
		        //Apply cvCanny

		        int rowOffset = imageToProcess.height()/200; //Offsets used for red boundaries (since usually the video isn't straight)
		        Boundary objectPixels = new Boundary();
		        
				IplImage cannyImage = IplImage.create(imageToProcess.width(), imageToProcess.height(), IPL_DEPTH_8U, 1);
		        cvCanny(imageToProcess, cannyImage, 75, 165, 3);
		       
		        Bitmap cannyResultImage = Bitmap.createBitmap(imageToProcess.width(), imageToProcess.height(), Bitmap.Config.ARGB_8888);
		        
		        //If within these boundaries, apply cvCanny, otherwise leave empty (for .png, this results in transparent space around the image)
		        for(int row = redAreaBoundary.getNorth()+(4*rowOffset); row < redAreaBoundary.getSouth()-(4*rowOffset); row++){
		        	int colOffset = imageToProcess.width()/110;
		        	for(int col = redAreaBoundary.getWest()+(7*colOffset); col < redAreaBoundary.getEast()-(7*colOffset); col++){
			            int keyPixel = (int) cvGet2D(cannyImage, row, col).getVal(0);

			            if (keyPixel > 100){
			            	cannyResultImage.setPixel(col, row, Color.argb(255, 255, 255, keyPixel)); //Object pixel
			            	
			            	if(objectPixels.getNorth() <= 0){
			            		objectPixels.setNorth(row);
			            	} else{
			            		objectPixels.setSouth(row);
			            	}
			            } else{
			            	cannyResultImage.setPixel(col, row, Color.argb(255, 0, 0, keyPixel)); //Background pixel
			            }
			        }
		        }
				writeImageToFile(cannyResultImage, String.valueOf(context.getText(R.string.cannyImagesFolder)), imageCounter);
				
				analysisData.setLength((objectPixels.getSouth()-objectPixels.getNorth())*(17.5/sizeOfMaxRedColumn));
				
				/**
				 * Create images for degree calculation - trying to isolate the tip of the key and some of the rows above it
				 */
				//Define area to isolate and save to file
				isolateKeyTip(imageCounter, imageToProcess, cannyImage, objectPixels.getSouth());
				
				//Load image for degree calculation
				imageToProcess = cvLoadImage(ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.cannyImagesFolder)) + File.separator + String.format(Locale.ENGLISH, "%03d", imageCounter) + ".png");
				cannyImage = IplImage.create(imageToProcess.width(), imageToProcess.height(), IPL_DEPTH_8U, 1);
				cvCvtColor(imageToProcess, cannyImage, CV_BGR2GRAY);
			
				//Calculate angle of tip
				analysisData.setAngle(angleCalculation(cannyImage));
				
				extractedFrame = videoFile.getFrameAtTime(100000*++frameCounter, MediaMetadataRetriever.OPTION_CLOSEST);
			}
			videoFile.release();

			analysisResult = matchKeyToDatabase(analysisData.getLength(), analysisData.getAngle(), (System.currentTimeMillis()-startTime)/1000, imageCounter);
		}
		return analysisResult;
		/** End **/
	}

	private void isolateKeyTip(int imageCount, IplImage imageToProcess, IplImage cannyImage, int lastObjectPixelRow) {
		Bitmap cannyResultImage;
		int rowOffsetDegree = imageToProcess.height()/200;
		int sourceCol = redAreaBoundary.getWest()+(imageToProcess.width()/3);
		
		if ((redAreaBoundary.getEast()-(imageToProcess.width()/3))-(redAreaBoundary.getWest()+(imageToProcess.width()/3)) < 0){
			cannyResultImage = Bitmap.createBitmap(imageToProcess.width()/3, ((7*rowOffsetDegree)+1), Bitmap.Config.ARGB_8888);
		} else{
			cannyResultImage = Bitmap.createBitmap((redAreaBoundary.getEast()-(imageToProcess.width()/3))-(redAreaBoundary.getWest()+(imageToProcess.width()/3)), 
												   ((7*rowOffsetDegree)+1),
												   Bitmap.Config.ARGB_8888);
		}
		
		
		for (int col = 0; col < ((redAreaBoundary.getEast()-(imageToProcess.width()/3)) - (redAreaBoundary.getWest()+(imageToProcess.width()/3))); col++, sourceCol++){
			int sourceRow = lastObjectPixelRow+1-(7*rowOffsetDegree);
			
			for (int row = 0; row < (7*rowOffsetDegree)+1; row++, sourceRow++){
				int keyPixel = (int) cvGet2D(cannyImage, sourceRow, sourceCol).getVal(0);
				
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
		int sumOfColumnsWithMaxRedPixels = redColumnCalculation(imageToProcess);
		
		redRowCalculation(imageToProcess);
		return sumOfColumnsWithMaxRedPixels;
	}

	public int redColumnCalculation(IplImage imageToProcess) {
		int sumOfColumnsWithMaxRedPixels = 0;
		int numOfRedPixelsInMostRedColumn = 0;
		
		for (int col = 0; col < imageToProcess.width(); col+=2){
			int numOfRedPixels = 0;
			
			for (int row = 0; row < imageToProcess.height(); row++){
				BytePointer imageRGBExtraction = imageToProcess.imageData();
				int blue = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3);
				int green = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3 + 1);
				int red = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3 + 2);
				
				numOfRedPixels = colourThresholding(numOfRedPixels, blue, green, red);
			}
			
			if (numOfRedPixels > numOfRedPixelsInMostRedColumn){
				numOfRedPixelsInMostRedColumn = numOfRedPixels;
			}
			
			if (numOfRedPixels > 10){
				if (redAreaBoundary.getWest() <= 0){
					redAreaBoundary.setWest(col);
				} else{
					redAreaBoundary.setEast(col);
				}
			}
		}
		sumOfColumnsWithMaxRedPixels += numOfRedPixelsInMostRedColumn;
		return sumOfColumnsWithMaxRedPixels;
	}
	
	public void redRowCalculation(IplImage imageToProcess) {
		int numOfRedPixelsInMostRedRow = 0;
		
		for (int row = 0; row < imageToProcess.height(); row+=3){
			int numOfRedPixels = 0;
			
			for (int col = 0; col < imageToProcess.width(); col++){						
				BytePointer imageRGBExtraction = imageToProcess.imageData();
				int blue = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3);
				int green = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3 + 1);
				int red = (int) imageRGBExtraction.get(imageToProcess.widthStep()*row + col*3 + 2);
				
				numOfRedPixels = colourThresholding(numOfRedPixels, blue, green, red);
			}
			
			if (numOfRedPixels > numOfRedPixelsInMostRedRow){
				numOfRedPixelsInMostRedRow = numOfRedPixels;
			}
			
			if (numOfRedPixels > 15){ //Red area, by start and finish rows, for Canny boundaries
				if (redAreaBoundary.getNorth() <= 0){
					redAreaBoundary.setNorth(row);
				} else{
					redAreaBoundary.setSouth(row);
				}
			}
		}
	}

	public int colourThresholding(int numOfRedPixels, int blue, int green, int red) {
		int redLimit = 145;
		int greenLimit = 65;
		int blueLimit = 50;
		
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
		return numOfRedPixels;
	}
	
	private Result matchKeyToDatabase(double length, double angle, long runTime, int passes){
		double errorThreshold = 15;
		double lengthThreshold = 2.5;
		double confidenceLevel = 100;
		double lengthDelta;
		double angleDelta;
		double delta;
		Key minKey = null;
		
		if(length < lengthThreshold){
			return new Result(String.valueOf(context.getText(R.string.noModelFound)), length, angle, runTime, passes, 100-confidenceLevel);
		}

		for(Key key : KeyAnalyserActivity.databaseKeys){
			lengthDelta = Math.abs(key.getLength()-length)*3;
			angleDelta = Math.abs(key.getAngle()-angle)*0.75;
			delta = lengthDelta + angleDelta;
			
			if(delta < confidenceLevel){
				confidenceLevel = delta;
				minKey = key;
			}
		}
		
		if(confidenceLevel <= errorThreshold){
			return new Result(minKey.getModelName(), length, angle, runTime, passes, 100-confidenceLevel);
		}
		return new Result(String.valueOf(context.getText(R.string.noModelFound)), length, angle, runTime, passes, 100-confidenceLevel);
	}
}