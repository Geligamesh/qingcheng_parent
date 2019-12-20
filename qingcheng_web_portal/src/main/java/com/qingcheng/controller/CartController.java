package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.service.order.CartService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("cart")
public class CartController {

    @Reference
    private CartService cartService;

    @GetMapping("findCartList")
    public List<Map<String,Object>> findCartList() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return cartService.findCartList(username);
    }

    @GetMapping("addItem")
    public Result addItem(String skuId,Integer num) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.addItem(username, skuId, num);
        return new Result();
    }
}
