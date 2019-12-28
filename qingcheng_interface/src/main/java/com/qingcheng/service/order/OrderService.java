package com.qingcheng.service.order;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderInfo;

import java.util.*;

/**
 * order业务逻辑层
 */
public interface OrderService {

    List<Order> findAll();

    PageResult<Order> findPage(int page, int size);

    List<Order> findList(Map<String,Object> searchMap);

    PageResult<Order> findPage(Map<String,Object> searchMap,int page, int size);

    Order findById(String id);

    Map<String,Object> add(Order order);

    void update(Order order);

    void delete(String id);

    OrderInfo findOrderInfoById(String orderId);

    void batchSend(List<Order> orders);

    void orderTimeOutLogic();

    void updatePayStatus(String orderId,String transactionId);
}
