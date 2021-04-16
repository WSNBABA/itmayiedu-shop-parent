package com.itmayidu.api.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.itmayidu.api.service.MemberService;
import com.itmayidu.base.BaseController;
import com.itmayidu.base.BaseRedisService;
import com.itmayidu.base.ResponseBase;
import com.itmayidu.constants.Constants;
import com.itmayidu.dao.MemberDao;
import com.itmayidu.entity.UserEntity;
import com.itmayidu.mq.RegisterMailboxProducer;
import com.itmayidu.utils.MD5Util;
import com.itmayidu.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @AUTHOR LZG
 * @DATE 2021/3/25 23:51
 * @VERSION 1.0
 */
@Slf4j
@RestController
public class MemberServiceImpl extends BaseController implements MemberService {

    @Autowired
    private BaseRedisService baseRedisService;
    @Autowired
    private MemberDao memberDao;
    @Value("${messages.queue}")
    private String MESSAGESQUEUE;
    @Autowired
    private RegisterMailboxProducer registerMailboxProducer;

    @Override
    public ResponseBase findUserById(Long userId) {
        UserEntity user = memberDao.findByID(userId);
        if (user == null) {
            return setResultError("未查找到用户信息.");
        }
        return setResultSuccess(user);
    }

    @Override
    public Map<String, Object> testRest() {
        Map<String, Object> result = new HashMap<>();
        result.put("errorCode", "200");
        result.put("errorMsg", "success");
        return result;
    }

    @Override
    public ResponseBase setRedisTest(String key, String value) {
        baseRedisService.setString(key, value);
        return setResultSuccess();
    }

    @Override
    public ResponseBase getRedis(String key) {
        String value = baseRedisService.getString(key);
        return setResultSuccess(value);
    }

    @Override
    public ResponseBase regUser(@RequestBody UserEntity user) {
        String passWord=user.getPassword();
        String newPassWord=MD5Util.MD5(passWord);
        user.setPassword(newPassWord);
        Integer insertUser = memberDao.insertUser(user);
        if (insertUser <= 0) {
            return setResultError("注册失败!");
        }
        //采用MQ异步发送邮件
        String email = user.getEmail();
        String messAageJson = message(email);
        log.info("######email:{},messAageJson:{}",email,messAageJson);
        sendMsg(messAageJson);
        return setResultSuccess();
    }

    @Override
    public ResponseBase login(@RequestBody UserEntity user) {
        String username = user.getUsername();
        if (StringUtils.isEmpty(username)) {
            return setResultError("用户名称不能为空!");
        }
        String password = user.getPassword();
        if (StringUtils.isEmpty(password)) {
            return setResultError("密码不能为空!");
        }
        String newPassWord = MD5Util.MD5(password);
        UserEntity userEntity = memberDao.login(username, newPassWord);
        if (userEntity == null) {
            return setResultError("账号或密码错误!");
        }
        // 生成token
        String token = TokenUtils.getMemberToken();
        baseRedisService.setString(token, userEntity.getId()+"",Constants.TOKEN_MEMBER_TIME);
        JSONObject JSONObject = new JSONObject();
        JSONObject.put("token", token);
        return setResultSuccess(JSONObject);
    }

    @Override
    public ResponseBase findByTokenUser(String token) {
        if (StringUtils.isEmpty(token)) {
            return setResultError("token不能为空.");
        }
        String userId = baseRedisService.getString(token);
        if(StringUtils.isEmpty(userId)){
            return setResultError("未查询到用户信息");
        }
        Long userIdl=Long.parseLong(userId);
        UserEntity userEntity = memberDao.findByID(userIdl);
        if (userEntity == null) {
            return setResultError("未查询到用户信息");
        }
        userEntity.setPassword(null);
        return setResultSuccess(userEntity);
    }

    private String message(String mail) {
        JSONObject root = new JSONObject();
        JSONObject header = new JSONObject();
        header.put("interfaceType", "sms_mail");
        JSONObject content = new JSONObject();
        content.put("mail", mail);
        root.put("header", header);
        root.put("content", content);
        return root.toJSONString();
    }

    private void sendMsg(String json) {
        ActiveMQQueue activeMQQueue = new ActiveMQQueue(MESSAGESQUEUE);
        registerMailboxProducer.sendMsg(activeMQQueue, json);
    }

    @Override
    public ResponseBase findByOpenIdUser(@RequestParam("openid") String openid) {
        // 1.验证参数
        if (StringUtils.isEmpty(openid)) {
            return setResultError("openid不能为空1");
        }
        // 2.使用openid 查询数据库 user表对应数据信息
        UserEntity userEntity = memberDao.findByOpenIdUser(openid);
        if (userEntity == null) {
            return setResultError(Constants.HTTP_RES_CODE_201, "该openid没有关联");
        }
        // 3.自动登录
        return setLogin(userEntity);
    }

    private ResponseBase setLogin(UserEntity userEntity) {
        if (userEntity == null) {
            return setResultError("账号或者密码不能正确");
        }
        // 3.如果账号密码正确，对应生成token
        String memberToken = TokenUtils.getMemberToken();
        // 4.存放在redis中，key为token value 为 userid
        Integer userId = userEntity.getId();
        log.info("####用户信息token存放在redis中... key为:{},value", memberToken, userId);
        baseRedisService.setString(memberToken, userId + "", Constants.TOKEN_MEMBER_TIME);
        // 5.直接返回token
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("memberToken", memberToken);
        return setResultSuccess(jsonObject);
    }

    @Override
    public ResponseBase qqLogin(@RequestBody UserEntity user) {
        // 1.验证参数
        String openid = user.getOpenid();
        if (StringUtils.isEmpty(openid)) {
            return setResultError("openid不能为空!");
        }
        // 2.先进行账号登录
        ResponseBase setLogin = login(user);
        if (!setLogin.getCode().equals(Constants.HTTP_RES_CODE_200)) {
            return setLogin;
        }
        // 3.自动登录
        JSONObject jsonObjcet = (JSONObject) setLogin.getData();
        // 4. 获取token信息
        String memberToken = jsonObjcet.getString("memberToken");
        ResponseBase userToken = findByTokenUser(memberToken);
        if (!userToken.getCode().equals(Constants.HTTP_RES_CODE_200)) {
            return userToken;
        }
        UserEntity userEntity = (com.itmayidu.entity.UserEntity) userToken.getData();
        // 5.修改用户openid
        Integer userId = userEntity.getId();
        Integer updateByOpenIdUser = memberDao.updateByOpenIdUser(openid, userId);
        if (updateByOpenIdUser <= 0) {
            return setResultError("QQ账号管理失败!");
        }
        return setLogin;
    }
}
