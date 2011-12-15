package de.matdue.isk.data;

public class ApiKey {
	
	private long _id;
	
	private String key;
	private String code;

	
	public long get_id() {
		return _id;
	}
	
	public void set_id(long _id) {
		this._id = _id;
	}
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
}
