package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.CategoryReportMapper;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = CategoryReportService.class)
public class CategoryReportServiceImpl implements CategoryReportService {

    @Autowired
    private CategoryReportMapper categoryReportMapper;

    @Override
    public List<CategoryReport> categoryReport(LocalDate localDate) {
        return categoryReportMapper.categoryReport(localDate);
    }

    @Override
    @Transactional
    public void createData() {
        //查询昨天的类目统计数据
        LocalDate localDate = LocalDate.of(2019,4,15);
        List<CategoryReport> categoryReports = categoryReportMapper.categoryReport(localDate);
        //保存到tb_category_report表中
        categoryReports.forEach(categoryReport -> categoryReportMapper.insertSelective(categoryReport));
    }

    /**
     * 根据第一类别查询类目信息
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public List<Map> category1Count(String date1, String date2) {
        return categoryReportMapper.category1Count(date1, date2);
    }

}
