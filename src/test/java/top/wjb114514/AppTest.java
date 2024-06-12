package top.wjb114514;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void testClassLoader() {
        // 1. 获取当前项目路径
//        URLClassLoader urlClassLoader = new URLClassLoader();
    }

    @Test
    public void testClassLoader2() throws IOException {
        // 1. 获取当前class对象的信息
        System.out.println("当前对象:" + this.getClass());
        System.out.println("当前对象类加载器:" + this.getClass().getClassLoader());
        System.out.println("当前线程上下文类加载器:" + Thread.currentThread().getContextClassLoader());

        // 2. 获取classLoader是从哪里获取到class文件的
        System.out.println("当前类加载器如何解析资源:" + this.getClass().getResource("/"));
        System.out.println("当前类加载器如何解析资源:" + this.getClass().getResource(""));

        // 3. 获取AppClassLoader 都可以加载哪里的类
        System.out.println("AppClassLoader 负责加载的类路径:");
        // URLClassLoader 用URL描述Resource对象，Resource对象则是需要被加载的class对象抽象。
        URL[] urLs = ((URLClassLoader) this.getClass().getClassLoader()).getURLs();
        for (URL url : urLs) {
            System.out.println(url.getPath());
        }

        // 4. 这些类路径是何时配置的呢? java.class.path user.dir
        Properties pp = System.getProperties();
        Enumeration<Object> keys = pp.keys();
        while (keys.hasMoreElements()) {
            String s = (String) keys.nextElement();
            String property = pp.getProperty(s);
            System.out.println("key=" + s + ",value=" + property);
        }

        // 5. classpath和project-path有什么区别呢?
        File file = new File("");
        System.out.println(file.getCanonicalPath());
    }

    @Test
    public void testResource() {
        // 1. 下面都输出啥?

        URL relativePath = this.getClass().getResource("");
        URL absPath = this.getClass().getResource("/");
        URL relativePathMapper = this.getClass().getResource("mapper/userMapper.xml");
        URL absPathMapper = this.getClass().getResource("/mapper/userMapper.xml");

        URL relativePathCL = this.getClass().getClassLoader().getResource("");
        URL absPathCL = this.getClass().getClassLoader().getResource("/");
        URL relativePathMapperCL = this.getClass().getClassLoader().getResource("mapper/userMapper.xml");
        URL absPathMapperCL = this.getClass().getClassLoader().getResource("/mapper/userMapper.xml");


        System.out.println("=============== 用 Class对象寻找资源 ================");
        System.out.println("相对路径:" + (relativePath == null ? "Null" : relativePath.getPath()));
        System.out.println("绝对路径:" + (absPath == null ? "Null" : absPath.getPath()));
        System.out.println("相对路径下的某个文件:" + (relativePathMapper == null ? "Null" : relativePathMapper.getPath()));
        System.out.println("绝对路径下的文件:" + (absPathMapper == null ? "Null" : absPathMapper.getPath()));


        System.out.println("=============== 用 ClassLoader对象寻找资源 ================");
        System.out.println("相对路径:" + (relativePathCL == null ? "Null" : relativePathCL.getPath()));
        System.out.println("绝对路径:" + (absPathCL == null ? "Null" : absPathCL.getPath()));
        System.out.println("相对路径下的某个文件:" + (relativePathMapperCL == null ? "Null" : relativePathMapperCL.getPath()));
        System.out.println("绝对路径下的文件:" + (absPathMapperCL == null ? "Null" : absPathMapperCL.getPath()));
    }
}
