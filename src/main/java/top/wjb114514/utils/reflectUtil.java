package top.wjb114514.utils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author wjb
 * @date 2024/6/11 20:27 周二
 */
public class reflectUtil {

    public static URLClassLoader of(File[] f1, File[] f2) throws Exception {
        return FileRepositoryClassLoaderFactory.createClassLoader(f1, f2);
    }

    /**
     * 生成 将一个任意的文件路径作为 class文件的仓库，并可以加载里面的类
     */
    public static class FileRepositoryClassLoaderFactory {

        /**
         * @param unpacked 没打包的文件路径，也就是文件路径下不存在 jar, zip这样压缩后的文件
         * @param packed   文件路径下存在 jar包等压缩后的文件
         * @return URLClassLoader 相比一般的ClassLoader， 可以以流的方式操作Resource(aka 字节码文件)
         */
        public static MyFileClassLoader createClassLoader(File[] unpacked, File[] packed) throws Exception {

            ArrayList<URL> urls = new ArrayList<>();

            // 1. 从unpacked路径中解为URL对象
            if (unpacked != null) {
                for (File f : unpacked) {
                    // 1.1 f必须存在，并且是一个目录，并且具有可读权限
                    if (!f.isDirectory() || !f.exists() || !f.canRead()) {
                        continue;
                    }

                    // 1.2 把File封装为URL对象, 这个URL就是java.net包下的，这里我们可能疑惑，类加载和url扯上啥关系了?
                    // 理解 file:/// 这样访问本地路径也可以称之为url。
                    // 文件协议没有主机信息，只有路径信息，我们希望获取传入文件的全路径名, 这个getCanonicalPath可以理解为正式的AbsolutePath, 在任何系统都代表绝对不会重复的一个文件路径
                    URL url = new URL("file", null, f.getCanonicalPath());
                    urls.add(url);
                }
            }

            if (packed != null) {
                // 2. 从packed中解析，这里要注意，如果目录下存在jar包，jar包也要作为一个url
                for (File f : packed) {
                    // 2.1 f必须存在，并且是一个目录，并且具有可读权限
                    if (!f.isDirectory() || !f.exists() || !f.canRead()) {
                        continue;
                    }

                    // 2.2 扫描目录下所有文件，看是否存在jar包
                    // 其实框架大部分工作都在类加载上下了功夫，大量的文件操作，jdk7好像有一个tree-traversal类，但我们就不看这么多了
                    File[] files = f.listFiles();
                    if (files == null) {
                        continue;
                    }

                    for (File file : files) {
                        if (file.getName().endsWith(".jar")) {
                            // jar包就是URL
                            URL url = new URL("file", null, file.getCanonicalPath());
                            urls.add(url);
                        }
                    }
                }
            }
            // 3. 将收集到的所有url，用于构造URLClassloader的类仓库地址
            URL[] urlArr = new URL[urls.size()];
            urls.toArray(urlArr);
            System.out.println(Arrays.toString(urlArr));

            return new MyFileClassLoader(urlArr);
        }
    }

}
