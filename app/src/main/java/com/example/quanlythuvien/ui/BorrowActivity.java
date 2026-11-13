package com.example.quanlythuvien.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlythuvien.R;
import com.example.quanlythuvien.data.LibraryDb;
import com.example.quanlythuvien.model.Book;

import java.util.ArrayList;
import java.util.List;

public class BorrowActivity extends Activity {
    private LibraryDb db;
    private EditText borrower, phone, borrowDate, dueDate;
    private Spinner bookSpinner;
    private TextView txtChooseBook;
    private Button saveButton;
    private List<Book> availableBooks = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_borrow);
        db = new LibraryDb(this);
        bookSpinner = findViewById(R.id.spinnerBorrowBook);
        txtChooseBook = findViewById(R.id.txtNoAvailableBook);
        saveButton = findViewById(R.id.btnConfirmBorrow);
        borrower = findViewById(R.id.edtBorrower);
        phone = findViewById(R.id.edtPhone);
        borrowDate = findViewById(R.id.edtBorrowDate);
        dueDate = findViewById(R.id.edtDueDate);

        borrowDate.setText(UiUtil.today());
        dueDate.setText(UiUtil.today());
        borrowDate.setInputType(InputType.TYPE_NULL);
        dueDate.setInputType(InputType.TYPE_NULL);
        borrowDate.setOnClickListener(v -> UiUtil.showDatePicker(this, borrowDate));
        dueDate.setOnClickListener(v -> UiUtil.showDatePicker(this, dueDate));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> save());
    }

    @Override protected void onResume() {
        super.onResume();
        loadAvailableBooks();
    }

    private void loadAvailableBooks() {
        availableBooks.clear();
        List<String> labels = new ArrayList<>();
        for (Book book : db.getBooks("")) {
            if (!book.borrowed) {
                availableBooks.add(book);
                labels.add(book.id + " — " + book.title);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bookSpinner.setAdapter(adapter);

        boolean hasBooks = !availableBooks.isEmpty();
        saveButton.setEnabled(hasBooks);
        txtChooseBook.setVisibility(hasBooks ? View.GONE : View.VISIBLE);
        bookSpinner.setVisibility(hasBooks ? View.VISIBLE : View.GONE);

        String preselectedId = getIntent().getStringExtra("book_id");
        if (preselectedId != null) {
            boolean found = false;
            for (int i = 0; i < availableBooks.size(); i++) {
                if (availableBooks.get(i).id.equalsIgnoreCase(preselectedId)) {
                    bookSpinner.setSelection(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Toast.makeText(this, "Sách đã được mượn hoặc không còn tồn tại", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void save() {
        int position = bookSpinner.getSelectedItemPosition();
        if (position < 0 || position >= availableBooks.size()) {
            Toast.makeText(this, "Không có sách sẵn để mượn", Toast.LENGTH_SHORT).show();
            return;
        }
        Book book = availableBooks.get(position);
        String name = borrower.getText().toString().trim();
        String phoneNumber = phone.getText().toString().trim();
        String from = borrowDate.getText().toString().trim();
        String to = dueDate.getText().toString().trim();
        if (name.isEmpty()) { borrower.setError("Nhập họ tên"); borrower.requestFocus(); return; }
        if (!phoneNumber.matches("[0-9]{9,11}")) {
            phone.setError("Số điện thoại phải có 9–11 chữ số"); phone.requestFocus(); return;
        }
        if (from.isEmpty()) { Toast.makeText(this, "Chọn ngày mượn", Toast.LENGTH_SHORT).show(); return; }
        if (to.isEmpty()) { Toast.makeText(this, "Chọn ngày dự kiến trả", Toast.LENGTH_SHORT).show(); return; }
        if (to.compareTo(from) < 0) {
            Toast.makeText(this, "Ngày trả không được trước ngày mượn", Toast.LENGTH_LONG).show();
            return;
        }
        if (!db.borrowBook(book.id, name, phoneNumber, from, to)) {
            Toast.makeText(this, "Không thể mượn: sách đã được người khác mượn", Toast.LENGTH_LONG).show();
            loadAvailableBooks();
            return;
        }
        Toast.makeText(this, "Đã tạo phiếu mượn", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
