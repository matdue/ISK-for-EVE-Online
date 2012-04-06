package de.matdue.isk.eve;

import java.math.BigDecimal;
import java.util.Date;

public class WalletTransaction {
	
	private static String IMAGE_BASE = "https://image.eveonline.com/";

	public static String getTypeUrl(String typeID, int resolution) {
		return IMAGE_BASE + "Type/" + typeID + "_" + resolution + ".png";
	}
	
	// No getters and setters to achive better performance
	public Date date;
	public long transactionID;
	public int quantity;
	public String typeName;
	public String typeID;
	public BigDecimal price;
	public String clientName;
	public String stationName;
	public String transactionType;
	public String transactionFor;
	public long journalTransactionID;

}
