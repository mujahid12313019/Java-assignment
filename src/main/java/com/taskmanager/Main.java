package com.taskmanager;

/**
 * Main
 * Standard non-Application launcher class.
 *
 * In JavaFX 11+, launching an Application directly from a class that extends
 * javafx.application.Application without modular parameters can cause runtime check errors.
 * Having this separate Main launcher class delegating to MainApp.main(args)
 * is the standard industry and academic best practice.
 */
public class Main {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
