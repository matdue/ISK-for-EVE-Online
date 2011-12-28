package de.matdue.isk.data;

import java.math.BigDecimal;

public class Balance {
	
	private String characterId;
	private BigDecimal balance;

	public String getCharacterId() {
		return characterId;
	}

	public void setCharacterId(String characterId) {
		this.characterId = characterId;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

}
