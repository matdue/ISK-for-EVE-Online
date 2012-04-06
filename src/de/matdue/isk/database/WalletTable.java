package de.matdue.isk.database;

public class WalletTable implements WalletColumns {
	
public static final String TABLE_NAME = "wallet";
	
	public static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		CHARACTER_ID + " TEXT," +
		DATE + " INTEGER," +
		REFTYPEID + " INTEGER," +
		OWNERNAME1 + " TEXT," +
		OWNERNAME2 + " TEXT," +
		AMOUNT + " TEXT," +
		TAX_AMOUNT + " TEXT," +
		QUANTITY + " INTEGER," +
		TYPE_NAME + " TEXT," +
		TYPE_ID + " TEXT," +
		PRICE + " TEXT," +
		CLIENT_NAME + " TEXT," +
		STATION_NAME + " TEXT," +
		TRANSACTION_TYPE + " TEXT," +
		TRANSACTION_FOR + " TEXT" +
		")";
	
	public static final String IDX_CREATE =
		"CREATE INDEX Idx" + TABLE_NAME + "CharDate ON " + TABLE_NAME + "(" +
		CHARACTER_ID + "," + DATE +
		")"
		;
	
	public static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	public static final String IDX_DROP =
		"DROP INDEX IF EXISTS Idx" + TABLE_NAME + "CharDate";
	
	public static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;

}
