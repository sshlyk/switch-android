package com.alisa.lswitch.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DevicesDatabaseHelper extends SQLiteOpenHelper {
  public static final String TABLE_DEVICES = "devices";
  public static final int VERSION = 8;

  public DevicesDatabaseHelper(Context context) {
    super(context, TABLE_DEVICES, null, VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    final String createTableSql = String.format(
        "CREATE TABLE %s (" +
            "_id integer primary key autoincrement, " +
            "uuid text," +
            "name text," +
            "status text," +
            "deleted integer," +
            "last_ip" +
            ");",
        TABLE_DEVICES
    );

    final String createIndex = String.format(
        "CREATE INDEX device_uuid_idx ON %s (uuid);",
        TABLE_DEVICES
    );

    db.beginTransaction();
    try {
      db.execSQL(createTableSql);
      db.execSQL(createIndex);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.beginTransaction();
    db.execSQL(String.format("DROP TABLE IF EXISTS %s;", TABLE_DEVICES));
    onCreate(db);
    db.endTransaction();
  }
}
