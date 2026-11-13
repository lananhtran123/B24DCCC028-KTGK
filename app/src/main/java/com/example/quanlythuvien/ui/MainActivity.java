package com.example.quanlythuvien.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlythuvien.R;
import com.example.quanlythuvien.data.LibraryDb;
import com.example.quanlythuvien.model.Book;

public class MainActivity extends Activity {
    private LibraryDb db;
    private BookAdapter adapter;
    private EditText search;
    private TextView total, available, borrowed, empty;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        db = new LibraryDb(this);
        adapter = new BookAdapter(this, new BookAdapter.BookActions() {
            @Override public void onEdit(Book book) { editBook(book); }
            @Override public void onDelete(Book book) { confirmDelete(book); }
        });
        search = findViewById(R.id.edtSearch);
        total = findViewById(R.id.txtTotal);
        available = findViewById(R.id.txtAvailable);
        borrowed = findViewById(R.id.txtBorrowed);
        empty = findViewById(R.id.txtEmpty);
        ListView list = findViewById(R.id.listBooks);
        list.setAdapter(adapter);

        findViewById(R.id.btnAddBook).setOnClickListener(v ->
                startActivity(new Intent(this, BookFormActivity.class)));
        findViewById(R.id.btnBorrowBook).setOnClickListener(v ->
                startActivity(new Intent(this, BorrowActivity.class)));
        findViewById(R.id.btnLoans).setOnClickListener(v ->
                startActivity(new Intent(this, LoansActivity.class)));
        list.setOnItemClickListener((parent, v, position, id) -> showBookDetail(adapter.getItem(position)));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refresh(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        adapter.setBooks(db.getBooks(search.getText().toString()));
        int all = db.totalBooks();
        int active = db.activeLoanCount();
        total.setText(String.valueOf(all));
        borrowed.setText(String.valueOf(active));
        available.setText(String.valueOf(all - active));
        empty.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        empty.setText(all == 0 ? "Chưa có sách nào.\nHãy nhấn + Thêm sách để bắt đầu."
                : "Không có sách phù hợp.\nThử tìm bằng tên sách hoặc tên tác giả khác.");
    }

    private void editBook(Book book) {
        if (db.getBook(book.id) == null) {
            toast("Sách này không còn tồn tại");
            refresh();
            return;
        }
        Intent intent = new Intent(this, BookFormActivity.class);
        intent.putExtra("book_id", book.id);
        startActivity(intent);
    }

    private void showBookDetail(Book book) {
        Book current = db.getBook(book.id);
        if (current == null) { toast("Sách không còn tồn tại"); refresh(); return; }
        String info = "Mã sách: " + current.id +
                "\nTên sách: " + current.title +
                "\nTác giả: " + current.author +
                "\nThể loại: " + current.genre +
                "\nTrạng thái: " + (current.borrowed ? "Đang được mượn" : "Có sẵn");
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Chi tiết sách")
                .setMessage(info)
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Sửa sách", (d, w) -> editBook(current));
        if (!current.borrowed) {
            builder.setNeutralButton("Mượn sách", (d, w) -> {
                Intent i = new Intent(this, BorrowActivity.class);
                i.putExtra("book_id", current.id);
                startActivity(i);
            });
        }
        builder.show();
    }

    private void confirmDelete(Book book) {
        Book current = db.getBook(book.id);
        if (current == null) { toast("Sách không còn tồn tại"); refresh(); return; }
        if (current.borrowed) {
            toast("Không thể xóa sách đang được mượn; hãy trả sách trước");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa sách")
                .setMessage("Bạn có chắc muốn xóa “" + current.title + "” (" + current.id + ")? " +
                        "Các phiếu mượn đã trả liên quan cũng sẽ bị xóa.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    boolean ok = db.deleteBook(current.id);
                    toast(ok ? "Đã xóa sách" : "Không thể xóa: sách đang được mượn hoặc đã bị xóa");
                    refresh();
                }).show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
