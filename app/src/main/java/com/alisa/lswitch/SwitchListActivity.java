package com.alisa.lswitch;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

import com.alisa.lswitch.contentproviders.DevicesContentProvider;
import com.alisa.lswitch.db.DevicesDatabaseHelper;


public class SwitchListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter cursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
        initCursorAdapter();
    }

    private void initCursorAdapter() {
        String[] from = new String[] { DevicesDatabaseHelper.COLUMN_DEVICE_NAME };
        int[] to = new int[] { R.id.device_name_view };
        getLoaderManager().initLoader(0, null, this);
        cursorAdapter = new SimpleCursorAdapter(this, R.layout.device, null, from, to, 0);
        setListAdapter(cursorAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.switch_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ****************************************************************************************** */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { DevicesDatabaseHelper.COLUMN_DEVICE_NAME };
        final CursorLoader cursorLoader = new CursorLoader(
                this,
                DevicesContentProvider.CONTENT_URI,
                projection,
                null,
                null,
                null
        );
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursorAdapter.swapCursor(null);
    }
}
