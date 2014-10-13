package com.alisa.lswitch.services;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.alisa.lswitch.client.model.BaseRequest;
import com.alisa.lswitch.client.model.BaseRequest;
import com.alisa.lswitch.client.model.StatusReply;
import com.alisa.lswitch.client.model.StatusRequest;
import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DeviceListService extends IntentService {

  private static String TAG = "DeviceListService";
  private final int statusRequestBurstSize = 100; //TODO move to config
  private final LinkedHashMap<UUID, Long> statusResponseLookup = new LinkedHashMap<UUID, Long>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) {
      return size() > 500;
    }
  };

  private final static String SECRET = "secret";

  public DeviceListService() {
    super(TAG);
  }

  public static void refreshListOfDevices(Context ctx, String passCode) {
    Intent intent = new Intent(ctx, DeviceListService.class);
    intent.putExtra(SECRET, passCode);
    ctx.startService(intent);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    final String passCode = intent.getStringExtra(SECRET);
    if (passCode == null) { return; }
    refresh(passCode);
    removeStaleDevices();

    //TODO remove testing only
    //insertDummyRecord();
  }

  private void refresh(String passCode) {
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

      final StatusRequest statusRequest = new StatusRequest();
      statusRequest.sign(passCode.getBytes("UTF-8"));
      final byte[] bytes = statusRequest.serialize();
      for (int i = 0; i < statusRequestBurstSize; i++) {
        finalSocket.send(new DatagramPacket(
            bytes,
            bytes.length,
            broadcastAddress,
            port
        ));
      }

      //wait for replies
      byte[] reply = new byte[1024];
      DatagramPacket deviceReply = new DatagramPacket(reply, reply.length);
      final int replyWaitInterval = 2000;
      final long waitStart = System.currentTimeMillis();
      long elapsed = 0;
      do {
        socket.setSoTimeout((int)(replyWaitInterval - elapsed));
        socket.receive(deviceReply);
        elapsed = System.currentTimeMillis() - waitStart;
        updateDevicesList(deviceReply, getContentResolver());
      } while (elapsed < replyWaitInterval);
    } catch (SocketTimeoutException e) {
      //refresh complete
      Log.d(TAG, "Refresh complete. Timeout.");
    } catch (IOException e) {
      Log.d(TAG, "Refresh failed due to IO exception", e);
    } finally {
      if (socket != null) { socket.close(); }
    }
  }

  private void updateDevicesList(DatagramPacket deviceReply, ContentResolver contentResolver) {
    if (deviceReply == null) { return; }
    try {
      final StatusReply status = new StatusReply(ByteBuffer.wrap(deviceReply.getData()));
      if (statusResponseLookup.containsValue(status.getRequestId())) { return; }
      ContentValues values = new ContentValues();
      values.put(DevicesContentProvider.ATTR_NAME, status.getDeviceType()); //TODO
      values.put(DevicesContentProvider.ATTR_STATE, status.getState() + ""); //TODO change underlying table so it stores int
      values.put(DevicesContentProvider.ATTR_DEVICE_ID, status.getDeviceId().toString());
      if (deviceReply.getAddress() != null) {
        values.put(DevicesContentProvider.ATTR_IP, deviceReply.getAddress().getHostAddress());
      }
      //TODO store port number

      contentResolver.insert(DevicesContentProvider.DEVICES_CONTENT_URI, values);
      statusResponseLookup.put(status.getRequestId(), System.currentTimeMillis());
    } catch (BaseRequest.SerializationException e) {
      Log.d(TAG, "Could not parse reply from a device. Ignoring. IP: " + deviceReply.getAddress());
    }
  }

  private void removeStaleDevices() {
    //TODO move to config
    final long stalenessThresholdSec = System.currentTimeMillis()/1000 - 60; //TODO
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

  //TODO remove
  private void insertDummyRecord() {
    final byte[] record = new StatusReply() {{
      setDeviceId(UUID.randomUUID());
    }}.serialize();
    updateDevicesList(new DatagramPacket(record, record.length), getContentResolver());
  }
}
