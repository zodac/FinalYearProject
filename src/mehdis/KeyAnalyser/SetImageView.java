package mehdis.KeyAnalyser;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class SetImageView {
	
	public static void setImageViewFromLocalFile(ImageView keyView, final String keyModelName){
		File imageFile = new  File(File.separator + "sdcard" + File.separator + "Keys" + File.separator + keyModelName +  ".png");
		
		if(imageFile.exists()){
		    Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
		    keyView.setImageBitmap(myBitmap);
		}
	}
}