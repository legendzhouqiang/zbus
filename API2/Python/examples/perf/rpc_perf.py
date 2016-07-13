#encoding=utf8
import sys  
from threading import Thread, Lock
from time import time
from zbus import SingleBroker, Rpc 

sys.path.append('../../') 


class AtomicCounter:
    def __init__(self, init_value):
        self.counter = init_value
        self.lock = Lock()
        
    def increase_and_get(self):
        try:
            self.lock.acquire()
            self.counter = self.counter + 1
        finally:
            self.lock.release()
            return self.counter
    def get(self):
        return self.counter



import logging
class Task:
    log = logging.getLogger(__name__)
    def __init__(self): 
        self.rpc = None
        self.start_time = None 
        self.loop_count = None
        self.counter = None
        self.fail_counter = None
        
    def run(self):
        if self.rpc is None: 
            return
        for i in xrange(self.loop_count):
            try:
                total = self.counter.increase_and_get()
                fail = self.fail_counter.get()
                self.rpc.getUserScore()
                if (total+1)%1000 == 0: 
                    qps = '%.2f'%(total/(time()-self.start_time))
                    msg = "QPS: %s, Failed/Total=%s/%s(%.2f%s)"%(qps, fail, total, fail*100.0/total, '%')
                    self.log.info(msg)
            except Exception, e:
                self.log.info(e)
                self.fail_counter.increase_and_get()
   
def task_run(task):
    task.run()
    

start_time = time()
thread_count = 100
thread_loop_count = 100000000

broker = SingleBroker(host='127.0.0.1', port=15555, maxsize=thread_count)
rpc = Rpc(broker=broker, 
          mq='MyRpc', 
          module='Interface',
          encoding='utf8',
          timeout=10)

counter = AtomicCounter(0)
fail_counter = AtomicCounter(0) 
threads = []
for i in range(thread_count):
    task = Task()
    task.rpc = rpc 
    task.start_time = start_time
    task.counter = counter
    task.fail_counter = fail_counter
    task.loop_count = thread_loop_count
    
    thread = Thread(target=task_run, args=(task,))
    threads.append(thread)
    
for thread in threads:
    thread.start()
    
for thread in threads:
    thread.join()
    
    







