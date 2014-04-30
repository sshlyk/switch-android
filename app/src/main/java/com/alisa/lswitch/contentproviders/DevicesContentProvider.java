package com.alisa.lswitch.contentproviders;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.alisa.lswitch.db.DevicesDatabaseHelper;

public class DevicesContentProvider extends ContentProvider {

    private DevicesDatabaseHelper dbHelper;
    private static final String AUTHORITY = "com.alisa.lswitch.devicesprovider";
    private static final String BASE_PATH = "devices";
    public static final Uri CONTENT_URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, BASE_PATH));

    @Override
    public boolean onCreate() {
        dbHelper = new DevicesDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DevicesDatabaseHelper.TABLE_NAME);
        if (projection != null) {
            final String[] projectionWithId = new String[projection.length + 1];
            projectionWithId[0] = DevicesDatabaseHelper.COLUMN_ID;
            System.arraycopy(projection, 0, projectionWithId, 1, projection.length);
            projection = projectionWithId;
        }

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
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
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
