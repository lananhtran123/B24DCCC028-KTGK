package com.example.quanlythuvien.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.quanlythuvien.R;
import com.example.quanlythuvien.model.Loan;

import java.util.ArrayList;
import java.util.List;

class LoanAdapter extends BaseAdapter {
    interface ReturnListener { void onReturn(Loan loan); }

    private final LayoutInflater inflater;
    private final ReturnListener returnListener;
    private List<Loan> loans = new ArrayList<>();

    LoanAdapter(Context context, ReturnListener listener) {
        inflater = LayoutInflater.from(context);
        returnListener = listener;
    }

    void setLoans(List<Loan> items) {
        loans = items;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return loans.size(); }
    @Override public Loan getItem(int position) { return loans.get(position); }
    @Override public long getItemId(int position) { return loans.get(position).id; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView == null ? inflater.inflate(R.layout.item_loan, parent, false) : convertView;
        Loan l = getItem(position);
        ((TextView) v.findViewById(R.id.txtLoanBook)).setText(l.bookTitle + " (" + l.bookId + ")");
        ((TextView) v.findViewById(R.id.txtLoanPerson)).setText("Người mượn: " + l.borrower);
        ((TextView) v.findViewById(R.id.txtLoanDates)).setText(
                "Mượn: " + l.borrowDate + "   •   Hẹn trả: " + l.dueDate);
        Button returnButton = v.findViewById(R.id.btnReturnBook);
        returnButton.setOnClickListener(view -> returnListener.onReturn(l));
        return v;
    }
}
