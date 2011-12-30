package de.matdue.isk.database;

public class EveApiCacheTable implements EveApiCacheColumns {
	
public static final String TABLE_NAME = "eveApiCache";
	
	public static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
		KEY + " TEXT," +
		CACHED_UNTIL + " INTEGER" +
		")";
	
	public static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	public static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;

}
