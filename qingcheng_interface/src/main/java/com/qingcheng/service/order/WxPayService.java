package com.qingcheng.service.order;

import java.util.Map;

public interface WxPayService {

    /**
     * 生成微信支付二维码（同意下单）
     * @param orderId 订单号
     * @param money 金额（分）
     * @param notifyUrl 回调地址
     * @return
     */
    Map createNative(String orderId,Integer money,String notifyUrl);

}
