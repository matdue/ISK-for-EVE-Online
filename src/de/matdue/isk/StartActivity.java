package de.matdue.isk;

import java.text.Collator;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import de.matdue.isk.data.Balance;
import de.matdue.isk.data.Character;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.ui.BitmapDownloadManager;
import de.matdue.isk.ui.BitmapDownloadTask;
import de.matdue.isk.ui.CacheManager;

public class StartActivity extends Activity {
	
	private EveApiQueryReceiver eveApiQueryReceiver;
	
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
				startActivity(new Intent(StartActivity.this, PilotsActivity.class));
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
		
		WakefulIntentService.scheduleAlarms(new EveApiUpdaterListener(), this, false);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		iskDatabase.close();
		bitmapDownloadManager.shutdown();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(eveApiQueryReceiver);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		updateCurrentCharacter();
		
		IntentFilter filter = new IntentFilter(EveApiQueryReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        eveApiQueryReceiver = new EveApiQueryReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String characterId = intent.getStringExtra("characterId");
				updateCharacter(characterId);
			}
		};
        registerReceiver(eveApiQueryReceiver, filter);
	}
	
	private void updateCharacter(String characterId) {
		new AsyncTask<String, Void, Object[]>() {
			@Override
			protected Object[] doInBackground(String... params) {
				Character character = iskDatabase.queryCharacter(params[0]);
				Balance balance = iskDatabase.queryBalance(params[0]);
				return new Object[] { params[0], character, balance };
			}
			
			protected void onPostExecute(Object[] result) {
				// Parse result
				String characterID = (String) result[0];
				Character character = (Character) result[1];
				Balance balance = (Balance) result[2];
				
				// Character name
				if (character != null) {
					TextView characterNameView = (TextView) findViewById(R.id.start_character_name);
					characterNameView.setText(character.getName());
					characterNameView.setTag(characterID);
				}
				
				// Character portrait
				ImageView imageView = (ImageView) findViewById(R.id.start_character_image);
				String imageUrl = de.matdue.isk.eve.Character.getCharacterUrl(characterID, 128);
				Bitmap bitmap = bitmapMemoryCache.get(imageUrl);
				if (bitmap != null) {
					imageView.setImageBitmap(bitmap);
				} else {
					new BitmapDownloadTask(imageView, cacheManager,	bitmapDownloadManager, bitmapMemoryCache).execute(imageUrl);
				}
				
				// Balance
				if (balance != null) {
					NumberFormat formatter = NumberFormat.getInstance();
					formatter.setMinimumFractionDigits(2);
					formatter.setMaximumFractionDigits(2);
					String sBalance = formatter.format(balance.getBalance()) + " ISK";
					TextView balanceView = (TextView) findViewById(R.id.start_character_balance);
					balanceView.setText(sBalance);
				}
			}
		}.execute(characterId);
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
				editor.putString("startCharacterID", c.getCharacterId());
				editor.apply();
				
				updateCharacter(c.getCharacterId());
				
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
	
	private void updateCurrentCharacter() {
		SharedPreferences preferences = getSharedPreferences("de.matdue.isk", MODE_PRIVATE);
		String characterID = preferences.getString("startCharacterID", null);
		if (characterID != null) {
			updateCharacter(characterID);
		}
	}
	
}
