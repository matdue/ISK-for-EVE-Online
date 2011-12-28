package de.matdue.isk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.SystemClock;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.commonsware.cwac.wakeful.WakefulIntentService.AlarmListener;

public class EveApiUpdaterListener implements AlarmListener {

	@Override
	public long getMaxAge() {
		return AlarmManager.INTERVAL_HOUR;
	}

	@Override
	public void scheduleAlarms(AlarmManager alarmManager, PendingIntent pendingIntent, Context context) {
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
				SystemClock.elapsedRealtime() + 60000, 
				AlarmManager.INTERVAL_HOUR / 60, 
				pendingIntent);
	}

	@Override
	public void sendWakefulWork(Context context) {
		WakefulIntentService.sendWakefulWork(context, EveApiUpdaterService.class);
	}

}
