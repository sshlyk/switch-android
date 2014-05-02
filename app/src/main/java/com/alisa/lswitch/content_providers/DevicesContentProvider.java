package com.alisa.lswitch.content_providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.alisa.lswitch.db.DevicesDatabaseHelper;

public class DevicesContentProvider extends ContentProvider {

  private DevicesDatabaseHelper dbHelper;
  private static final String AUTHORITY = "com.alisa.lswitch.devicesprovider";
  private static final String DEVICES_PATH = "/devices";
  public static final Uri DEVICES_CONTENT_URI =
      Uri.parse(String.format("content://%s%s", AUTHORITY, DEVICES_PATH));

  private static final String ATTR_ID = "_id";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_UUID = "uuid";
  public static final String ATTR_STATUS = "status";
  public static final String ATTR_IP = "last_ip";

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

    String path = uri.getPath();
    if (DEVICES_PATH.equals(path)) {
      final Cursor cursor = qb.query(
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
    String path = uri.getPath();
    if (DEVICES_PATH.equals(path)) {
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      db.insert(DevicesDatabaseHelper.TABLE_DEVICES, null, values);
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return null; //TODO
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }
}
