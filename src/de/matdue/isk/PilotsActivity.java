package de.matdue.isk;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import de.matdue.isk.database.ApiKeyColumns;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.ui.BitmapDownloadManager;
import de.matdue.isk.ui.BitmapDownloadTask;
import de.matdue.isk.ui.CacheManager;

public class PilotsActivity extends ExpandableListActivity {

	private IskDatabase iskDatabase;

	private BitmapDownloadManager bitmapDownloadManager;
	private CacheManager cacheManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bitmapDownloadManager = new BitmapDownloadManager(cacheManager);
		cacheManager = new CacheManager(getCacheDir());
		cacheManager.cleanup();  // TODO: not that often...

		iskDatabase = new IskDatabase(this);
		Cursor apiKeyCursor = iskDatabase.getApiKeyCursor();
		startManagingCursor(apiKeyCursor);

		PilotsExpandableListAdapter aaa = new PilotsExpandableListAdapter(apiKeyCursor,
				this, android.R.layout.simple_expandable_list_item_1,
				R.layout.expandable_list_item_with_image,
				new String[] { ApiKeyColumns.KEY },
				new int[] { android.R.id.text1 }, 
				null, 
				null);
		setListAdapter(aaa);

		int groupCount = aaa.getGroupCount();
		for (int iGroup = 0; iGroup < groupCount; ++iGroup) {
			getExpandableListView().expandGroup(iGroup);
		}

		registerForContextMenu(getExpandableListView());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		iskDatabase.close();
		bitmapDownloadManager.shutdown();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			getMenuInflater().inflate(R.menu.context_pilots, menu);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			switch (item.getItemId()) {
			case R.id.pilots_context_remove:
				int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
				return true;
			}
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		CheckBox cb = (CheckBox) v.findViewById(R.id.pilot_checked);
		if (cb != null) {
			cb.toggle();
		}
		return true;
	}

	public class PilotsExpandableListAdapter extends SimpleCursorTreeAdapter {

		public PilotsExpandableListAdapter(Cursor cursor, Context context,
				int groupLayout, int childLayout, String[] groupFrom,
				int[] groupTo, String[] childrenFrom, int[] childrenTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo,
					childLayout, childrenFrom, childrenTo);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			long apiId = groupCursor.getLong(0);
			Cursor characterCursor = iskDatabase.getCharacterCursor(apiId);

			return characterCursor;
		}

		@Override
		protected void bindChildView(View view, Context context, final Cursor cursor, boolean isLastChild) {
			TextView textView = (TextView) view.findViewById(R.id.pilot_name);
			textView.setText(cursor.getString(1));

			ImageView imageView = (ImageView) view.findViewById(R.id.pilot_image);
			String imageViewUrl = (String) imageView.getTag();
			String imageUrl = de.matdue.isk.eve.Character.getCharacterUrl(cursor.getInt(3), 64);
			if (!imageUrl.equals(imageViewUrl)) {
				imageView.setTag(imageUrl);
				new BitmapDownloadTask(imageView, cacheManager,	bitmapDownloadManager).execute(imageUrl);
			}

			CheckBox checkBox = (CheckBox) view.findViewById(R.id.pilot_checked);
			checkBox.setOnCheckedChangeListener(null);
			checkBox.setChecked(cursor.getInt(2) != 0);
			checkBox.setTag(cursor.getPosition());
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					Integer currentPosition = (Integer) buttonView.getTag();
					if (cursor.moveToPosition(currentPosition)) {
						long id = cursor.getLong(0);
						iskDatabase.setCharacterSelection(id, isChecked);

						notifyDataSetChanged(true);
					}
				}

			});
		}
	}

}
