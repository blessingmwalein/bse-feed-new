package com.bse.feed.web.controller;

import com.bse.feed.core.engine.OrderBookManager;
import com.bse.feed.core.model.FeedStatus;
import com.bse.feed.core.model.OrderBook;
import com.bse.feed.core.model.OrderBookLevel;
import com.bse.feed.core.model.RecentMessageLog;
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
     * GET /api/messages - Get recent decoded message log for diagnostics.
     * Shows ALL messages including heartbeats, with full decode details.
     */
    @GetMapping("/messages")
    public ResponseEntity<List<Map<String, Object>>> getRecentMessages(
            @RequestParam(defaultValue = "200") int limit) {
        RecentMessageLog log = gatewayService.getMessageLog();
        if (log == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (RecentMessageLog.LogEntry entry : log.getRecent(limit)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("time", entry.getTimestampFormatted());
            map.put("feed", entry.getFeed());
            map.put("type", entry.getType());
            map.put("templateId", entry.getTemplateId());
            map.put("applId", entry.getApplId());
            map.put("seqNum", entry.getSeqNum());
            map.put("symbol", entry.getSymbol());
            map.put("details", entry.getDetails());
            result.add(map);
        }
        return ResponseEntity.ok(result);
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
}
