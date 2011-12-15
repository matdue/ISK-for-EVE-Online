package de.matdue.isk.eve;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.Attributes;

import android.net.http.AndroidHttpClient;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import android.util.Xml.Encoding;

public class Api {
	
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String AGENT = "Android de.matdue.isk";
	private static final String URL_BASE = "https://api.eveonline.com";

	private static final HashMap<String, SoftReference<CacheInformation>> cachedItems = new HashMap<String, SoftReference<CacheInformation>>();
	
	static {
		// EVE Online API always uses GMT
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public Account validateKey(String keyID, String vCode) {
		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey("/account/APIKeyInfo.xml.aspx", keyID, vCode);
		SoftReference<CacheInformation> cachedData = cachedItems.get(cacheKey);
		if (cachedData != null) {
			CacheInformation cachedInformation = cachedData.get();
			if (cachedInformation != null) {
				boolean cachedDataValid = cachedInformation.cachedUntil.after(new Date());
				if (cachedDataValid) {
					return (Account) cachedInformation.cachedData;
				}
			}
			
			cachedItems.remove(cacheKey);
		}
		
		Account result = new Account();
		CacheInformation cacheInformation = new CacheInformation();
		
		// Prepare XML parser
		RootElement root = prepareXmlParser(result, cacheInformation);
		
		AndroidHttpClient httpClient = null;
		HttpEntity entity = null;
		InputStream inputStream = null;
		
		try {
			// Create request
			httpClient = AndroidHttpClient.newInstance(AGENT);
			HttpPost request = new HttpPost(URL_BASE + "/account/APIKeyInfo.xml.aspx");
			
			// Prepare parameters
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("keyID", keyID));
			nameValuePairs.add(new BasicNameValuePair("vCode", vCode));
			request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			// Submit request
			HttpResponse response = httpClient.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Log.e(Api.class.toString(), "API returned with code " + statusCode);
				return null;
			}
			
			entity = response.getEntity();
			inputStream = entity.getContent();
			Xml.parse(inputStream, Encoding.UTF_8, root.getContentHandler());
		} catch (Exception e) {
			result = null;
			Log.e(Api.class.toString(), "Error in API communication", e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// Ignore error while closing, there's nothing we could do
				}
			}
			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					// Ignore error while closing, there's nothing we could do
				}
			}
			if (httpClient != null) {
				httpClient.close();
			}
		}
		
		// Plausibility check
		if (result != null) {
			if (result.accessMask == 0 ||
				result.type == null ||
				result.characters.size() == 0) {
				result = null;
			}
		}
		
		// Cache result
		cacheInformation.cachedData = result;
		cachedItems.put(cacheKey, new SoftReference<CacheInformation>(cacheInformation));
		
		return result;
	}
	
	private RootElement prepareXmlParser(final Account result, final CacheInformation cacheInformation) {
		RootElement root = new RootElement("eveapi");
		root.getChild("currentTime").setEndTextElementListener(new EndTextElementListener() {
			@Override
			public void end(String body) {
				try {
					cacheInformation.currentTime = dateFormatter.parse(body);
				} catch (ParseException e) {
					// Ignore parsing errors
				}
			}
		});
		root.getChild("cachedUntil").setEndTextElementListener(new EndTextElementListener() {
			@Override
			public void end(String body) {
				try {
					cacheInformation.cachedUntil = dateFormatter.parse(body);
				} catch (ParseException e) {
					// Ignore parsing errors
				}
			}
		});
		root.getChild("result").getChild("key").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				try {
					result.accessMask = Long.parseLong(attributes.getValue("accessMask"), 10);
					result.type = attributes.getValue("type");
					String expires = attributes.getValue("expires");
					if (!"".equals(expires)) {
						result.expires = dateFormatter.parse(expires);
					}
				} catch (Exception e) {
					// Ignore any errors
				}
			}
		});
		root.getChild("result").getChild("key").getChild("rowset").getChild("row").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				Character newChar = new Character();
				newChar.characterID = attributes.getValue("characterID");
				newChar.characterName = attributes.getValue("characterName");
				newChar.corporationID = attributes.getValue("corporationID");
				newChar.corporationName = attributes.getValue("corporationName");
				
				result.characters.add(newChar);
			}
		});
		
		return root;
	}

}
