package com.example.quanlythuvien.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.quanlythuvien.R;
import com.example.quanlythuvien.model.Book;

import java.util.ArrayList;
import java.util.List;

class BookAdapter extends BaseAdapter {
    interface BookActions {
        void onEdit(Book book);
        void onDelete(Book book);
    }

    private final LayoutInflater inflater;
    private final BookActions actions;
    private List<Book> books = new ArrayList<>();

    BookAdapter(Context context, BookActions actions) {
        inflater = LayoutInflater.from(context);
        this.actions = actions;
    }

    void setBooks(List<Book> items) {
        books = items;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return books.size(); }
    @Override public Book getItem(int position) { return books.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView == null ? inflater.inflate(R.layout.item_book, parent, false) : convertView;
        Book b = getItem(position);
        ((TextView) v.findViewById(R.id.txtBookTitle)).setText(b.title);
        TextView initial = v.findViewById(R.id.txtBookInitial);
        String letter = (b.title != null && !b.title.trim().isEmpty()) ? b.title.trim().substring(0, 1).toUpperCase() : "B";
        initial.setText(letter);
        ((TextView) v.findViewById(R.id.txtBookCode)).setText("Mã sách: " + b.id);
        ((TextView) v.findViewById(R.id.txtBookAuthor)).setText("Tác giả: " + b.author);
        ((TextView) v.findViewById(R.id.txtBookGenre)).setText("Thể loại: " + b.genre);
        TextView status = v.findViewById(R.id.txtBookStatus);
        status.setText(b.borrowed ? "Đang được mượn" : "Có sẵn");
        status.setTextColor(v.getContext().getColor(b.borrowed ? R.color.red : R.color.green));
        status.setBackgroundResource(b.borrowed ? R.drawable.bg_pill_borrowed : R.drawable.bg_pill_available);
        Button edit = v.findViewById(R.id.btnEditBook);
        Button delete = v.findViewById(R.id.btnDeleteBook);
        edit.setOnClickListener(view -> actions.onEdit(b));
        delete.setOnClickListener(view -> actions.onDelete(b));
        return v;
    }
}
