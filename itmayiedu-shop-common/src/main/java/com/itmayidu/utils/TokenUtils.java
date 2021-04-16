package com.itmayidu.utils;

import com.itmayidu.constants.Constants;

import java.util.UUID;

public class TokenUtils {

	 public static String getMemberToken(){
		 return Constants.TOKEN_MEMBER+"-"+UUID.randomUUID();
	 }
	
}
