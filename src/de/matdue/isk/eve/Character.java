package de.matdue.isk.eve;

public class Character {
	
	private static String IMAGE_BASE = "https://image.eveonline.com/";

	public static String getCharacterUrl(int characterId, int resolution) {
		return IMAGE_BASE + "Character/" + characterId + "_" + resolution + ".jpg";
	}
	
}
