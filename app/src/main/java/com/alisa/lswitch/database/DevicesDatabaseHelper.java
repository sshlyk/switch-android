package com.alisa.lswitch.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DevicesDatabaseHelper extends SQLiteOpenHelper {
  public static final String TABLE_DEVICES = "devices";
  public static final int VERSION = 20;

  public DevicesDatabaseHelper(Context context) {
    super(context, TABLE_DEVICES, null, VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    final String createTableSql = String.format(
        "CREATE TABLE %s (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "device_id TEXT," +
            "name TEXT," +
            "type TEXT," +
            "state INTEGER," +
            "last_updated INTEGER," +
            "UNIQUE (device_id) ON CONFLICT REPLACE" +
            ");",
        TABLE_DEVICES
    );

    final String createDeviceIndex = String.format(
        "CREATE INDEX device_uuid_idx ON %s (device_id);",
        TABLE_DEVICES
    );

    db.beginTransaction();
    try {
      db.execSQL(createTableSql);
      db.execSQL(createDeviceIndex);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.beginTransaction();
    try {
      db.execSQL(String.format("DROP TABLE IF EXISTS %s;", TABLE_DEVICES));
      onCreate(db);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}
