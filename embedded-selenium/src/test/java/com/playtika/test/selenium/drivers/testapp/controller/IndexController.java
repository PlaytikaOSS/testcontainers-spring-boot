package com.playtika.test.selenium.drivers.testapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
    @GetMapping("/")
    public String getIndex() {
        return "index.html";
    }
}
