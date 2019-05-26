package net.noyark.www.utils.jar;

import net.noyark.www.utils.Message;
import net.noyark.www.utils.encode.DecryptStart;
import net.noyark.www.utils.encode.Util;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;

/**
 * 将加密的jar包运行
 */

public class DecodeJar {

    private File jarFile;

    private String keyFile;

    public DecodeJar(String jarFileName,String keyFile) {
        this.jarFile = new File(jarFileName);
        this.keyFile = keyFile;
    }

    public void runJar(String[] args) throws IllegalAccessException,NoSuchMethodException, InvocationTargetException {
        Class<?> claz = getMainClass();
        if(claz == null){
            Message.error("没有主清单目录");
            return;
        }
        claz.getMethod("main",String[].class).invoke(null,(Object) args);
    }

    public Class<?> getMainClass(){
        try{
            URL url = jarFile.toURI().toURL();
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url},this.getClass().getClassLoader());

            InputStream in = urlClassLoader.getResourceAsStream("META-INF/MANIFEST.MF");
            if(in == null){
                throw new JarException("这个jar文件无法被运行，由于它没有MANIFEST.MF");
            }else{
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                String main_class = null;
                while ((line = reader.readLine())!=null){
                    if(line.startsWith("Main-Class")){
                        String[] entry = line.split(":");
                        main_class = entry[1].trim();
                    }
                }
                if(main_class == null){
                    return null;
                }
                JarFile jarFile = new JarFile(this.jarFile);
                Enumeration<JarEntry> entries = jarFile.entries();
                long len = 0;
                String path = main_class.replace(".","/");
                while (entries.hasMoreElements()){
                    JarEntry jar = entries.nextElement();
                    if(jar.getRealName().equals(path+".class")) {
                        len = jar.getSize();
                    }
                }
                InputStream main = urlClassLoader.getResourceAsStream(path);
                DecryptStart start = new DecryptStart(Util.readKey(keyFile),main,len);
                return start.loadClass(main_class);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
