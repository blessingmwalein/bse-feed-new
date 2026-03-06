package com.bse.feed.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * MVC controller serving Thymeleaf dashboard pages.
 */
@Controller
public class DashboardController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/orderbook")
    public String orderbook() {
        return "orderbook";
    }

    @GetMapping("/replay")
    public String replay() {
        return "replay";
    }
}
