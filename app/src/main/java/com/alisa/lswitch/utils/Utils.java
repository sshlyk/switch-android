package com.alisa.lswitch.utils;

import android.content.Context;
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
  public static InetAddress getWiFiBroadcastIp(Context ctx) throws SocketException {
    WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    String wifiMacAddress = wifiManager.getConnectionInfo().getMacAddress();
    for (NetworkInterface iface: Collections.list(NetworkInterface.getNetworkInterfaces())) {
      String hardwareMac = toMacAddress(iface.getHardwareAddress());
      if (toMacAddress(iface.getHardwareAddress()).equals(wifiMacAddress)) {
        for (InterfaceAddress interfaceAddress : iface.getInterfaceAddresses()) {
          if (interfaceAddress.getBroadcast() != null) {
            final String broadcastAddress = interfaceAddress.getBroadcast().toString().substring(1);
            try {
              return InetAddress.getByName(broadcastAddress);
            } catch (UnknownHostException e) {
              Log.d(TAG, "Failed to resolve broadcast address: " + broadcastAddress);
              return null;
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Convert hardware address to a string representation.
   * @param hardwareAddress
   * @return
   */
  public static String toMacAddress(byte[] hardwareAddress) {
    if (hardwareAddress == null) return "";
    StringBuilder sb = new StringBuilder(18);
    for (byte b : hardwareAddress) {
      if (sb.length() > 0) { sb.append(':'); }
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
