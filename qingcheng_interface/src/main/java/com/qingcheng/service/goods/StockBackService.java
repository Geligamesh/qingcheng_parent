package com.qingcheng.service.goods;

import com.qingcheng.pojo.order.OrderItem;

import java.util.List;

public interface StockBackService {

    void addList(List<OrderItem> orderItemList);

    void doBack();
}
