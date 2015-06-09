package com.alisa.lswitch.services;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.alisa.lswitch.client.model.BaseRequest;
import com.alisa.lswitch.client.model.StatusReply;
import com.alisa.lswitch.client.model.StatusRequest;
import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.utils.Utils;

import java.io.IOException;
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
  private static volatile boolean refreshing = false;

  public DeviceListService() {
    super(TAG);
  }

  public static void refreshListOfDevices(Context ctx, String passCode) {
    if (refreshing) { return; }
    Intent intent = new Intent(ctx, DeviceListService.class);
    intent.putExtra(SECRET, passCode);
    ctx.startService(intent);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    refreshing = true;
    final String passCode = intent.getStringExtra(SECRET);
    if (passCode == null) { return; }
    refresh(passCode);
    DevicesContentProvider.removeStaleDevices(getApplicationContext());

    //TODO remove testing only
    //insertDummyRecord();
    refreshing = false;
  }

  private void refresh(String passCode) {
    //TODO broadcast devices status request, listen for response and update list of devices
    DatagramSocket socket = null;
    final int port = 51235; //TODO move to a config
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
      final int replyWaitInterval = 1000  ;
      final long waitStart = SystemClock.elapsedRealtime();
      long elapsed = 0;
      do {
        socket.setSoTimeout((int)(replyWaitInterval - elapsed));
        socket.receive(deviceReply);
        elapsed = SystemClock.elapsedRealtime() - waitStart;
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
      DevicesContentProvider.insertDevice(
              getApplicationContext(),
              status.getDeviceId(),
              status.getDeviceName(),
              status.getDeviceType(),
              status.getState());
      statusResponseLookup.put(status.getRequestId(), System.currentTimeMillis());
    } catch (BaseRequest.SerializationException e) {
      Log.d(TAG, "Could not parse reply from a device. Ignoring. IP: " + deviceReply.getAddress());
    }
  }

  //TODO remove
  private void insertDummyRecord() {
    final byte[] record = new StatusReply() {{
      setDeviceId(UUID.randomUUID());
    }}.serialize();
    updateDevicesList(new DatagramPacket(record, record.length), getContentResolver());
  }
}
