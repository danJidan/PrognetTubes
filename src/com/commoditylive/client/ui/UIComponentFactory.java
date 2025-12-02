package com.commoditylive.client.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public final class UIComponentFactory {
    // Palet Warna Elegan
    public static final Color COLOR_PRIMARY = new Color(44, 62, 80);    // Charcoal (Gelap)
    public static final Color COLOR_ACCENT = new Color(211, 84, 0);     // Burnt Orange (Mewah)
    public static final Color COLOR_SUCCESS = new Color(39, 174, 96);   // Hijau Elegan
    public static final Color COLOR_DANGER = new Color(192, 57, 43);    // Merah Elegan
    public static final Color COLOR_BG_MAIN = new Color(236, 240, 241); // Abu-abu sangat muda (Background)
    public static final Color COLOR_WHITE = Color.WHITE;

    private UIComponentFactory() {}

    public static JPanel createPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_BG_MAIN);
        return panel;
    }

    // Panel putih dengan efek "Kartu" (Bayangan halus berupa border)
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 220, 220), 1, true), // Rounded border halus
            new EmptyBorder(15, 15, 15, 15) // Padding dalam
        ));
        return panel;
    }

    public static void applyHeaderStyle(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setForeground(COLOR_PRIMARY);
    }

    public static void applyModernButtonStyle(JButton button, Color bgColor) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));
        
        // Efek Hover Sederhana
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }
}
