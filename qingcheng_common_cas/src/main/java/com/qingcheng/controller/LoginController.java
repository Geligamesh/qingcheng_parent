package com.qingcheng.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("login")
public class LoginController {

    /**
     * 获取用户名
     * @return
     */
    @GetMapping("username")
    public Map<String,String> username() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前登录用户:" + username);
        //此时为未登录
        if (username.equals("anonymousUser")) {
            username = "";
        }
        Map<String,String> map = new HashMap<String, String>();
        map.put("username", username);
        return map;
    }
}
