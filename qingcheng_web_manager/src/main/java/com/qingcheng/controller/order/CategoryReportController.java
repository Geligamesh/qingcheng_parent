package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("categoryReport")
public class CategoryReportController {

    @Reference
    private CategoryReportService categoryReportService;

    /**
     * 获取昨天的商品类目信息
     * @return
     */
    @GetMapping("yesterday")
    public List<CategoryReport> yesterday(){
        //获得昨天的日期
        // LocalDate localDate = LocalDate.now().minusDays(1);
        LocalDate localDate = LocalDate.of(2019,4,15);
        List<CategoryReport> categoryReports = categoryReportService.categoryReport(localDate);
        return categoryReports;
    }

    /**
     * 根据类别1查询类目信息
     * @param date1
     * @param date2
     * @return
     */
    @GetMapping("category1Count")
    public List<Map> category1Count(String date1,String date2) {
        return categoryReportService.category1Count(date1, date2);
    }
}
