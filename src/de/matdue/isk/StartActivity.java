package de.matdue.isk;

import java.text.Collator;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import de.matdue.isk.R;
import de.matdue.isk.data.Character;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.eve.AccountBalance;
import de.matdue.isk.eve.Api;
import de.matdue.isk.ui.BitmapDownloadManager;
import de.matdue.isk.ui.BitmapDownloadTask;
import de.matdue.isk.ui.CacheManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class StartActivity extends Activity {
	
	private IskDatabase iskDatabase;
	
	private BitmapDownloadManager bitmapDownloadManager;
	private CacheManager cacheManager;
	private HashMap<String, Bitmap> bitmapMemoryCache;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.start);
		
		cacheManager = new CacheManager(getCacheDir());
		cacheManager.cleanup();  // TODO: not that often...
		bitmapMemoryCache = new HashMap<String, Bitmap>();
		bitmapDownloadManager = new BitmapDownloadManager(cacheManager);
		
		iskDatabase = new IskDatabase(this);

		Button pilotsButton = (Button) findViewById(R.id.pilots);
		pilotsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(StartActivity.this,
						PilotsActivity.class));
			}
		});
		
		View balanceLayout = findViewById(R.id.start_balance_layout);
		balanceLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences preferences = getSharedPreferences("de.matdue.isk", MODE_PRIVATE);
				String characterID = preferences.getString("startCharacterID", null);
				showChooseCharacterDialog(characterID);
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		iskDatabase.close();
		bitmapDownloadManager.shutdown();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		displayBalance();
	}
	
	private void showChooseCharacterDialog(String currentCharacterID) {
		final List<Character> characters = iskDatabase.queryAllCharacters();
		if (characters.isEmpty()) {
			return;
		}
		
		// Sort by character name
		final Collator collator = Collator.getInstance();
		Collections.sort(characters, new Comparator<Character>() {
			@Override
			public int compare(Character lhs, Character rhs) {
				return collator.compare(lhs.getName(), rhs.getName());
			}
		});
		
		// Convert into array for dialog
		String[] characterNames = new String[characters.size()];
		for (int i = 0; i < characterNames.length; ++i) {
			characterNames[i] = characters.get(i).getName();
		}
		
		// Which one is the current character?
		int currentCharIdx = -1;
			if (currentCharacterID != null) {
			for (Character character : characters) {
				if (currentCharacterID.equals(character.getCharacterId())) {
					currentCharIdx = characters.indexOf(character);
					break;
				}
			}
		}
		
		// Create dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setSingleChoiceItems(characterNames, currentCharIdx, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Character c = characters.get(which);
				SharedPreferences preferences = getSharedPreferences("de.matdue.isk", MODE_PRIVATE);
				Editor editor = preferences.edit();
				editor.putString("startCharacterName", c.getName());
				editor.putString("startCharacterID", c.getCharacterId());
				editor.apply();
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
	
	private void displayBalance() {
		SharedPreferences preferences = getSharedPreferences("de.matdue.isk", MODE_PRIVATE);
		String characterName = preferences.getString("startCharacterName", null);
		String characterID = preferences.getString("startCharacterID", null);
		
		// Predisplay character name
		if (characterName != null) {
			TextView characterNameView = (TextView) findViewById(R.id.start_character_name);
			characterNameView.setText(characterName);
		}
		
		// Predisplay character portrait
		if (characterID != null) {
			ImageView imageView = (ImageView) findViewById(R.id.start_character_image);
			String imageUrl = de.matdue.isk.eve.Character.getCharacterUrl(characterID, 128);
			Bitmap bitmap = bitmapMemoryCache.get(imageUrl);
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);
			} else {
				new BitmapDownloadTask(imageView, cacheManager,	bitmapDownloadManager, bitmapMemoryCache).execute(imageUrl);
			}
		}
		
		// Start task to fetch current balance and display it
		if (characterID != null) {
			setProgressBarIndeterminateVisibility(true);
			new AsyncTask<String, Void, AccountBalance>() {
				private String[] characterData;  // Will contain information about character after doInBackground()
				
				@Override
				protected AccountBalance doInBackground(String... params) {
					// Load current character
					characterData = iskDatabase.queryCharacter(params[0]);
					if (characterData != null) {
						// Query balance
						Api eveApi = new Api();
						AccountBalance accountBalance = eveApi.queryAccountBalance(characterData[2], characterData[3], characterData[0]);
						return accountBalance;
					}
					
					return null;
				}
				
				protected void onPostExecute(AccountBalance result) {
					setProgressBarIndeterminateVisibility(false);
					if (result != null) {
						// Display character name
						TextView characterNameView = (TextView) findViewById(R.id.start_character_name);
						characterNameView.setText(characterData[1]);
						
						// Format and display balance
						NumberFormat formatter = NumberFormat.getInstance();
						formatter.setMinimumFractionDigits(2);
						formatter.setMaximumFractionDigits(2);
						String sBalance = formatter.format(result.balance) + " ISK";
						TextView balanceView = (TextView) findViewById(R.id.start_character_balance);
						balanceView.setText(sBalance);
						
						// Load character picture
						ImageView imageView = (ImageView) findViewById(R.id.start_character_image);
						String imageUrl = de.matdue.isk.eve.Character.getCharacterUrl(characterData[0], 128);
						Bitmap bitmap = bitmapMemoryCache.get(imageUrl);
						if (bitmap != null) {
							imageView.setImageBitmap(bitmap);
						} else {
							new BitmapDownloadTask(imageView, cacheManager,	bitmapDownloadManager, bitmapMemoryCache).execute(imageUrl);
						}
					}
				}
			}.execute(characterID);
		}
	}
	
}
