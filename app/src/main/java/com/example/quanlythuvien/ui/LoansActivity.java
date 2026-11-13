package com.example.quanlythuvien.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlythuvien.R;
import com.example.quanlythuvien.data.LibraryDb;
import com.example.quanlythuvien.model.Loan;

public class LoansActivity extends Activity {
    private LibraryDb db;
    private LoanAdapter adapter;
    private TextView empty;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_loans);
        db = new LibraryDb(this);
        adapter = new LoanAdapter(this, this::confirmReturn);
        empty = findViewById(R.id.txtNoLoans);
        ListView list = findViewById(R.id.listLoans);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, position, id) -> showLoan(adapter.getItem(position)));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNewLoan).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, BorrowActivity.class)));
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        adapter.setLoans(db.getActiveLoans());
        empty.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.txtLoanCount)).setText(
                adapter.getCount() + " phiếu đang còn hiệu lực");
    }

    private void showLoan(Loan l) {
        String details = "Mã phiếu: #" + l.id +
                "\nMã sách: " + l.bookId +
                "\nTên sách: " + l.bookTitle +
                "\nNgười mượn: " + l.borrower +
                "\nSố điện thoại: " + l.phone +
                "\nNgày mượn: " + l.borrowDate +
                "\nNgày dự kiến trả: " + l.dueDate +
                "\nTrạng thái: Đang mượn";
        new AlertDialog.Builder(this)
                .setTitle("Chi tiết phiếu mượn")
                .setMessage(details)
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Trả sách", (dialog, which) -> confirmReturn(l))
                .show();
    }

    private void confirmReturn(Loan l) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận trả sách")
                .setMessage("Đánh dấu “" + l.bookTitle + "” đã được trả?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    boolean ok = db.returnBook(l.id, UiUtil.today());
                    Toast.makeText(this, ok ? "Trả sách thành công" : "Phiếu đã được xử lý",
                            Toast.LENGTH_SHORT).show();
                    refresh();
                }).show();
    }

    @Override protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
