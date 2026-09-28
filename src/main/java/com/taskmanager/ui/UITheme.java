package com.taskmanager.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * UITheme
 * Pure Java styling helper replacing all external CSS files.
 *
 * Demonstrates:
 * - 100% Pure JavaFX programmatic UI styling (Zero CSS files)
 * - Cohesive color palette and typography configured directly in code
 * - Reusable UI component styling methods
 */
public final class UITheme {

    private UITheme() {}

    // Color Palette
    public static final String COLOR_PRIMARY = "#1e3c72";
    public static final String COLOR_PRIMARY_LIGHT = "#2a5298";
    public static final String COLOR_BG = "#f4f6f9";
    public static final String COLOR_CARD_BG = "#ffffff";
    public static final String COLOR_BORDER = "#e2e8f0";
    public static final String COLOR_TEXT_MAIN = "#1e293b";
    public static final String COLOR_TEXT_MUTED = "#64748b";

    public static final String COLOR_SUCCESS = "#10b981";
    public static final String COLOR_SUCCESS_LIGHT = "#ecfdf5";
    public static final String COLOR_DANGER = "#ef4444";
    public static final String COLOR_DANGER_LIGHT = "#fef2f2";
    public static final String COLOR_WARNING = "#f59e0b";
    public static final String COLOR_WARNING_LIGHT = "#fffbeb";
    public static final String COLOR_INFO = "#3b82f6";
    public static final String COLOR_INFO_LIGHT = "#eff6ff";

    // Fonts
    public static final Font FONT_HEADER_TITLE = Font.font("Segoe UI", FontWeight.BOLD, 20);
    public static final Font FONT_HEADER_SUB = Font.font("Segoe UI", FontWeight.NORMAL, 12);
    public static final Font FONT_CARD_TITLE = Font.font("Segoe UI", FontWeight.BOLD, 14);
    public static final Font FONT_CARD_VALUE = Font.font("Segoe UI", FontWeight.BOLD, 26);
    public static final Font FONT_REGULAR = Font.font("Segoe UI", FontWeight.NORMAL, 13);
    public static final Font FONT_BOLD = Font.font("Segoe UI", FontWeight.BOLD, 13);
    public static final Font FONT_SMALL = Font.font("Segoe UI", FontWeight.NORMAL, 11);
    public static final Font FONT_BADGE = Font.font("Segoe UI", FontWeight.BOLD, 11);

    /**
     * Styles a container as a modern elevated white card.
     */
    public static void styleCard(Region region) {
        region.setStyle(
                "-fx-background-color: " + COLOR_CARD_BG + ";" +
                "-fx-background-radius: 8px;" +
                "-fx-border-radius: 8px;" +
                "-fx-border-color: " + COLOR_BORDER + ";" +
                "-fx-border-width: 1px;" +
                "-fx-padding: 16px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 6, 0, 0, 2);"
        );
    }

    /**
     * Styles a primary button (Blue).
     */
    public static void stylePrimaryButton(Button button) {
        button.setFont(FONT_BOLD);
        button.setStyle(
                "-fx-background-color: #2563eb;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #1d4ed8;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #2563eb;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        ));
    }

    /**
     * Styles a secondary button (Light Slate).
     */
    public static void styleSecondaryButton(Button button) {
        button.setFont(FONT_BOLD);
        button.setStyle(
                "-fx-background-color: #e2e8f0;" +
                "-fx-text-fill: #334155;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #cbd5e1;" +
                "-fx-text-fill: #1e293b;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #e2e8f0;" +
                "-fx-text-fill: #334155;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        ));
    }

    /**
     * Styles a success button (Green).
     */
    public static void styleSuccessButton(Button button) {
        button.setFont(FONT_BOLD);
        button.setStyle(
                "-fx-background-color: " + COLOR_SUCCESS + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #059669;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + COLOR_SUCCESS + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        ));
    }

    /**
     * Styles a danger button (Red).
     */
    public static void styleDangerButton(Button button) {
        button.setFont(FONT_BOLD);
        button.setStyle(
                "-fx-background-color: " + COLOR_DANGER + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #dc2626;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + COLOR_DANGER + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-cursor: hand;"
        ));
    }

    /**
     * Creates a styled pill badge in pure Java.
     */
    public static Label createBadge(String text, String bgColor, String textColor) {
        Label badge = new Label(text);
        badge.setFont(FONT_BADGE);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-background-radius: 12px;" +
                "-fx-padding: 3px 10px;"
        );
        return badge;
    }
}
