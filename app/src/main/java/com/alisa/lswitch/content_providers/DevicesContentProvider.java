package com.alisa.lswitch.content_providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.alisa.lswitch.db.DevicesDatabaseHelper;

import java.util.Arrays;
import java.util.List;

public class DevicesContentProvider extends ContentProvider {

  private DevicesDatabaseHelper dbHelper;
  private static final String AUTHORITY = "com.alisa.lswitch.devicesprovider";
  private static final String DEVICES_PATH = "devices";
  public static final Uri DEVICES_CONTENT_URI =
      Uri.parse(String.format("content://%s/%s", AUTHORITY, DEVICES_PATH));

  public static final String ATTR_ID = "_id";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_DEVICE_ID = "device_id";
  public static final String ATTR_DELETED = "deleted";
  public static final String ATTR_STATUS = "status";
  public static final String ATTR_IP = "last_ip";
  public static final String ATTR_LAST_UPDATED = "last_updated";

  private static final String TAG = DevicesContentProvider.class.getSimpleName();

  @Override
  public boolean onCreate() {
    dbHelper = new DevicesDatabaseHelper(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setTables(DevicesDatabaseHelper.TABLE_DEVICES);
    if (projection != null) {
      final String[] projectionWithId = new String[projection.length + 1];
      projectionWithId[0] = ATTR_ID;
      System.arraycopy(projection, 0, projectionWithId, 1, projection.length);
      projection = projectionWithId;
    }

    List<String> segments = uri.getPathSegments();
    if (segments.size() < 1) { return null; }
    String path = segments.get(0);

    if (DEVICES_PATH.equals(path)) {
      SQLiteQueryBuilder query = new SQLiteQueryBuilder();
      query.setTables(DevicesDatabaseHelper.TABLE_DEVICES);
      if (selection != null ) { query.appendWhere(selection); }
      query.appendWhere(String.format("%s = 0", ATTR_DELETED));
      if (segments.size() > 2) {
        final String deviceId = segments.get(1);
        query.appendWhereEscapeString(String.format("%s = %s", ATTR_DEVICE_ID, deviceId));
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
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      if (values.containsKey(ATTR_DEVICE_ID)) {
        values.put(ATTR_LAST_UPDATED, System.currentTimeMillis()/1000);
        db.insert(DevicesDatabaseHelper.TABLE_DEVICES, null, values);
      }
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return null; //TODO
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    List<String> segments = uri.getPathSegments();
    if (segments.size() < 2) return 0;
    final String path = segments.get(0);

    int recordsRemoved = 0;
    if (DEVICES_PATH.equals(path)) {
      final String deviceId = segments.get(1);
      final ContentValues values = new ContentValues();
      values.put(ATTR_DELETED, 1);

      recordsRemoved = dbHelper.getWritableDatabase().update(
          DevicesDatabaseHelper.TABLE_DEVICES,
          values,
          String.format("%s = ?", ATTR_DEVICE_ID),
          new String[]{ deviceId }
      );
      if (recordsRemoved > 0) {
        Log.d(TAG, "Device has been marked as deleted: " + deviceId);
      }
    }
    return recordsRemoved;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }
}
