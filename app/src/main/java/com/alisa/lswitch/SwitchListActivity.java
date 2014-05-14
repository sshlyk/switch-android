package com.alisa.lswitch;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.services.DeviceListService;
import com.alisa.lswitch.services.DevicesService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class SwitchListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

  private SimpleCursorAdapter cursorAdapter;
  private PopupWindow popupWindow;
  private static final long DEVICE_LIST_UPDATE_INTERVAL_SEC = 10;
  private static final String TAG = SwitchListActivity.class.getSimpleName();

  private static final String BUNDLE_LIST_TIMESTAMP = "list_timestamp";
  private DeviceListRefresher mDeviceListRefresher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.device_list);

    cursorAdapter = new DeviceCursorAdapter(this);
    setListAdapter(cursorAdapter);
    getLoaderManager().initLoader(0, null, this);

    mDeviceListRefresher = new DeviceListRefresher(
        DEVICE_LIST_UPDATE_INTERVAL_SEC, getApplicationContext());

  }

  @Override
  protected void onStart() {
    super.onStart();
    mDeviceListRefresher.start();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mDeviceListRefresher.stop();
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
      DeviceListService.refreshListOfDevices(getApplicationContext());
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  //TODO device info popup
//  public void deviceInfo(View view) {
//    View popupView = getLayoutInflater().inflate(R.layout.device_info_popup, null);
//    if (popupWindow != null) { popupWindow.dismiss(); }
//    popupWindow = new PopupWindow(
//        popupView,
//        LinearLayout.LayoutParams.WRAP_CONTENT,
//        LinearLayout.LayoutParams.WRAP_CONTENT,
//        false //focusable
//    );
//    popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
//    popupWindow.showAtLocation(popupView, Gravity.CENTER, 0,0);
//    popupView.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View v) { popupWindow.dismiss(); }
//    });
//  }

  public void toggleSimpleSwitch(View view) {
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
