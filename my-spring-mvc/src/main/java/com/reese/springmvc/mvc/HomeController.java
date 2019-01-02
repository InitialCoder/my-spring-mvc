package com.reese.springmvc.mvc;

import com.reese.springmvc.annotation.Autowired;
import com.reese.springmvc.annotation.Controller;
import com.reese.springmvc.annotation.RequestMapping;
import com.reese.springmvc.mvc.service.UserService;

@Controller
@RequestMapping(name="home")
public class HomeController {

	@Autowired
	private UserService userService;
	
	@RequestMapping(name="/index.action")
	public void index(){
		userService.printUser();

		
	}
	
	
	
}
