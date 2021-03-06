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

import java.io.File;

import mehdis.Entities.Boundary;
import mehdis.Entities.Settings;
import mehdis.Utils.FileUtilities;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.common.util.concurrent.AtomicDoubleArray;
import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


public class ImageProcessor extends Thread {
	private int id;
	private Boundary redAreaBoundary = new Boundary();
	private IplImage imageToProcess;
	private AtomicDoubleArray lengths;
	private AtomicDoubleArray angles;
	private int progressPercentage;
	
	public ImageProcessor(int id, IplImage imageToProcess, AtomicDoubleArray lengths, AtomicDoubleArray angles){
		this.id = id;
		this.imageToProcess = imageToProcess;
		this.lengths = lengths;
		this.angles = angles;
		this.progressPercentage = (int) ((100000/lengths.length())/4);
	}
	
	public void run(){
		int sizeOfMaxRedColumn = redAreaCalculation(imageToProcess);
		updateProgressBar(progressPercentage/2);
		/**
		 * Moving on to cvCanny analysis: isolating image within red area
		 */
		int rowOffset = imageToProcess.height()/200; //Offsets used for red boundaries (since usually the video isn't straight)
		Boundary objectPixels = new Boundary();
		
		IplImage cannyImage = IplImage.create(imageToProcess.width(), imageToProcess.height(), IPL_DEPTH_8U, 1);
		cvCanny(imageToProcess, cannyImage, 65, 175, 3);
      
		Bitmap cannyResultImage = Bitmap.createBitmap(imageToProcess.width(), imageToProcess.height(), Bitmap.Config.ARGB_8888);
		updateProgressBar(progressPercentage/2);
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
		FileUtilities.writeImageToFile(cannyResultImage, String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.cannyImagesFolder)), this.id+1);
		
		double ratio = (17.5/sizeOfMaxRedColumn);
		lengths.set(this.id, (objectPixels.getSouth()-objectPixels.getNorth())*ratio);
		updateProgressBar(progressPercentage*2);
		/**
		 * Create images for degree calculation - trying to isolate the tip of the key and some of the rows above it
		 */
		isolateKeyTip((int) this.id+1, imageToProcess, cannyImage, objectPixels.getSouth());
		
		//Load image for degree calculation
		imageToProcess = cvLoadImage(FileUtilities.ROOT_LOCATION + File.separator + String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.cannyImagesFolder)) + File.separator + String.format(Settings.getSettings().getLocale(), "%03d.png", this.id+1));
		cannyImage = IplImage.create(imageToProcess.width(), imageToProcess.height(), IPL_DEPTH_8U, 1);
		cvCvtColor(imageToProcess, cannyImage, CV_BGR2GRAY);

		//Calculate angle of tip
		angles.set(this.id, calculateTipAngle(cannyImage));
		updateProgressBar(progressPercentage);
	}

	private void updateProgressBar(int progressPercentage) {
		KeyAnalyserActivity.updateProgressBar(progressPercentage);
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
		FileUtilities.writeImageToFile(cannyResultImage, String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.degreeImagesFolder)), imageCount);
	}

	private double calculateTipAngle(IplImage cannyImage) {
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
				
				numOfRedPixels = redAreaThresholding(numOfRedPixels, blue, green, red);
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
				
				numOfRedPixels = redAreaThresholding(numOfRedPixels, blue, green, red);
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

	public int redAreaThresholding(int numOfRedPixels, int blue, int green, int red) {
		int redLimit = 140;
		int greenLimit = 70;
		int blueLimit = 55;
		
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

	public long getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
