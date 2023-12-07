package com.github.frimtec.android.pikettassist.service.dao;

import static java.util.stream.Collectors.joining;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.github.frimtec.android.pikettassist.domain.Contact;
import com.github.frimtec.android.pikettassist.domain.ContactPerson;
import com.github.frimtec.android.pikettassist.domain.ContactReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ContactDao {

  private static final String TAG = "ContactDao";

  static final String[] PROJECTION_URI = new String[]{
      ContactsContract.Contacts._ID,
      ContactsContract.Contacts.LOOKUP_KEY,
      ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
  };

  private final ContentResolver contentResolver;

  public ContactDao(Context context) {
    this.contentResolver = context.getContentResolver();
  }

  public Optional<Contact> getContact(long id) {
    try (Cursor cursor = this.contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
        new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
        ContactsContract.Contacts._ID + " = ?",
        new String[]{String.valueOf(id)}, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        ContactReference reference = new ContactReference(id, cursor.getString(0));
        return Optional.of(new Contact(reference, true, cursor.getString(1)));
      }
    }
    return Optional.empty();
  }

  public Optional<Contact> getContact(Uri contactUri) {
    try (Cursor cursor = this.contentResolver.query(contactUri, PROJECTION_URI, null, null, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        ContactReference reference = new ContactReference(cursor.getLong(0), cursor.getString(1));
        return Optional.of(new Contact(reference, true, cursor.getString(2)));
      }
    }
    return Optional.empty();
  }

  public Map<String, ContactPerson> findContactPersonsByAliases(Set<String> aliases) {
    try (Cursor cursor = this.contentResolver.query(ContactsContract.Data.CONTENT_URI,
        new String[]{
            ContactsContract.CommonDataKinds.Nickname.DATA1,
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME
        },
        ContactsContract.CommonDataKinds.Nickname.DATA1 + " IN (" + in(aliases) + ")",
        null,
        null)) {
      if (cursor != null && cursor.moveToFirst()) {
        Map<String, ContactPerson> contactPeople = new HashMap<>();
        do {
          String nickname = cursor.getString(0);
          contactPeople.put(nickname, new ContactPerson(
              nickname,
              cursor.getLong(1),
              cursor.getString(2)
          ));
        } while (cursor.moveToNext());
        return contactPeople;
      }
      return Collections.emptyMap();
    }
  }

  private String in(Set<String> elements) {
    return elements.stream().map(element -> String.format("'%s'", element)).collect(joining(","));
  }

  public Set<Long> lookupContactIdsByPhoneNumber(String phoneNumber) {
    try (Cursor cursor = this.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID},
        ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER + " = ?",
        new String[]{phoneNumber}, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        Set<Long> contactIds = new HashSet<>();
        do {
          int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
          if (columnIndex >= 0) {
            contactIds.add(cursor.getLong(columnIndex));
          } else {
            Log.e(TAG, "Column CONTACT_ID not found in cursor");
          }
        } while (cursor.moveToNext());
        return contactIds;
      }
      return Collections.emptySet();
    }
  }

  public Set<String> getPhoneNumbers(Contact contact) {
    Set<String> phoneNumbers = new HashSet<>();

    // add real phone numbers
    try (Cursor cursor = this.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        new String[]{ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, ContactsContract.CommonDataKinds.Phone.NUMBER},
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
        new String[]{String.valueOf(contact.reference().id())}, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        do {
          int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
          String normalizedNumber = null;
          if (columnIndex >= 0) {
            normalizedNumber = cursor.getString(columnIndex);
          } else {
            Log.e(TAG, "Column NORMALIZED_NUMBER not found in cursor");
          }
          if (normalizedNumber != null) {
            phoneNumbers.add(normalizedNumber);
          } else {
            int columnIndexNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            Log.e(TAG, "Skipping phone number as normalized number is null for number: " + (columnIndexNumber >= 0 ? cursor.getString(columnIndexNumber) : "???"));
          }
        } while (cursor.moveToNext());
      }
    }

    // add alphanumeric short codes from contact organization field as comma separated list
    phoneNumbers.addAll(getAlphanumericShortCodesFromContact(contact));
    return phoneNumbers;
  }

  public Set<String> getAlphanumericShortCodesFromContact(Contact contact) {
    try (Cursor cursor = this.contentResolver.query(ContactsContract.Data.CONTENT_URI,
        null, ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?", new String[]{String.valueOf(contact.reference().id()),
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE}, null)) {
      if (cursor == null) return Collections.emptySet();
      if (cursor.moveToFirst()) {
        int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY);
        if (columnIndex >= 0) {
          String company = cursor.getString(columnIndex);
          if (!TextUtils.isEmpty(company)) {
            return Arrays.stream(company.split(","))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(companyName -> companyName.length() > 0)
                .collect(Collectors.toSet());
          }
        } else {
          Log.e(TAG, "Column COMPANY not found in cursor");
        }
      }
    }
    return Collections.emptySet();
  }
}
