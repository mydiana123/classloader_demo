package top.wjb114514.utils;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * @author wjb
 * @date 2024/6/12 0:28 周三
 */
public class MyFileClassLoader extends URLClassLoader {
    public MyFileClassLoader(URL[] urls) {
        super(urls);
    }

    // 0. 如果是我们自己的类，就自己加载，否则委托给父类
    // 1. 从类仓库里找到字节码文件，将其放入方法区
    // 2. 生成堆中的class对象 1+2 = findClass
    // 3. 如果没进行符号解析，则首次解析 3 = resolveClass
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        // 如果类是我们自己的类，则直接加载，否则给父类加载

        // 1. 从方法区查看是否已经加载过
        Class<?> aClass = findLoadedClass(name);

        // 2. 如果首次加载
        if (aClass == null) {
            // 2.1 如果要加载的类是我们自己的类，则自己加载
            if (name.startsWith("top.wjb114514.demo")) {
                // findClass: 从给定的URL类文件的仓库寻找对应类名，找到的话就加载，并返回内存里的class对象，找不到就丢一个ClassNotFoundException
                aClass = findClass(name);
            } else {
                // 2.2 系统类按照双亲委派机制加载
                aClass = this.getParent().loadClass(name);
            }
        }

        // 3. 如果目前只是把类加载进了方法区，在堆上生成了其class对象，还没进行符号解析，则进行符号解析。
        if (resolve) {
            resolveClass(aClass);
        }
        return aClass;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 1. 将类的全限定名转换为class文件名
        String classFilePath = name.replace(".", "\\") + ".class";

        List<String> allFilePaths = new ArrayList<>();

        for (URL url : getURLs()) {
            allFilePaths.add(
                    url.getPath() + "\\" + classFilePath
            );
        }
        System.out.println(allFilePaths);
        for (String filePath : allFilePaths) {
            File file = new File(filePath);
            // 1. 检查是否存在文件
            if (!file.exists() || !file.isFile()) {
                continue;
            }
            // 2. 否则将字节码文件读到字节数组里
            FileInputStream fis = null;
            ByteOutputStream bos = null;
            try {
                fis = new FileInputStream(file);
                bos = new ByteOutputStream();

                int size = 0;
                byte[] buf = new byte[1024];

                // 2.1 把文件内容分批次读到buffer里，然后把读到的内容写到Bos里
                while ((size = fis.read(buf)) != -1) {
                    // 本轮读到了size个字节到buf里，全写入bos的缓冲区
                    bos.write(buf, 0, size);
                }

                // 2.2 最终通过defineClass 将字节码加入内存，并返回class对象
                byte[] bytes = bos.toByteArray();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {

                    if (fis != null) fis.close();
                    if (bos != null) bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 3. 如果走到这里，说明没处理任何一个类，反应无法在任何url里找到希望的类，直接扔一个找不到类的异常
        throw new ClassNotFoundException(
                "Can't Find Any Class named " + name + " in URLs: " + Arrays.toString(getURLs())
        );
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new UnsupportedOperationException(
                "This ClassLoader Not Support find Resource from Anywhere" +
                        " So it default delegate to its parent ClassLoader, aka AppClassLoader to getResource" +
                        " In this implement, this method is forbidden"
        );
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        throw new UnsupportedOperationException(
                "This ClassLoader Not Support find Resource from Anywhere" +
                        " So it default delegate to its parent ClassLoader, aka AppClassLoader to getResource" +
                        " In this implement, this method is forbidden"
        );
    }


    private String[] resolveName(String name) {
        if (name.startsWith("/")) {
            // 如果是绝对路径，不处理，否则拼一个串
            return new String[]{name};
        } else {
            // 否则直接从自己所有的url里拼出合法的路径
            List<String> paths = new ArrayList<>();
            for (URL url : getURLs()) {
                paths.add(
                        url.getPath() + "\\" + name
                );
            }
            String[] ss = new String[paths.size()];
            paths.toArray(ss);
            return ss;
        }
    }

    @Override
    public URL findResource(String name) {
        // 如果自己的父亲找不到，就自己来找

        // 1. 解析路径
        String[] names = resolveName(name);

        for (String pathName : names) {
            // 2. 判断路径是否合法
            File f = new File(pathName);
            if (!f.exists() || !f.isFile()) {
                continue;
            }

            try {
                return new URL("file", null, f.getCanonicalPath());
            } catch (IOException e) {
                // ignore
            }
        }

        System.err.println("Warning: Can't find file " + name + " in any urls " + Arrays.toString(getURLs()));
        return null;
    }

    @Override
    public URL getResource(String name) {
        return super.getResource(name);
    }
}
