package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.user.User;
import com.qingcheng.service.user.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("user")
public class UserController {

    @Reference
    private UserService userService;

    /**
     * 发送短信验证码
     * @param phone
     * @return
     */
    @GetMapping("sendSms")
    public Result sendSms(@RequestParam("phone") String phone) {
        userService.sendSms(phone);
        return new Result();
    }

    @PostMapping("save")
    public Result save(@RequestBody User user,String smsCode) {
        //密码加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String newPassword = encoder.encode(user.getPassword());
        user.setPassword(newPassword);
        userService.add(user, smsCode);
        return new Result();
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // String encode = encoder.encode("123456");
        //$2a$10$ZiVgpqRfG6lcqonEesr1..O5pdlpr1oz5xfSLIeRzUuV9Cl.9HJKi
        // System.out.println(encode);
        boolean matches = encoder.matches("123456", "$2a$10$ZiVgpqRfG6lcqonEesr1..O5pdlpr1oz5xfSLIeRzUuV9Cl.9HJKi");
        System.out.println(matches);
    }
}
