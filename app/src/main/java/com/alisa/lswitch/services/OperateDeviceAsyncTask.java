package com.alisa.lswitch.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.alisa.lswitch.client.Auth;
import com.alisa.lswitch.client.SwitchProxy;
import com.alisa.lswitch.client.model.SwitchRequest;
import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.utils.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OperateDeviceAsyncTask extends AsyncTask<OperateDeviceAsyncTask.Request, Void, Void> {

  private Context context;
  private final String TAG = OperateDeviceAsyncTask.class.getSimpleName();
  private final int requestBurstSize = 20;
  private final SwitchProxy proxy = new SwitchProxy(new Auth("".getBytes(StandardCharsets.UTF_8)));

  public OperateDeviceAsyncTask(final Context context) {
    this.context = context;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
  }

  @Override
  protected Void doInBackground(Request... params) {
    if (params == null || params.length == 0) { return null; }
    final Request request = params[0];
    final String deviceId = request.getDeviceId();
    setDeviceState(deviceId, true);

    try {

      toggleSimpleSwitch(deviceId, context);

      //TODO
      DeviceListService.refreshListOfDevices(context);

    } finally {
      setDeviceState(deviceId, false);
    }
    return null;
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);
  }

  private void setDeviceState(String deviceId, boolean inProgress) {
    final ContentValues values = new ContentValues();
    values.put(
        DevicesContentProvider.ATTR_OPERATION_TIMESTAMP,
        inProgress ? System.currentTimeMillis()/1000 : 0
    );
    this.context.getContentResolver().update(
        DevicesContentProvider.DEVICES_CONTENT_URI.buildUpon().appendPath(deviceId).build(),
        values,
        null,
        null
    );
  }

  public static class Request {

    private String deviceId;

    public String getDeviceId() {
      return deviceId;
    }

    public void setDeviceId(String deviceId) {
      this.deviceId = deviceId;
    }
  }


  private void toggleSimpleSwitch(String deviceId, final Context context) {
    Cursor cursor = context.getContentResolver().query(
        DevicesContentProvider.DEVICES_CONTENT_URI.buildUpon().appendPath(deviceId).build(),
        new String[] { DevicesContentProvider.ATTR_STATE},
        null,
        null,
        null
    );
    if (cursor != null && cursor.moveToNext()) {
      final int state = cursor.getInt(cursor.getColumnIndex(DevicesContentProvider.ATTR_STATE));
      final int newState = (state == 0 ? 1 : 0);
      operateSimpleSwitch(UUID.fromString(deviceId), newState, context);
    } else {
      Log.w(TAG, "Could not toggle switch. Device not found: " + deviceId);
    }

    if (cursor != null) { cursor.close(); }
  }

  /* Operate simple switch device */
  private void operateSimpleSwitch(final UUID deviceId, final int operationVal, final Context context) {
    DatagramSocket socketToClose = null;
    final int port = 61235; //TODO move to config
    try {
      final DatagramSocket socket = new DatagramSocket();
      socket.setBroadcast(true);
      final InetAddress broadcastAddress = Utils.getWiFiBroadcastIp(context);
      if (broadcastAddress == null) {
        Log.d(TAG, "Broadcast address is missing");
        return;
      }
      final SwitchRequest.Operation operation = operationVal == 0 ?
          SwitchRequest.Operation.SET_OFF : SwitchRequest.Operation.SET_ON;
      proxy.changeSwitchStatus(deviceId, operation, new SwitchProxy.Wire() {
        @Override
        public void send(byte[] bytes) throws IOException {
          for (int i = 0; i < requestBurstSize; i++) {
            socket.send(new DatagramPacket(
                bytes,
                bytes.length,
                broadcastAddress,
                port
            ));
          }
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
