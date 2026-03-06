package com.bse.feed.web.controller;

import com.bse.feed.gateway.tcp.SnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for manual TCP Snapshot channel operations.
 * Allows connecting to BSE snapshot channel, requesting snapshots,
 * and monitoring the connection from the web dashboard.
 */
@RestController
@RequestMapping("/api/snapshot")
public class SnapshotController {

    private final SnapshotService snapshotService;

    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /**
     * POST /api/snapshot/connect - Connect to BSE TCP Snapshot channel
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect() {
        return ResponseEntity.ok(snapshotService.connect());
    }

    /**
     * POST /api/snapshot/disconnect - Disconnect from snapshot channel
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        return ResponseEntity.ok(snapshotService.disconnect());
    }

    /**
     * POST /api/snapshot/request?symbols=FNBB-EQO,BIHL-EQO
     * Request snapshot for specific symbols
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestSnapshot(
            @RequestParam("symbols") String symbols) {
        String[] symbolArray = symbols.split(",");
        return ResponseEntity.ok(snapshotService.requestSnapshot(symbolArray));
    }

    /**
     * POST /api/snapshot/request-all - Request snapshot for all known symbols
     */
    @PostMapping("/request-all")
    public ResponseEntity<Map<String, Object>> requestAllSnapshots() {
        // Request all instruments by sending a wildcard or known symbols
        return ResponseEntity.ok(snapshotService.requestSnapshot("*"));
    }

    /**
     * GET /api/snapshot/status - Get snapshot channel status and logs
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(snapshotService.getStatus());
    }
}
