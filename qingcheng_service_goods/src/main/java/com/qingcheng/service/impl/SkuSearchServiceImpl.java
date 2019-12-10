package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.BrandMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.service.goods.SkuSearchService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkuSearchServiceImpl implements SkuSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private SpecMapper specMapper;

    @Override
    public Map search(Map<String,String> searchMap) {
        //1.封装查询请求
        SearchRequest searchRequest=new SearchRequest("sku");
        searchRequest.types("doc"); //设置查询的类型

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();//布尔查询构建器

        //1.1关键字搜索
        MatchQueryBuilder matchQueryBuilder= QueryBuilders.matchQuery("name",searchMap.get("keywords"));
        boolQueryBuilder.must(matchQueryBuilder);

        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);

        // 1.2 商品分类过滤
        if (searchMap.get("category") != null) {
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("categoryName", searchMap.get("category"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //1.3 品牌过滤
        if (searchMap.get("brand") != null) {
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("brandName", searchMap.get("brand"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //1.4 规格过滤
        for (String key:searchMap.keySet()) {
              if (key.startsWith("spec.")) {//如果是规格参数
                  TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(key + ".keyword", searchMap.get(key));
                  boolQueryBuilder.filter(termQueryBuilder);
              }
        }

        //1.5 价格过滤
        if (searchMap.get("price") != null) {
            String[] prices = searchMap.get("price").split("-");
            if (!prices[0].equals("0")) {//最低价格不为0
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").gte(prices[0] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
            if (!prices[1].equals("*")) {//如果价格有上限
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").gte(prices[1] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }

        }

        //聚合查询（商品分类）
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("sku_category").field("categoryName");
        searchSourceBuilder.aggregation(termsAggregationBuilder);
        // searchSourceBuilder.size(0);

        //2.封装查询结果

        SearchResponse searchResponse;

        Map resultMap = new HashMap();
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits searchHits = searchResponse.getHits();
            long totalHits = searchHits.getTotalHits();
            System.out.println("记录数："+totalHits);
            SearchHit[] hits = searchHits.getHits();
            //2.1商品列表搜索
            List<Map<String,Object>> resultList = new ArrayList<>();
            for(SearchHit hit:hits){
                Map<String, Object> skuMap = hit.getSourceAsMap();
                resultList.add(skuMap);
            }
            resultMap.put("rows", resultList);

            //2.2 商品分类列表
            Aggregations aggregations = searchResponse.getAggregations();
            Map<String, Aggregation> aggregationMap = aggregations.getAsMap();
            Terms terms = (Terms) aggregationMap.get("sku_category");
            List<? extends Terms.Bucket> buckets =  terms.getBuckets();
            List<String> categoryList=new ArrayList();
            for( Terms.Bucket bucket:buckets ){
                categoryList.add(bucket.getKeyAsString());
            }
            resultMap.put("categoryList",categoryList);

            //2.3 品牌列表
            String categoryName = "";

            if (searchMap.get("category") == null) {
                if (categoryList.size() > 0) {
                    //提取分类列表中的第一个分类
                    categoryName = categoryList.get(0);
                }
            }else {
                //取出参数中的分类
                categoryName = searchMap.get("category");
            }

            //如果没有分类条件
            if (searchMap.get("brand") == null) {
                //查询品牌列表
                List<Map> brandList = brandMapper.findListByCategoryName(categoryName);
                resultMap.put("brandList",brandList);
            }

            //2.4 品牌列表

            //规格列表
            List<Map> specList = specMapper.findListByCategoryName(categoryName);
            specList.forEach(spec -> {
                String[] options = ((String) spec.get("options")).split(",");
                spec.put("options", options);
            });

            resultMap.put("specList", specList);


        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultMap;
    }
}
