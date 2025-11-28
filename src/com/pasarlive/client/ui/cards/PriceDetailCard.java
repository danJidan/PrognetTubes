package com.pasarlive.client.ui.cards;

import java.awt.Color;

/**
 * Colored card for displaying user price detail information.
 */
public class PriceDetailCard extends AbstractInfoCard {
    public PriceDetailCard(String title, String value, Color baseColor) {
        super(title, value);
        Color background = baseColor.brighter();
        Color border = baseColor;
        Color text = baseColor.darker();
        applyColors(background, border, text, text.darker());
    }
}
