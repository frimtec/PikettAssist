package com.github.frimtec.android.pikettassist.ui.alerts;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.github.frimtec.android.pikettassist.R;
import com.github.frimtec.android.pikettassist.domain.Alert;
import com.github.frimtec.android.pikettassist.domain.OnOffState;
import com.github.frimtec.android.pikettassist.service.AlarmService;
import com.github.frimtec.android.pikettassist.state.DbFactory;
import com.github.frimtec.android.pikettassist.state.SharedState;
import com.github.frimtec.android.pikettassist.ui.common.AbstractListFragment;
import com.github.frimtec.android.pikettassist.utility.Feature;
import com.github.frimtec.android.pikettassist.utility.NotificationHelper;

import org.threeten.bp.Instant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.github.frimtec.android.pikettassist.state.DbFactory.Mode.READ_ONLY;
import static com.github.frimtec.android.pikettassist.state.DbFactory.Mode.WRITABLE;
import static com.github.frimtec.android.pikettassist.state.DbHelper.BOOLEAN_TRUE;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_CONFIRM_TIME;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_END_TIME;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_ID;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_IS_CONFIRMED;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_ALERT_COLUMN_START_TIME;
import static com.github.frimtec.android.pikettassist.ui.FragmentName.ALERT_LOG;

public class AlertListFragment extends AbstractListFragment<Alert> {

  private static final int MENU_CONTEXT_VIEW_ID = 1;
  private static final int MENU_CONTEXT_DELETE_ID = 2;
  private final DbFactory dbFactory;

  public AlertListFragment() {
    this(DbFactory.instance());
  }

  @SuppressLint("ValidFragment")
  AlertListFragment(DbFactory dbFactory) {
    super(ALERT_LOG);
    this.dbFactory = dbFactory;
  }

  @Override
  protected void configureListView(ListView listView) {
    listView.setClickable(true);
    listView.setOnItemClickListener((parent, view1, position, id) -> {
      Alert selectedAlert = (Alert) listView.getItemAtPosition(position);
      showAlertDetails(selectedAlert);
    });
    registerForContextMenu(listView);
  }

  @Override
  protected ArrayAdapter<Alert> createAdapter() {
    return new AlertArrayAdapter(getContext(), loadAlertList());
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    menu.add(Menu.NONE, MENU_CONTEXT_VIEW_ID, Menu.NONE, R.string.list_item_menu_view);
    menu.add(Menu.NONE, MENU_CONTEXT_DELETE_ID, Menu.NONE, R.string.list_item_menu_delete);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    ListView listView = getListView();
    Alert selectedAlert = (Alert) listView.getItemAtPosition(info.position);
    switch (item.getItemId()) {
      case MENU_CONTEXT_VIEW_ID:
        showAlertDetails(selectedAlert);
        return true;
      case MENU_CONTEXT_DELETE_ID:
        NotificationHelper.areYouSure(getContext(), (dialog, which) -> {
          deleteAlert(selectedAlert);
          refresh();
          Toast.makeText(getContext(), R.string.general_entry_deleted, Toast.LENGTH_SHORT).show();
        }, (dialog, which) -> {
        });
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  @Override
  protected Optional<View.OnClickListener> addAction() {
    if (Feature.PERMISSION_CALENDAR_READ.isAllowed(getContext()) &&
        SharedState.getPikettState(getContext()) == OnOffState.ON) {
      return Optional.of(view -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.manually_created_alarm_reason));
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(R.string.manually_created_alarm_reason_default);
        input.requestFocus();
        builder.setView(input);
        builder.setPositiveButton(R.string.general_ok, (dialog, which) -> {
          dialog.dismiss();
          String comment = input.getText().toString();
          AlarmService alarmService = new AlarmService(getContext());
          alarmService.newManuallyAlarm(Instant.now(), comment);
          refresh();
        });
        builder.setNegativeButton(R.string.general_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
      });
    }
    return Optional.empty();
  }

  private void showAlertDetails(Alert selectedAlert) {
    Intent intent = new Intent(this.getContext(), AlertDetailActivity.class);
    Bundle bundle = new Bundle();
    bundle.putLong(AlertDetailActivity.EXTRA_ALERT_ID, selectedAlert.getId());
    intent.putExtras(bundle);
    startActivity(intent);
  }

  private void deleteAlert(Alert selectedAlert) {
    try (SQLiteDatabase db = this.dbFactory.getDatabase(WRITABLE)) {
      db.delete(TABLE_ALERT, TABLE_ALERT_COLUMN_ID + "=?", new String[]{String.valueOf(selectedAlert.getId())});
    }
  }

  private List<Alert> loadAlertList() {
    List<Alert> alertList = new ArrayList<>();
    try (SQLiteDatabase db = dbFactory.getDatabase(READ_ONLY);
         Cursor cursor = db.rawQuery("SELECT " + TABLE_ALERT_COLUMN_ID + ", " + TABLE_ALERT_COLUMN_START_TIME + ", " + TABLE_ALERT_COLUMN_CONFIRM_TIME + ", " + TABLE_ALERT_COLUMN_END_TIME + ", " + TABLE_ALERT_COLUMN_IS_CONFIRMED + " FROM " + TABLE_ALERT + " ORDER BY " + TABLE_ALERT_COLUMN_START_TIME + " DESC", null)) {
      if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
        do {
          long id = cursor.getLong(0);
          alertList.add(new Alert(
              id,
              Instant.ofEpochMilli(cursor.getLong(1)),
              cursor.getLong(2) > 0 ? Instant.ofEpochMilli(cursor.getLong(2)) : null,
              cursor.getInt(4) == BOOLEAN_TRUE,
              cursor.getLong(3) > 0 ? Instant.ofEpochMilli(cursor.getLong(3)) : null,
              Collections.emptyList()));
        } while (cursor.moveToNext());
      }
    }
    if (alertList.isEmpty()) {
      Toast.makeText(getContext(), getString(R.string.general_no_data), Toast.LENGTH_LONG).show();
    }
    return alertList;
  }
}
