package de.matdue.isk.data;

public class Character {
	
	private long _id;
	
	private long apiId;
	private String characterId;
	private String name;
	private String corporationId;
	private String corporationName;
	private boolean selected;
	
	public long get_id() {
		return _id;
	}
	
	public void set_id(long _id) {
		this._id = _id;
	}
	
	public long getApiId() {
		return apiId;
	}
	
	public void setApiId(long apiId) {
		this.apiId = apiId;
	}
	
	public String getCharacterId() {
		return characterId;
	}
	
	public void setCharacterId(String characterId) {
		this.characterId = characterId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getCorporationId() {
		return corporationId;
	}
	
	public void setCorporationId(String corporationId) {
		this.corporationId = corporationId;
	}
	
	public String getCorporationName() {
		return corporationName;
	}
	
	public void setCorporationName(String corporationName) {
		this.corporationName = corporationName;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}
