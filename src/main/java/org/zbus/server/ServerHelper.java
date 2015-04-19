package org.zbus.server;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.nio.Session;

public class ServerHelper { 
	private static final Logger log = LoggerFactory.getLogger(ServerHelper.class); 

	public static Object callMethod(Object instance, String methodName,
			Object... params) throws Exception {
		return callMethod(instance, instance.getClass(), methodName, params);
	}

	private static Object callMethod(Object instance, Class<?> clazz,
			String methodName, Object... params) throws Exception {
		Method best = null;
		int bestMatch = 0;
		boolean isStatic = instance == null;
		for (Method m : clazz.getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) == isStatic
					&& m.getName().equals(methodName)) {
				int p = match(m.getParameterTypes(), params);
				if (p > bestMatch) {
					bestMatch = p;
					best = m;
				}
			}
		}
		if (best == null) {
			throw new NoSuchMethodException(methodName);
		}
		return best.invoke(instance, params);
	}

	private static int match(Class<?>[] params, Object[] values) {
		int len = params.length;
		if (len == values.length) {
			int points = 1;
			for (int i = 0; i < len; i++) {
				Class<?> pc = getNonPrimitiveClass(params[i]);
				Object v = values[i];
				Class<?> vc = v == null ? null : v.getClass();
				if (pc == vc) {
					points++;
				} else if (vc == null) {
					// can't verify
				} else if (!pc.isAssignableFrom(vc)) {
					return 0;
				}
			}
			return points;
		}
		return 0;
	}

	public static Class<?> getNonPrimitiveClass(Class<?> clazz) {
		if (!clazz.isPrimitive()) {
			return clazz;
		} else if (clazz == boolean.class) {
			return Boolean.class;
		} else if (clazz == byte.class) {
			return Byte.class;
		} else if (clazz == char.class) {
			return Character.class;
		} else if (clazz == double.class) {
			return Double.class;
		} else if (clazz == float.class) {
			return Float.class;
		} else if (clazz == int.class) {
			return Integer.class;
		} else if (clazz == long.class) {
			return Long.class;
		} else if (clazz == short.class) {
			return Short.class;
		} else if (clazz == void.class) {
			return Void.class;
		}
		return clazz;
	}

	public static Object callStaticMethod(String classAndMethod,
			Object... params) throws Exception {
		int lastDot = classAndMethod.lastIndexOf('.');
		String className = classAndMethod.substring(0, lastDot);
		String methodName = classAndMethod.substring(lastDot + 1);
		return callMethod(null, Class.forName(className), methodName, params);
	}

	public static Object newInstance(String className, Object... params)
			throws Exception {
		Constructor<?> best = null;
		int bestMatch = 0;
		for (Constructor<?> c : Class.forName(className).getConstructors()) {
			int p = match(c.getParameterTypes(), params);
			if (p > bestMatch) {
				bestMatch = p;
				best = c;
			}
		}
		if (best == null) {
			throw new NoSuchMethodException(className);
		}
		return best.newInstance(params);
	}

	 
	
	public static String getProperty(String key, String defaultValue) {
        try {
            return System.getProperty(key, defaultValue);
        } catch (SecurityException se) {
            return defaultValue;
        }
    }
	
	public static String[] arraySplit(String s, char separatorChar, boolean trim) {
        if (s == null) {
            return null;
        }
        int length = s.length();
        if (length == 0) {
            return new String[0];
        }
        ArrayList<String> list = new ArrayList<String>();
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == separatorChar) {
                String e = buff.toString();
                list.add(trim ? e.trim() : e);
                buff.setLength(0);
            } else if (c == '\\' && i < length - 1) {
                buff.append(s.charAt(++i));
            } else {
                buff.append(c);
            }
        }
        String e = buff.toString();
        list.add(trim ? e.trim() : e);
        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }
	public static String replaceAll(String s, String before, String after) {
        int next = s.indexOf(before);
        if (next < 0) {
            return s;
        }
        StringBuilder buff = new StringBuilder(
                s.length() - before.length() + after.length());
        int index = 0;
        while (true) {
            buff.append(s.substring(index, next)).append(after);
            index = next + before.length();
            next = s.indexOf(before, index);
            if (next < 0) {
                buff.append(s.substring(index));
                break;
            }
        }
        return buff.toString();
    }
	
	
	public static void openBrowser(String url) throws Exception {
        try {
            String osName = ServerHelper.getProperty("os.name", "Windows").toLowerCase();
            Runtime rt = Runtime.getRuntime();
            String browser = null;
            if (browser == null) {
                // under Linux, this will point to the default system browser
                try {
                    browser = System.getenv("BROWSER");
                } catch (SecurityException se) {
                    // ignore
                }
            }
            if (browser != null) {
                if (browser.startsWith("call:")) {
                    browser = browser.substring("call:".length());
                    ServerHelper.callStaticMethod(browser, url);
                } else if (browser.indexOf("%url") >= 0) {
                    String[] args = ServerHelper.arraySplit(browser, ',', false);
                    for (int i = 0; i < args.length; i++) {
                        args[i] = ServerHelper.replaceAll(args[i], "%url", url);
                    }
                    rt.exec(args);
                } else if (osName.indexOf("windows") >= 0) {
                    rt.exec(new String[] { "cmd.exe", "/C",  browser, url });
                } else {
                    rt.exec(new String[] { browser, url });
                }
                return;
            }
            try {
                Class<?> desktopClass = Class.forName("java.awt.Desktop");
                // Desktop.isDesktopSupported()
                Boolean supported = (Boolean) desktopClass.
                    getMethod("isDesktopSupported").
                    invoke(null, new Object[0]);
                URI uri = new URI(url);
                if (supported) {
                    // Desktop.getDesktop();
                    Object desktop = desktopClass.getMethod("getDesktop").
                        invoke(null, new Object[0]);
                    // desktop.browse(uri);
                    desktopClass.getMethod("browse", URI.class).
                        invoke(desktop, uri);
                    return;
                }
            } catch (Exception e) {
                // ignore
            }
            if (osName.indexOf("windows") >= 0) {
                rt.exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
            } else if (osName.indexOf("mac") >= 0 || osName.indexOf("darwin") >= 0) {
                // Mac OS: to open a page with Safari, use "open -a Safari"
                Runtime.getRuntime().exec(new String[] { "open", url });
            } else {
                String[] browsers = { "chromium", "google-chrome", "firefox",
                        "mozilla-firefox", "mozilla", "konqueror", "netscape",
                        "opera", "midori" };
                boolean ok = false;
                for (String b : browsers) {
                    try {
                        rt.exec(new String[] { b, url });
                        ok = true;
                        break;
                    } catch (Exception e) { 
                    }
                }
                if (!ok) { 
                    throw new Exception( "Browser detection failed and system property");
                }
            }
        } catch (Exception e) {
        	//ignore
        	log.debug(e.getMessage(), e);
        }
    }
	
	public static void loadStartupService(String serviceBase, int serverPort){
		try{
			Class<?> loaderClass = Class.forName("org.zbus.client.service.ServiceLoader");
			Method m = loaderClass.getMethod("load", String.class, String.class);
			String brokerAddress = String.format("127.0.0.1:%d", serverPort);
			m.invoke(null, serviceBase, brokerAddress); 
		} catch(Exception e){
			//ignore
			log.debug("loading service error, ignore");
		}
	}
	
	
	
	public static void reply404(Message msg, Session sess) throws IOException{
    	Message res = new Message();
    	String mqName = msg.getMq();
		res.setMsgId(msg.getMsgId());  
		res.setStatus("404");
		res.setMqReply(sess.id()); //mark
		res.setBody(String.format("MQ(%s) Not Found", mqName));
		
		sess.write(res);
    }
   
    public static void reply403(Message msg, Session sess) throws IOException{
    	Message res = new Message();
    	String mqName = msg.getMq();
    	
    	res.setMsgId(msg.getMsgId()); 
    	res.setStatus("403");
    	res.setMqReply(sess.id()); //mark
    	res.setBody(String.format("MQ(%s) forbbiden, token(%s) mismatched", mqName, msg.getToken()));
    	
    	sess.write(res);
    }
    
    public static void reply200(String msgId, Session sess) throws IOException{
    	Message res = new Message();
    	res.setMsgId(msgId); 
    	res.setStatus("200");
    	res.setMqReply(sess.id()); //mark
    	res.setBody(""+System.currentTimeMillis()); 
    	 
    	sess.write(res);
    }
    
    
    public static void reply400(Message msg, Session sess) throws IOException{
    	Message res = new Message();
    	res.setMsgId(msg.getMsgId()); 
    	res.setStatus("400");
    	res.setMqReply(sess.id()); //mark
    	res.setBody(String.format("Bad format: %s", msg.getBodyString())); 
    	sess.write(res);
    }
    
}
