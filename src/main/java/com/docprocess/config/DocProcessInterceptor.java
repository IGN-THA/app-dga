package com.docprocess.config;

import com.docprocess.model.AppLogin;
import com.docprocess.repository.AppLoginRepostiroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


@Component
public class DocProcessInterceptor implements HandlerInterceptor {

    @Autowired
    AppLoginRepostiroy appLoginRepostiroy;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String headerName = request.getHeader("Authorization");
        if(headerName==null) return false;
        String loginData[] = headerName.split("/");
        if(loginData!=null && loginData.length==2){
            String appKey = loginData[0];
            String sercretKey = loginData[1];
            AppLogin appLog = appLoginRepostiroy.findByAppKey(appKey);
            if(appLog==null || !sercretKey.equals(appLog.getSecretKey())) return false;
        }else{
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {}

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) throws Exception {}

}

