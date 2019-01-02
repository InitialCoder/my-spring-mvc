package com.reese.springmvc.mvc.service.impl;

import com.reese.springmvc.annotation.Service;
import com.reese.springmvc.mvc.service.UserService;

@Service
public class UserServiceImpl implements UserService{

	public void printUser() {
		System.out.println("-------------gali 咖喱嘎嘎嘎----------------------");
	}
	
}
