package com.alisa.lswitch.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import java.util.List;
import java.util.UUID;

public class DevicesContentProvider extends ContentProvider {

  private DevicesDatabaseHelper dbHelper;
  private static final String AUTHORITY = "com.alisa.lswitch.devicesprovider";
  private static final String DEVICES_PATH = "devices";
  public static final Uri DEVICES_CONTENT_URI =
      Uri.parse(String.format("content://%s/%s", AUTHORITY, DEVICES_PATH));

  public static final String ATTR_DEVICE_ID = "device_id";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_STATE = "state";
  public static final String ATTR_LAST_UPDATED = "last_updated";

  private static final String TAG = DevicesContentProvider.class.getSimpleName();

  @Override
  public boolean onCreate() {
    dbHelper = new DevicesDatabaseHelper(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    List<String> segments = uri.getPathSegments();
    if (segments.size() == 0) { return null; }
    String path = segments.get(0);

    if (DEVICES_PATH.equals(path)) {
      SQLiteQueryBuilder query = new SQLiteQueryBuilder();
      query.setTables(DevicesDatabaseHelper.TABLE_DEVICES);
      if (selection != null ) { query.appendWhere(selection); }
      if (segments.size() > 1) {
        final String deviceId = segments.get(1);
        query.appendWhere(String.format(" AND %s = '%s'", ATTR_DEVICE_ID, deviceId));
      }
      final Cursor cursor = query.query(
          dbHelper.getReadableDatabase(),
          projection,
          selection,
          selectionArgs,
          null, //group by
          null, //having
          sortOrder);
      cursor.setNotificationUri(getContext().getContentResolver(), uri);
      return cursor;
    } else {
      throw new RuntimeException("Unknown path: " + uri);
    }
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    List<String> segments = uri.getPathSegments();
    if (segments.size() < 1) return null;
    final String path = segments.get(0);

    if (DEVICES_PATH.equals(path)) {
      final String deviceId = values.getAsString(ATTR_DEVICE_ID);
      if (deviceId == null) { return null; }

      SQLiteDatabase db = dbHelper.getWritableDatabase();
      Cursor cursor = null;
      db.beginTransaction();
      try {
        cursor = db.query(DevicesDatabaseHelper.TABLE_DEVICES,
                new String[] { ATTR_DEVICE_ID }, ATTR_DEVICE_ID + "=?", new String[] { deviceId },
                null, null, null);
        if (cursor.moveToNext()) {
          update(deviceUri(deviceId), values, null, null);
        } else {
          db.insert(DevicesDatabaseHelper.TABLE_DEVICES, null, values);
        }
        db.setTransactionSuccessful();
      } finally {
        if (cursor != null) { cursor.close(); }
        db.endTransaction();
      }
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return null; //TODO
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    List<String> segments = uri.getPathSegments();
    if (segments.size() < 1) return 0;
    final String path = segments.get(0);

    int deletedRows = 0;
    if (DEVICES_PATH.equals(path)) {
      final String whereClause;
      final String[] whereArgs;
      if (segments.size() > 1) {
        final String deviceId = segments.get(1);
        whereClause = String.format("%s = ?", ATTR_DEVICE_ID);
        whereArgs = new String[]{ deviceId };
      } else {
        whereClause = selection;
        whereArgs = selectionArgs;
      }

      deletedRows = dbHelper.getWritableDatabase().delete(
              DevicesDatabaseHelper.TABLE_DEVICES, whereClause, whereArgs);
    }
    if (deletedRows > 0) {
      getContext().getContentResolver().notifyChange(uri, null);
    }

    return deletedRows;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    List<String> segments = uri.getPathSegments();
    if (segments.size() < 1) return 0;
    final String path = segments.get(0);

    int rowsUpdated = 0;
    if (DEVICES_PATH.equals(path)) {
      if (segments.size() > 1) {
        final String deviceId = segments.get(1);
        if (!TextUtils.isEmpty(selection)) {
          selection += " AND ";
        } else {
          selection = "";
        }
        selection += String.format("%s = '%s'", ATTR_DEVICE_ID, deviceId);
      }
      rowsUpdated = dbHelper.getWritableDatabase().update(
          DevicesDatabaseHelper.TABLE_DEVICES,
          values,
          selection,
          selectionArgs
      );
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return rowsUpdated;
  }

  public static void removeStaleDevices(final Context context) {
    final long elapsedTime = SystemClock.elapsedRealtime();
    final long threshold = 10000;
    final String whereClause = DevicesContentProvider.ATTR_LAST_UPDATED + " < ?"
            + " OR " + DevicesContentProvider.ATTR_LAST_UPDATED + " > ?";
    final String[] whereArgs = {
            Long.toString((elapsedTime - threshold)),
            Long.toString((elapsedTime + threshold)) };
    context.getContentResolver().delete(
            DevicesContentProvider.DEVICES_CONTENT_URI, whereClause, whereArgs);
  }

  public static void insertDevice(final Context context, final UUID deviceId, final String name,
                                  final String type, final int state) {
    final ContentValues values = new ContentValues();
    values.put(DevicesContentProvider.ATTR_DEVICE_ID, deviceId.toString());
    values.put(DevicesContentProvider.ATTR_NAME, name);
    values.put(DevicesContentProvider.ATTR_TYPE, type);
    values.put(DevicesContentProvider.ATTR_STATE, state);
    values.put(DevicesContentProvider.ATTR_LAST_UPDATED, SystemClock.elapsedRealtime());
    context.getContentResolver().insert(DevicesContentProvider.DEVICES_CONTENT_URI, values);
  }

  public static void updateDeviceState(final Context context, final String deviceId, final int state) {
    final ContentValues values = new ContentValues();
    values.put(DevicesContentProvider.ATTR_STATE, state);
    context.getContentResolver().update(deviceUri(deviceId), values, null, null);
  }

  public static Uri deviceUri(final UUID deviceId) {
    return deviceUri(deviceId.toString());
  }

  public static Uri deviceUri(final String deviceId) {
    return DEVICES_CONTENT_URI.buildUpon().appendEncodedPath(deviceId).build();
  }
}
