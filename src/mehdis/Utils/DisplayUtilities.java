package mehdis.Utils;

import mehdis.Entities.Settings;
import mehdis.KeyAnalyser.KeyAnalyserActivity;
import mehdis.KeyAnalyser.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class DisplayUtilities {	
	public static void statusToast(CharSequence toastMessage){
		Toast.makeText(KeyAnalyserActivity.thisContext, toastMessage, Toast.LENGTH_SHORT).show();
	}

	public static void showNotification(String resultModelName){
		Intent intent = new Intent(KeyAnalyserActivity.thisContext, KeyAnalyserActivity.class);
		int instanceCounter = Settings.getSettings().getInstanceCounter();

        Notification notification = new NotificationCompat.Builder(KeyAnalyserActivity.thisContext)
			.setContentTitle(String.format(Settings.getSettings().getLocale(), String.valueOf(KeyAnalyserActivity.thisContext.getText(R.string.analysisComplete)), instanceCounter))
			.setContentText(resultModelName)
			.setSmallIcon(R.drawable.notification)
			.setContentIntent(PendingIntent.getActivity(KeyAnalyserActivity.thisContext, 0, intent, 0))
			.setAutoCancel(true)
			.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
			.build();
			
        NotificationManager notificationManager = (NotificationManager) KeyAnalyserActivity.thisContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(instanceCounter, notification);
        
        if(isPhoneSilent()){
			((Vibrator) KeyAnalyserActivity.thisContext.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
		}
	}
	
	private static boolean isPhoneSilent() {
		return ((AudioManager) KeyAnalyserActivity.thisContext.getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_RING) == 0;
	}
}
