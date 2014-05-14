package com.alisa.lswitch.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.Log;

import com.alisa.lswitch.client.Auth;
import com.alisa.lswitch.client.SwitchProxy;
import com.alisa.lswitch.client.model.BaseModel;
import com.alisa.lswitch.client.model.StatusReply;
import com.alisa.lswitch.client.model.SwitchRequest;
import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.UUID;

public class DevicesService extends IntentService {

  private static enum Action {
    REFRESH, TOGGLE_SIMPLE_SWITCH
  }
  private static final String KEY_ACTION = "action";
  private static final String KEY_SWITCH_OPERATION = "switch_operation";
  private static final String KEY_DEVICE_ID = "device_id";
  private static final String TAG = "DevicesService";

  private SwitchProxy proxy;

  public DevicesService() {
    super(TAG);
  }

  public static void refreshListOfDevices(Context ctx) {
    Intent intent = new Intent(ctx, DevicesService.class);
    intent.putExtra(KEY_ACTION, Action.REFRESH.ordinal());
    ctx.startService(intent);
  }

  public static void toggleSimpleSwitch(final String deviceId, Context ctx) {
    Intent intent = new Intent(ctx, DevicesService.class);
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
        case REFRESH:
          refresh();
          removeStaleDevices();
          break;
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
    //get current state
    //TODO

  }

  private Cursor queryForDeviceId(String deviceId) {
    //TODO
    return null;
  }

  /* Operate simple switch device */
  private void operateSimpleSwitch(final UUID deviceId, final int operationVal) {
    DatagramSocket socketToClose = null;
    final int port = 61235; //TODO move to config
    try {
      final DatagramSocket socket = new DatagramSocket();
      socket.setBroadcast(true);
      final InetAddress broadcastAddress = Utils.getWiFiBroadcastIp(getApplicationContext());
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
    } catch (IOException e) {
      Log.d(TAG, "Failed to operates simple switch due to IO exception", e);
    } finally {
      if (socketToClose != null) { socketToClose.close(); }
    }
  }

  private void refresh() {
    //TODO broadcast devices status request, listen for response and update list of devices
    DatagramSocket socket = null;
    final int port = 51235; //TODO move to a config
    final long startTimestamp = System.currentTimeMillis();
    try {
      final InetAddress broadcastAddress = Utils.getWiFiBroadcastIp(getApplicationContext());
      if (broadcastAddress == null) {
        Log.d(TAG, "Not connected to WiFi or broadcast address is not available");
        return;
      }
      Log.d(TAG, "Broadcast address: " + broadcastAddress);
      socket = new DatagramSocket();
      socket.setBroadcast(true);
      final DatagramSocket finalSocket = socket;
      proxy.requestStatusBroadcast(new SwitchProxy.Wire() {
        @Override
        public void send(byte[] bytes) throws IOException {
          //TODO send burst
          finalSocket.send(new DatagramPacket(
              bytes,
              bytes.length,
              broadcastAddress,
              port
          ));
        }
      });
      //wait for replies
      byte[] reply = new byte[1024];
      DatagramPacket deviceReply = new DatagramPacket(reply, reply.length);

      socket.setSoTimeout(2000); //TODO move to config
      socket.receive(deviceReply);
      //update devices list
      updateDevicesList(deviceReply);
    } catch (SocketTimeoutException e) {
      //refresh complete
      Log.d(TAG, "Refresh complete. Timeout.");
    } catch (IOException e) {
      Log.d(TAG, "Refresh failed due to IO exception", e);
    } finally {
      if (socket != null) { socket.close(); }
    }
  }

  private void updateDevicesList(DatagramPacket deviceReply) {
    try {
      final StatusReply status = new StatusReply(ByteBuffer.wrap(deviceReply.getData()));

      ContentValues values = new ContentValues();
      values.put(DevicesContentProvider.ATTR_NAME, "Generic device"); //TODO
      values.put(DevicesContentProvider.ATTR_STATUS, status.getState() + ""); //TODO change underlying table so it stores int
      values.put(DevicesContentProvider.ATTR_DEVICE_ID, status.getDeviceId().toString());
      values.put(DevicesContentProvider.ATTR_IP, deviceReply.getAddress().getHostAddress());
      //TODO store port number

      this.getContentResolver().insert(DevicesContentProvider.DEVICES_CONTENT_URI, values);
    } catch (BaseModel.SerializationException e) {
      Log.d(TAG, "Could not parse reply from a device. Ignoring. IP: " + deviceReply.getAddress());
    }
  }

  private void removeStaleDevices() {
    //TODO move to config
    final long stalenessThresholdSec = System.currentTimeMillis()/1000 - 10 * 60;
    Cursor cursor = this.getContentResolver().query(
        DevicesContentProvider.DEVICES_CONTENT_URI,
        null, //projection
        null, //selection
        null, //selection args
        null
    );

    while (cursor != null && cursor.moveToNext()) {
      final int lastUpdatedCol = cursor.getColumnIndex(DevicesContentProvider.ATTR_LAST_UPDATED);
      final int deviceIdCol = cursor.getColumnIndex(DevicesContentProvider.ATTR_DEVICE_ID);
      final String deviceId = cursor.getString(deviceIdCol);
      final int lastUpdated = cursor.getInt(lastUpdatedCol);
      if (lastUpdated < stalenessThresholdSec) {
        final Uri uri = DevicesContentProvider.DEVICES_CONTENT_URI.buildUpon()
            .appendPath(deviceId).build();
        this.getContentResolver().delete(
            uri,
            null,
            null
        );
        this.getContentResolver().notifyChange(uri, null);
      }
    }

    cursor.close();
  }
}
