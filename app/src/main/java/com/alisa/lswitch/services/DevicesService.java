package com.alisa.lswitch.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alisa.lswitch.content_providers.DevicesContentProvider;

import java.util.UUID;

public class DevicesService extends IntentService {

  private static enum Action {
    REFRESH
  }
  private static final String KEY_ACTION = "action";
  private static final String TAG = "DevicesService";

  public DevicesService() {
    super("DevicesService");
  }

  public static void refreshListOfDevices(Context ctx) {
    Intent intent = new Intent(ctx, DevicesService.class);
    intent.putExtra(KEY_ACTION, Action.REFRESH.ordinal());
    ctx.startService(intent);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    int actionOrdinal = intent.getIntExtra(KEY_ACTION, -1);
    if (actionOrdinal < 0 || actionOrdinal > Action.values().length - 1) {
      Log.i(TAG, "Undefined action ordinal: " + actionOrdinal);
      return;
    }
    Action action = Action.values()[actionOrdinal];
    switch (action) {
    case REFRESH:
      refresh();
      break;
    default:
      throw new RuntimeException("Action is not implemented: " + action);
    }
  }

  private void refresh() {
    //TODO broadcast devices status request, listen for response and update list of devices
    ContentValues values = new ContentValues();
    values.put(DevicesContentProvider.ATTR_NAME, "blah");
    values.put(DevicesContentProvider.ATTR_STATUS, "off");
    values.put(DevicesContentProvider.ATTR_UUID, UUID.randomUUID().toString());
    values.put(DevicesContentProvider.ATTR_IP, "1.1.1.1");

    this.getContentResolver().insert(DevicesContentProvider.DEVICES_CONTENT_URI, values);
  }
}
