package com.alisa.lswitch;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;

import com.alisa.lswitch.database.DevicesContentProvider;

public class DeviceCursorAdapter extends SimpleCursorAdapter {
  private static final String[] from = new String[] {
      DevicesContentProvider.ATTR_NAME,
      DevicesContentProvider.ATTR_TYPE,
      DevicesContentProvider.ATTR_DEVICE_ID,
      DevicesContentProvider.ATTR_STATE
  };
  private static final int[] to = new int[] {
      R.id.device_name
  };

  public DeviceCursorAdapter(Context context) {
    super(context, R.layout.device, null, from, to, 0);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    super.bindView(view, context, cursor);
    final String name = cursor.getString(cursor.getColumnIndex(DevicesContentProvider.ATTR_NAME));
    final String type = cursor.getString(cursor.getColumnIndex(DevicesContentProvider.ATTR_TYPE));
    final String deviceId = cursor.getString(cursor.getColumnIndex(DevicesContentProvider.ATTR_DEVICE_ID));
    final int state = cursor.getInt(cursor.getColumnIndex(DevicesContentProvider.ATTR_STATE));

    view.setTag(R.integer.tag_device_id, deviceId);
    view.setTag(R.integer.tag_state, state);
    view.setTag(R.integer.tag_type, type);
    view.setActivated("switch".equals(type) && state > 0);
  }
}
