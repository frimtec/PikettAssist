package com.github.frimtec.android.pikettassist.ui.preferences;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.MultiSelectListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.github.frimtec.android.pikettassist.R;
import com.github.frimtec.android.pikettassist.state.DbFactory;
import com.github.frimtec.android.pikettassist.state.SharedState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.frimtec.android.pikettassist.state.DbFactory.Mode.READ_ONLY;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_TEST_ALARM_STATE;
import static com.github.frimtec.android.pikettassist.state.DbHelper.TABLE_TEST_ALARM_STATE_COLUMN_ID;

public class TestContextMultiSelectListPreference extends MultiSelectListPreference {

  public TestContextMultiSelectListPreference(Context context, AttributeSet attrs) {
    this(context, attrs, DbFactory.instance());
  }

  TestContextMultiSelectListPreference(Context context, AttributeSet attrs, DbFactory dbFactory) {
    super(context, attrs);

    List<CharSequence> validEntries = new ArrayList<>();
    try (SQLiteDatabase db = dbFactory.getDatabase(READ_ONLY);
         Cursor cursor = db.query(TABLE_TEST_ALARM_STATE, new String[]{TABLE_TEST_ALARM_STATE_COLUMN_ID}, null, null, null, null, null)) {
      if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
        do {
          String id = cursor.getString(0);
          validEntries.add(id);
        } while (cursor.moveToNext());
      }
    }
    Set<String> persistedEntries = SharedState.getSuperviseTestContexts(context);
    Set<String> filteredEntries = persistedEntries.stream().filter(validEntries::contains).collect(Collectors.toSet());
    if (!filteredEntries.containsAll(persistedEntries)) {
      SharedState.setSuperviseTestContexts(context, filteredEntries);
    }
    setEntries(validEntries.toArray(new CharSequence[]{}));
    setEntryValues(validEntries.toArray(new CharSequence[]{}));
    setOnPreferenceChangeListener((preference, newValue) -> {
      String summary = newValue.toString();
      if (Set.class.isAssignableFrom(newValue.getClass())) {
        summary = toSummary((Set) newValue);
      }
      TestContextMultiSelectListPreference.this.setSummary(summary);
      return true;
    });
  }

  @Override
  public CharSequence getSummary() {
    String summary = super.getSummary().toString();
    if (summary.contains("%s")) {
      summary = toSummary(getValues());
    }
    return summary;
  }

  private String toSummary(Set<String> values) {
    String summary;
    summary = TextUtils.join(", ", values);
    if (summary.isEmpty()) {
      summary = getContext().getResources().getString(R.string.preferences_test_context_empty_selection);
    }
    return summary;
  }
}
