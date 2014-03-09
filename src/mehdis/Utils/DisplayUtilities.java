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
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class DisplayUtilities {	
	public static void statusToast(Context context, String statusMessage){
		Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show();
	}

	public static void showNotification(Context context, String resultModelName){
		Uri completionSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Intent intent = new Intent(context, KeyAnalyserActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		int instanceCounter = Settings.getSettings().getInstanceCounter();

        Notification notification = new NotificationCompat.Builder(context)
			.setContentTitle("Analysis #" + instanceCounter + " complete")
			.setContentText(resultModelName)
			.setSmallIcon(R.drawable.notification)
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.setSound(completionSound)
			.addAction(R.drawable.launcher, "View", pendingIntent)
			.addAction(0, "Remind", pendingIntent)
			.build();
			
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(instanceCounter, notification);
        
        if(isPhoneSilent(context)){
			((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
		}
	}
	
	private static boolean isPhoneSilent(Context context) {
		return ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_RING) == 0;
	}
}
