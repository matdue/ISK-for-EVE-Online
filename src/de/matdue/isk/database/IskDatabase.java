package de.matdue.isk.database;

import java.util.ArrayList;
import java.util.List;

import de.matdue.isk.data.ApiKey;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IskDatabase extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "isk.db";
	private static final int DATABASE_VERSION = 3;
	
	public IskDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(ApiKeyTable.SQL_CREATE);
		db.execSQL(CharacterTable.SQL_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(CharacterTable.SQL_DROP);
		db.execSQL(ApiKeyTable.SQL_DROP);
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
			db.close();
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
		try {
			ContentValues values = new ContentValues();
			values.put(CharacterColumns.SELECTED, checked);
			db.update(CharacterTable.TABLE_NAME, 
					values, 
					CharacterColumns.ID + "=?", 
					new String[] { Long.toString(id) });
		} finally {
			db.close();
		}
	}
	
	public void deleteApiKey(long id) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.delete(CharacterTable.TABLE_NAME,
					CharacterColumns.API_ID + "=?",
					new String[] { Long.toString(id) });
			
			db.delete(ApiKeyTable.TABLE_NAME, 
					ApiKeyColumns.ID + "=?", 
					new String[] { Long.toString(id) });
		} finally {
			db.close();
		}
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
			db.close();
		}
	}

	public String[] queryCharacter(String characterID) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = null;
		try {
			// Lookup character
			cursor = db.query(CharacterTable.TABLE_NAME, 
					new String[] { CharacterColumns.NAME, CharacterColumns.API_ID },
					CharacterColumns.CHARACTER_ID + "=?", 
					new String[] { characterID }, 
					null,   // groupBy
					null,   // having
					null);  // orderBy
			if (cursor.moveToNext()) {
				String characterName = cursor.getString(0);
				String apiID = cursor.getString(1);
				cursor.close();
				
				// Lookup corresponding API login
				cursor = db.query(ApiKeyTable.TABLE_NAME, 
						new String[] { ApiKeyColumns.KEY, ApiKeyColumns.CODE }, 
						ApiKeyColumns.ID + "=?",      // where
						new String[] { apiID },  // where arguments
						null,  // group by
						null,  // having
						null); // order by
				if (cursor.moveToNext()) {
					String keyID = cursor.getString(0);
					String vCode = cursor.getString(1);
					
					return new String[] { characterID, characterName, keyID, vCode };
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			db.close();
		}
		
		return null;
	}
	
	public List<de.matdue.isk.data.Character> queryAllCharacters() {
		List<de.matdue.isk.data.Character> result = new ArrayList<de.matdue.isk.data.Character>();
		SQLiteDatabase db = getReadableDatabase();
		try {
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
		} finally {
			db.close();
		}
		
		return result;
	}
	
}
