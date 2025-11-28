package com.pasarlive.client.ui.admin;

import com.pasarlive.model.MarketDataModel;
import java.util.function.Supplier;
import com.pasarlive.client.ui.CommodityChartPanel;

public class AdminChartPanel extends CommodityChartPanel {
    public AdminChartPanel(Supplier<MarketDataModel.CommodityData> commoditySupplier) {
        super(commoditySupplier);
    }
}
