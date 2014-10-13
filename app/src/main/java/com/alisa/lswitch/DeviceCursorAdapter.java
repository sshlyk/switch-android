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
      DevicesContentProvider.ATTR_DEVICE_ID,
      DevicesContentProvider.ATTR_STATE
  };
  private static final int[] to = new int[] {
      R.id.device_name,
      R.id.device_id,
      R.id.device_status
  };

  public DeviceCursorAdapter(Context context) {
    super(context, R.layout.device, null, from, to, 0);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    super.bindView(view, context, cursor);
    final int deviceName = cursor.getColumnIndex(DevicesContentProvider.ATTR_NAME);
    final int stateCol = cursor.getColumnIndex(DevicesContentProvider.ATTR_STATE);
    final int lastOperationTimestampCol = cursor.getColumnIndex(
        DevicesContentProvider.ATTR_OPERATION_TIMESTAMP
    );

    //TODO instead of dynamically changing the view, each switch type should has its own view
    if ("switch".equals(cursor.getInt(deviceName))) {
      int statusBackground = R.color.LightGrey;
      if (cursor.getInt(stateCol) == 1) {
        statusBackground = R.color.Yellow;
      }
      view.findViewById(R.id.device_status).setBackgroundResource(statusBackground);
    } else {
      view.findViewById(R.id.device_status).setVisibility(View.GONE);
    }

    final int lastOperationTimestamp = cursor.getInt(lastOperationTimestampCol);

    //TODO move to constants somewhere
    final long maxOperationInervalSec = 5;
    final boolean operationInProgress = lastOperationTimestamp > 0
        && System.currentTimeMillis()/1000 - lastOperationTimestamp < maxOperationInervalSec;
    view.setEnabled(!operationInProgress);

    final FrameLayout imgHolder = (FrameLayout) view.findViewById(R.id.device_status_img_holder);
    imgHolder.removeAllViews();

    if (operationInProgress) {
      final ImageView statusImage = new ImageView(context);
      statusImage.setBackgroundResource(R.drawable.gears_spinning);
      imgHolder.addView(statusImage);

      //TODO animation
      final AnimationDrawable frameAnimation = (AnimationDrawable) statusImage.getBackground();
      if (!frameAnimation.isRunning()) {
        frameAnimation.setVisible(true, true);
        frameAnimation.start();
        view.postDelayed(new Runnable() {
          @Override
          public void run() {
            frameAnimation.stop();
          }
        }, 2000); //TODO
      }
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
