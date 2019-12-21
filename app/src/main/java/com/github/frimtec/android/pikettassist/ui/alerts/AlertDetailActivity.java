package com.github.frimtec.android.pikettassist.ui.alerts;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.frimtec.android.pikettassist.R;
import com.github.frimtec.android.pikettassist.domain.Alert;
import com.github.frimtec.android.pikettassist.domain.Alert.AlertCall;
import com.github.frimtec.android.pikettassist.state.DbFactory;

import org.threeten.bp.Instant;

import java.util.ArrayList;
import java.util.List;

import static com.github.frimtec.android.pikettassist.state.DbFactory.Mode.READ_ONLY;
import static com.github.frimtec.android.pikettassist.state.DbHelper.BOOLEAN_TRUE;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_CALL;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_CALL_COLUMN_ALERT_ID;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_CALL_COLUMN_MESSAGE;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_CALL_COLUMN_TIME;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_CONFIRM_TIME;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_END_TIME;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_ID;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_IS_CONFIRMED;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_START_TIME;

public class AlertDetailActivity extends AppCompatActivity {

  public static final String EXTRA_ALERT_ID = "alertId";

  private final DbFactory dbFactory;

  public AlertDetailActivity() {
    this(DbFactory.instance());
  }

  AlertDetailActivity(DbFactory dbFactory) {
    this.dbFactory = dbFactory;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_alert_detail);

    Bundle b = getIntent().getExtras();
    if (b != null) {
      long alertId = b.getLong(EXTRA_ALERT_ID);
      Alert alert = loadAlert(alertId);
      ListView listView = findViewById(R.id.alert_call_list);
      ArrayAdapter<AlertCall> adapter = new AlertCallArrayAdapter(this, alert.getCalls());

      View headerView = getLayoutInflater().inflate(R.layout.activity_alert_detail_header, listView, false);
      TextView timeWindow = headerView.findViewById(R.id.alert_detail_header_time_window);
      TextView currentState = headerView.findViewById(R.id.alert_detail_header_current_state);
      TextView durations = headerView.findViewById(R.id.alert_detail_header_durations);

      timeWindow.setText(AlertViewHelper.getTimeWindow(alert));
      currentState.setText(AlertViewHelper.getState(this, alert));
      durations.setText(AlertViewHelper.getDurations(this, alert));

      listView.addHeaderView(headerView);
      listView.setAdapter(adapter);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      // Override home navigation button to call onBackPressed (b/35152749).
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private Alert loadAlert(long id) {
    try (SQLiteDatabase db = dbFactory.getDatabase(READ_ONLY);
         Cursor alertCursor = db.rawQuery("SELECT " + TABLE_ALERT_COLUMN_START_TIME + ", " + TABLE_ALERT_COLUMN_CONFIRM_TIME + ", " + TABLE_ALERT_COLUMN_END_TIME + ", " + TABLE_ALERT_COLUMN_IS_CONFIRMED + " FROM " + TABLE_ALERT + " where " + TABLE_ALERT_COLUMN_ID + "=?", new String[]{String.valueOf(id)});
         Cursor callCursor = db.rawQuery("SELECT " + TABLE_ALERT_CALL_COLUMN_TIME + ", " + TABLE_ALERT_CALL_COLUMN_MESSAGE + " FROM " + TABLE_ALERT_CALL + " where " + TABLE_ALERT_CALL_COLUMN_ALERT_ID + "=? order by " + TABLE_ALERT_CALL_COLUMN_TIME, new String[]{String.valueOf(id)})) {
      if (alertCursor != null && alertCursor.getCount() > 0 && alertCursor.moveToFirst()) {
        List<AlertCall> calls = new ArrayList<>();
        if (callCursor != null && callCursor.getCount() > 0 && callCursor.moveToFirst()) {
          do {
            Instant time = Instant.ofEpochMilli(callCursor.getLong(0));
            String message = callCursor.getString(1);
            calls.add(new AlertCall(time, message));
          } while (callCursor.moveToNext());
        }
        return new Alert(
            id,
            Instant.ofEpochMilli(alertCursor.getLong(0)),
            alertCursor.getLong(1) > 0 ? Instant.ofEpochMilli(alertCursor.getLong(1)) : null,
            alertCursor.getInt(3) == BOOLEAN_TRUE,
            alertCursor.getLong(2) > 0 ? Instant.ofEpochMilli(alertCursor.getLong(2)) : null,
            calls);
      } else {
        throw new IllegalStateException("No alert found with id: " + id);
      }
    }
  }

}
