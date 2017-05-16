using System;
using System.Collections.Concurrent;
using System.Threading;

namespace Zbus.Mq.Net
{

    public interface IPoolable : IDisposable
    {
        bool Active { get; }
    }

    public class Pool<T> : IDisposable
       where T : IPoolable
    {
        public int MaxCount
        {
            get { return maxCount; }
            set { maxCount = value; }
        }

        public int Count
        {
            get { return count; }
        }

        public Func<T> Generator { get; set; }
        private ConcurrentBag<T> bag = new ConcurrentBag<T>();
        private AutoResetEvent notFullEvent = new AutoResetEvent(false);
        private int maxCount = 32;
        private int count = 0;


        public T Borrow()
        {
            T value;
            bool ok = bag.TryTake(out value);
            if (ok)
            {
                if (value.Active)
                {
                    return value;
                }
                else
                {
                    HandleInactive(value);
                    return Borrow();
                }
            }
            if (count < maxCount)
            {
                value = Generator();
                Interlocked.Increment(ref count);
                return value;
            }
            notFullEvent.WaitOne();
            return Borrow();

        }


        public void Return(T value)
        {
            if (!value.Active)
            {
                HandleInactive(value);
            }
            else
            {
                bag.Add(value);
                notFullEvent.Set();
            }
        }

        private void HandleInactive(T value)
        {
            if (!value.Active)
            {
                value.Dispose();
                Interlocked.Decrement(ref count);
            }
        }

        public void Dispose()
        {
            foreach (T value in bag)
            {
                value.Dispose();
            }
        }
    }
}
