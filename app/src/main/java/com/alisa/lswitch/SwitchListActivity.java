package com.alisa.lswitch;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.services.DevicesService;


public class SwitchListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

  private SimpleCursorAdapter cursorAdapter;
  private PopupWindow popupWindow;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.device_list);
    initCursorAdapter();
  }

  @Override
  protected void onResume() {
    super.onResume();
    DevicesService.refreshListOfDevices(getApplicationContext());
  }

  private void initCursorAdapter() {
    String[] from = new String[] {
        DevicesContentProvider.ATTR_DEVICE_ID,
        DevicesContentProvider.ATTR_STATUS
    };
    int[] to = new int[] {
        R.id.device_name,
        R.id.device_status
    };
    cursorAdapter = new SimpleCursorAdapter(this, R.layout.device, null, from, to, 0) {
      @Override
      public void setViewText(TextView v, String text) {
        super.setViewText(v, convert(v.getId(), text));
      }

      private String convert(int id, String text) {
        switch (id) {
        case (R.id.device_status):
          return "0".equals(text) ? "OFF" : "ON";
        default:
          return text;
        }
      }
    };
    setListAdapter(cursorAdapter);
    getLoaderManager().initLoader(0, null, this);
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
    switch (id) {
    case R.id.action_refresh:
      DevicesService.refreshListOfDevices(getApplicationContext());
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  public void deviceInfo(View view) {
    View popupView = getLayoutInflater().inflate(R.layout.device_info_popup, null);
    if (popupWindow != null) { popupWindow.dismiss(); }
    popupWindow = new PopupWindow(
        popupView,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        false //focusable
    );
    popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
    popupWindow.showAtLocation(popupView, Gravity.CENTER, 0,0);
    popupView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { popupWindow.dismiss(); }
    });
  }

  public void operateSimpleSwitch(View view) {
    final String deviceId = ((TextView) view.findViewById(R.id.device_name)).getText().toString();
    DevicesService.toggleSimpleSwitch(deviceId, getApplicationContext());
  }

  /* ****************************************************************************************** */
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    final CursorLoader cursorLoader = new CursorLoader(
        this,
        DevicesContentProvider.DEVICES_CONTENT_URI,
        null, //projection, for now return all fields
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
