package mehdis.KeyAnalyser;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

//Class used to fill ImageView with key model
public class SetImageView {
	
	//Can I name these methods better?
	static void set(ImageView mImageView, final String keyModel, Resources res){
		int id = 0;
		
		//Define image to be used
		if (keyModel.equals("MS2")){
			id = R.drawable.ms2;
		} else if (keyModel.equals("UL050")){
			id = R.drawable.ul050;
		} else if (keyModel.equals("UNI3")){
			id = R.drawable.uni3;
		} else if (keyModel.equals("UL054")){
			id = R.drawable.ul054;
		} else{
			id = R.drawable.quit_huge;
		}
		
		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;

		//Scale image if larger that boundaries of ImageView
		int scaleFactor = 1;
		if ((mImageView.getWidth() > 0) || (mImageView.getHeight() > 0)){
			scaleFactor = Math.min(bitmapOptions.outWidth/mImageView.getWidth(), bitmapOptions.outHeight/mImageView.getHeight());
		}
	
		//Set bitmap options to scale the image decode target
		bitmapOptions.inJustDecodeBounds = false;
		bitmapOptions.inSampleSize = scaleFactor;
		bitmapOptions.inPurgeable = true;
	
		Bitmap tempBitmap = BitmapFactory.decodeResource(res, id, bitmapOptions);
							
		//Associate the Bitmap to the ImageView
		mImageView.setImageBitmap(tempBitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
	
	static void set(ImageView mImageView, String mCurrentPhotoPath){
		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bitmapOptions);

		int scaleFactor = 1;
		if ((mImageView.getWidth() > 0) || (mImageView.getHeight() > 0)){
			scaleFactor = Math.min(bitmapOptions.outWidth/mImageView.getWidth(), bitmapOptions.outHeight/mImageView.getHeight());
		}
	
		bitmapOptions.inJustDecodeBounds = false;
		bitmapOptions.inSampleSize = scaleFactor;
		bitmapOptions.inPurgeable = true;
	
		Bitmap tempBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bitmapOptions);
							
		mImageView.setImageBitmap(tempBitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
}