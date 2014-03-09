package mehdis.Utils;

import java.io.File;

import mehdis.KeyAnalyser.R;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.widget.ImageView;

public class SetImageView {
	
	public static void setImageViewFromLocalFile(Resources resources, ImageView keyView, final String keyModelName){
		
		if(keyModelName.equals("No model found")){
			keyView.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.notfound));
		}
		
		File imageFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "Keys" + File.separator + keyModelName +  ".png");
		
		if(imageFile.exists()){
		    Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
		    keyView.setImageBitmap(myBitmap);
		}
	}
}