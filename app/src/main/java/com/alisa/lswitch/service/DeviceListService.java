package com.alisa.lswitch.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alisa.lswitch.R;
import com.alisa.lswitch.client.model.BaseRequest;
import com.alisa.lswitch.client.model.StatusReply;
import com.alisa.lswitch.client.model.StatusRequest;
import com.alisa.lswitch.database.DevicesContentProvider;
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
    refreshing = false;
  }

  private void refresh(String passCode) {
    DatagramSocket socket = null;
    try {
      socket = new DatagramSocket();
      socket.setBroadcast(true);
      sendDeviceStatusRequest(socket, passCode);
      listenForDevicesStatus(socket, 1000);
    } catch (SocketTimeoutException e) {
      Log.d(TAG, "Refresh complete. Timeout.");
    } catch (IOException e) {
      Log.d(TAG, "Refresh failed due to IO exception", e);
    } finally {
      if (socket != null) { socket.close(); }
    }
  }

  private void sendDeviceStatusRequest(final DatagramSocket socket, final String passCode)
          throws IOException {
    final DatagramSocket finalSocket = socket;
    final InetAddress broadcastAddress = Utils.getWiFiBroadcastIp(getApplicationContext());
    final SharedPreferences preferences =  PreferenceManager.getDefaultSharedPreferences(this);
    final int port = Integer.parseInt(preferences.getString(
            getResources().getString(R.string.portNumberKey),
            getResources().getString(R.string.defaultPortNumber)));
    if (broadcastAddress == null) {
      Log.d(TAG, "Not connected to WiFi or broadcast address is not available");
      return;
    }
    Log.d(TAG, "Broadcast address: " + broadcastAddress);
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
  }

  private void listenForDevicesStatus(final DatagramSocket socket, final int listenTime)
          throws IOException {
    byte[] reply = new byte[1024];
    DatagramPacket deviceReply = new DatagramPacket(reply, reply.length);
    final long waitStart = SystemClock.elapsedRealtime();
    long elapsed = 0;
    do {
      socket.setSoTimeout((int)(listenTime - elapsed));
      socket.receive(deviceReply);
      elapsed = SystemClock.elapsedRealtime() - waitStart;
      updateDevicesList(deviceReply);
    } while (elapsed < listenTime);

  }

  private void updateDevicesList(DatagramPacket deviceReply) {
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
}
