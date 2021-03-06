package de.matdue.isk;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import de.matdue.isk.data.ApiKey;
import de.matdue.isk.data.Balance;
import de.matdue.isk.data.Wallet;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.eve.AccountBalance;
import de.matdue.isk.eve.CacheInformation;
import de.matdue.isk.eve.EveApi;
import de.matdue.isk.eve.EveApiCache;
import de.matdue.isk.eve.WalletJournal;

public class EveApiUpdaterService extends WakefulIntentService {
	
	public static final String ACTION_RESP = "de.matdue.isk.EVE_API_UPDATER_FINISHED";
	
	private IskDatabase iskDatabase;
	private EveApi eveApi;

	public EveApiUpdaterService() {
		super("de.matdue.isk.EveApiUpdaterService");
	}
	
	@Override
	protected void doWakefulWork(Intent intent) {
		Log.d("EveApiUpdaterService", "Performing update");
		
		// Skip update if no network is available
		if (!isNetworkAvailable()) {
			sendEmptyResponseIntent();
			return;
		}
		
		boolean forcedUpdate = intent.getBooleanExtra("force", false);

		if (!forcedUpdate) {
			// Skip update if WIFI inactive, but required
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			boolean requireWifi = sharedPreferences.getBoolean("requireWifi", false);
			if (requireWifi && !isWifiConnected()) {
				sendEmptyResponseIntent();
				return;
			}
			
			// Skip update if global sync is switched off
			boolean honorGlobalSync = sharedPreferences.getBoolean("honorGlobalSync", true);
			if (honorGlobalSync && !ContentResolver.getMasterSyncAutomatically()) {
				sendEmptyResponseIntent();
				return;
			}
		}
		
		try {
			iskDatabase = new IskDatabase(this);
			eveApi = new EveApi(new EveApiCacheDatabase(forcedUpdate));
			
			// If 'characterId' is given, update that specific character only
			// else update all characters
			String characterId = intent.getStringExtra("characterId");
			if (characterId != null) {
				updateCharacter(characterId);
			} else {
				updateAllCharacters();
			}
		} catch (Exception e) {
			Log.e("EveApiUpdaterService",  "Error while performing update", e);
			sendEmptyResponseIntent();
		} finally {
			iskDatabase.close();
		}
	}
	
	private void sendEmptyResponseIntent() {
		sendBroadcast(new Intent(ACTION_RESP).addCategory(Intent.CATEGORY_DEFAULT));
	}
	
	/**
	 * Network detection
	 */
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}
	
	/**
	 * WIFI detection
	 */
	private boolean isWifiConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnectedOrConnecting();
	}

	/**
	 * Update all characters
	 */
	private void updateAllCharacters() {
		List<de.matdue.isk.data.Character> characters = iskDatabase.queryAllCharacters();
		for (de.matdue.isk.data.Character character : characters) {
			updateCharacter(character.getCharacterId());
		}
		iskDatabase.cleanupEveApiHistory();
	}
	
	/**
	 * Update a single character by querying Eve Online API and storing all data in database.
	 * 
	 * @param characterId Character's EVE id
	 */
	private void updateCharacter(String characterId) {
		updateBalance(characterId);
		updateWallet(characterId);
		
		// Inform listeners about updated character
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ACTION_RESP);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra("characterId", characterId);
		sendBroadcast(broadcastIntent);
	}
	
	/**
	 * Update a character's balance.
	 * 
	 * @param characterId Character's EVE id
	 */
	private void updateBalance(String characterId) {
		ApiKey apiKey = iskDatabase.queryApiKey(characterId);
		if (apiKey != null) {
			AccountBalance accountBalance = eveApi.queryAccountBalance(apiKey.getKey(), apiKey.getCode(), characterId);
			if (accountBalance != null) {
				Balance balance = new Balance();
				balance.setBalance(accountBalance.balance);
				balance.setCharacterId(characterId);
				iskDatabase.storeBalance(balance);
			}
		}
	}
	
	private void updateWallet(String characterId) {
		ApiKey apiKey = iskDatabase.queryApiKey(characterId);
		if (apiKey != null) {
			List<WalletJournal> wallet = eveApi.queryWallet(apiKey.getKey(), apiKey.getCode(), characterId);
			if (wallet != null) {
				// Prepare data objects
				ArrayList<Wallet> walletDatas = new ArrayList<Wallet>();
				for (WalletJournal walletJournalEntry : wallet) {
					Wallet walletData = new Wallet();
					walletData.date = walletJournalEntry.date;
					walletData.refTypeID = walletJournalEntry.refTypeID;
					walletData.ownerName1 = walletJournalEntry.ownerName1;
					walletData.ownerName2 = walletJournalEntry.ownerName2;
					walletData.amount = walletJournalEntry.amount;
					walletData.taxAmount = walletJournalEntry.taxAmount;
					
					if (walletJournalEntry.transaction != null) {
						walletData.quantity = walletJournalEntry.transaction.quantity;
						walletData.typeName = walletJournalEntry.transaction.typeName;
						walletData.typeID = walletJournalEntry.transaction.typeID;
						walletData.price = walletJournalEntry.transaction.price;
						walletData.clientName = walletJournalEntry.transaction.clientName;
						walletData.stationName = walletJournalEntry.transaction.stationName;
						walletData.transactionType = walletJournalEntry.transaction.transactionType;
						walletData.transactionFor = walletJournalEntry.transaction.transactionFor;
					}
					
					walletDatas.add(walletData);
				}
				iskDatabase.storeEveWallet(characterId, walletDatas);
			}
		}
	}
	
	private class EveApiCacheDatabase implements EveApiCache {
		
		private boolean forcedUpdate;
		
		public EveApiCacheDatabase(boolean forcedUpdate) {
			this.forcedUpdate = forcedUpdate;
		}

		@Override
		public boolean isCached(String key) {
			if (forcedUpdate) {
				return false;
			} else {
				return iskDatabase.isEveApiCacheValid(key);
			}
		}

		@Override
		public void cache(String key, CacheInformation cacheInformation) {
			iskDatabase.storeEveApiCache(key, cacheInformation.cachedUntil);
		}

		@Override
		public void urlAccessed(String url, String keyID, String result) {
			iskDatabase.storeEveApiHistory(url, keyID, result);
		}
		
	}
	
}
