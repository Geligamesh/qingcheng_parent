package com.qingcheng.pojo.order;

import java.util.List;

public class OrderInfo {

    private Order order;
    private List<OrderItem> orderItemList;

    public OrderInfo() {
    }

    public OrderInfo(Order order, List<OrderItem> orderItemList) {
        this.order = order;
        this.orderItemList = orderItemList;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public List<OrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }
}
