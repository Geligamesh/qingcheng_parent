package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("wxpay")
public class WxPayController {

    @Reference
    private OrderService orderService;
    @Reference
    private WxPayService wxPayService;

    @GetMapping("createNative")
    public Map createNative(String orderId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (StringUtils.isBlank(username)) {
            throw new RuntimeException("用户未登录");
        }
        Order order = orderService.findById(orderId);
        if (order != null) {
            if ("0".equals(order.getPayStatus()) && "0".equals(order.getOrderStatus()) && username.equals(order.getUsername())) {
                return wxPayService.createNative(orderId, order.getPayMoney(), "http://geligamesh.natapp1.cc/wxpay/notify.do");
            }
            return null;
        }else {
            return null;
        }
    }

    @RequestMapping("notify")
    public void notifyLogic() {
        System.out.println("支付成功，执行回调......");
    }
}
