package com.qingcheng.service.order;

import java.util.List;
import java.util.Map;

/**
 * 服务器服务
 */
public interface CartService {

    /**
     * 从redis中提取某用户的购物车
     * @param username
     * @return
     */
    List<Map<String,Object>> findCartList(String username);

    /**
     * 添加商品到购物车
     * @param username 用户名
     * @param skuId 商品id
     * @param num 数量
     */
    void addItem(String username,String skuId,Integer num);

}
