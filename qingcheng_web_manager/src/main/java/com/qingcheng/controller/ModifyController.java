package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.service.system.AdminService;
import com.qingcheng.util.org.mindrot.jbcrypt.BCrypt;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("modify")
public class ModifyController {

    @Reference
    private AdminService adminService;

    @PostMapping("modifyPassword")
    public Result modifyPassword(@RequestBody Map<String,String> map) {
        String oldPassword = map.get("oldPassword");
        String newPassword = map.get("newPassword");
        String rePassword = map.get("rePassword");
        if (StringUtils.isBlank(oldPassword)) {
            throw new RuntimeException("原密码不能为空");
        }
        if (StringUtils.isBlank(newPassword)) {
            throw new RuntimeException("新密码不能为空");
        }
        if (StringUtils.isBlank(rePassword)) {
            throw new RuntimeException("确认密码不能为空");
        }
        if (!StringUtils.equals(newPassword, rePassword)) {
            throw new RuntimeException("两次密码不正确，请重新输入");
        }
        String loginName = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String,Object> searchMap = new HashMap<>();
        searchMap.put("loginName", loginName);
        searchMap.put("status", "1");
        List<Admin> adminList = adminService.findList(searchMap);

        Admin admin = adminList.get(0);
        String password = admin.getPassword();
        //检验是否跟数据库中的密码匹配
        boolean checkpw = BCrypt.checkpw(oldPassword, password);
        if (checkpw) {
            //匹配成功的话修改密码
            //将新密码加密之后存入数据库
            String salt = BCrypt.gensalt();
            String hashpw = BCrypt.hashpw(newPassword, salt);
            admin.setPassword(hashpw);
            adminService.update(admin);
        }else {
            throw new RuntimeException("原密码错误，请输入正确密码");
        }
        return new Result();
    }

    public static void main(String[] args) {
        String password = "$2a$10$61ogZY7EXsMDWeVGQpDq3OBF1.phaUu7.xrwLyWFTOu8woE08zMIW";
        System.out.println(BCrypt.checkpw("123456", password));
    }
}
