package com.docprocess.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @Autowired
    private ErrorAttributes errorAttributes;

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<String> handleError(HttpServletRequest request, WebRequest webRequest, HttpServletResponse response) {
        HttpStatus status = getStatus(response);
        ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(webRequest, options);

        if (status == HttpStatus.NOT_FOUND) {
            return new ResponseEntity<>("404 Error - Page Not Found", HttpStatus.NOT_FOUND);
        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return new ResponseEntity<>("500 Error - Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (status == HttpStatus.METHOD_NOT_ALLOWED) {
            return new ResponseEntity<>("405 Error - Method Not Allowed", HttpStatus.METHOD_NOT_ALLOWED);
        } else {
            return new ResponseEntity<>("Error - An unexpected error occurred: " + attributes.get("message"), status);
        }
    }

    private HttpStatus getStatus(HttpServletResponse response) {
        return HttpStatus.valueOf(response.getStatus());
    }
}
