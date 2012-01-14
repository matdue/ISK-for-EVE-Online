package de.matdue.isk.database;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.matdue.isk.data.ApiKey;
import de.matdue.isk.data.Balance;
import de.matdue.isk.data.Wallet;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IskDatabase extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "isk.db";
	private static final int DATABASE_VERSION = 9;
	
	public IskDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(EveApiCacheTable.SQL_CREATE);
		db.execSQL(EveApiHistoryTable.SQL_CREATE);
		db.execSQL(ApiKeyTable.SQL_CREATE);
		db.execSQL(CharacterTable.SQL_CREATE);
		db.execSQL(BalanceTable.SQL_CREATE);
		db.execSQL(WalletTable.SQL_CREATE);
		db.execSQL(WalletTable.IDX_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(WalletTable.IDX_DROP);
		db.execSQL(WalletTable.SQL_DROP);
		db.execSQL(BalanceTable.SQL_DROP);
		db.execSQL(CharacterTable.SQL_DROP);
		db.execSQL(ApiKeyTable.SQL_DROP);
		db.execSQL(EveApiHistoryTable.SQL_DROP);
		db.execSQL(EveApiCacheTable.SQL_DROP);
		onCreate(db);
	}
	
	public Cursor getApiKeyCursor() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(ApiKeyTable.TABLE_NAME,
				new String[] {
				ApiKeyTable.ID,
				ApiKeyTable.KEY
			},
			null,  // where
			null,  // where arguments
			null,  // group by
			null,  // having
			null); // order by
		
		return cursor;
	}
	
	public String getApiKeyID(long id) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = null;
		try {
			cursor = db.query(ApiKeyTable.TABLE_NAME, 
					new String[] { ApiKeyColumns.KEY }, 
					ApiKeyColumns.ID + "=?",      // where
					new String[] { Long.toString(id) },  // where arguments
					null,  // group by
					null,  // having
					null); // order by
			if (cursor.moveToNext()) {
				return cursor.getString(0);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		return null;
	}
	
	public Cursor getCharacterCursor(long apiId) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(CharacterTable.TABLE_NAME,
				new String[] {
				CharacterTable.ID,
				CharacterTable.NAME,
				CharacterTable.SELECTED,
				CharacterTable.CHARACTER_ID
			},
			CharacterTable.API_ID + "=?",  // where
			new String[] { 
				Long.toString(apiId) 
			},  // where arguments
			null,  // group by
			null,  // having
			CharacterTable.NAME); // order by
		
		return cursor;
	}
	
	public void setCharacterSelection(long id, boolean checked) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(CharacterColumns.SELECTED, checked);
		db.update(CharacterTable.TABLE_NAME, 
				values, 
				CharacterColumns.ID + "=?", 
				new String[] { Long.toString(id) });
	}
	
	public void deleteApiKey(long id) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(CharacterTable.TABLE_NAME,
				CharacterColumns.API_ID + "=?",
				new String[] { Long.toString(id) });
		
		db.delete(ApiKeyTable.TABLE_NAME, 
				ApiKeyColumns.ID + "=?", 
				new String[] { Long.toString(id) });
	}
	
	public void insertApiKey(ApiKey apiKey, List<de.matdue.isk.data.Character> characters) {
		SQLiteDatabase db = getWritableDatabase();
		InsertHelper apiKeyInsertHelper = new InsertHelper(db, ApiKeyTable.TABLE_NAME);
		InsertHelper characterInsertHelper = new InsertHelper(db, CharacterTable.TABLE_NAME);
		try {
			db.beginTransaction();
			
			ContentValues values = new ContentValues();
			values.put(ApiKeyColumns.KEY, apiKey.getKey());
			values.put(ApiKeyColumns.CODE, apiKey.getCode());
			long apiKeyId = apiKeyInsertHelper.insert(values);
			
			for (de.matdue.isk.data.Character character : characters) {
				values = new ContentValues();
				values.put(CharacterColumns.API_ID, apiKeyId);
				values.put(CharacterColumns.CHARACTER_ID, character.getCharacterId());
				values.put(CharacterColumns.NAME, character.getName());
				values.put(CharacterColumns.CORPORATION_ID, character.getCorporationId());
				values.put(CharacterColumns.CORPORATION_NAME, character.getCorporationName());
				values.put(CharacterColumns.SELECTED, character.isSelected());
				characterInsertHelper.insert(values);
			}
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			characterInsertHelper.close();
			apiKeyInsertHelper.close();
		}
	}

	public List<de.matdue.isk.data.Character> queryAllCharacters() {
		List<de.matdue.isk.data.Character> result = new ArrayList<de.matdue.isk.data.Character>();
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(true, 
				CharacterTable.TABLE_NAME, 
				new String[] { CharacterColumns.NAME, CharacterColumns.CHARACTER_ID }, 
				null, 
				null, 
				null, 
				null, 
				null, 
				null);
		while (cursor.moveToNext()) {
			de.matdue.isk.data.Character character = new de.matdue.isk.data.Character();
			character.setName(cursor.getString(0));
			character.setCharacterId(cursor.getString(1));
			
			result.add(character);
		}
		cursor.close();
		
		return result;
	}
	
	public de.matdue.isk.data.Character queryCharacter(String characterId) {
		de.matdue.isk.data.Character result = null;
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(CharacterTable.TABLE_NAME, 
				new String[] { CharacterColumns.NAME }, 
				CharacterColumns.CHARACTER_ID + "=?", 
				new String[] { characterId }, 
				null, 
				null, 
				null, 
				null);
		if (cursor.moveToNext()) {
			result = new de.matdue.isk.data.Character();
			result.setCharacterId(characterId);
			result.setName(cursor.getString(0));
		}
		cursor.close();
		
		return result;
	}
	
	public Balance queryBalance(String characterId) {
		Balance result = null;
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(BalanceTable.TABLE_NAME, 
				new String[] { BalanceColumns.BALANCE }, 
				BalanceColumns.CHARACTER_ID + "=?", 
				new String[] { characterId }, 
				null, 
				null, 
				null, 
				null);
		if (cursor.moveToNext()) {
			result = new Balance();
			result.setCharacterId(characterId);
			result.setBalance(new BigDecimal(cursor.getString(0)));
		}
		cursor.close();
		
		return result;
	}

	public void storeBalance(Balance balance) {
		SQLiteDatabase db = getWritableDatabase();
		InsertHelper balanceInsertHelper = new InsertHelper(db, BalanceTable.TABLE_NAME);
		try {
			db.beginTransaction();
			
			db.delete(BalanceTable.TABLE_NAME, 
					BalanceColumns.CHARACTER_ID + "=?", 
					new String[] { balance.getCharacterId() });
			
			ContentValues values = new ContentValues();
			values.put(BalanceColumns.CHARACTER_ID, balance.getCharacterId());
			values.put(BalanceColumns.BALANCE, balance.getBalance().toString());
			balanceInsertHelper.insert(values);
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			balanceInsertHelper.close();
		}
	}
	
	public ApiKey queryApiKey(String characterId) {
		ApiKey result = null;
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(ApiKeyTable.TABLE_NAME + " INNER JOIN " + CharacterTable.TABLE_NAME + " ON " + ApiKeyTable.TABLE_NAME + "." + ApiKeyColumns.ID + " = " + CharacterTable.TABLE_NAME + "." + CharacterColumns.API_ID, 
				new String[] { ApiKeyTable.TABLE_NAME + "." + ApiKeyColumns.KEY, ApiKeyTable.TABLE_NAME + "." + ApiKeyColumns.CODE }, 
				CharacterTable.TABLE_NAME + "." + CharacterColumns.CHARACTER_ID + "=?", 
				new String [] { characterId }, 
				null, 
				null, 
				null);
		if (cursor.moveToNext()) {
			result = new ApiKey();
			result.setKey(cursor.getString(0));
			result.setCode(cursor.getString(1));
		}
		cursor.close();
		
		return result;
	}
	
	public boolean isEveApiCacheValid(String key) {
		boolean result = false;
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(EveApiCacheTable.TABLE_NAME, 
				new String[] { EveApiCacheColumns.CACHED_UNTIL }, 
				EveApiCacheColumns.KEY + "=?", 
				new String[] { key }, 
				null, 
				null, 
				null);
		if (cursor.moveToNext()) {
			long cachedUntil = cursor.getLong(0);
			result = cachedUntil > System.currentTimeMillis();
		}
		cursor.close();
		
		return result;
	}
	
	public void storeEveApiCache(String key, Date cachedUntil) {
		SQLiteDatabase db = getWritableDatabase();
		InsertHelper insertHelper = new InsertHelper(db, EveApiCacheTable.TABLE_NAME);
		try {
			db.beginTransaction();
			
			db.delete(EveApiCacheTable.TABLE_NAME, 
					EveApiCacheColumns.KEY + "=?", 
					new String[] { key });
			
			ContentValues values = new ContentValues();
			values.put(EveApiCacheColumns.KEY, key);
			values.put(EveApiCacheColumns.CACHED_UNTIL, cachedUntil.getTime());
			insertHelper.insert(values);
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			insertHelper.close();
		}
	}
	
	public void cleanupEveApiHistory() {
		SQLiteDatabase db = getWritableDatabase();
		
		long aWeekAgo = System.currentTimeMillis() - 7l*24l*60l*60l*1000l;
		db.delete(EveApiHistoryTable.TABLE_NAME, 
				EveApiHistoryColumns.TIMESTAMP + "<?", 
				new String[] { Long.toString(aWeekAgo) });
	}
	
	public void storeEveApiHistory(String url, String keyID, String result) {
		SQLiteDatabase db = getWritableDatabase();
		InsertHelper insertHelper = new InsertHelper(db, EveApiHistoryTable.TABLE_NAME);
		try {
			ContentValues values = new ContentValues();
			values.put(EveApiHistoryColumns.URL, url);
			values.put(EveApiHistoryColumns.KEY_ID, keyID);
			values.put(EveApiHistoryColumns.RESULT, result);
			values.put(EveApiHistoryColumns.TIMESTAMP, System.currentTimeMillis());
			insertHelper.insert(values);
		} finally {
			insertHelper.close();
		}
	}
	
	public Cursor getEveApiHistoryCursor() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(EveApiHistoryTable.TABLE_NAME,
				new String[] {
				"rowid _id",
				EveApiHistoryColumns.TIMESTAMP,
				EveApiHistoryColumns.URL,
				EveApiHistoryColumns.KEY_ID,
				EveApiHistoryColumns.RESULT
			},
			null,  // where
			null,  // where arguments
			null,  // group by
			null,  // having
			EveApiHistoryColumns.TIMESTAMP + " desc"); // order by
		
		return cursor;
	}
	
	public void storeEveWallet(String characterId, List<Wallet> wallets) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(WalletTable.TABLE_NAME, 
				WalletColumns.CHARACTER_ID + "=?", 
				new String[] { characterId });
		
		InsertHelper insertHelper = new InsertHelper(db, WalletTable.TABLE_NAME);
		try {
			for (Wallet wallet : wallets) {
				ContentValues values = new ContentValues();
				values.put(WalletColumns.CHARACTER_ID, characterId);
				values.put(WalletColumns.DATE, wallet.date.getTime());
				values.put(WalletColumns.REFTYPEID, wallet.refTypeID);
				values.put(WalletColumns.OWNERNAME1, wallet.ownerName1);
				values.put(WalletColumns.OWNERNAME2, wallet.ownerName2);
				values.put(WalletColumns.AMOUNT, wallet.amount.toString());
				values.put(WalletColumns.TAX_AMOUNT, wallet.taxAmount.toString());
				values.put(WalletColumns.QUANTITY, wallet.quantity);
				values.put(WalletColumns.TYPE_NAME, wallet.typeName);
				values.put(WalletColumns.PRICE, wallet.price != null ? wallet.price.toString() : null);
				values.put(WalletColumns.CLIENT_NAME, wallet.clientName);
				values.put(WalletColumns.STATION_NAME, wallet.stationName);
				values.put(WalletColumns.TRANSACTION_TYPE, wallet.transactionType);
				values.put(WalletColumns.TRANSACTION_FOR, wallet.transactionFor);
				insertHelper.insert(values);
			}
		} finally {
			insertHelper.close();
		}
	}
	
	public Cursor getEveWallet(String characterId) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(WalletTable.TABLE_NAME,
				new String[] {
				"rowid _id",
				WalletColumns.DATE,
				WalletColumns.REFTYPEID,
				WalletColumns.OWNERNAME1,
				WalletColumns.OWNERNAME2,
				WalletColumns.OWNERNAME2,
				WalletColumns.AMOUNT,
				WalletColumns.TAX_AMOUNT,
				WalletColumns.QUANTITY,
				WalletColumns.TYPE_NAME,
				WalletColumns.PRICE,
				WalletColumns.CLIENT_NAME,
				WalletColumns.STATION_NAME,
				WalletColumns.TRANSACTION_TYPE,
				WalletColumns.TRANSACTION_FOR
			},
			WalletColumns.CHARACTER_ID + "=?",  // where
			new String[] { characterId },  // where arguments
			null,  // group by
			null,  // having
			WalletColumns.DATE + " desc"); // order by
		
		return cursor;
	}

}
