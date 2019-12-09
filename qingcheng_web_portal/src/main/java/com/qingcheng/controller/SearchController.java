package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.util.WebUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class SearchController {

    @Reference
    private SkuSearchService skuSearchService;

    @GetMapping("search")
    public String search(Model model,@RequestParam Map<String, String> searchMap) throws Exception {

        //字符集处理
        searchMap = WebUtil.convertCharsetToUTF8(searchMap);

        Map search = skuSearchService.search(searchMap);
        model.addAttribute("result", search);

        //url处理
        StringBuffer url = new StringBuffer("/search.do?");
        for (String key : searchMap.keySet()) {
            url.append("&").append(key).append("=").append(searchMap.get(key));
        }
        model.addAttribute("url", url);
        model.addAttribute("searchMap", searchMap);
        return "search";
    }
}
