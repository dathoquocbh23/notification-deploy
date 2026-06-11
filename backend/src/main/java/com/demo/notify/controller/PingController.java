package com.demo.notify.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Health-check tối giản cho nghiệm thu khởi động backend. */
@RestController
public class PingController {
    @GetMapping("/ping")
    public String ping() { return "ok"; }
}
