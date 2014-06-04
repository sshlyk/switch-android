package com.alisa.lswitch;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.alisa.lswitch.content_providers.DevicesContentProvider;

public class DeviceCursorAdapter extends SimpleCursorAdapter {
  private static final String[] from = new String[] {
      DevicesContentProvider.ATTR_DEVICE_ID,
      DevicesContentProvider.ATTR_STATE
  };
  private static final int[] to = new int[] {
      R.id.device_name,
      R.id.device_status
  };

  public DeviceCursorAdapter(Context context) {
    super(context, R.layout.device, null, from, to, 0);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    super.bindView(view, context, cursor);
    final int stateCol = cursor.getColumnIndex(DevicesContentProvider.ATTR_STATE);
    int statusBackground = R.color.LightGrey;
    if (cursor.getInt(stateCol) == 1) {
      statusBackground = R.color.Yellow;
    }
    view.findViewById(R.id.device_status).setBackgroundResource(statusBackground);

    final int lastOperationTimestampCol = cursor.getColumnIndex(
        DevicesContentProvider.ATTR_OPERATION_TIMESTAMP
    );
    final int lastOperationTimestamp = cursor.getInt(lastOperationTimestampCol);

    //TODO move to constants somewhere
    final long maxOperationInervalSec = 5;
    if (lastOperationTimestamp > 0
        && System.currentTimeMillis() - lastOperationTimestamp < maxOperationInervalSec) {
      view.setEnabled(false);
    }

  }

  @Override
  public void setViewText(TextView v, String text) {
    super.setViewText(v, convert(v.getId(), text));
  }

  private String convert(int id, String text) {
    switch (id) {
      case (R.id.device_status):
        return "0".equals(text) ? "OFF" : "ON";
      default:
        return text;
    }
  }
}
