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

    /**
     * 更新选中状态
     * @param username
     * @param skuId
     * @param checked
     * @return
     */
    boolean updateChecked(String username,String skuId,boolean checked);

    /**
     * 删除选中的购物车
     * @param username
     */
    void deleteCheckedCart(String username);

    /**
     * 计算购物车的优惠金额
     * @param username
     * @return
     */
    int preferential(String username);

    /**
     * 获取最新购物车列表
     * @param username
     * @return
     */
    List<Map<String,Object>> findNewOrderItemList(String username);
}
