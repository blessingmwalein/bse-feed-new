package com.bse.feed.web.websocket;

import com.bse.feed.core.engine.OrderBookManager;
import com.bse.feed.core.model.OrderBook;
import com.bse.feed.core.model.OrderBookLevel;
import com.bse.feed.web.dto.OrderBookDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pushes order book updates to WebSocket clients at a fixed interval.
 * Avoids overwhelming the dashboard with per-tick updates.
 */
@Component
@EnableScheduling
public class MarketDataWebSocketPublisher implements OrderBookManager.OrderBookUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(MarketDataWebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderBookManager orderBookManager;

    // Track which symbols have been updated since last push
    private final java.util.Set<String> dirtySymbols = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public MarketDataWebSocketPublisher(SimpMessagingTemplate messagingTemplate,
                                         OrderBookManager orderBookManager) {
        this.messagingTemplate = messagingTemplate;
        this.orderBookManager = orderBookManager;
        orderBookManager.addUpdateListener(this);
        log.info("WebSocket market data publisher initialized");
    }

    @Override
    public void onOrderBookUpdate(String symbol, UpdateType type) {
        dirtySymbols.add(symbol);
    }

    /**
     * Push updated order books to WebSocket clients every 500ms.
     */
    @Scheduled(fixedRate = 500)
    public void pushUpdates() {
        if (dirtySymbols.isEmpty()) return;

        // Swap the dirty set
        java.util.Set<String> symbols = new java.util.HashSet<>(dirtySymbols);
        dirtySymbols.clear();

        for (String symbol : symbols) {
            OrderBook book = orderBookManager.getBook(symbol);
            if (book != null) {
                OrderBookDto dto = toDto(book);
                messagingTemplate.convertAndSend("/topic/orderbook/" + symbol, dto);
            }
        }

        // Also send a summary to the "all" topic
        messagingTemplate.convertAndSend("/topic/orderbook/updated", symbols);
    }

    private OrderBookDto toDto(OrderBook book) {
        OrderBookDto dto = new OrderBookDto();
        dto.setSymbol(book.getSymbol());
        dto.setSubBookType(book.getSubBookType().name());
        dto.setRptSeq(book.getRptSeq());
        dto.setLastUpdateTime(book.getLastUpdateTime());

        OrderBookLevel[] bidLevels = book.getBidSnapshot();
        OrderBookDto.LevelDto[] bidDtos = new OrderBookDto.LevelDto[bidLevels.length];
        for (int i = 0; i < bidLevels.length; i++) {
            bidDtos[i] = new OrderBookDto.LevelDto(
                    bidLevels[i].getLevel(), bidLevels[i].getPrice(),
                    bidLevels[i].getQuantity(), bidLevels[i].getNumberOfOrders());
        }
        dto.setBids(bidDtos);

        OrderBookLevel[] offerLevels = book.getOfferSnapshot();
        OrderBookDto.LevelDto[] offerDtos = new OrderBookDto.LevelDto[offerLevels.length];
        for (int i = 0; i < offerLevels.length; i++) {
            offerDtos[i] = new OrderBookDto.LevelDto(
                    offerLevels[i].getLevel(), offerLevels[i].getPrice(),
                    offerLevels[i].getQuantity(), offerLevels[i].getNumberOfOrders());
        }
        dto.setOffers(offerDtos);

        dto.setLastPrice(book.getLastTradePrice());
        dto.setLastSize(book.getLastTradeSize());
        dto.setOpenPrice(book.getOpenPrice());
        dto.setHighPrice(book.getHighPrice());
        dto.setLowPrice(book.getLowPrice());
        dto.setClosePrice(book.getClosePrice());
        dto.setPreviousClose(book.getPreviousClosePrice());
        dto.setNetChange(book.getNetChange());
        dto.setPercentChange(book.getPercentChange());
        dto.setVwap(book.getVwap());
        dto.setTotalVolume(book.getTotalTradedVolume());
        dto.setTotalTrades(book.getTotalNumberOfTrades());
        dto.setBestBid(book.getBestBid());
        dto.setBestOffer(book.getBestOffer());
        dto.setSpread(book.getSpread());
        dto.setTradingStatus(book.getSecurityTradingStatus());

        return dto;
    }
}
