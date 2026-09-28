package com.taskmanager.model;

/**
 * Category Entity Model
 *
 * Demonstrates:
 * - Relational lookup entity mapping (categories table)
 * - One-to-Many relationship (One Category -> Many Tasks)
 */
public class Category {

    private int id;
    private String name;

    public Category() {}

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
