package de.matdue.isk.eve;

public class Character {
	
	private static String IMAGE_BASE = "https://image.eveonline.com/";

	public static String getCharacterUrl(int characterId, int resolution) {
		return IMAGE_BASE + "Character/" + characterId + "_" + resolution + ".jpg";
	}
	
	// No getters and setters to achive better performance
	public String characterID;
	public String characterName;
	public String corporationID;
	public String corporationName;
	
}
