package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderConfigMapper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderLogMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.*;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.util.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderLogMapper orderLogMapper;
    @Autowired
    private OrderConfigMapper orderConfigMapper;
    @Autowired
    private CartService cartService;
    @Reference
    private SkuService skuService;
    @Autowired
    private IdWorker idWorker;


    private Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /**
     * 返回全部记录
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<>(orders.getTotal(), orders.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<>(orders.getTotal(), orders.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param order
     */
    public Map<String,Object> add(Order order) {
        //获取选中的购物车
        List<Map<String, Object>> cartList = cartService.findNewOrderItemList(order.getUsername());
        List<OrderItem> orderItemList = cartList.stream().filter(cart -> (boolean) cart.get("checked"))
                .map(cart -> (OrderItem) cart.get("item"))
                .collect(Collectors.toList());
        //扣减库存
        if (!skuService.duductionStock(orderItemList)) {
            throw new RuntimeException("库存扣减失败");
        }
        //保存订单主表
        order.setId(idWorker.nextId() + "");
        IntStream numStream = orderItemList.stream().mapToInt(OrderItem::getNum);
        IntStream moneyStream = orderItemList.stream().mapToInt(OrderItem::getMoney);

        int totalNum = numStream.sum();
        int totalMoney = moneyStream.sum();
        //满减优惠
        int preMoney = cartService.preferential(order.getUsername());
        //总数量
        order.setTotalNum(totalNum);
        //总金额
        order.setTotalMoney(totalMoney);
        order.setPreMoney(preMoney);
        //支付金额
        order.setPayMoney(totalMoney - preMoney);
        order.setCreateTime(new Date());
        //订单状态
        order.setOrderStatus("0");
        //支付状态
        order.setPayStatus("0");
        //发货状态
        order.setConsignStatus("0");
        orderMapper.insertSelective(order);

        //打折比例
        double proportion = (double)order.getPayMoney() / order.getTotalMoney();

        //保存订单明细表
        for (OrderItem orderItem : orderItemList) {
            orderItem.setId(idWorker.nextId() + "");
            orderItem.setOrderId(order.getId());
            orderItem.setPayMoney((int) (orderItem.getMoney() * proportion));
            orderItemMapper.insertSelective(orderItem);
        }
        //清除购物车
        cartService.deleteCheckedCart(order.getUsername());
        Map<String,Object> map = new HashMap<>();
        map.put("ordersn", order.getId());
        map.put("money", order.getPayMoney());
        return map;
    }

    /**
     * 修改
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据订单id查询订单组合实体
     * @param orderId
     * @return
     */
    public OrderInfo findOrderInfoById(String orderId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        Example orderItemExample = new Example(OrderItem.class);
        Example.Criteria criteria = orderItemExample.createCriteria();
        criteria.andEqualTo("orderId", orderId);
        //根据订单id查询订单项信息
        List<OrderItem> orderItemList = orderItemMapper.selectByExample(orderItemExample);
        if (orderItemList == null || orderItemList.size() == 0) {
            orderItemList = new ArrayList<>();
            log.info("订单id：{} 没有订单项",orderId);
        }
        return new OrderInfo(order,orderItemList);
    }

    /**
     * 批量发货
     * @param orders
     */
    public void batchSend(List<Order> orders) {
        //判断运单号和物流公司是否为空
        for (Order order : orders) {
            if (order.getShippingCode() == null || order.getShippingName() == null) {
                throw new RuntimeException("请选择快递公司和填写快递单号");
            }
        }
        //循环订单
        for (Order order : orders) {
            Date date = new Date();
            //订单状态 已经发货
            order.setOrderStatus("3");
            //发货状态 已经发货
            order.setConsignStatus("2");
            order.setConsignTime(date);
            order.setUpdateTime(date);
            orderMapper.updateByPrimaryKeySelective(order);
            //记录订单日志
            OrderLog orderLog = new OrderLog();
            //设置操作员
            orderLog.setOperater("admin");
            //设置操作时间
            orderLog.setOperateTime(new Date());
            //订单ID
            orderLog.setOrderId(order.getId());
            //订单装填
            orderLog.setOrderStatus(order.getOrderStatus());
            //设置支付状态
            orderLog.setPayStatus(order.getPayStatus());
            //设置发货状态
            orderLog.setConsignStatus(order.getConsignStatus());
            //设置备注
            orderLog.setRemarks(order.getBuyerMessage());
            orderLogMapper.insertSelective(orderLog);
        }
    }

    /**
     * 订单超时处理逻辑
     */
    public void orderTimeOutLogic() {
        //订单超时未付款，自动关闭
        //查询超时时间
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);
        //超时时间 60分钟
        Integer orderTimeout = orderConfig.getOrderTimeout();
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(orderTimeout);

        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andLessThan("createTime", localDateTime);
        //未付款的订单
        criteria.andEqualTo("orderStatus", "0");
        //未被删除的订单
        criteria.andEqualTo("isDelete", "0");

        List<Order> orders = orderMapper.selectByExample(example);
        orders.forEach(order -> {
            //记录订单变动日志
            OrderLog orderLog = new OrderLog();
            //系统
            orderLog.setOperater("system");
            //操作时间
            orderLog.setOperateTime(new Date());
            //将订单设置成已关闭
            orderLog.setOrderStatus("4");
            orderLog.setPayStatus(order.getPayStatus());
            orderLog.setConsignStatus(order.getConsignStatus());
            orderLog.setRemarks("超时订单，系统自动关闭");
            orderLog.setOrderId(order.getId());
            orderLogMapper.insertSelective(orderLog);
            //订单状态为已关闭
            order.setOrderStatus("4");
            //关闭时间
            order.setCloseTime(new Date());
            //将订单设置成已删除
            order.setIsDelete("1");

            orderMapper.updateByPrimaryKeySelective(order);

        });
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andLike("payType","%"+searchMap.get("payType")+"%");
            }
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
            }
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
            }
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
            }
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
            }
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
            }
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
            }
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
            }
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andLike("orderStatus","%"+searchMap.get("orderStatus")+"%");
            }

            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andLike("payStatus","%"+searchMap.get("payStatus")+"%");
            }
            //根据id数组查询
            if (searchMap.get("ids") != null) {
                criteria.andIn("ids", Arrays.asList((String[])searchMap.get("ids")));
            }
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andLike("consignStatus","%"+searchMap.get("consignStatus")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }

        }
        return example;
    }
}
