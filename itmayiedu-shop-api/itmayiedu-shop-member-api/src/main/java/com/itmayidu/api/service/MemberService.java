package com.itmayidu.api.service;

import com.itmayidu.base.ResponseBase;
import com.itmayidu.entity.UserEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @AUTHOR LZG
 * @DATE 2021/3/25 23:35
 * @VERSION 1.0
 */

@RequestMapping("/member")
public interface MemberService {

    @RequestMapping("/testRest")
    public Map<String, Object> testRest();

    @RequestMapping("/setRedisTest")
    public ResponseBase setRedisTest(String key, String value);

    @RequestMapping("/getRedis")
    public ResponseBase getRedis(String key);

    // 使用userId查找用户信息
    @RequestMapping("/findUserById")
    ResponseBase findUserById(Long userId);

    @RequestMapping("/register")
    ResponseBase regUser(@RequestBody UserEntity user);

    // 用户登录
    @RequestMapping("/login")
    ResponseBase login(@RequestBody UserEntity user);
    // 使用token进行登录
    @RequestMapping("/findByTokenUser")
    ResponseBase findByTokenUser(String token);

    //使用openid查找用户信息
    @RequestMapping("/findByOpenIdUser")
    ResponseBase findByOpenIdUser(@RequestParam("openid") String openid);
    // 用户登录
    @RequestMapping("/qqLogin")
    ResponseBase qqLogin(@RequestBody UserEntity user);
}
