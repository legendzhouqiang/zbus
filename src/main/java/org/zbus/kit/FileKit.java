package org.zbus.kit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;

public class FileKit {   
	
	public static InputStream loadFile(String resource, Class<?> clazz) {
		ClassLoader classLoader = null;
		try {
			Method method = Thread.class.getMethod("getContextClassLoader");
			classLoader = (ClassLoader) method.invoke(Thread.currentThread());
		} catch (Exception e) {
			System.out.println("loadConfigFile error: ");
			e.printStackTrace();
		}
		if (classLoader == null) {
			classLoader = clazz.getClassLoader();
		}
		try {
			if (classLoader != null) {
				URL url = classLoader.getResource(resource);
				if (url == null) {
					System.out.println("Can not find resource:" + resource);
					return null;
				}
				if (url.toString().startsWith("jar:file:")) { 
					return clazz.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource);
				} else { 
					return new FileInputStream(new File(url.toURI()));
				}
			}
		} catch (Exception e) {
			System.out.println("loadConfigFile error: ");
			e.printStackTrace();
		}
		return null;
	}  
	
	public static InputStream loadFile(String resource) {
		ClassLoader classLoader = null;
		try {
			Method method = Thread.class.getMethod("getContextClassLoader");
			classLoader = (ClassLoader) method.invoke(Thread.currentThread());
		} catch (Exception e) {
			System.out.println("loadConfigFile error: ");
			e.printStackTrace();
		}
		if (classLoader == null) {
			classLoader = FileKit.class.getClassLoader();
		}
		try {
			if (classLoader != null) {
				URL url = classLoader.getResource(resource);
				if (url == null) {
					System.out.println("Can not find resource:" + resource);
					return null;
				}
				if (url.toString().startsWith("jar:file:")) { 
					return FileKit.class.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource);
				} else { 
					return new FileInputStream(new File(url.toURI()));
				}
			}
		} catch (Exception e) {
			System.out.println("loadConfigFile error: ");
			e.printStackTrace();
		}
		return null;
	}  
	
	
	public static String loadFileContent(String resource) { 
		InputStream in = FileKit.class.getClassLoader().
				getResourceAsStream(resource);
		if(in == null) return "";
		
		 Writer writer = new StringWriter(); 
         char[] buffer = new char[1024];
         try {
        	 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, "UTF-8"));
             int n;
             while ((n = reader.read(buffer)) != -1) {
                 writer.write(buffer, 0, n);
             }
         } catch (UnsupportedEncodingException e) { 
			e.printStackTrace();
		} catch (IOException e) { 
			e.printStackTrace();
		} finally {
             try {
				in.close();
			} catch (IOException e) { 
				e.printStackTrace();
			}
         }
         return writer.toString();
	}  
	
}
