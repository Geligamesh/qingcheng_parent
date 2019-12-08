package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.order.CategoryReportService;
import com.qingcheng.service.order.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTask {

    @Reference
    private OrderService orderService;
    @Reference
    private CategoryReportService categoryReportService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void createCategoryReportDate(){
        System.out.println("生成类目统计数据");
        categoryReportService.createData();
    }

    //没两分钟执行一次删除超时订单的定时任务
    // @Scheduled(cron = "0 0/2 * * * ?")
    // public void orderTimeOutLogic() {
    //     System.out.println("每两分钟间隔执行一次任务" + new Date());
    //     orderService.orderTimeOutLogic();
    // }

}
