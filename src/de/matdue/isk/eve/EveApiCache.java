package de.matdue.isk.eve;

public interface EveApiCache {
	
	boolean isCached(String key);
	void cache(String key, CacheInformation cacheInformation);

}
