package com.alisa.lswitch.utils;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;

public class Utils {

  private static final String TAG = "Utils";

  /**
   * Returns broadcast address for connected wifi network.
   * @param ctx
   * @return broadcast address or null if not connected to wifi
   * @throws SocketException
   */
  public static InetAddress getWiFiBroadcastIp(Context ctx) {
    WifiManager myWifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
    if (myDhcpInfo == null || myDhcpInfo.ipAddress == 0) {
      return null;
    }
    int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask) | ~myDhcpInfo.netmask;
    byte[] quads = new byte[4];
    for (int k = 0; k < 4; k++)
      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
    try {
      return InetAddress.getByAddress(quads);
    } catch (UnknownHostException e) {
      Log.d(TAG, "Failed to get broadcast address: " + e);
      return null;
    }
  }
}
