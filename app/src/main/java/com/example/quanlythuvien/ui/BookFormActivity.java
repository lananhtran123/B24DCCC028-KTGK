package com.example.quanlythuvien.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlythuvien.R;
import com.example.quanlythuvien.data.LibraryDb;
import com.example.quanlythuvien.model.Book;

public class BookFormActivity extends Activity {
    private LibraryDb db;
    private EditText id, title, author, genre;
    private String editingId;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_book_form);
        db = new LibraryDb(this);
        id = findViewById(R.id.edtBookId);
        title = findViewById(R.id.edtBookTitle);
        author = findViewById(R.id.edtAuthor);
        genre = findViewById(R.id.edtGenre);
        editingId = getIntent().getStringExtra("book_id");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveBook).setOnClickListener(v -> save());
        if (editingId != null) {
            Book book = db.getBook(editingId);
            if (book == null) {
                Toast.makeText(this, "Không tìm thấy sách", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            ((TextView) findViewById(R.id.txtFormTitle)).setText("Chỉnh sửa thông tin sách");
            ((TextView) findViewById(R.id.txtFormNote)).setText(
                    "Có thể sửa cả mã sách. Mã mới không được trùng sách đã có.");
            ((TextView) findViewById(R.id.txtFormStatus)).setText(
                    "Trạng thái hiện tại: " + (book.borrowed ? "Đang được mượn" : "Có sẵn") +
                    " (tự động cập nhật khi mượn/trả)");
            findViewById(R.id.txtFormStatus).setVisibility(View.VISIBLE);
            ((android.widget.Button) findViewById(R.id.btnSaveBook)).setText("Lưu thay đổi");
            id.setText(book.id);
            title.setText(book.title);
            author.setText(book.author);
            genre.setText(book.genre);
        }
    }

    private void save() {
        String code = id.getText().toString().trim();
        String name = title.getText().toString().trim();
        String writer = author.getText().toString().trim();
        String category = genre.getText().toString().trim();

        if (code.isEmpty()) { id.setError("Vui lòng nhập mã sách"); id.requestFocus(); return; }
        if (code.length() > 30) { id.setError("Mã sách tối đa 30 ký tự"); id.requestFocus(); return; }
        if (name.isEmpty()) { title.setError("Vui lòng nhập tên sách"); title.requestFocus(); return; }
        if (writer.isEmpty()) { author.setError("Vui lòng nhập tác giả"); author.requestFocus(); return; }
        if (category.isEmpty()) { genre.setError("Vui lòng nhập thể loại"); genre.requestFocus(); return; }

        Book existing = db.getBook(code);
        if (existing != null && (editingId == null || !existing.id.equalsIgnoreCase(editingId))) {
            id.setError("Mã sách đã tồn tại, vui lòng nhập mã khác");
            id.requestFocus();
            return;
        }
        boolean ok = editingId == null ? db.addBook(code, name, writer, category)
                : db.updateBook(editingId, code, name, writer, category);
        if (!ok) {
            Toast.makeText(this, "Không thể lưu. Kiểm tra mã sách hoặc thử lại.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, editingId == null ? "Đã thêm sách mới" : "Đã cập nhật thông tin sách",
                Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
