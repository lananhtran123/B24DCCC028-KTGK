package com.example.quanlythuvien.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class UiUtil {
    private UiUtil() { }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
    }

    static void showDatePicker(Context ctx, EditText editText) {
        Calendar today = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(ctx, (view, year, month, day) -> {
            String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            editText.setText(date);
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }
}
