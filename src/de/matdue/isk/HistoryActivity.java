package de.matdue.isk;

import java.text.DateFormat;
import java.util.Date;

import de.matdue.isk.database.IskDatabase;
import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class HistoryActivity extends ListActivity {
	
	private IskDatabase iskDatabase;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.history);
		
		iskDatabase = new IskDatabase(this);
		Cursor historyCursor = iskDatabase.getEveApiHistoryCursor();
		startManagingCursor(historyCursor);
		
		ListAdapter adapter = new HistoryAdapter(this, 
				R.layout.history_entry, 
				historyCursor);
		setListAdapter(adapter);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		iskDatabase.close();
	}
	
	class HistoryAdapter extends ResourceCursorAdapter {
		
		private DateFormat dateFormatter;

		public HistoryAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);
			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// View holder pattern: http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
			// Cache elements instead of looking up using slow findViewById() 
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			if (viewHolder == null) {
				viewHolder = new ViewHolder();
				viewHolder.datetime = (TextView) view.findViewById(R.id.history_entry_datetime);
				viewHolder.url = (TextView) view.findViewById(R.id.history_entry_url);
				viewHolder.key_id = (TextView) view.findViewById(R.id.history_entry_key_id);
				viewHolder.result = (TextView) view.findViewById(R.id.history_entry_result);
				view.setTag(viewHolder);
			}
			
			// Date and time
			viewHolder.datetime.setText(dateFormatter.format(new Date(cursor.getLong(1))));
			
			// URL; display path only
			Uri uri = Uri.parse(cursor.getString(2));
			viewHolder.url.setText(uri.getPath());
			
			// Character
			viewHolder.key_id.setText(cursor.getString(3));
			
			// Result
			viewHolder.result.setText(cursor.getString(4));
		}
		
		class ViewHolder {
			TextView datetime;
			TextView url;
			TextView key_id;
			TextView result;
		}
		
	}

}
