package com.face;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 *  Author : Nhu Dinh Thuan
 *          Email:nhudinhthuan@yahoo.com
 * Jun 4, 2007
 */
public class RWData {

  private static final RWData writer = new RWData();

  public final static RWData getInstance() { return writer; }

  private ConcurrentHashMap<String, LockData> lock = new ConcurrentHashMap<String, LockData>();

  protected Logger       log;

  public RWData() {
    log = Logger.getLogger(getClass());
  }

  //*********************************************** writing ************************************

  public void save(File file, byte[] data) throws Exception {
    if(file.isDirectory()) return;  

    wait(file.getAbsolutePath(), LockData.WRITE);
    lock.put(file.getAbsolutePath(), new LockData(LockData.WRITE));    

    if(!file.exists()) file.createNewFile();          
    FileOutputStream output = null;
    try {
      output = new FileOutputStream(file);
      output.write(data);
      //      save(output, data);    
    } finally {
      try {
        if(output != null) output.close();
      } catch (Exception e) {
        log.error(e);
      }
      lock.remove(file.getAbsolutePath());
    }
  }

  public void append(File file, byte[] data) throws Exception {
    if(file.isDirectory()) return ;

    wait(file.getAbsolutePath(), LockData.WRITE);
    lock.put(file.getAbsolutePath(), new LockData(LockData.WRITE));

    if(!file.exists()) file.createNewFile();    
    FileOutputStream output = null;
    try {
      output = new FileOutputStream(file, true);
      append(output, data);
      output.close();
    } finally {
      try {
        if(output != null) output.close();
      } catch (Exception e) {
        log.error(e);
      }
      lock.remove(file.getAbsolutePath());
    }
  }

  private void append(FileOutputStream output, byte[] data) throws Exception {
    FileChannel channel = null;
    try { 
      channel = output.getChannel();
      append(channel, data);
      channel.close();
    } finally {
      try {
        if(channel != null) channel.close();
      } catch (Exception e) {
        log.error(e);
      }
    }
  } 

  private void append(FileChannel channel, byte[] data) throws Exception {
    ByteBuffer buff = ByteBuffer.allocateDirect(data.length);
    buff.put(data);
    buff.rewind();
    if(channel.isOpen()) channel.write(buff);
    buff.clear(); 
  }

  public void save(File file, InputStream input) throws Exception {
    save(file, input, false);
  }

  public void save(File file, InputStream input, boolean append) throws Exception {
    if(file.isDirectory()) return;  

    wait(file.getAbsolutePath(), LockData.WRITE);
    lock.put(file.getAbsolutePath(), new LockData(LockData.WRITE));

    if(!file.exists()) file.createNewFile();          
    FileOutputStream output = null;
    try {
      output = new FileOutputStream(file, append);
      save(output, input);
      output.close();
    } finally {
      try {
        if(output != null) output.close();
      } catch (Exception e) {
        log.error(e);
      }
      lock.remove(file.getAbsolutePath());
    }
  }

  public void save(FileOutputStream output, InputStream input) throws Exception {
    byte [] bytes = new byte[4*1024];
    int read = 0;
    while((read = input.read(bytes)) > -1) {
      output.write(bytes, 0, read);
    }
    output.flush();
  } 

  public void copy(String from, String to) throws Exception {
    File src = new File(from);
    File des = new File(to);
    copy(src, des);
  }

  public void copy(File from, File to) throws Exception {
    FileInputStream inputStream = null;
    FileOutputStream outputStream = null;

    wait(from.getAbsolutePath(), LockData.READ);
    lock.put(from.getAbsolutePath(), new LockData(LockData.READ));

    wait(to.getAbsolutePath(), LockData.WRITE);
    lock.put(to.getAbsolutePath(), new LockData(LockData.WRITE));

    try {
      outputStream = new FileOutputStream(to);
      inputStream = new FileInputStream(from);

      copy(inputStream, outputStream);

      outputStream.close();
      inputStream.close();
    } finally {
      try {
        if(outputStream != null) outputStream.close();
      } catch (Exception e) {
        log.error(e);
      }

      try {
        if(inputStream != null) inputStream.close();
      } catch (Exception e) {
        log.error(e);
      }

      lock.remove(from.getAbsolutePath());
      lock.remove(to.getAbsolutePath());
    }
  }

  private void copy(FileInputStream inputStream, FileOutputStream outputStream) throws Exception {
    FileChannel srcChannel = null;
    FileChannel desChannel = null;

    try {
      srcChannel = inputStream.getChannel();
      desChannel = outputStream.getChannel();

      srcChannel.transferTo(0, srcChannel.size(), desChannel);

      srcChannel.close();
      desChannel.close();
    } finally {
      try {
        if(srcChannel != null) srcChannel.close();
      } catch (Exception e) {
        log.error(e);
      }

      try {
        if(desChannel != null) desChannel.close();
      } catch (Exception e) {
        log.error(e);
      }
    }
  }

  //*********************************************** reading ************************************


  public byte[] load(String path) throws Exception {
    return load(new File(path));
  }

  public String[] loadLines(File file) throws Exception {
    String value = new String(load(file));
    String[] elemnents = value.split("\n");
    for(int i = 0; i < elemnents.length; i++) {
      elemnents[i] = elemnents[i].trim();
    }
    return elemnents;
  }

  public byte[] load(File file) throws Exception {
    if (!file.exists() || !file.isFile()) return new byte[0];

    wait(file.getAbsolutePath(), LockData.READ);
    lock.put(file.getAbsolutePath(), new LockData(LockData.READ));

    try (FileInputStream input = new FileInputStream(file)) {
      return load(input, file.length());    
    } finally {
      lock.remove(file.getAbsolutePath());
    }
  }

  private byte[] load(FileInputStream input, long fsize) throws Exception {
    try (FileChannel channel = input.getChannel()) {
      ByteBuffer buff = ByteBuffer.allocate((int)fsize);      
      channel.read(buff);
      buff.rewind();      
      byte[] data = buff.array();

      buff.clear();
      channel.close();

      return data;
    } 
  }

  public ByteArrayOutputStream loadInputStream(InputStream input) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if(input == null) return output;
    byte[] data  = new byte[1024];      
    int available = -1;
    while((available = input.read(data)) != -1) {
      output.write(data, 0, available);
    }
    //    if(output.size() == 2 
    //       && new String(output.toByteArray()).equals("-1")) {
    //      return new BytesOutputStream();
    //    }
    return output;
  }

  public void writeInputStream(InputStream input, OutputStream output) throws Exception {
    byte[] data  = new byte[1024];      
    int available = -1;
    while((available = input.read(data)) > -1) {
      output.write(data, 0, available);
    }   
  }



  //*********************************************** writing ************************************

  private void wait(String path, short mod) throws LockIOException {
    long start = System.currentTimeMillis();
    while(lock.containsKey(path)) {
      LockData lockData = lock.get(path);
      if(lockData == null) break;
      if(mod == LockData.READ 
          && lockData.operator == LockData.READ) break;
      try {
        Thread.sleep(100l);
      } catch (Exception e) {
      }
      if((System.currentTimeMillis() - start) >= 5*60*1000l) {
        throw new LockIOException(path, lockData);
      }
    }
  }

  public static class LockData {

    public static short  NONE = 0;
    public static short  READ = 1;
    public static short  WRITE = 2;

    private short operator = NONE;

    private long start;

    private LockData(short oper) {
      this.operator = oper;
      this.start = System.currentTimeMillis();
    }
  }

  @SuppressWarnings("all")
  public static class LockIOException extends Exception {

    private String path;
    private LockData lockData;

    private LockIOException(String path, LockData data) {
      this.path = path;
      this.lockData = data;
    }

    public String getMessage() {
      StringBuilder builder = new StringBuilder();
      builder.append("File locked: ").append(path);
      builder.append(", ").append(lockData.start).append(", ").append(lockData.operator);
      return builder.toString();
    }

  }


  //public static void main(String[] args) throws Exception {
  //File file = new File("F:\\Setup\\Project\\libs spider\\data\\track\\logs\\a.txt");
  //if(!file.exists()) file.createNewFile();
  //DataWriter writer = new DataWriter();
  //writer.append(file, "thuan nhu".getBytes());
  //writer.append(file, "tiep tuc".getBytes());
  //}
}
