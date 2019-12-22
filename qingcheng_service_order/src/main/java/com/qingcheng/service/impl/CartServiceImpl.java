package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.PreferentialService;
import com.qingcheng.util.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Reference
    private SkuService skuService;
    @Reference
    private CategoryService categoryService;
    @Autowired
    private PreferentialService preferentialService;

    /**
     * 从redis中提取某用户的购物车
     * @param username
     * @return
     */
    @Override
    public List<Map<String, Object>> findCartList(String username) {
        System.out.println("从redis中提取购物车:" + username);
        List<Map<String, Object>> cartList = (List<Map<String, Object>>) redisTemplate.boundHashOps(CacheKey.CART_LIST).get(username);
        if (cartList == null) {
            cartList = new ArrayList<>();
        }
        return cartList;
    }

    /**
     * 添加商品到购物车
     * @param username 用户名
     * @param skuId 商品id
     * @param num 数量
     */
    @Override
    public void addItem(String username, String skuId, Integer num) {
        //遍历购物车，如果购物车中存在该商品则累加数量，如果不存在则添加购物车项
        //获取购物车
        boolean flag = false;//是否在购物车中存在
        List<Map<String, Object>> cartList = findCartList(username);
        for (Map<String, Object> map : cartList) {
              OrderItem orderItem = (OrderItem) map.get("item");
              if (orderItem.getSkuId().equals(skuId)) {//购物车中存在该商品
                  //单个商品重量
                  if (orderItem.getNum() <= 0) {
                      cartList.remove(map);
                      flag = true;
                      break;
                  }
                  int weight = orderItem.getWeight() / orderItem.getNum();
                  //数量变更
                  orderItem.setNum(orderItem.getNum() + num);
                  //金额变更
                  orderItem.setMoney(orderItem.getPrice() * orderItem.getNum());
                  //重量变量
                  orderItem.setWeight(weight * orderItem.getNum() );

                  if (orderItem.getNum() <= 0) {
                      cartList.remove(map);
                  }
                  flag = true;
                  break;
              }
        }
        //如果购物车中没有该商品，则添加
        if (flag == false) {
            OrderItem orderItem = new OrderItem();
            Sku sku = skuService.findById(skuId);
            if (sku == null) {
                throw new RuntimeException("商品不存在");
            }
            if (!"1".equals(sku.getStatus())) {
                throw new RuntimeException("商品状态不合法");
            }
            if (num <= 0) {//数量不能为0或者负数
                throw new RuntimeException("商品数量不合法");
            }

            orderItem.setCategoryId3(sku.getCategoryId());

            Category category3 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(sku.getCategoryId());
            if (category3 == null) {
                category3 = categoryService.findById(sku.getCategoryId());
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(sku.getCategoryId(), category3);
            }
            //根据三级分类id查询二级分类
            Integer categoryId2 = category3.getParentId();
            orderItem.setCategoryId2(categoryId2);
            //根据二级分类id查询一级分类
            Category category2 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(categoryId2);
            if (category2 == null) {
                category2 = categoryService.findById(categoryId2);
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(categoryId2, category2);
            }
            Integer categoryId1 = category2.getParentId();
            orderItem.setCategoryId1(categoryId1);

            orderItem.setSkuId(skuId);
            orderItem.setSpuId(sku.getSpuId());
            orderItem.setNum(num);
            orderItem.setImage(sku.getImage());
            orderItem.setPrice(sku.getPrice());
            orderItem.setName(sku.getName());
            //总金额计算
            orderItem.setMoney(orderItem.getPrice() * num);
            //总重量计算
            if (sku.getWeight() == null) {
                sku.setWeight(0);
            }
            orderItem.setWeight(sku.getWeight() * num);

            Map map = new HashMap();
            map.put("item", orderItem);
            map.put("checked", true);//默认被选中
            cartList.add(map);
        }
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
    }

    /**
     * 更新选中状态
     * @param username
     * @param skuId
     * @param checked
     * @return
     */
    @Override
    public boolean updateChecked(String username, String skuId, boolean checked) {
        List<Map<String, Object>> cartList = findCartList(username);
        boolean isOk = false;
        for (Map<String, Object> map : cartList) {
            OrderItem orderItem = (OrderItem) map.get("item");
            if (orderItem.getSkuId().equals(skuId)) {
                map.put("checked", checked);
                isOk = true;
                break;
            }
        }
        if (isOk) {
            redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
        }
        return isOk;
    }

    /**
     * 删除选中的购物车
     * @param username
     */
    @Override
    public void deleteCheckedCart(String username) {
        //获得未选中的购物车
        List<Map<String, Object>> cartList = findCartList(username).stream().filter(cart -> !((boolean) cart.get("checked"))).collect(Collectors.toList());
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
    }

    /**
     * 计算购物车的优惠金额
     * @param username
     * @return
     */
    @Override
    public int preferential(String username) {
        //获取选中的购物车
        List<OrderItem> orderItemList = this.findCartList(username).stream().filter(cart -> (boolean) cart.get("checked"))
                .map(cart -> (OrderItem) cart.get("item"))
                .collect(Collectors.toList());
        //按分类聚合统计每个分类的金额
        //分类 金额
        // 1   3500
        // 2   2000
        Map<Integer, IntSummaryStatistics> cartMap = orderItemList.stream()
                .collect(Collectors.groupingBy(OrderItem::getCategoryId3, Collectors.summarizingInt(OrderItem::getMoney)));

        //累计优惠金额
        int allPreMoney = 0;
        //循环结果，统计每个分类的优惠金额，并累加
        for (Integer categoryId : cartMap.keySet()) {
            //获取每个品类的消费金额
            int money = (int) cartMap.get(categoryId).getSum();
            //获取优惠金额
            int preferentialMoney = preferentialService.findPreMoneyByCategoryId(categoryId, money);
            allPreMoney += preferentialMoney;
            System.out.println("分类：" + categoryId + ",消费金额：" + money + ",优惠金额：" + preferentialMoney);
        }
        return allPreMoney;
    }

    /**
     * 获取最新购物车列表
     * @param username
     * @return
     */
    @Override
    public List<Map<String, Object>> findNewOrderItemList(String username) {
        //获取购物车
        List<Map<String, Object>> cartList = findCartList(username);
        //循环购物车，刷新价格
        for (Map<String, Object> cart : cartList) {
            OrderItem orderItem = (OrderItem)cart.get("item");
            Sku sku = skuService.findById(orderItem.getSkuId());
            //更新价格
            orderItem.setPrice(sku.getPrice());
            //更新金额
            orderItem.setMoney(sku.getPrice() * orderItem.getNum());
        }
        //保存最新购物车
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
        return cartList;
    }

}
