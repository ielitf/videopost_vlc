package com.ceiv.communication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MD5Util
{
  protected static char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
  protected static MessageDigest messagedigest = null;
  
  static
  {
    try
    {
      messagedigest = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException nsaex)
    {
      System.err.println(MD5Util.class.getName() + "初始化失败，MessageDigest不支持MD5Util。");
      nsaex.printStackTrace();
    }
  }
  
  public static String getFileMD5String(File file)
    throws IOException
  {
    FileInputStream in = new FileInputStream(file);
    FileChannel ch = in.getChannel();
    
    int maxSize = 700000000;
    
    long startPosition = 0L;
    long step = file.length() / maxSize;
    if (step == 0L)
    {
      MappedByteBuffer byteBuffer = ch.map(MapMode.READ_ONLY, 0L, file.length());
      messagedigest.update(byteBuffer);
      ch.close();
      in.close();
      return bufferToHex(messagedigest.digest());
    }
    for (int i = 0; i < step; i++)
    {
      MappedByteBuffer byteBuffer = ch.map(MapMode.READ_ONLY, startPosition, maxSize);
      messagedigest.update(byteBuffer);
      startPosition += maxSize;
    }
    if (startPosition == file.length())
    {
      ch.close();
      in.close();
      return bufferToHex(messagedigest.digest());
    }
    MappedByteBuffer byteBuffer = ch.map(MapMode.READ_ONLY, startPosition, file.length() - startPosition);
    messagedigest.update(byteBuffer);
    
    ch.close();
    in.close();
    return bufferToHex(messagedigest.digest());
  }
  
  public static String getFileMD5String(File file, int bufSize)
    throws IOException
  {
    FileInputStream in = new FileInputStream(file);
    byte[] buffer = new byte[bufSize];
    int numRead = 0;
    while ((numRead = in.read(buffer)) > 0) {
      messagedigest.update(buffer, 0, numRead);
    }
    in.close();
    return bufferToHex(messagedigest.digest());
  }
  
//  public static String getMultipartFileMd5(MultipartFile upload)
//    throws Exception
//  {
//    byte[] bytes = new byte[2097152];
//    upload.getInputStream().read(bytes);
//    MessageDigest md5 = MessageDigest.getInstance("MD5");
//    byte[] digest = md5.digest(bytes);
//    String hashString = new BigInteger(1, digest).toString(16);
//    return hashString.toUpperCase();
//  }
  
  public static String getFileMD5StringByNio(File file, int bufSize)
    throws IOException
  {
    FileInputStream in = new FileInputStream(file);
    FileChannel fc = in.getChannel();
    ByteBuffer bf = ByteBuffer.allocate(bufSize);
    while (fc.read(bf) != -1)
    {
      bf.flip();
      messagedigest.update(bf);
      bf.clear();
    }
    fc.close();
    in.close();
    return bufferToHex(messagedigest.digest());
  }
  
  public static String getFileMD5String(File file, int bufSize, boolean nio)
    throws IOException
  {
    if (nio) {
      return getFileMD5StringByNio(file, bufSize);
    }
    return getFileMD5String(file, bufSize);
  }
  
  public static String getMD5String(String s)
  {
    return getMD5String(s.getBytes());
  }
  
  public static String getMD5String(byte[] bytes)
  {
    messagedigest.update(bytes);
    return bufferToHex(messagedigest.digest());
  }
  
  private static String bufferToHex(byte[] bytes)
  {
    return bufferToHex(bytes, 0, bytes.length);
  }
  
  private static String bufferToHex(byte[] bytes, int m, int n)
  {
    StringBuffer stringbuffer = new StringBuffer(2 * n);
    int k = m + n;
    for (int l = m; l < k; l++) {
      appendHexPair(bytes[l], stringbuffer);
    }
    return stringbuffer.toString();
  }
  
  private static void appendHexPair(byte bt, StringBuffer stringbuffer)
  {
    char c0 = hexDigits[((bt & 0xF0) >> 4)];
    char c1 = hexDigits[(bt & 0xF)];
    stringbuffer.append(c0);
    stringbuffer.append(c1);
  }
  
  public static boolean checkPassword(String password, String md5PwdStr)
  {
    String s = getMD5String(password);
    return s.equals(md5PwdStr);
  }
  
  public static void main(String[] args)
    throws IOException
  {
    long begin = System.currentTimeMillis();
    
    File big = new File("E:/flv/stream_test_HD_02.ts");
    
    String md5 = getFileMD5String(big, 8196, false);
    
    long end = System.currentTimeMillis();
    System.out.println("md5:" + md5 + " time:" + (end - begin) / 1000L + "s");
  }
}
