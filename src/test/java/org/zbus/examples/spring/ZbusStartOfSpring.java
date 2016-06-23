package org.zbus.examples.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zbus.mq.server.MqServer;


public class ZbusStartOfSpring
{
    
    public static void main(String[] args)
    {
           ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {
                "spring.xml"
            });
            context.start();
            
            try
            {
                MqServer mqServer =  (MqServer)context.getBean("mqServer");
                mqServer.start();
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
    }

}
