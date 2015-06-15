package com.alisa.lswitch;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SimpleCursorAdapter;

import com.alisa.lswitch.database.DevicesContentProvider;
import com.alisa.lswitch.service.DeviceListService;
import com.alisa.lswitch.service.OperateDeviceAsyncTask;

public class SwitchListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

  private SimpleCursorAdapter cursorAdapter;
  private static final long DEVICE_LIST_UPDATE_INTERVAL_SEC = 2;
  private static final String TAG = SwitchListActivity.class.getSimpleName();
  private DeviceListRefresher mDeviceListRefresher;
  private SharedPreferences preferences;

  private int serverPort;
  private String passCode;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.device_list);

    cursorAdapter = new DeviceCursorAdapter(this);
    setListAdapter(cursorAdapter);
    getLoaderManager().initLoader(0, null, this);

    preferences = PreferenceManager.getDefaultSharedPreferences(this);

    mDeviceListRefresher = new DeviceListRefresher(
        DEVICE_LIST_UPDATE_INTERVAL_SEC, getApplicationContext());
  }

  @Override
  protected void onPause() {
    super.onPause();
    mDeviceListRefresher.stop();
  }

  @Override
  protected void onResume() {
    super.onResume();
    serverPort = Integer.parseInt(preferences.getString(
        getResources().getString(R.string.portNumberKey),
        getResources().getString(R.string.defaultPortNumber)));
    passCode = preferences.getString(getResources().getString(R.string.passCodeKey), "");
    mDeviceListRefresher.start(passCode);
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
      DeviceListService.refreshListOfDevices(getApplicationContext(), passCode);
      return true;
    case R.id.action_settings:
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  public void operateSwitch(View view) {
    final String deviceId = (String) view.getTag(R.integer.tag_device_id);
    final int state = (Integer) view.getTag(R.integer.tag_state);
    final String type = (String) view.getTag(R.integer.tag_type);

    OperateDeviceAsyncTask.Request.Operation op;
    if ("switch".equals(type)) {
      op = state == 0 ?
              OperateDeviceAsyncTask.Request.Operation.TURN_ON :
              OperateDeviceAsyncTask.Request.Operation.TURN_OFF;
    } else {
      op = OperateDeviceAsyncTask.Request.Operation.PULSE;
    }
    final OperateDeviceAsyncTask.Request request = new OperateDeviceAsyncTask.Request();
    request.setDeviceId(  deviceId);
    request.setOperation(op);
    new OperateDeviceAsyncTask(passCode, serverPort, getApplicationContext())
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(
        this,
        DevicesContentProvider.DEVICES_CONTENT_URI,
        null, // projection
        null, // selection
        null, // selection args
        DevicesContentProvider.ATTR_NAME // ordering
    );
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
