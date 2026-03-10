package com.bse.feed.web.websocket;

import com.bse.feed.core.engine.OrderBookManager;
import com.bse.feed.core.model.OrderBook;
import com.bse.feed.core.model.OrderBookLevel;
import com.bse.feed.core.model.RecentMessageLog;
import com.bse.feed.gateway.FeedGatewayService;
import com.bse.feed.web.dto.OrderBookDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Pushes order book updates and live decoded messages to WebSocket clients at a fixed interval.
 * Avoids overwhelming the dashboard with per-tick updates.
 *
 * Topics published:
 *   /topic/orderbook/{symbol}  – full order book DTO on change
 *   /topic/orderbook/updated   – set of dirty symbols
 *   /topic/messages            – array of new decoded log entries since last push
 */
@Component
@EnableScheduling
public class MarketDataWebSocketPublisher implements OrderBookManager.OrderBookUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(MarketDataWebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderBookManager orderBookManager;
    private final FeedGatewayService feedGatewayService;

    // Track which symbols have been updated since last push
    private final java.util.Set<String> dirtySymbols = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Timestamp of the newest message we have already pushed – used as a cursor
    private volatile Instant lastPushedMessageTime = Instant.EPOCH;

    public MarketDataWebSocketPublisher(SimpMessagingTemplate messagingTemplate,
                                         OrderBookManager orderBookManager,
                                         FeedGatewayService feedGatewayService) {
        this.messagingTemplate = messagingTemplate;
        this.orderBookManager = orderBookManager;
        this.feedGatewayService = feedGatewayService;
        orderBookManager.addUpdateListener(this);
        log.info("WebSocket market data publisher initialized");
    }

    @Override
    public void onOrderBookUpdate(String symbol, UpdateType type) {
        dirtySymbols.add(symbol);
    }

    /**
     * Push updated order books and new message-log entries to WebSocket clients every 500 ms.
     */
    @Scheduled(fixedRate = 500)
    public void pushUpdates() {
        // --- order book updates ---
        if (!dirtySymbols.isEmpty()) {
            java.util.Set<String> symbols = new java.util.HashSet<>(dirtySymbols);
            dirtySymbols.clear();

            for (String symbol : symbols) {
                OrderBook book = orderBookManager.getBook(symbol);
                if (book != null) {
                    OrderBookDto dto = toDto(book);
                    messagingTemplate.convertAndSend("/topic/orderbook/" + symbol, dto);
                }
            }
            messagingTemplate.convertAndSend("/topic/orderbook/updated", symbols);
        }

        // --- live message console push ---
        pushNewMessages();
    }

    /**
     * Detect messages added to the log since last push and broadcast them.
     * Entries in the log are stored newest-first; we walk the list collecting
     * everything newer than {@code lastPushedMessageTime}, then reverse so the
     * client receives them in chronological (oldest-first) order.
     */
    private void pushNewMessages() {
        RecentMessageLog messageLog = feedGatewayService.getMessageLog();
        if (messageLog == null) return;

        List<RecentMessageLog.LogEntry> all = messageLog.getAll();
        if (all.isEmpty()) return;

        Instant cursor = lastPushedMessageTime;
        Instant newLatest = cursor;
        List<Map<String, Object>> newMsgs = new ArrayList<>();

        for (RecentMessageLog.LogEntry entry : all) {
            if (!entry.getTimestamp().isAfter(cursor)) break; // rest are older
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time",       entry.getTimestampFormatted());
            m.put("feed",       entry.getFeed());
            m.put("type",       entry.getType());
            m.put("templateId", entry.getTemplateId());
            m.put("applId",     entry.getApplId());
            m.put("seqNum",     entry.getSeqNum());
            m.put("symbol",     entry.getSymbol());
            m.put("details",    entry.getDetails());
            newMsgs.add(m);
            if (entry.getTimestamp().isAfter(newLatest)) {
                newLatest = entry.getTimestamp();
            }
        }

        if (!newMsgs.isEmpty()) {
            Collections.reverse(newMsgs); // send oldest-first
            lastPushedMessageTime = newLatest;
            messagingTemplate.convertAndSend("/topic/messages", newMsgs);
        }
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
