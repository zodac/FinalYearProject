package mehdis.Utils;

import mehdis.KeyAnalyser.KeyAnalyserActivity;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class FlingTouchListener implements OnTouchListener{
	private float prevX = -1;
	private boolean onTouchBlocked = false;
	private Handler onTouchHandler = new Handler();
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final float currentX = event.getX();
        
        if(!onTouchBlocked){
        	onTouchBlocked = true;
        	onTouchHandler.postDelayed(new Runnable() {
        		@Override
				public void run() {
					onTouchBlocked = false;
					if(prevX == -1){
		            	prevX = currentX;
		            }
					float delta = prevX-currentX; 
					
		            if(delta < 0){
		            	KeyAnalyserActivity.loadNextResult();
		            } else if(delta > 0){
		            	KeyAnalyserActivity.loadPreviousResult();
		            }
				}
			}, 500);
        }          
        return false;
	}
}
