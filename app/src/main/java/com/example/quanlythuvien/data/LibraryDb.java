package com.example.quanlythuvien.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.quanlythuvien.model.Book;
import com.example.quanlythuvien.model.Loan;

import java.util.ArrayList;
import java.util.List;

public class LibraryDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "library.db";
    private static final int VERSION = 1;

    public LibraryDb(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE books (" +
                "id TEXT PRIMARY KEY COLLATE NOCASE, " +
                "title TEXT NOT NULL, author TEXT NOT NULL, genre TEXT NOT NULL)");
        db.execSQL("CREATE TABLE loans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "book_id TEXT NOT NULL, borrower TEXT NOT NULL, phone TEXT NOT NULL, " +
                "borrow_date TEXT NOT NULL, due_date TEXT NOT NULL, " +
                "returned INTEGER NOT NULL DEFAULT 0, return_date TEXT, " +
                "FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE)");
        db.execSQL("CREATE UNIQUE INDEX idx_unique_active_loan " +
                "ON loans(book_id) WHERE returned = 0");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No migration is required for schema v1.
    }

    private Book readBook(Cursor c) {
        return new Book(c.getString(0), c.getString(1), c.getString(2),
                c.getString(3), c.getInt(4) == 1);
    }

    private static final String BOOK_QUERY =
            "SELECT b.id, b.title, b.author, b.genre, " +
            "CASE WHEN EXISTS(SELECT 1 FROM loans l WHERE l.book_id=b.id " +
            "AND l.returned=0) THEN 1 ELSE 0 END AS borrowed " +
            "FROM books b";

    public List<Book> getBooks(String keyword) {
        List<Book> result = new ArrayList<>();
        String q = keyword == null ? "" : keyword.trim();
        String sql = BOOK_QUERY +
                " WHERE (b.title LIKE ? OR b.author LIKE ?) " +
                "ORDER BY b.title COLLATE NOCASE, b.id COLLATE NOCASE";
        try (Cursor c = getReadableDatabase().rawQuery(sql,
                new String[]{"%" + q + "%", "%" + q + "%"})) {
            while (c.moveToNext()) result.add(readBook(c));
        }
        return result;
    }

    public Book getBook(String id) {
        if (id == null) return null;
        try (Cursor c = getReadableDatabase().rawQuery(
                BOOK_QUERY + " WHERE b.id = ?", new String[]{id})) {
            return c.moveToFirst() ? readBook(c) : null;
        }
    }

    public boolean addBook(String id, String title, String author, String genre) {
        ContentValues cv = bookValues(title, author, genre);
        cv.put("id", id);
        try {
            return getWritableDatabase().insertOrThrow("books", null, cv) != -1;
        } catch (SQLiteConstraintException ex) {
            return false;
        }
    }

    // Sửa cả mã sách và các thông tin khác; giữ nguyên phiếu mượn đã có.
    // Đổi mã bằng cách thêm bản ghi cha mới, chuyển FK của các phiếu mượn,
    // sau đó xóa bản ghi cha cũ, tất cả trong cùng một transaction.
    public boolean updateBook(String oldId, String newId, String title, String author, String genre) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery("SELECT 1 FROM books WHERE id=? LIMIT 1",
                    new String[]{oldId})) {
                if (!c.moveToFirst()) return false;
            }
            if (oldId.equalsIgnoreCase(newId)) {
                ContentValues cv = bookValues(title, author, genre);
                cv.put("id", newId);
                int changed = db.update("books", cv, "id = ?", new String[]{oldId});
                if (changed != 1) return false;
                // Đồng bộ cả cách viết hoa/thường trong các phiếu mượn.
                ContentValues fk = new ContentValues();
                fk.put("book_id", newId);
                db.update("loans", fk, "book_id = ?", new String[]{oldId});
            } else {
                ContentValues cv = bookValues(title, author, genre);
                cv.put("id", newId);
                try {
                    if (db.insertOrThrow("books", null, cv) == -1) return false;
                } catch (SQLiteConstraintException ex) {
                    return false;
                }
                ContentValues fk = new ContentValues();
                fk.put("book_id", newId);
                db.update("loans", fk, "book_id = ?", new String[]{oldId});
                if (db.delete("books", "id = ?", new String[]{oldId}) != 1) return false;
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues bookValues(String title, String author, String genre) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("author", author);
        cv.put("genre", genre);
        return cv;
    }

    public boolean deleteBook(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (isBorrowed(db, id)) return false;
            int deleted = db.delete("books", "id = ?", new String[]{id});
            db.setTransactionSuccessful();
            return deleted > 0;
        } finally {
            db.endTransaction();
        }
    }

    private boolean isBorrowed(SQLiteDatabase db, String bookId) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM loans WHERE book_id=? " +
                "AND returned=0 LIMIT 1", new String[]{bookId})) {
            return c.moveToFirst();
        }
    }

    public int totalBooks() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM books", null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    public int activeLoanCount() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM loans WHERE returned=0", null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    public boolean borrowBook(String bookId, String borrower, String phone,
                              String borrowDate, String dueDate) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery("SELECT 1 FROM books WHERE id=? LIMIT 1",
                    new String[]{bookId})) {
                if (!c.moveToFirst()) return false;
            }
            if (isBorrowed(db, bookId)) return false;
            ContentValues cv = new ContentValues();
            cv.put("book_id", bookId);
            cv.put("borrower", borrower);
            cv.put("phone", phone);
            cv.put("borrow_date", borrowDate);
            cv.put("due_date", dueDate);
            cv.put("returned", 0);
            long id;
            try {
                id = db.insertOrThrow("loans", null, cv);
            } catch (SQLiteConstraintException ex) {
                return false;
            }
            if (id == -1) return false;
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public List<Loan> getActiveLoans() {
        List<Loan> result = new ArrayList<>();
        String sql = "SELECT l.id, l.book_id, b.title, l.borrower, " +
                "l.phone, l.borrow_date, l.due_date FROM loans l " +
                "JOIN books b ON b.id=l.book_id WHERE l.returned=0 " +
                "ORDER BY l.borrow_date DESC, l.id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                result.add(new Loan(c.getLong(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getString(5), c.getString(6)));
            }
        }
        return result;
    }

    public boolean returnBook(long loanId, String returnDate) {
        ContentValues cv = new ContentValues();
        cv.put("returned", 1);
        cv.put("return_date", returnDate);
        return getWritableDatabase().update("loans", cv,
                "id = ? AND returned = 0", new String[]{String.valueOf(loanId)}) == 1;
    }
}
