package com.reese.springmvc.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.reese.springmvc.annotation.Autowired;
import com.reese.springmvc.annotation.Controller;
import com.reese.springmvc.annotation.RequestMapping;
import com.reese.springmvc.annotation.Service;
/**
 * 
 *
 * @ClassName: DispathcherServlet 
 * @Description: 模拟springMVC 核心类  dispathcherservlet
 * @author wu
 * @date 2018-12-28 17:46:07
 *
 */
public class DispathcherServlet extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Properties properties=new Properties();
	
	private List<String> classNames=new ArrayList<String>();
	
	private Map<String,Object> ioc=new HashMap<String,Object>();
	
	private Map<String,Method> handleMap = new HashMap<String,Method>();
	
	private Map<String,Object> controllerMap =new HashMap<String,Object>();
	
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		
		//1、加载web.xml  的配置文件
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		//2、扫描配置的MVC 相关联的包的相关类
		doScanner(properties.getProperty("scanmvcPath"));
		//3、通过java 反射机制 实例化所有相关类
		doInstance();
		//4、初始化controller容器，key-value:url-mothod
		initHandleMapping();
		//5、注入
		doAutoweired();
		
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(handleMap.isEmpty()){
			resp.getWriter().write("handlMap is empty!!! ");
			return ;
		}
		
		String url=req.getRequestURI();
		String contentPath=req.getContextPath();
		url = url.replace(contentPath,"").replace("/+", "/");
		if(!handleMap.containsKey(url)){
			resp.getWriter().write("Error 404 ");
			return ;
		}
		
		Method method=handleMap.get(url);
		//获取方法的参数列表类型
		Class<? extends Object>[] requestParams=method.getParameterTypes();
		//获取请求的参数
		Map<String, String[]> parameterMap = req.getParameterMap();
		//保存参数值
		Object[] paramValues = new Object[requestParams.length];
		for (int i = 0; i < requestParams.length; i++) {
			String methodParam=requestParams[i].getSimpleName();
			if(methodParam.equals("HttpServletRequest")){
				paramValues[i]=req;
				continue;
			}
			
			if(methodParam.equals("HttpServletResponse")){
				paramValues[i]=resp;
				continue;
			}
			
			if(methodParam.equals("String")){
				for(Entry<String,String[]> param: parameterMap.entrySet()){
					String[] paramValue=param.getValue();
					String value = Arrays.toString(paramValue).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
					paramValues[i++] = value;
				}
			}
			
		}
		
		try {
			method.invoke(controllerMap.get(url), paramValues);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	private void doAutoweired() {
		if(ioc.isEmpty()){
			return;
		}
		
		for ( Entry<String,Object> entry :ioc.entrySet()) {
			// 查找所有被Autowired注解的属性
			// getFields()获得某个类的所有的公共（public）的字段，包括父类;
			// getDeclaredFields()获得某个类的所有申明的字段，即包括public、private和proteced，但是不包括父类的申明字段
			Object instance = entry.getValue();
			Class<? extends Object> clazz=entry.getValue().getClass();
			Field[] fields= clazz.getDeclaredFields();
			
			for(Field field :fields){
				if(!field.isAnnotationPresent(Autowired.class)){
					continue;
				}
				
				String beanName;
				Autowired autowired = field.getAnnotation(Autowired.class);
				//获取属性对应的class
				if("".equals(autowired.value())){
//					beanName = field.getName();
					beanName=toLowerFirstWord(field.getType().getName());
				}else{
					beanName=autowired.value();
				}
				
				//开始注入
				//将私有属性设置为可到达
				field.setAccessible(true);
				if(ioc.get(beanName)!=null){
					field.setAccessible(true);//避免多线程访问时将可见性关闭
					try {
						field.set(instance, ioc.get(beanName));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * 
	 *
	 * @Title:  initHandleMapping 
	 * @Description:  初始化requestMapping  容器     
	 *
	 * @author：wu  
	 * @date：2018-12-29 14:29:10
	 *
	 */
	private void initHandleMapping() {
		if(ioc.isEmpty()){
			return ;
		}
		try {
			
			for (Entry<String,Object> entr :ioc.entrySet()) {
				Class<? extends Object> clazz= entr.getValue().getClass();
				if (!clazz.isAnnotationPresent(Controller.class)) {
					continue ;
				}
				Object instance = entr.getValue();
				String baseUrl="";
				if(clazz.isAnnotationPresent(RequestMapping.class)){
					RequestMapping annotation= clazz.getAnnotation(RequestMapping.class);
					baseUrl+="/"+annotation.name();
				}
				
				Method[] methods= clazz.getMethods();
				for (Method method :methods) {
					 if(!method.isAnnotationPresent(RequestMapping.class)){
						 continue;
					 }
					 RequestMapping annotation= method.getAnnotation(RequestMapping.class);
					 baseUrl=baseUrl+annotation.name().replace("/+", "/");
					 
					 handleMap.put(baseUrl, method);
					 controllerMap.put(baseUrl, instance);
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	

	private void doInstance() {
		if(classNames.isEmpty()){
			return ;
		}
		//将扫描到的类实例化并且放到IOC容器中
		for(String className:classNames){
			try {
				Class<?> clazz = Class.forName(className);
				if(clazz.isAnnotationPresent(Controller.class)){
					ioc.put(toLowerFirstWord(className), clazz.newInstance());
				}else if(clazz.isAnnotationPresent(Service.class)){
					Object instance=clazz.newInstance();
					Service service=(Service)clazz.getAnnotation(Service.class);
					String value=service.value();
					if(!"".equals(value)){
						ioc.put(value, instance);
					}else{
						Class<?>[] faces = clazz.getInterfaces();
						String superName=faces[0].getName();
						ioc.put(toLowerFirstWord(superName), instance);
					}
				}else{
					continue;
				}
			} catch (ClassNotFoundException |InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	private void doLoadConfig(String param) {
		//读取web.xml 配置 的properties 文件，加载至properties对象中
		InputStream resourceInput = this.getClass().getClassLoader().getResourceAsStream(param);
		try {
			properties.load(resourceInput);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if(null!=resourceInput){
				try {
					resourceInput.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
	}
	
	/**
	 *
	 *
	 * @Title:  doScanner 
	 * @Description: 扫描此路径下所有的class文件
	 * @param object void     
	 *
	 * @author：wu  
	 * @date：2018-12-29 09:04:45
	 *
	 */
	private void doScanner(String scanPackege) {
		String path="/"+scanPackege.replace(".", "/");
		URL url=this.getClass().getClassLoader().getResource(path);
		File dir=new File(url.getFile());
		for(File file:dir.listFiles()){
			if(file.isDirectory()){
				//递归读取
				doScanner(scanPackege+"."+file.getName());
			}else{
				String className=scanPackege+"."+file.getName().replace(".class", "");
				classNames.add(className);
			}
			
		}
		
	}
	/**
	 * 
	 *
	 * @Title:  toLowerFirstWord 
	 * @Description: 将字符串的第一个大写字母转成小写
	 * @param name
	 * @return String     
	 *
	 * @author：wu  
	 * @date：2018-12-29 10:06:34
	 *
	 */
	private String toLowerFirstWord(String name){
		char[] charArray=name.toCharArray();
		if(65<=charArray[0]&&charArray[0]<=97){
			charArray[0]+=32;
		}
		return String.valueOf(charArray);
	}
}
