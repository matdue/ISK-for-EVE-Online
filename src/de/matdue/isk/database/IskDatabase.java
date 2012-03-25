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
import android.util.Log;

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
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "getApiKeyCursor", e);
		}
		
		return null;
	}
	
	public String getApiKeyID(long id) {
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "getApiKeyID", e);
		}
		
		return null;
	}
	
	public Cursor getCharacterCursor(long apiId) {
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "getCharacterCursor", e);
		}
		
		return null;
	}
	
	public void setCharacterSelection(long id, boolean checked) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(CharacterColumns.SELECTED, checked);
			db.update(CharacterTable.TABLE_NAME, 
					values, 
					CharacterColumns.ID + "=?", 
					new String[] { Long.toString(id) });
		} catch (Exception e) {
			Log.e("IskDatabase", "setCharacterSelection", e);
		}
	}
	
	public void deleteApiKey(long id) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			db.delete(CharacterTable.TABLE_NAME,
					CharacterColumns.API_ID + "=?",
					new String[] { Long.toString(id) });
			
			db.delete(ApiKeyTable.TABLE_NAME, 
					ApiKeyColumns.ID + "=?", 
					new String[] { Long.toString(id) });
		} catch (Exception e) {
			Log.e("IskDatabase", "deleteApiKey", e);
		}
	}
	
	public void insertApiKey(ApiKey apiKey, List<de.matdue.isk.data.Character> characters) {
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "insertApiKey", e);
		}
	}

	public List<de.matdue.isk.data.Character> queryAllCharacters() {
		List<de.matdue.isk.data.Character> result = new ArrayList<de.matdue.isk.data.Character>();
		
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(true, 
					CharacterTable.TABLE_NAME, 
					new String[] { CharacterColumns.NAME, CharacterColumns.CHARACTER_ID }, 
					CharacterColumns.SELECTED + "=?", 
					new String[] { "1" }, 
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
		} catch (Exception e) {
			Log.e("IskDatabase", "queryAllCharacters", e);
		}
		
		return result;
	}
	
	public de.matdue.isk.data.Character queryCharacter(String characterId) {
		de.matdue.isk.data.Character result = null;
		
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "queryCharacter", e);
		}
		
		return result;
	}
	
	public Balance queryBalance(String characterId) {
		Balance result = null;
		
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "queryBalance", e);
		}
		
		return result;
	}

	public void storeBalance(Balance balance) {
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "storeBalance", e);
		}
	}
	
	public ApiKey queryApiKey(String characterId) {
		ApiKey result = null;
		
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "queryApiKey", e);
		}
		
		return result;
	}
	
	public boolean isEveApiCacheValid(String key) {
		boolean result = false;
		
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "isEveApiCacheValid", e);
		}
		
		return result;
	}
	
	public void storeEveApiCache(String key, Date cachedUntil) {
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "storeEveApiCache", e);
		}
	}
	
	public void cleanupEveApiHistory() {
		try {
			SQLiteDatabase db = getWritableDatabase();
			
			long aWeekAgo = System.currentTimeMillis() - 7l*24l*60l*60l*1000l;
			db.delete(EveApiHistoryTable.TABLE_NAME, 
					EveApiHistoryColumns.TIMESTAMP + "<?", 
					new String[] { Long.toString(aWeekAgo) });
		} catch (Exception e) {
			Log.e("IskDatabase", "cleanupEveApiHistory", e);
		}
	}
	
	public void storeEveApiHistory(String url, String keyID, String result) {
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "storeEveApiHistory", e);
		}
	}
	
	public Cursor getEveApiHistoryCursor() {
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "getEveApiHistoryCursor", e);
		}
		
		return null;
	}
	
	public void storeEveWallet(String characterId, List<Wallet> wallets) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			InsertHelper insertHelper = new InsertHelper(db, WalletTable.TABLE_NAME);
			try {
				db.beginTransaction();
				
				db.delete(WalletTable.TABLE_NAME, 
						WalletColumns.CHARACTER_ID + "=?", 
						new String[] { characterId });
				
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
				
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
				insertHelper.close();
			}
		} catch (Exception e) {
			Log.e("IskDatabase", "storeEveWallet", e);
		}
	}
	
	public Cursor getEveWallet(String characterId) {
		try {
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
		} catch (Exception e) {
			Log.e("IskDatabase", "getEveWallet", e);
		}
		return null;
	}

}
