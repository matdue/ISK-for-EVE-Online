package de.matdue.isk.database;

public class BalanceTable implements BalanceColumns {
	
public static final String TABLE_NAME = "balance";
	
	public static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		CHARACTER_ID + " TEXT," +
		BALANCE + " TEXT" +
		")";
	
	public static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	public static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;

}
