using System;
using System.Text;
using System.IO;

namespace Zbus.Net
{
    public class IoBuffer
    {
        private int mark;
        private int position;
        private int limit;
        private int capacity;
        private byte[] data;

        public byte[] Data
        {
            get { return this.data; }
        }
        public int Limit
        {
            get { return this.limit; }
        }

        public int Capacity
        {
            get { return this.capacity; }
        }

        public int Position
        {
            get { return this.position; }
        }


        public IoBuffer(): this(256) { }
        public IoBuffer(byte[] data)
        {
            this.data = data;
            this.capacity = data.Length;
            this.limit = this.capacity;
            this.mark = -1;
            this.position = 0;
        }

        public IoBuffer(int capacity):this(new byte[capacity])
        {
        }

        public IoBuffer Duplicate()
        {
            IoBuffer buf = new IoBuffer(this.data);
            buf.capacity = this.capacity;
            buf.limit = this.limit;
            buf.mark = -1;
            buf.position = this.position;

            return buf;
        }

        public int Move(int leftwardCount)
        {
            if (leftwardCount > this.position) return 0;
            Buffer.BlockCopy(this.data, leftwardCount, this.data, 0, this.position - leftwardCount);
            this.position -= leftwardCount;
            if (this.mark > this.position) this.mark = -1;
            return leftwardCount;
        }

        public void Mark()
        {
            this.mark = this.position;
        }
        public void Reset()
        {
            int m = this.mark;
            if (m < 0)
            {
                throw new System.SystemException("reset state invalid");
            }
            this.position = this.mark;
        }
        public int Remaining()
        {
            return this.limit - this.position;
        }
        public void Flip()
        {
            this.limit = this.position;
            this.position = 0;
            this.mark = -1;
        }

        public void SetNewLimit(int newLimit)
        {
            if (newLimit > this.capacity || newLimit < 0)
            {
                throw new System.SystemException("new limit invalid");
            }
            this.limit = newLimit;
            if (this.position > this.limit) this.position = this.limit;
            if (this.mark > this.limit) this.mark = -1;
        }

        private void AutoExpand(int need)
        {
            int newCap = this.capacity;
            int newSize = this.position + need;
            while (newSize > newCap)
            {
                newCap *= 2;
            }
            if (newCap == this.capacity) return;//nothing changed

            byte[] newData = new byte[newCap];
            Buffer.BlockCopy(this.data, 0, newData, 0, this.data.Length);
            this.data = newData;
            this.capacity = newCap;
            this.limit = newCap;
        }

        public string ReadAllToString()
        {
            return Encoding.Default.GetString(this.data, 0, this.position);
        }

        public void Drain(int n)
        { 
            int newPos = this.position + n; 
            if (newPos > this.limit)
            {
                newPos = this.limit; 
            }
            this.position = newPos;
            if (this.mark > this.position) this.mark = -1; 
        }

        public void Put(string format, params object[] args){
            Put(string.Format(format, args));
        }

        public void Put(byte[] data, int offset, int count)
        {
            AutoExpand(count);
            Buffer.BlockCopy(data, offset, this.data, this.position, count);
            Drain(count);
        }

        public void Put(byte[] data)
        {
            Put(data, 0, data.Length);
        }

        public void Put(string data, Encoding encoding)
        {
            Put(encoding.GetBytes(data));
        }
        public void Put(string data)
        {
            Put(data, Encoding.Default);
        }

        public void PutKeyValue(string key, object value)
        {
            Put(key);
            Put(": ");
            Put(value.ToString());
            Put("\r\n");
        }

        public int Copyout(byte[] copy)
        {
            if (this.Remaining() < copy.Length)
            {
                return -1;
            }
            Buffer.BlockCopy(this.data, this.position, copy, 0, copy.Length);
            return copy.Length;
        }

        public byte[] Get(int len)
        {
            byte[] copy = new byte[len];
            int res = Copyout(copy);
            if (res != copy.Length)
            {
                return null;
            }
            Drain(len);
            return copy;
        }

        public override string ToString()
        {
            return Encoding.Default.GetString(this.data, 0, this.position);
        }

        public void WriteTo(Stream stream)
        {
            stream.Write(this.data, 0, this.position);
        }
    }
}
