package com.pasarlive.client.ui.cards;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JLabel;

/**
 * Neutral card with optional emoji icon for admin stats.
 */
public class StatInfoCard extends AbstractInfoCard {
    public StatInfoCard(String title, String value, String emoji) {
        super(title, value);
        applyColors(Color.WHITE, new Color(230, 230, 230), Color.GRAY, Color.DARK_GRAY);
        insertEmoji(emoji);
    }

    private void insertEmoji(String emoji) {
        if (emoji == null || emoji.isEmpty()) {
            return;
        }
        JLabel iconLabel = new JLabel(emoji);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(Box.createVerticalStrut(5), 0);
        add(iconLabel, 0);
    }
}
