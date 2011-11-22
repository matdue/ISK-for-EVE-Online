package de.matdue.isk.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IskDatabase extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "isk.db";
	private static final int DATABASE_VERSION = 2;
	
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
	
	public Cursor getCharacterCursor(long apiId) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(CharacterTable.TABLE_NAME,
				new String[] {
				CharacterTable.ID,
				CharacterTable.NAME,
				CharacterTable.SELECTED
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
	
	public boolean dummyRead() {
		boolean hasEntries = false;
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = null;
		try {
			cursor = db.query(ApiKeyTable.TABLE_NAME,
					new String[] {
						ApiKeyTable.ID,
						ApiKeyTable.KEY,
						ApiKeyTable.CODE
					},
					null,  // where
					null,  // where arguments
					null,  // group by
					null,  // having
					null); // order by
			while (cursor.moveToNext()) {
				hasEntries = true;
			}
		} finally {
			cursor.close();
			db.close();
		}
		
		return hasEntries;
	}
	
	public void insertSampleData() {
		SQLiteDatabase db = getWritableDatabase();
		InsertHelper apiKeyInsertHelper = new InsertHelper(db, ApiKeyTable.TABLE_NAME);
		InsertHelper characterInsertHelper = new InsertHelper(db, CharacterTable.TABLE_NAME);
		try {
			db.beginTransaction();
			
			ContentValues values = new ContentValues();
			values.put(ApiKeyColumns.KEY, 19629);
			values.put(ApiKeyColumns.CODE, "leVPxDuOwXauU7xeEwDN85p1Wsgs8YmfQew1VqkFVaenthfemRjOYt4TxDCi9J0t");
			long apiKeyId = apiKeyInsertHelper.insert(values);
			
			values = new ContentValues();
			values.put(CharacterColumns.API_ID, apiKeyId);
			values.put(CharacterColumns.CHARACTER_ID, 90173007);
			values.put(CharacterColumns.NAME, "Quaax");
			values.put(CharacterColumns.CORPORATION_ID, 1373425524);
			values.put(CharacterColumns.CORPORATION_NAME, "GER Weyland Yutani Corp");
			values.put(CharacterColumns.SELECTED, true);
			characterInsertHelper.insert(values);
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			characterInsertHelper.close();
			apiKeyInsertHelper.close();
			db.close();
		}
	}

}
