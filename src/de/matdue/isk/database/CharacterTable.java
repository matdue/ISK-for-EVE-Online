package de.matdue.isk.database;

public class CharacterTable implements CharacterColumns {
	
	public static final String TABLE_NAME = "character";
	
	public static final String SQL_CREATE =
			"CREATE TABLE " + TABLE_NAME + " (" +
			ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			API_ID + " INTEGER," +
			CHARACTER_ID + " TEXT," +
			NAME + " TEXT," +
			CORPORATION_ID + " TEXT," +
			CORPORATION_NAME + " TEXT," +
			SELECTED + " INTEGER" +
			")";
		
		public static final String SQL_DROP = 
			"DROP TABLE IF EXISTS " + TABLE_NAME;
		
		public static final String STMT_CLEAR = 
			"DELETE FROM " + TABLE_NAME;

}
