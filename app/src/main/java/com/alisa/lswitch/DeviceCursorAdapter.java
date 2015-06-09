package com.alisa.lswitch;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Movie;
import android.graphics.drawable.AnimationDrawable;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.alisa.lswitch.content_providers.DevicesContentProvider;

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
    if ("switch".equals(type) && state == 1) {
      view.setBackgroundColor(view.getResources().getColor(R.color.Yellow));
    }
  }
}
