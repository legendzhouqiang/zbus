/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.mq.disk;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.zbus.kit.log.Logger;

/**
 *  Many thanks to @huamingweiwen in OSChina who presented us a light weight solution
 *  of disk-based queue design and implementation, DiskQueuePool is mainly refined from
 *  his brilliant work.
 *  
 *  http://my.oschina.net/xnkl/blog/477690
 */
public class DiskQueuePool { 
    static final Logger log = Logger.getLogger(DiskQueuePool.class);
    private static final BlockingQueue<String> deletingQueue = new LinkedBlockingQueue<String>();
    
    private static DiskQueuePool instance = null;
    private String fileBackupPath;
    private Map<String, DiskQueue> queueMap;
    private ScheduledExecutorService syncService;
 
    private DiskQueuePool(String fileBackupPath) {
        this.fileBackupPath = fileBackupPath;
        File fileBackupDir = new File(fileBackupPath);
        if (!fileBackupDir.exists() && !fileBackupDir.mkdir()) {
            throw new IllegalArgumentException("can not create directory");
        }
        this.queueMap = scanDir(fileBackupDir);
        this.syncService = Executors.newSingleThreadScheduledExecutor();
        this.syncService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (DiskQueue q : queueMap.values()) {
                    q.sync();
                }
                deleteBlockFile();
            }
        }, 1000, 10000, TimeUnit.MILLISECONDS);
    }
    
    public static Map<String, DiskQueue> getQueryMap(){
    	return instance.queueMap;
    }
 
    private void deleteBlockFile() {
        String blockFilePath = null;
        while( (blockFilePath = deletingQueue.poll()) != null){
        	blockFilePath = blockFilePath.trim();
        	if(blockFilePath.equals("")) continue;
        	log.info("Delete File[%s]", blockFilePath);   
            File delFile = new File(blockFilePath);
            try {
                if (!delFile.delete()) {
                    log.warn("block file:%s delete failed", blockFilePath);
                }
            } catch (SecurityException e) {
                log.error("security manager exists, delete denied");
            } 
        }
    }
 
    static void toClear(String filePath) {
        deletingQueue.add(filePath);
    }
 
    private Map<String, DiskQueue> scanDir(File fileBackupDir) {
        if (!fileBackupDir.isDirectory()) {
            throw new IllegalArgumentException("it is not a directory");
        }
        Map<String, DiskQueue> queues = new HashMap<String, DiskQueue>();
        File[] indexFiles = fileBackupDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return DiskQueueIndex.isIndexFile(name);
            }
        });
        if (indexFiles != null && indexFiles.length> 0) {
            for (File indexFile : indexFiles) {
                String queueName = DiskQueueIndex.parseQueueName(indexFile.getName());
                queues.put(queueName, new DiskQueue(queueName, fileBackupPath));
            }
        }
        return queues;
    }
 
    public synchronized static void init(String deployPath) {
        if (instance == null) {
            instance = new DiskQueuePool(deployPath);
        }
    }
 
    private void dispose() {
        this.syncService.shutdown();
        for (DiskQueue q : queueMap.values()) {
            q.close();
        }
        while (!deletingQueue.isEmpty()) {
            deleteBlockFile();
        }
    }
 
    public synchronized static void destory() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }
 
    private DiskQueue getQueueFromPool(String queueName) {
        if (queueMap.containsKey(queueName)) {
            return queueMap.get(queueName);
        }
        DiskQueue q = new DiskQueue(queueName, fileBackupPath);
        queueMap.put(queueName, q);
        return q;
    }
 
    public synchronized static DiskQueue getDiskQueue(String queueName) {
    	if(instance == null){
    		throw new IllegalStateException("call DiskQueuePool.init(dir) first");
    	}
        if (queueName==null || queueName.trim().equals("")) {
            throw new IllegalArgumentException("empty queue name");
        }
        return instance.getQueueFromPool(queueName);
    }
 
}