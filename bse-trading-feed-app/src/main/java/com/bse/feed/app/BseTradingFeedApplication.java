package com.bse.feed.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BSE Trading Feed Application - Main entry point.
 * 
 * Connects to the Botswana Stock Exchange (BSE) Millennium Exchange
 * market data feed via FAST/FIX protocol over UDP multicast,
 * maintains real-time order books, and serves a web dashboard.
 */
@SpringBootApplication(scanBasePackages = {
    "com.bse.feed.core",
    "com.bse.feed.gateway",
    "com.bse.feed.web",
    "com.bse.feed.app"
})
@EnableScheduling
public class BseTradingFeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(BseTradingFeedApplication.class, args);
    }
}
