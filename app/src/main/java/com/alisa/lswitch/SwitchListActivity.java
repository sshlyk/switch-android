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
import android.widget.PopupWindow;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.services.DeviceListService;
import com.alisa.lswitch.services.OperateDeviceAsyncTask;

import java.nio.charset.StandardCharsets;

public class SwitchListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

  private SimpleCursorAdapter cursorAdapter;
  private static final long DEVICE_LIST_UPDATE_INTERVAL_SEC = 10;
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
  protected void onStop() {
    super.onStop();
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

  public void blinkSimpleSwitch(View view) {
    operateSwitch(view, OperateDeviceAsyncTask.Request.Operation.BLINK);
  }

  public void toggleSimpleSwitch(View view) {
    operateSwitch(view, OperateDeviceAsyncTask.Request.Operation.TOGGLE);
  }

  private void operateSwitch(View view, OperateDeviceAsyncTask.Request.Operation operation) {
    final String deviceId = ((TextView) view.findViewById(R.id.device_id)).getText().toString();
    final OperateDeviceAsyncTask.Request request = new OperateDeviceAsyncTask.Request();
    request.setDeviceId(deviceId);
    request.setOperation(operation);
    new OperateDeviceAsyncTask(passCode, serverPort, getApplicationContext())
        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
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
