package com.bse.feed.web.controller;

import com.bse.feed.core.engine.OrderBookManager;
import com.bse.feed.core.event.ActivityLog;
import com.bse.feed.core.model.FeedStatus;
import com.bse.feed.core.model.OrderBook;
import com.bse.feed.core.model.OrderBookLevel;
import com.bse.feed.gateway.FeedGatewayService;
import com.bse.feed.web.dto.FeedStatusDto;
import com.bse.feed.web.dto.OrderBookDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for market data access and monitoring.
 */
@RestController
@RequestMapping("/api")
public class MarketDataRestController {

    private final FeedGatewayService gatewayService;
    private final Instant startTime = Instant.now();

    public MarketDataRestController(FeedGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    /**
     * GET /api/symbols - List all tracked symbols
     */
    @GetMapping("/symbols")
    public ResponseEntity<Set<String>> getSymbols() {
        OrderBookManager obm = gatewayService.getOrderBookManager();
        return ResponseEntity.ok(obm.getSymbols());
    }

    /**
     * GET /api/orderbook/{symbol} - Get full order book for a symbol
     */
    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<OrderBookDto> getOrderBook(@PathVariable String symbol) {
        OrderBookManager obm = gatewayService.getOrderBookManager();
        OrderBook book = obm.getBook(symbol);

        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDto(book));
    }

    /**
     * GET /api/orderbooks - Get all order books (summary)
     */
    @GetMapping("/orderbooks")
    public ResponseEntity<List<OrderBookDto>> getAllOrderBooks() {
        OrderBookManager obm = gatewayService.getOrderBookManager();
        List<OrderBookDto> books = obm.getAllBooks().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(OrderBookDto::getSymbol))
                .collect(Collectors.toList());
        return ResponseEntity.ok(books);
    }

    /**
     * GET /api/status - Get feed connection status
     */
    @GetMapping("/status")
    public ResponseEntity<List<FeedStatusDto>> getStatus() {
        List<FeedStatusDto> statuses = new ArrayList<>();

        for (FeedStatus fs : gatewayService.getFeedStatuses()) {
            FeedStatusDto dto = new FeedStatusDto();
            dto.setChannelName(fs.getChannelName());
            dto.setState(fs.getState().name());
            dto.setLastMessageTime(fs.getLastMessageTime());
            dto.setConnectionTime(fs.getConnectionTime());
            dto.setTotalMessages(fs.getTotalMessagesReceived());
            dto.setTotalGaps(fs.getTotalGapsDetected());
            dto.setTotalDuplicates(fs.getTotalDuplicatesSkipped());
            dto.setLastSequence(fs.getLastSequenceNumber());
            dto.setHeartbeats(fs.getHeartbeatCount());
            dto.setSnapshots(fs.getSnapshotCount());
            dto.setIncrementals(fs.getIncrementalCount());
            dto.setSecurityDefinitions(fs.getSecurityDefinitionCount());
            dto.setDecodeErrors(fs.getDecodeErrorCount());
            dto.setLastError(fs.getLastError());
            dto.setAvgDecodeTimeMs(fs.getAvgDecodeTimeMs());
            dto.setMaxDecodeTimeMs(fs.getMaxDecodeTimeMs());

            OrderBookManager obm = gatewayService.getOrderBookManager();
            dto.setInstrumentCount(gatewayService.getInstrumentRegistry().getInstrumentCount());
            dto.setOrderBookCount(obm.getInstrumentCount());
            dto.setUptimeSeconds(Instant.now().getEpochSecond() - startTime.getEpochSecond());
            dto.setActiveSymbols(new ArrayList<>(obm.getSymbols()));

            statuses.add(dto);
        }

        return ResponseEntity.ok(statuses);
    }

    /**
     * GET /api/health - Simple health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", gatewayService.isRunning() ? "UP" : "DOWN");
        health.put("instruments", gatewayService.getOrderBookManager().getInstrumentCount());
        health.put("uptime", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        return ResponseEntity.ok(health);
    }

    // ==================== CONVERSION ====================

    private OrderBookDto toDto(OrderBook book) {
        OrderBookDto dto = new OrderBookDto();
        dto.setSymbol(book.getSymbol());
        dto.setSubBookType(book.getSubBookType().name());
        dto.setRptSeq(book.getRptSeq());
        dto.setLastUpdateTime(book.getLastUpdateTime());

        // Bid levels
        OrderBookLevel[] bidLevels = book.getBidSnapshot();
        OrderBookDto.LevelDto[] bidDtos = new OrderBookDto.LevelDto[bidLevels.length];
        for (int i = 0; i < bidLevels.length; i++) {
            bidDtos[i] = new OrderBookDto.LevelDto(
                    bidLevels[i].getLevel(), bidLevels[i].getPrice(),
                    bidLevels[i].getQuantity(), bidLevels[i].getNumberOfOrders());
        }
        dto.setBids(bidDtos);

        // Offer levels
        OrderBookLevel[] offerLevels = book.getOfferSnapshot();
        OrderBookDto.LevelDto[] offerDtos = new OrderBookDto.LevelDto[offerLevels.length];
        for (int i = 0; i < offerLevels.length; i++) {
            offerDtos[i] = new OrderBookDto.LevelDto(
                    offerLevels[i].getLevel(), offerLevels[i].getPrice(),
                    offerLevels[i].getQuantity(), offerLevels[i].getNumberOfOrders());
        }
        dto.setOffers(offerDtos);

        // Statistics
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

        // Spread
        dto.setBestBid(book.getBestBid());
        dto.setBestOffer(book.getBestOffer());
        dto.setSpread(book.getSpread());

        // Status
        dto.setTradingStatus(book.getSecurityTradingStatus());

        return dto;
    }

    // ==================== ACTIVITY LOG ====================

    /**
     * GET /api/activity-log?since={id} - Get feed activity log entries.
     * If 'since' is provided, returns only entries newer than that id (incremental polling).
     * Otherwise returns the most recent 50 entries.
     */
    @GetMapping("/activity-log")
    public ResponseEntity<Map<String, Object>> getActivityLog(
            @RequestParam(value = "since", required = false) Long since) {

        ActivityLog log = gatewayService.getActivityLog();
        Map<String, Object> result = new LinkedHashMap<>();

        if (log == null) {
            result.put("latestId", 0);
            result.put("entries", Collections.emptyList());
            return ResponseEntity.ok(result);
        }

        List<ActivityLog.Entry> entries;
        if (since != null && since > 0) {
            entries = log.getSince(since);
        } else {
            entries = log.getRecent(50);
        }

        // Convert to maps for JSON
        List<Map<String, Object>> entryList = new ArrayList<>();
        for (ActivityLog.Entry e : entries) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("timestamp", e.getTimestamp());
            m.put("source", e.getSource());
            m.put("type", e.getType());
            m.put("detail", e.getDetail());
            m.put("level", e.getLevel());
            entryList.add(m);
        }

        result.put("latestId", log.getLatestId());
        result.put("entries", entryList);
        return ResponseEntity.ok(result);
    }
}
