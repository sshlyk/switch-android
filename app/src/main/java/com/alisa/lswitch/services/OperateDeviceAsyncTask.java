package com.alisa.lswitch.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.alisa.lswitch.client.model.SwitchRequest;
import com.alisa.lswitch.content_providers.DevicesContentProvider;
import com.alisa.lswitch.utils.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;

public class OperateDeviceAsyncTask extends AsyncTask<OperateDeviceAsyncTask.Request, Void, Void> {

  private Context context;
  private final String TAG = OperateDeviceAsyncTask.class.getSimpleName();
  private final int requestBurstSize = 20;
  private int serverPort;
  private String passCode;

  public OperateDeviceAsyncTask(final String passCode, final int serverPort, final Context context) {
    this.context = context;
    this.passCode = passCode;
    this.serverPort = serverPort;
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
      switch (request.getOperation()) {
          case TOGGLE:
            toggleSimpleSwitch(deviceId, context);
            break;
          case BLINK:
            blinkSimpleSwitch(deviceId, context);
            break;
          default:
            return null;
      }
      DeviceListService.refreshListOfDevices(context, passCode);
    } finally {
      setDeviceState(deviceId, false);
    }
    return null;
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
    private Operation operation;
    public enum Operation {
      TOGGLE, BLINK
    }

    public String getDeviceId() {
      return deviceId;
    }
    public void setDeviceId(String deviceId) {
      this.deviceId = deviceId;
    }
    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
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
        final SwitchRequest.Operation operation = newState == 0 ?
                SwitchRequest.Operation.SET_OFF : SwitchRequest.Operation.SET_ON;
      operateSimpleSwitch(deviceId, operation, context);
    } else {
      Log.w(TAG, "Could not toggle switch. Device not found: " + deviceId);
    }
    if (cursor != null) { cursor.close(); }
  }

  private void blinkSimpleSwitch(String deviceId, final Context context) {
    operateSimpleSwitch(deviceId, SwitchRequest.Operation.BLINK, context);
  }

  /* Operate simple switch device */
  private void operateSimpleSwitch(final String deviceId, final SwitchRequest.Operation operation, final Context context) {
    final UUID deviceUUID;
    try {
      deviceUUID = UUID.fromString(deviceId);
    } catch (IllegalArgumentException e) {
      Log.d(TAG, "Invalid device id: " + deviceId);
      return;
    }
    DatagramSocket socketToClose = null;
    try {
      final DatagramSocket socket = new DatagramSocket(0);
      socket.setBroadcast(true);
      final InetAddress broadcastAddress = Utils.getWiFiBroadcastIp(context);
      if (broadcastAddress == null) {
        Log.d(TAG, "Broadcast address is missing");
        return;
      }

      final SwitchRequest switchRequest = new SwitchRequest();
      switchRequest.setDeviceId(deviceUUID);
      switchRequest.setOperation(operation);
      switchRequest.sign(passCode.getBytes("UTF-8"));
      final byte[] bytesToSend = switchRequest.serialize();

      for (int i = 0; i < requestBurstSize; i++) {
        socket.send(new DatagramPacket(
                    bytesToSend,
                    bytesToSend.length,
                    broadcastAddress,
                    serverPort
        ));
      }
      Log.i(TAG, "New state request has been sent. DeviceId: " + deviceId
          + ". New state: " + operation);

      //TODO now listen for reply instead of triggering refresh
      //socket.setBroadcast(false);

    } catch (IOException e) {
      Log.d(TAG, "Failed to operates simple switch due to IO exception", e);
    } finally {
      if (socketToClose != null) { socketToClose.close(); }
    }
  }
}
