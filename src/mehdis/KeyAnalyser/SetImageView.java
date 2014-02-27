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
		if (keyModel.equals("MS2"))
			id = R.drawable.ic_ms2;
		else if (keyModel.equals("UL050"))
			id = R.drawable.ic_ul050;
		else if (keyModel.equals("UNI3"))
			id = R.drawable.ic_uni3;
		else if (keyModel.equals("UL054"))
			id = R.drawable.ic_ul054;
		else id = R.drawable.ic_quit_huge;
		
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;

		//Scale image if larger that boundaries of ImageView
		int scaleFactor = 1;
		if ((mImageView.getWidth() > 0) || (mImageView.getHeight() > 0))
			scaleFactor = Math.min(bmOptions.outWidth/mImageView.getWidth(), bmOptions.outHeight/mImageView.getHeight());	
	
		//Set bitmap options to scale the image decode target
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;
	
		Bitmap tempBitmap = BitmapFactory.decodeResource(res, id, bmOptions);
							
		//Associate the Bitmap to the ImageView
		mImageView.setImageBitmap(tempBitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
	
	static void set(ImageView mImageView, String mCurrentPhotoPath){
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		int scaleFactor = 1;
		if ((mImageView.getWidth() > 0) || (mImageView.getHeight() > 0))
			scaleFactor = Math.min(bmOptions.outWidth/mImageView.getWidth(), bmOptions.outHeight/mImageView.getHeight());	
	
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;
	
		Bitmap tempBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
							
		mImageView.setImageBitmap(tempBitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
}