package mehdis.KeyAnalyser;

import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

import java.io.File;
import java.util.Locale;

import mehdis.Entities.Key;
import mehdis.Entities.Result;
import mehdis.Utils.FileUtilities;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.ProgressBar;

import com.google.common.util.concurrent.AtomicDoubleArray;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class VideoProcesser extends AsyncTask<Void, Void, Void>{	
	private int numOfPasses;
	private Context context;
	private Result analysisResult;
	private ProgressBar progressBar;
	private Handler mHandler;
	
	public VideoProcesser(Context context, int numOfPasses, ProgressBar progressBar, Handler mHandler){
		this.context = context;
		this.numOfPasses = numOfPasses;
		this.progressBar = progressBar;
		this.mHandler = mHandler;
	}
	
	protected Void doInBackground(Void... params) {
		resetProgressBar();
		analysisResult = processVideo();
		KeyAnalyserActivity.result = analysisResult;
		return null;
	}

	private void resetProgressBar() {
		mHandler.post(new Runnable(){
			public void run(){
				progressBar.setProgress(0);
			}
		});
	}
	
	protected void onPostExecute(Void result) {
		if(analysisResult != null){
			KeyAnalyserActivity.saveResult();
		}
	}
	
	private Result processVideo(){
		long startTime = System.currentTimeMillis();
		Result analysisResult = null;
		
		MediaMetadataRetriever videoFile = FileUtilities.getVideoFile();
		Bitmap extractedFrame = videoFile.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
		
		int frameCounter = 0;
		AtomicDoubleArray lengths = new AtomicDoubleArray(new double[numOfPasses]);
		AtomicDoubleArray angles = new AtomicDoubleArray(new double[numOfPasses]);
		
		ImageProcesser[] imageProcessers = new ImageProcesser[numOfPasses];
		
		while(extractedFrame != null && frameCounter < numOfPasses){
			FileUtilities.writeImageToFile(extractedFrame, String.valueOf(context.getText(R.string.rawImagesFolder)), frameCounter+1);
			IplImage imageToProcess = cvLoadImage(FileUtilities.ROOT_LOCATION + File.separator + String.valueOf(context.getText(R.string.rawImagesFolder)) + File.separator + String.format(Locale.ENGLISH, "%03d.png", frameCounter+1));	
			
			imageProcessers[frameCounter] = new ImageProcesser(frameCounter, imageToProcess, lengths, angles, context, progressBar, mHandler);
			imageProcessers[frameCounter].start();
			
			extractedFrame = getNextFrame(videoFile, ++frameCounter);
		}
		
		for(ImageProcesser imageProcesser : imageProcessers){
			try {
				imageProcesser.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		videoFile.release();

		double averageLength = getAverage(lengths, frameCounter);
		double averageAngle = getAverage(angles, frameCounter);
		
		analysisResult = matchKeyToDatabase(averageLength, averageAngle, (System.currentTimeMillis()-startTime)/1000, frameCounter);
		return analysisResult;
	}
	
	private double getAverage(AtomicDoubleArray array, int usedFrames){
		double total = 0;
		
		for(int i = 0; i < usedFrames; i++){
			total += array.get(i);
		}
		
		return (total/usedFrames);
	}

	private Bitmap getNextFrame(MediaMetadataRetriever videoFile, int frameCounter) {
		return videoFile.getFrameAtTime(100000*frameCounter, MediaMetadataRetriever.OPTION_CLOSEST);
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