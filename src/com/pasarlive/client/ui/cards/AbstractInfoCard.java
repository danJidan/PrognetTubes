package com.pasarlive.client.ui.cards;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Base panel for displaying label/value pairs in card style.
 */
public abstract class AbstractInfoCard extends JPanel {
    private final JLabel titleLabel;
    private final JLabel valueLabel;

    protected AbstractInfoCard(String title, String value) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        titleLabel = createLabel(title, new Font("Segoe UI", Font.PLAIN, 11));
        valueLabel = createLabel(value, new Font("Segoe UI", Font.BOLD, 14));

        add(titleLabel);
        add(Box.createVerticalStrut(5));
        add(valueLabel);
    }

    private JLabel createLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    protected void applyColors(Color background, Color borderColor, Color titleColor, Color valueColor) {
        setBackground(background);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            new EmptyBorder(10, 10, 10, 10)
        ));
        titleLabel.setForeground(titleColor);
        valueLabel.setForeground(valueColor);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public String getValue() {
        return valueLabel.getText();
    }
}
