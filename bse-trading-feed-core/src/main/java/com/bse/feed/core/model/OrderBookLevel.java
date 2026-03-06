package com.bse.feed.core.model;

import java.math.BigDecimal;

/**
 * Represents a single price level in the order book.
 * Each level aggregates all orders at the same price.
 */
public class OrderBookLevel {

    private int level;           // 1-based price level
    private BigDecimal price;
    private BigDecimal quantity;
    private int numberOfOrders;

    public OrderBookLevel() {}

    public OrderBookLevel(int level, BigDecimal price, BigDecimal quantity, int numberOfOrders) {
        this.level = level;
        this.price = price;
        this.quantity = quantity;
        this.numberOfOrders = numberOfOrders;
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public int getNumberOfOrders() { return numberOfOrders; }
    public void setNumberOfOrders(int numberOfOrders) { this.numberOfOrders = numberOfOrders; }

    /**
     * Returns true if this level has meaningful data (non-null price and quantity).
     */
    public boolean isValid() {
        return price != null && quantity != null && quantity.signum() > 0;
    }

    @Override
    public String toString() {
        return String.format("L%d: %s @ %s (%d orders)",
                level,
                quantity != null ? quantity.toPlainString() : "0",
                price != null ? price.toPlainString() : "0",
                numberOfOrders);
    }
}
