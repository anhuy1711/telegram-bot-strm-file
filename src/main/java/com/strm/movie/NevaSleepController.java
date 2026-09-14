package com.strm.movie;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NevaSleepController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}