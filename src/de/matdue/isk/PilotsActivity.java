package de.matdue.isk;

import de.matdue.isk.R;
import de.matdue.isk.database.ApiKeyColumns;
import de.matdue.isk.database.IskDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class PilotsActivity extends ExpandableListActivity {
	
	private IskDatabase iskDatabase;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		iskDatabase = new IskDatabase(this);
		Cursor apiKeyCursor = iskDatabase.getApiKeyCursor();
		startManagingCursor(apiKeyCursor);
		
		MyExpandableListAdapter aaa = new MyExpandableListAdapter(apiKeyCursor, 
				this, 
				android.R.layout.simple_expandable_list_item_1, 
				R.layout.expandable_list_item_with_image,
				//android.R.layout.simple_expandable_list_item_1,  
				new String[] { ApiKeyColumns.KEY }, 
				new int[] { android.R.id.text1 }, 
				null, //new String[] { CharacterColumns.NAME }, 
				null); //new int[] { android.R.id.text1 });
		setListAdapter(aaa);
		
	    int groupCount = aaa.getGroupCount();
	    for (int iGroup = 0; iGroup < groupCount; ++iGroup) {
	    	getExpandableListView().expandGroup(iGroup);
	    }
	    
	    registerForContextMenu(getExpandableListView());

/*		// Construct Expandable List
	    final String NAME = "name";
	    final String IMAGE = "image";
	    final LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    final ArrayList<HashMap<String, String>> headerData = new ArrayList<HashMap<String, String>>();
	    
	    final HashMap<String, String> group1 = new HashMap<String, String>();
	    group1.put(NAME, "Group 1");
	    headerData.add(group1);

	    final HashMap<String, String> group2 = new HashMap<String, String>();
	    group2.put(NAME, "Group 2");
	    headerData.add(group2);
	    
	    final ArrayList<ArrayList<HashMap<String, Object>>> childData = new ArrayList<ArrayList<HashMap<String, Object>>>();

	    final ArrayList<HashMap<String, Object>> group1data = new ArrayList<HashMap<String, Object>>();
	    childData.add(group1data);

	    final ArrayList<HashMap<String, Object>> group2data = new ArrayList<HashMap<String, Object>>();
	    childData.add(group2data);

	    // Set up some sample data in both groups
	    for( int i=0; i<10; ++i) {
	        final HashMap<String, Object> map = new HashMap<String,Object>();
	        map.put(NAME, "Child " + i );
	        map.put(IMAGE, getResources().getDrawable(R.drawable.ic_launcher));
	        ( i%2==0 ? group1data : group2data ).add(map);
	    }

	    ExpandableListAdapter adapter = new SimpleExpandableListAdapter(
	            this,
	            headerData,
	            android.R.layout.simple_expandable_list_item_1,
	            new String[] { NAME },            // the name of the field data
	            new int[] { android.R.id.text1 }, // the text field to populate with the field data
	            childData,
	            0,
	            null,
	            new int[] {}
	        ) {
	            @SuppressWarnings("unchecked")
				@Override
	            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
	                final View v = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);

	                // Populate your custom view here
	                ((TextView)v.findViewById(R.id.pilot_name)).setText( (String) ((Map<String,Object>)getChild(groupPosition, childPosition)).get(NAME) );
	                ((ImageView)v.findViewById(R.id.pilot_image)).setImageDrawable( (Drawable) ((Map<String,Object>)getChild(groupPosition, childPosition)).get(IMAGE) );
	                ((CheckBox)v.findViewById(R.id.pilot_checked)).setChecked(true);
	                ((CheckBox)v.findViewById(R.id.pilot_checked)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
						
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							Log.d("Clicked", buttonView.getTag().toString());
						}
						
					});
	                ((CheckBox)v.findViewById(R.id.pilot_checked)).setTag(Integer.valueOf(groupPosition*100 + childPosition));

	                return v;
	            }

	            @Override
	            public View newChildView(boolean isLastChild, ViewGroup parent) {
	                 return layoutInflater.inflate(R.layout.expandable_list_item_with_image, null, false);
	            }
	        }
	    ;
	    setListAdapter(adapter);
	    
	    int groupCount = adapter.getGroupCount();
	    for (int iGroup = 0; iGroup < groupCount; ++iGroup) {
	    	getExpandableListView().expandGroup(iGroup);
	    }
	    
	    registerForContextMenu(getExpandableListView());*/
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
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

	
	public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

		public MyExpandableListAdapter(Cursor cursor, Context context,
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
		protected void bindChildView(View view, Context context, final Cursor cursor,
				boolean isLastChild) {
			TextView textView = (TextView) view.findViewById(R.id.pilot_name);
			textView.setText(cursor.getString(1));
			
			ImageView imageView = (ImageView) view.findViewById(R.id.pilot_image);
			imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			
			CheckBox checkBox = (CheckBox) view.findViewById(R.id.pilot_checked);
			checkBox.setChecked(cursor.getInt(2) != 0);
			checkBox.setTag(cursor.getPosition());
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					Integer currentPosition = (Integer)buttonView.getTag();
					if (cursor.moveToPosition(currentPosition)) {
						long id = cursor.getLong(0);
						iskDatabase.setCharacterSelection(id, isChecked);
					}
				}
				
			});
		}
	}
}
