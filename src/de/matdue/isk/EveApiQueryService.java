package de.matdue.isk;

import java.util.List;

import de.matdue.isk.data.ApiKey;
import de.matdue.isk.data.Balance;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.eve.AccountBalance;
import de.matdue.isk.eve.EveApi;
import android.app.IntentService;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class EveApiQueryService extends IntentService {
	
	private IskDatabase iskDatabase;

	public EveApiQueryService() {
		super("de.matdue.isk.EveApiQueryService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		iskDatabase = new IskDatabase(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		iskDatabase.close();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String characterId = intent.getStringExtra("characterId");
		
		if (!isNetworkAvailable()) {
			return;
		}
		
		if (characterId == null) {
			queryAll();
		}
		if (characterId != null) {
			queryCharacter(characterId);
			
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(EveApiQueryReceiver.ACTION_RESP);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			broadcastIntent.putExtra("characterId", characterId);
			sendBroadcast(broadcastIntent);
		}
	}
	
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}
	
	private void queryAll() {
		List<de.matdue.isk.data.Character> characters = iskDatabase.queryAllCharacters();
		for (de.matdue.isk.data.Character character : characters) {
			queryCharacter(character.getCharacterId());
		}
	}
	
	private void queryCharacter(String characterId) {
		queryBalance(characterId);
	}
	
	private void queryBalance(String characterId) {
		EveApi eveApi = new EveApi();
		
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

}
