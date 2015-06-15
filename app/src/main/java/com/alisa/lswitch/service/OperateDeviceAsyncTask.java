package com.alisa.lswitch.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.alisa.lswitch.client.model.SwitchRequest;
import com.alisa.lswitch.database.DevicesContentProvider;
import com.alisa.lswitch.utils.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;

public class OperateDeviceAsyncTask extends AsyncTask<OperateDeviceAsyncTask.Request, Void, Void> {

  private Context context;
  private final String TAG = OperateDeviceAsyncTask.class.getSimpleName();
  private final int requestBurstSize = 100;
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
    if (params == null || params.length == 0) {
      return null;
    }
    final Request request = params[0];
    final String deviceId = request.getDeviceId();
    SwitchRequest.Operation operation;
    switch (request.getOperation()) {
      case TURN_ON:
        operation = SwitchRequest.Operation.SET_ON;
        break;
      case TURN_OFF:
        operation = SwitchRequest.Operation.SET_OFF;
        break;
      case PULSE:
        operation = SwitchRequest.Operation.PULSE;
        break;
      default:
        return null;
    }
    DevicesContentProvider.updateDeviceState(context, deviceId,
            operation == SwitchRequest.Operation.SET_ON ? 1 : 0);
    operateSimpleSwitch(deviceId, operation, context);
    DeviceListService.refreshListOfDevices(context, passCode);
    return null;
  }

  /* Operate simple switch device */
  private void operateSimpleSwitch(final String deviceId, final SwitchRequest.Operation operation,
                                   final Context context) {
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
    } catch (IOException e) {
      Log.d(TAG, "Failed to operates simple switch due to IO exception", e);
    } finally {
      if (socketToClose != null) { socketToClose.close(); }
    }
  }

  public static class Request {

    private String deviceId;
    private Operation operation;
    public enum Operation {
      TURN_ON, TURN_OFF, PULSE
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
}
