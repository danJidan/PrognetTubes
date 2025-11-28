package com.pasarlive.client.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.LayoutManager;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Utility factory for common Swing component styles to reduce duplication
 * across individual UI screens.
 */
public final class UIComponentFactory {
    private static final Color DEFAULT_WHITE = Color.WHITE;

    private UIComponentFactory() {
    }

    public static JPanel createWhitePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(DEFAULT_WHITE);
        return panel;
    }

    public static JPanel createWhitePanel(LayoutManager layout) {
        JPanel panel = createWhitePanel();
        if (layout != null) {
            panel.setLayout(layout);
        }
        return panel;
    }

    public static JPanel createWhitePanel(LayoutManager layout, int padding) {
        JPanel panel = createWhitePanel(layout);
        if (padding > 0) {
            panel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        }
        return panel;
    }

    public static void applyFlatButtonStyle(AbstractButton button, Font font, Color background, Color foreground) {
        if (button == null) {
            return;
        }
        if (font != null) {
            button.setFont(font);
        }
        if (background != null) {
            button.setBackground(background);
        }
        if (foreground != null) {
            button.setForeground(foreground);
        }
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
