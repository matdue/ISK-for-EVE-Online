package de.matdue.isk.database;

public class EveApiHistoryTable implements EveApiHistoryColumns {

	public static final String TABLE_NAME = "eveApiHistory";
	
	public static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		URL + " TEXT," +
		TIMESTAMP + " INTEGER," +
		RESULT + " TEXT" +
		")";
	
	public static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	public static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;
	
}
