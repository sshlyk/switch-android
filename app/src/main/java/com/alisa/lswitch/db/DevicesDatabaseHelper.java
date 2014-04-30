package com.alisa.lswitch.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DevicesDatabaseHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "devices";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DEVICE_NAME = "name";
    public DevicesDatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format(
                "create table %s (_id integer primary key autoincrement, name text);",
                TABLE_NAME
        ));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(String.format("drop table %s;", TABLE_NAME));
    }
}
