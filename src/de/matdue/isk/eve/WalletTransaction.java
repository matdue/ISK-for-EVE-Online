package de.matdue.isk.eve;

import java.math.BigDecimal;

public class WalletTransaction {
	
	// No getters and setters to achive better performance
	public long transactionID;
	public int quantity;
	public String typeName;
	public BigDecimal price;
	public String clientName;
	public String stationName;
	public String transactionType;
	public String transactionFor;

}
