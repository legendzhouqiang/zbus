// File generated by hadoop record compiler. Do not edit.
/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.zbus.zookeeper.server.persistence;

import org.zbus.zookeeper.jute.*;
public class FileHeader implements Record {
  private int magic;
  private int version;
  private long dbid;
  public FileHeader() {
  }
  public FileHeader(
        int magic,
        int version,
        long dbid) {
    this.magic=magic;
    this.version=version;
    this.dbid=dbid;
  }
  public int getMagic() {
    return magic;
  }
  public void setMagic(int m_) {
    magic=m_;
  }
  public int getVersion() {
    return version;
  }
  public void setVersion(int m_) {
    version=m_;
  }
  public long getDbid() {
    return dbid;
  }
  public void setDbid(long m_) {
    dbid=m_;
  }
  public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(this,tag);
    a_.writeInt(magic,"magic");
    a_.writeInt(version,"version");
    a_.writeLong(dbid,"dbid");
    a_.endRecord(this,tag);
  }
  public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(tag);
    magic=a_.readInt("magic");
    version=a_.readInt("version");
    dbid=a_.readLong("dbid");
    a_.endRecord(tag);
}
  public String toString() {
    try {
      java.io.ByteArrayOutputStream s =
        new java.io.ByteArrayOutputStream();
      CsvOutputArchive a_ = 
        new CsvOutputArchive(s);
      a_.startRecord(this,"");
    a_.writeInt(magic,"magic");
    a_.writeInt(version,"version");
    a_.writeLong(dbid,"dbid");
      a_.endRecord(this,"");
      return new String(s.toByteArray(), "UTF-8");
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
    return "ERROR";
  }
  public void write(java.io.DataOutput out) throws java.io.IOException {
    BinaryOutputArchive archive = new BinaryOutputArchive(out);
    serialize(archive, "");
  }
  public void readFields(java.io.DataInput in) throws java.io.IOException {
    BinaryInputArchive archive = new BinaryInputArchive(in);
    deserialize(archive, "");
  }
  public int compareTo (Object peer_) throws ClassCastException {
    if (!(peer_ instanceof FileHeader)) {
      throw new ClassCastException("Comparing different types of records.");
    }
    FileHeader peer = (FileHeader) peer_;
    int ret = 0;
    ret = (magic == peer.magic)? 0 :((magic<peer.magic)?-1:1);
    if (ret != 0) return ret;
    ret = (version == peer.version)? 0 :((version<peer.version)?-1:1);
    if (ret != 0) return ret;
    ret = (dbid == peer.dbid)? 0 :((dbid<peer.dbid)?-1:1);
    if (ret != 0) return ret;
     return ret;
  }
  public boolean equals(Object peer_) {
    if (!(peer_ instanceof FileHeader)) {
      return false;
    }
    if (peer_ == this) {
      return true;
    }
    FileHeader peer = (FileHeader) peer_;
    boolean ret = false;
    ret = (magic==peer.magic);
    if (!ret) return ret;
    ret = (version==peer.version);
    if (!ret) return ret;
    ret = (dbid==peer.dbid);
    if (!ret) return ret;
     return ret;
  }
  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)magic;
    result = 37*result + ret;
    ret = (int)version;
    result = 37*result + ret;
    ret = (int) (dbid^(dbid>>>32));
    result = 37*result + ret;
    return result;
  }
  public static String signature() {
    return "LFileHeader(iil)";
  }
}
