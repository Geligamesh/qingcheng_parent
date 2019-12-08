package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.system.LoginLog;
import com.qingcheng.service.system.LoginLogService;
import com.qingcheng.util.WebUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    @Reference
    private LoginLogService loginLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //登录成功后会调用
        System.out.println("登录成功了，我要配置登录日志");

        LoginLog loginLog = new LoginLog();
        //当前登录用户
        loginLog.setLoginName(authentication.getName());
        //当前登录时间
        loginLog.setLoginTime(new Date());
        //远程客户端ip
        loginLog.setIp(request.getRemoteAddr());
        loginLog.setLocation(WebUtil.getCityByIP(request.getRemoteAddr()));
        loginLog.setBrowserName(WebUtil.getBrowserName(request.getHeader("User-Agent")));

        loginLogService.add(loginLog);

        request.getRequestDispatcher("/main.html").forward(request, response);


    }
}
