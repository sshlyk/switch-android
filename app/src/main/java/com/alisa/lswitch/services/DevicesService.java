package com.alisa.lswitch.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.alisa.lswitch.client.Auth;
import com.alisa.lswitch.client.SwitchProxy;
import com.alisa.lswitch.client.model.BaseRequest;
import com.alisa.lswitch.client.model.StatusReply;
import com.alisa.lswitch.client.model.SwitchRequest;
import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.UUID;

//TODO make it regual service so requests are executed in parallel
public class DevicesService extends IntentService {

  private static enum Action {
    TOGGLE_SIMPLE_SWITCH
  }
  private static final String KEY_ACTION = "action";
  private static final String KEY_SWITCH_OPERATION = "switch_operation";
  private static final String KEY_DEVICE_ID = "device_id";
  private static final String TAG = "DevicesService";

  private SwitchProxy proxy;

  public DevicesService() {
    super(TAG);
  }

  public static void toggleSimpleSwitch(final String deviceId, Context ctx) {
    Intent intent = new Intent(ctx, DevicesService.class);
    intent.putExtra(KEY_ACTION, Action.TOGGLE_SIMPLE_SWITCH.ordinal());
    intent.putExtra(KEY_DEVICE_ID, deviceId);
    ctx.startService(intent);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    int actionOrdinal = intent.getIntExtra(KEY_ACTION, -1);
    if (actionOrdinal < 0 || actionOrdinal > Action.values().length - 1) {
      Log.i(TAG, "Undefined action ordinal: " + actionOrdinal);
      return;
    }

    if (proxy == null) {
      try {
        proxy = new SwitchProxy(new Auth("TODO".getBytes("UTF-8")));
      } catch (UnsupportedEncodingException e) {
        Log.w(TAG, "Failed to create switch proxy", e);
        return;
      }
    }

    Action action = Action.values()[actionOrdinal];
    try {
      switch (action) {
        case TOGGLE_SIMPLE_SWITCH:
          final String deviceId = intent.getStringExtra(KEY_DEVICE_ID);
          toggleSimpleSwitch(deviceId);
          break;
        default:
          throw new RuntimeException("Action is not implemented: " + action);
      }
    } catch (Exception e) {
      Log.w(TAG, "Failed to handle intent: " + intent, e);
    }
  }

  private void toggleSimpleSwitch(String deviceId) {
    Cursor cursor = this.getContentResolver().query(
        DevicesContentProvider.DEVICES_CONTENT_URI.buildUpon().appendPath(deviceId).build(),
        new String[] { DevicesContentProvider.ATTR_STATE},
        null,
        null,
        null
     );
    if (cursor != null && cursor.moveToNext()) {
      final int state = cursor.getInt(cursor.getColumnIndex(DevicesContentProvider.ATTR_STATE));
      final int newState = state == 0 ? 1 : 0;
      operateSimpleSwitch(UUID.fromString(deviceId), newState);
    } else {
      Log.w(TAG, "Could not toggle switch. Device not found: " + deviceId);
    }

    if (cursor != null) { cursor.close(); }
  }

  /* Operate simple switch device */
  private void operateSimpleSwitch(final UUID deviceId, final int operationVal) {
    DatagramSocket socketToClose = null;
    final int port = 61235; //TODO move to config
    try {
      final DatagramSocket socket = new DatagramSocket();
      socket.setBroadcast(true);
      final InetAddress broadcastAddress = Utils.getWiFiBroadcastIp(getApplicationContext());
      if (broadcastAddress == null) {
        Log.d(TAG, "Broadcast address is missing");
        return;
      }
      final SwitchRequest.Operation operation = operationVal == 0 ?
          SwitchRequest.Operation.SET_OFF : SwitchRequest.Operation.SET_ON;
      proxy.changeSwitchStatus(deviceId, operation, new SwitchProxy.Wire() {
        @Override
        public void send(byte[] bytes) throws IOException {
          socket.send(new DatagramPacket(
              bytes,
              bytes.length,
              broadcastAddress,
              port
          ));
        }
      });
      Log.i(TAG, "New state request has been sent. DeviceId: " + deviceId
          + ". New state: " + operationVal);
    } catch (IOException e) {
      Log.d(TAG, "Failed to operates simple switch due to IO exception", e);
    } finally {
      if (socketToClose != null) { socketToClose.close(); }
    }
  }
}
