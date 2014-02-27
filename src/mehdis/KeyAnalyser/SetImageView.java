package mehdis.KeyAnalyser;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

public class SetImageView {
	
	static void set(ImageView mImageView, final String keyModel, Resources res, boolean exist)
	{
		Bitmap bitmap = null;
		int id = 0;
		
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		
		if (keyModel.equals("MS2"))
			//bitmap = BitmapFactory.decodeResource(res, R.drawable.ic_ms2, bmOptions);
			id = R.drawable.ic_ms2;
		else if (keyModel.equals("UL050"))
			//bitmap = BitmapFactory.decodeResource(res, R.drawable.ic_ul050, bmOptions);
			id = R.drawable.ic_ul050;
		else if (keyModel.equals("UNI3"))
			//bitmap = BitmapFactory.decodeResource(res, R.drawable.ic_uni3, bmOptions);
			id = R.drawable.ic_uni3;
		else if (keyModel.equals("UL054"))
			//bitmap = BitmapFactory.decodeResource(res, R.drawable.ic_ul054, bmOptions);
			id = R.drawable.ic_ul054;
		else
		{
			id = R.drawable.ic_quit_huge;
			exist = false;
		}

		bitmap = BitmapFactory.decodeResource(res, id, bmOptions);
		
		int scaleFactor = 1;
		if ((mImageView.getWidth() > 0) || (mImageView.getHeight() > 0)) {
			scaleFactor = Math.min(bmOptions.outWidth/mImageView.getWidth(), bmOptions.outHeight/mImageView.getHeight());	
		}
	
		// Set bitmap options to scale the image decode target
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;
	
		bitmap = BitmapFactory.decodeResource(res, id, bmOptions);
							
		//Associate the Bitmap to the ImageView
		mImageView.setImageBitmap(bitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
	
	static void set(ImageView mImageView, String mCurrentPhotoPath)
	{
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		int scaleFactor = 1;
		if ((mImageView.getWidth() > 0) || (mImageView.getHeight() > 0)) {
			scaleFactor = Math.min(bmOptions.outWidth/mImageView.getWidth(), bmOptions.outHeight/mImageView.getHeight());	
		}
	
		// Set bitmap options to scale the image decode target
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;
	
		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
							
		//Associate the Bitmap to the ImageView
		mImageView.setImageBitmap(bitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
}