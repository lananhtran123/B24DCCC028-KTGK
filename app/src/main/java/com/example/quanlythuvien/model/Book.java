package com.example.quanlythuvien.model;

public class Book {
    public final String id, title, author, genre;
    public final boolean borrowed;

    public Book(String id, String title, String author, String genre, boolean borrowed) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.borrowed = borrowed;
    }
}
