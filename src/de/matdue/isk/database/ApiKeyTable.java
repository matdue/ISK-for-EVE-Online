package de.matdue.isk.database;

public class ApiKeyTable implements ApiKeyColumns {
	
	public static final String TABLE_NAME = "apiKey";
	
	public static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
		KEY + " INTEGER," +
		CODE + " TEXT" +
		")";
	
	public static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	public static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;

}
