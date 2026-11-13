package com.example.quanlythuvien.model;

public class Loan {
    public final long id;
    public final String bookId, bookTitle, borrower, phone, borrowDate, dueDate;

    public Loan(long id, String bookId, String bookTitle, String borrower,
                String phone, String borrowDate, String dueDate) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrower = borrower;
        this.phone = phone;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
    }
}
