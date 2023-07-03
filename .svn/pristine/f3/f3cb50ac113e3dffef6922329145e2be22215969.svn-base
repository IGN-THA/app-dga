package com.docprocess.filter;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        httpServletResponse.setHeader(
                "strict-transport-security", "max-age=31536000;includeSubDomains;preload");
        httpServletResponse.setHeader(
                "x-frame-options", "SAMEORIGIN");
        httpServletResponse.setHeader(
                "x-content-type-options", "nosniff");
        httpServletResponse.setHeader("x-xss-protection", "1; mode=block");
        httpServletResponse.setHeader("referrer-policy", "strict-origin");
        httpServletResponse.setHeader("permissions-policy", "fullscreen=(),geolocation=*");
        httpServletResponse.setHeader("expect-ct", "enforce, max-age=43200");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        //Filter.super.destroy();
    }
}
