package org.zbus.server;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
 

public class ZbusServerTray implements ActionListener, MouseListener, WindowListener { 
    private boolean trayIconUsed;
    private Font font; 
    private Object tray;
    private Object trayIcon;
    private boolean isWindows;
    
    private ZbusServer zbus;
    private String webUrl = "http://localhost:15555";
    
    public ZbusServerTray() {
    	isWindows = Helper.getProperty("os.name", "").startsWith("Windows");
	}
    
	@Override
	public void windowActivated(WindowEvent e) { 
		
	}

	@Override
	public void windowClosed(WindowEvent e) { 
		
	}

	@Override
	public void windowClosing(WindowEvent e) { 
		shutdown();
	}

	@Override
	public void windowDeactivated(WindowEvent e) { 
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) { 
		
	}

	@Override
	public void windowIconified(WindowEvent e) { 
		
	}

	@Override
	public void windowOpened(WindowEvent e) { 
		
	}

	@Override
	public void mouseClicked(MouseEvent e) { 
		if (e.getButton() == MouseEvent.BUTTON1) {
			startBrowser(); 
        }
	}

	@Override
	public void mouseEntered(MouseEvent e) { 
		
	}

	@Override
	public void mouseExited(MouseEvent e) { 
		
	}

	@Override
	public void mousePressed(MouseEvent e) { 
		
	}

	@Override
	public void mouseReleased(MouseEvent e) { 
		
	}

	
	@Override
	public void actionPerformed(ActionEvent e) { 
		String command = e.getActionCommand();
        if ("exit".equals(command)) {
            shutdown();
        } else if ("admin".equals(command)) {
            startBrowser(); 
        } 
	}
	
	private void startBrowser() {
		try {
			openBrowser(webUrl);
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}
	
	public void run(String... args) throws Exception{ 
		if(GraphicsEnvironment.isHeadless()){
			zbus = ZbusServer.startServer(args);
			webUrl = "http://"+zbus.getServerAddress();
			return;
		}
		
		loadFont();
		createTrayIcon();
		
		zbus = ZbusServer.startServer(args);
		webUrl = "http://"+zbus.getServerAddress();
		startBrowser(); 
	}
	
	private void loadFont() {
        if (isWindows) {
            font = new Font("Dialog", Font.PLAIN, 11);
        } else {
            font = new Font("Dialog", Font.PLAIN, 12);
        }
    }
	
	
	private boolean createTrayIcon() {
        try { 
            boolean supported = (Boolean) Helper.callStaticMethod("java.awt.SystemTray.isSupported");
            if (!supported) {
                return false;
            }
            PopupMenu menuConsole = new PopupMenu();
            MenuItem itemConsole = new MenuItem("Admin");
            itemConsole.setActionCommand("admin");
            itemConsole.addActionListener(this);
            itemConsole.setFont(font);
            menuConsole.add(itemConsole); 
            MenuItem itemExit = new MenuItem("Exit");
            itemExit.setFont(font);
            itemExit.setActionCommand("exit");
            itemExit.addActionListener(this);
            menuConsole.add(itemExit);

            tray = Helper.callStaticMethod("java.awt.SystemTray.getSystemTray");
            Dimension d = (Dimension) Helper.callMethod(tray, "getTrayIconSize");
            String iconFile;
            if (d.width >= 24 && d.height >= 24) {
                iconFile = "/zbus-24.png";
            } else if (d.width >= 22 && d.height >= 22) {
                // for Mac OS X 10.8.1 with retina display:
                // the reported resolution is 22 x 22, but the image
                // is scaled and the real resolution is 44 x 44
                iconFile = "/zbus-64.png"; 
            } else {
                iconFile = "/zbus.png";
            }
            Image icon = loadImage(iconFile);

            trayIcon = Helper.newInstance("java.awt.TrayIcon", icon, "Zbus Server", menuConsole);
            Helper.callMethod(trayIcon, "addMouseListener", this);
            Helper.callMethod(tray, "add", trayIcon);

            this.trayIconUsed = true;

            return true;
        } catch (Exception e) {
            return false;
        }
    }
	private static Image loadImage(String name) {
        try {
        	ZbusServerTray.class.getResourceAsStream(name);
            byte[] imageData = Helper.getResource(name);
            if (imageData == null) {
                return null;
            }
            return Toolkit.getDefaultToolkit().createImage(imageData);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
	
	
	
	public void shutdown() {  
		if(zbus != null){
			zbus.close();
		}
        if (trayIconUsed) {
            try { 
                Helper.callMethod(tray, "remove", trayIcon);
            } catch (Exception e) { 
            } finally {
                trayIcon = null;
                tray = null;
                trayIconUsed = false;
            }
            System.gc();
        } 
        System.exit(0);
    }
	
	public static void openBrowser(String url) throws Exception {
        try {
            String osName = Helper.getProperty("os.name", "Windows");
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
                    Helper.callStaticMethod(browser, url);
                } else if (browser.indexOf("%url") >= 0) {
                    String[] args = Helper.arraySplit(browser, ',', false);
                    for (int i = 0; i < args.length; i++) {
                        args[i] = Helper.replaceAll(args[i], "%url", url);
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
            throw new Exception("Failed to start a browser to open the URL " +
            url + ": " + e.getMessage());
        }
    }

	
	public static void main(String[] args) throws Exception{
		ZbusServerTray tray = new ZbusServerTray();
		tray.run(args);
	}
}

class Helper {
	public static final int IO_BUFFER_SIZE = 4 * 1024;
	private static final HashMap<String, byte[]> RESOURCES = new HashMap<String, byte[]>();

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

	public static byte[] getResource(String name) throws IOException {
		byte[] data = RESOURCES.get(name);
		if (data == null) {
			data = loadResource(name);
			RESOURCES.put(name, data);
		}
		return data == null ? new byte[0] : data;
	}

	private static byte[] loadResource(String name) throws IOException {
		InputStream in = Helper.class.getResourceAsStream("data.zip");
		if (in == null) {
			in = Helper.class.getResourceAsStream(name);
			if (in == null) {
				return null;
			}
			return Helper.readBytesAndClose(in, 0);
		}
		ZipInputStream zipIn = new ZipInputStream(in);
		try {
			while (true) {
				ZipEntry entry = zipIn.getNextEntry();
				if (entry == null) {
					break;
				}
				String entryName = entry.getName();
				if (!entryName.startsWith("/")) {
					entryName = "/" + entryName;
				}
				if (entryName.equals(name)) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					copy(zipIn, out);
					zipIn.closeEntry();
					return out.toByteArray();
				}
				zipIn.closeEntry();
			}
		} catch (IOException e) {
			// if this happens we have a real problem
			e.printStackTrace();
		} finally {
			zipIn.close();
		}
		return null;
	}

	public static long copy(InputStream in, OutputStream out)
			throws IOException {
		return copy(in, out, Long.MAX_VALUE);
	}

	public static long copy(InputStream in, OutputStream out, long length)
			throws IOException {
		try {
			long copied = 0;
			int len = (int) Math.min(length, IO_BUFFER_SIZE);
			byte[] buffer = new byte[len];
			while (length > 0) {
				len = in.read(buffer, 0, len);
				if (len < 0) {
					break;
				}
				if (out != null) {
					out.write(buffer, 0, len);
				}
				copied += len;
				length -= len;
				len = (int) Math.min(length, IO_BUFFER_SIZE);
			}
			return copied;
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public static byte[] readBytesAndClose(InputStream in, int length)
			throws IOException {
		try {
			if (length <= 0) {
				length = Integer.MAX_VALUE;
			}
			int block = Math.min(IO_BUFFER_SIZE, length);
			ByteArrayOutputStream out = new ByteArrayOutputStream(block);
			copy(in, out, length);
			return out.toByteArray();
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			in.close();
		}
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

}
