package com.itmayidu.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserEntity {
	private Integer id;
	private String username;
	private String password;
	private String phone;
	private String email;
	private Date created;
	private Date updated;
	private String openid;

}
