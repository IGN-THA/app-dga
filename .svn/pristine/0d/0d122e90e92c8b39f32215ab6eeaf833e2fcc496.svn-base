package com.docprocess.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        return "Not valid request";
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}