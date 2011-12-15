package de.matdue.isk.eve;

import java.util.Date;

public class CacheInformation {
	
	public Date currentTime;
	public Date cachedUntil;
	public Object cachedData;
	
	public static String buildHashKey(String url, String keyID, String vCode) {
		return url + "-" + keyID + "-" + vCode;
	}

}
