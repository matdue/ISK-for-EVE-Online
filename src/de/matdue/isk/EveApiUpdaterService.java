package de.matdue.isk;

import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class EveApiUpdaterService extends WakefulIntentService {

	public EveApiUpdaterService() {
		super("de.matdue.isk.EveApiUpdaterService");
	}
	
	@Override
	protected void doWakefulWork(Intent intent) {
		Log.i("EveApiUpdaterService", "doWakefulWork()");
	}

}
