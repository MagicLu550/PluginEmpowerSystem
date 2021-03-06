package net.noyark.www.utils.encode;


import net.noyark.www.utils.Message;
import net.noyark.www.utils.Coder;
import net.noyark.www.utils.jar.DecodeJar;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.*;
import java.util.*;

public class Util
{
    private static InputStream readApplication;
    // 把文件读入byte数组
    static public byte[] readFile(String filename) throws IOException {
        File file = new File(filename);
        long len = file.length();
        return readFile(new FileInputStream(file),len);
    }

    static public byte[] readFile(InputStream in,long len) throws IOException{

        byte[] bytes = new byte[(int)len];
        in.read(bytes);
        in.close();
        return bytes;

    }

    // 把byte数组写出到文件
    static public void writeFile(String filename, byte data[]) throws IOException {
        FileOutputStream fout = new FileOutputStream(filename);
        fout.write(data);
        fout.close();
    }

    static public void writeFile(String fileName,String data) throws IOException{
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8")));
        writer.println(data);
        writer.close();
    }

    static public String readKeyFile(String fileName) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"UTF-8"));
        return reader.readLine();
    }

    static public void writeClassData(String filename,byte[] encryptedClassData) throws IOException{
        String out = Util.getClassOut();
        if(out.startsWith("THIS")) {
            Util.writeFile(filename + ".class", encryptedClassData);  // 保存加密后的内容
        }else{
            Util.writeFile(out,encryptedClassData);
        }
    }

    /**
     * classpath:file
     * @param file
     */
    public static void setReadApplication(String file){
        try{
            if(file.startsWith("classpath:")){
                readApplication = Util.class.getResourceAsStream(file.replace("classpath:",""));
            }else{
                readApplication = new FileInputStream(file);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static File getClassFile(){
        return new File(getJarInFIle()+"classes");
    }

    public static File getKeyFile(){
        return new File(getJarInFIle()+"/keyfile");
    }

    public static File getJarInFIle(){
        return new File(Util.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
    }

    public static String getClassPath(String name){
        String filename = null;
        try{
            Properties properties = new Properties();
            InputStream in;
            //自定义配置文件位置
            if(readApplication == null){
                in = new FileInputStream(getJarInFIle()+"/application.properties");
            }else{
                in = readApplication;
            }
            if(in == null){
                filename = getJarInFIle()+"/"+name;
            }else{
                properties.load(in);
                String cp = properties.getProperty("classpath");
                if(cp.startsWith("now:")){
                    filename = (cp.replace("now:",getJarInFIle().toString()+"/")+"/"+name).replace(".","/");
                }

            }

        }catch (IOException e){
            e.printStackTrace();
        }
        return filename;
    }


    public static String getClassOut(){
        try{
            Properties properties = new Properties();
            properties.load(getInputStreamOfApplication());
            String to = properties.getProperty("encode.to");
            if(to.equals("this")){
                return "THIS:在当前的class文件所在地，会被覆盖";
            }else{
                //输出在out文件
                return getJarInFIle()+"/"+to;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public static InputStream getInputStreamOfApplication() throws IOException{
        InputStream in;
        if(readApplication == null){
            in = new FileInputStream(getJarInFIle()+"/application.properties");
        }else{
            in = readApplication;
        }
        return in;
    }

    public static boolean getDecodeMessageOut(){
        try{
            InputStream in =getInputStreamOfApplication();
            Properties properties = new Properties();
            properties.load(in);
            return Boolean.parseBoolean(properties.getProperty("decode.message"));
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public static SecretKey readKey(String keyFilename) throws Exception{
        // 读取密匙
        if(Util.getDecodeMessageOut()) {
            Message.info("[DecryptStart: reading key]");
        }
        byte rawKey[] = Util.readFile(keyFilename);
        DESKeySpec dks = new DESKeySpec(rawKey);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(dks);
    }


    public static Class<?> getClassInJar(String jarFile, String classname, String keyFile, Coder.DecryptMethod method) {
        return getClassInJar(jarFile,classname,keyFile,null,method);
    }


    public static Class<?> getClassInJar(String jarFile, String classname, String keyFile, ClassLoader loader, Coder.DecryptMethod method) {

        try{
            return new DecodeJar(jarFile,keyFile,method.getMethod()).getDecodeClass(classname,loader);
        }catch (Exception e){
            return null;
        }
    }

}
