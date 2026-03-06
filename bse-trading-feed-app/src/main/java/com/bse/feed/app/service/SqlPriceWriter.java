package com.bse.feed.app.service;

import com.bse.feed.core.engine.OrderBookManager;
import com.bse.feed.core.model.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes market data prices to SQL Server CompanyPrices table.
 * 
 * Periodically batches order book snapshots and upserts them
 * into [BO].[dbo].[CompanyPrices] table at the configured interval.
 */
@Service
public class SqlPriceWriter {

    private static final Logger log = LoggerFactory.getLogger(SqlPriceWriter.class);

    private final JdbcTemplate jdbcTemplate;
    private final OrderBookManager orderBookManager;

    @Value("${bse.feed.sql.enabled:true}")
    private boolean enabled;

    @Value("${bse.feed.sql.table:[BO].[dbo].[CompanyPrices]}")
    private String tableName;

    // Track which symbols have been updated since last write
    private final ConcurrentHashMap<String, Boolean> dirtySymbols = new ConcurrentHashMap<>();

    public SqlPriceWriter(JdbcTemplate jdbcTemplate, OrderBookManager orderBookManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderBookManager = orderBookManager;
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            log.info("SQL Price Writer initialized - target table: {}", tableName);
            // Register as order book update listener to track dirty symbols
            orderBookManager.addUpdateListener((symbol, type) -> {
                dirtySymbols.put(symbol, true);
            });
        } else {
            log.info("SQL Price Writer is DISABLED");
        }
    }

    /**
     * Periodically writes dirty order book prices to SQL Server.
     * Runs at the configured batch interval (default 1000ms).
     */
    @Scheduled(fixedDelayString = "${bse.feed.sql.batch-interval-ms:1000}")
    public void writePrices() {
        if (!enabled || dirtySymbols.isEmpty()) return;

        // Swap out the dirty set
        Map<String, Boolean> toWrite = new ConcurrentHashMap<>(dirtySymbols);
        dirtySymbols.clear();

        int updated = 0;
        for (String symbol : toWrite.keySet()) {
            try {
                OrderBook book = orderBookManager.getBook(symbol);
                if (book == null) continue;

                BigDecimal lastPrice = book.getLastTradePrice();
                BigDecimal highPrice = book.getHighPrice();
                BigDecimal lowPrice = book.getLowPrice();
                BigDecimal openPrice = book.getOpenPrice();
                BigDecimal previousClose = book.getPreviousClosePrice();
                BigDecimal totalVolume = book.getTotalTradedVolume();
                BigDecimal vwap = book.getVwap();
                BigDecimal bestBid = book.getBestBid();
                BigDecimal bestOffer = book.getBestOffer();
                BigDecimal netChange = book.getNetChange();
                BigDecimal percentChange = book.getPercentChange();

                // MERGE (upsert) into CompanyPrices
                String sql = String.format(
                    "MERGE %s AS target " +
                    "USING (SELECT ? AS Symbol) AS source " +
                    "ON target.Symbol = source.Symbol " +
                    "WHEN MATCHED THEN UPDATE SET " +
                    "  LastPrice = ?, HighPrice = ?, LowPrice = ?, OpenPrice = ?, " +
                    "  PreviousClose = ?, Volume = ?, VWAP = ?, " +
                    "  BidPrice = ?, AskPrice = ?, " +
                    "  NetChange = ?, PercentChange = ?, LastUpdate = ? " +
                    "WHEN NOT MATCHED THEN INSERT " +
                    "  (Symbol, LastPrice, HighPrice, LowPrice, OpenPrice, " +
                    "   PreviousClose, Volume, VWAP, BidPrice, AskPrice, " +
                    "   NetChange, PercentChange, LastUpdate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                    tableName
                );

                LocalDateTime now = LocalDateTime.now();
                jdbcTemplate.update(sql,
                    // USING source
                    symbol,
                    // UPDATE SET
                    lastPrice, highPrice, lowPrice, openPrice,
                    previousClose, totalVolume, vwap,
                    bestBid, bestOffer,
                    netChange, percentChange, now,
                    // INSERT VALUES
                    symbol, lastPrice, highPrice, lowPrice, openPrice,
                    previousClose, totalVolume, vwap,
                    bestBid, bestOffer,
                    netChange, percentChange, now
                );
                updated++;
            } catch (Exception e) {
                log.warn("Failed to write price for {}: {}", symbol, e.getMessage());
            }
        }

        if (updated > 0) {
            log.debug("Wrote {} symbol prices to SQL Server", updated);
        }
    }
}
