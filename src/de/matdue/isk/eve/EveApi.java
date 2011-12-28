package de.matdue.isk.eve;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.math.BigDecimal;
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
import org.xml.sax.ContentHandler;

import android.net.http.AndroidHttpClient;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import android.util.Xml.Encoding;

public class EveApi {
	
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String AGENT = "Android de.matdue.isk";
	private static final String URL_BASE = "https://api.eveonline.com";

	private static final HashMap<String, SoftReference<CacheInformation>> cachedItems = new HashMap<String, SoftReference<CacheInformation>>();
	
	static {
		// EVE Online API always uses GMT
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private static Object lookupInCache(String cacheKey) {
		SoftReference<CacheInformation> cachedData = cachedItems.get(cacheKey);
		if (cachedData != null) {
			CacheInformation cachedInformation = cachedData.get();
			if (cachedInformation != null) {
				boolean cachedDataValid = cachedInformation.cachedUntil.after(new Date());
				if (cachedDataValid) {
					Log.d("Api", "Cache hit " + cacheKey);
					return cachedInformation.cachedData;
				}
			}
			
			cachedItems.remove(cacheKey);
		}
		
		return null;
	}
	
	private boolean queryApi(ContentHandler xmlParser, String url, String keyID, String vCode) {
		return queryApi(xmlParser, url, keyID, vCode, null);
	}
	
	private boolean queryApi(ContentHandler xmlParser, String url, String keyID, String vCode, String characterID) {
		AndroidHttpClient httpClient = null;
		HttpEntity entity = null;
		InputStream inputStream = null;
		
		try {
			// Create request
			httpClient = AndroidHttpClient.newInstance(AGENT);
			HttpPost request = new HttpPost(URL_BASE + url);
			
			// Prepare parameters
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("keyID", keyID));
			nameValuePairs.add(new BasicNameValuePair("vCode", vCode));
			if (characterID != null) {
				nameValuePairs.add(new BasicNameValuePair("characterID", characterID));
			}
			request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			// Submit request
			HttpResponse response = httpClient.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Log.e(EveApi.class.toString(), "API returned with code " + statusCode);
				return false;
			}
			
			entity = response.getEntity();
			inputStream = entity.getContent();
			Xml.parse(inputStream, Encoding.UTF_8, xmlParser);
		} catch (Exception e) {
			Log.e(EveApi.class.toString(), "Error in API communication", e);
			return false;
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
		
		return true;
	}
	
	public Account validateKey(String keyID, String vCode) {
		final String URL = "/account/APIKeyInfo.xml.aspx";
		
		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey(URL, keyID, vCode);
		Account result = (Account) lookupInCache(cacheKey);
		if (result != null) {
			return result;
		}
		
		result = new Account();
		CacheInformation cacheInformation = new CacheInformation();
		
		// Prepare XML parser
		RootElement root = prepareAPIKeyInfoXmlParser(result, cacheInformation);
		
		// Query API
		if (!queryApi(root.getContentHandler(), URL, keyID, vCode)) {
			return null;
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
	
	private void prepareCacheInformationXmlParser(RootElement root, final CacheInformation cacheInformation) {
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
	}
	
	private RootElement prepareAPIKeyInfoXmlParser(final Account result, final CacheInformation cacheInformation) {
		RootElement root = new RootElement("eveapi");
		prepareCacheInformationXmlParser(root, cacheInformation);
		
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
	
	public AccountBalance queryAccountBalance(String keyID, String vCode, String characterID) {
		final String URL = "/char/AccountBalance.xml.aspx";
		
		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey(URL, keyID, vCode, characterID);
		AccountBalance result = (AccountBalance) lookupInCache(cacheKey);
		if (result != null) {
			return result;
		}
		
		result = new AccountBalance();
		CacheInformation cacheInformation = new CacheInformation();
		
		// Prepare XML parser
		RootElement root = prepareAccountBalanceXmlParser(result, cacheInformation);
		
		// Query API
		if (!queryApi(root.getContentHandler(), URL, keyID, vCode, characterID)) {
			return null;
		}
		
		// Plausibility check
		if (result != null) {
			if (result.accountID == null || result.accountKey == null) {
				result = null;
			}
		}
		
		// Cache result
		cacheInformation.cachedData = result;
		cachedItems.put(cacheKey, new SoftReference<CacheInformation>(cacheInformation));
		
		return result;
	}
	
	private RootElement prepareAccountBalanceXmlParser(final AccountBalance result, final CacheInformation cacheInformation) {
		RootElement root = new RootElement("eveapi");
		prepareCacheInformationXmlParser(root, cacheInformation);
		
		root.getChild("result").getChild("rowset").getChild("row").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				result.accountID = attributes.getValue("accountID");
				result.accountKey = attributes.getValue("accountKey");
				try {
					result.balance = new BigDecimal(attributes.getValue("balance"));
				} catch (NumberFormatException e) {
					// Ignore error, leave balance as 0.0
				}
			}
		});
		
		return root;
	}

}
