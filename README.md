使用IDEA可以方便的进行一次运行，甚至一个Junit单元测试也可以直接运行，那么背后发生了什么?
如果我们有一个位于项目外的classes仓库，想使用其中的代码，又该如何应对呢？
如果我们希望加载任意位置的资源文件呢？

本章将简单实现
1. 从磁盘的任意位置加载字节码文件
2. 从磁盘任意位置加载资源文件
3. 简单解析资源文件，并用我们自己的加载器根据配置解析结果生成对象。

从源代码到被执行，发生了什么？

测试1:

// 当前路径

// C:/test

// 1. 建立一个java文件 a.java

package foo.bar

public static void main(String[] args) {
        System.out.println("Hello, world");
        a ains = new a();
        Class<?> aClass = ains.getClass();
        ClassLoader cl = aClass.getClassLoader();
        System.out.println(cl);
        
        System.out.println(System.getProperty("user.dir"));
        System.out.println(System.getProperty("java.class.path"));
        
        
}

问题1:
操作者位于当前路径
1. javac a.java 能过吗?，产物输出到哪里?
  可以: 因为没有编译错误，会在当前路径生成a.class，即C:\test\a.class
2. java a 之后，会提示 "无法找到主类a"，这是因为什么
  显然，a的全限定名是foo.bar.a，自然无法找到 "a"这个类
3. java foo.bar.a 会提示 "无法找到主类foo.bar.a" 这又是因为什么
  如果我们没配置classpath，则加载foo.bar.a这个类的加载器为AppClassLoader
  如果我们用java 命令直接运行一个全限定名为foo.bar.a，他会将包全限定名转换为路径。
  即从运行java的当前目录C:\test开始，查找foo/bar 下，看看能不能找到一个名为a的class对象。
  即foo.bar.a 会被字符串处理为foo\bar\a.class，这里\ 定义为一个平台相关的常量FILE_SPERATER
4. 究竟怎么做，才能让上面的代码运行，System.getProperty("user.dir") 输出什么?
  答案一是，在C:\test 目录下，建立foo\bar 文件夹，然后把a.class丢到里面。最后在C:\test目录运行java foo.bar.a，user.dir此时为C:\test
  答案二是: 把C:\test 加入classpath，然后把package信息去掉，然后随处都可以直接运行java a，在哪运行java a，user.dir就是哪，或者以 java -classpath "目录" a 运行之，在哪运行java，user.dir就是哪里。

为什么可以在idea里运行代码。
    // 必须严格保证项目路径和包路径一样
    // 也就是，这个文件应该位于 {user.dir}/foo/bar
    package foo.bar
    
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
}
[图片]
简单理解，Content Root作为项目的根路径，也就是user.dir。
在idea里运行java代码，相当于在这个路径上运行java {类的全限定名}。
那么，编译产物会被放在{user.dir}/target/classes，对于测试的产物则会放在{user.dir}/target/test-classes。
而这两个路径会被Java加入java.class.path这个环境变量。
加载非classpath外的class文件

现在我们的需求是，在主函数foo.bar.App#main() 中加载一个类，这个类有一个方法demo，我们需要调用这个demo方法。

1. 如果该类和主函数位于同一个项目，那么很简单做到这一点。
这几个反射相关的受查异常，具体发生在类加载的哪个阶段，需要了解，才能真正理解类加载的机制。
另外，反射类的Method对象，能不能代替c++里的std::function呢，二者是有区别的，c++的std::function是无类型的，换句话说，只要参数满足，就可以封装到这里，而java的Method是有类型的，二者虽然同为Callable的对象，但是理解"是否有类型"这个区别，是理解动态语言和静态语言的关键。
public class App 
{
    public static void main( String[] args ) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // App#main作为一个没有main方法的类的执行容器

        // 这个Class.forName 相当于

        // 1. 尝试在classpath中寻找全限定名为className的字节码
        // 如果存在，则加入。如果不存在，则在类的加载阶段会扔一个ClassNotFoundException
        Class<?> demoClass = Class.forName("top.wjb114514.demo.Demo");

        // 2. 尝试访问字节码里的一个方法，根据方法的名字(SimpleName)和参数信息(NameAndType)访问
        // 如果对应字节码不存在相关符号信息，在验证阶段会抛出NoSuchMethodException异常
        Method demo = demoClass.getMethod("demo");

        // 3. 尝试根据类元信息，创建一个实例对象
        // 如果不能实例化(如果是字节码的类型信息为基本类型，数组类型，或者抽象类或者接口)，则扔一个InstantiationException
        // 如果不能访问，比如构造器私有，则扔一个IllegalAccessException异常
        Object o = demoClass.newInstance();

        // 4. InvocationTargetException 方法对象本身的异常会被封装成这个类型然后扔出去。
        demo.invoke(o);
    }
}

// foo.bar.demo.Demo 简单的打印一下文件是在哪个classpath被加载的。
package top.wjb114514.demo;

/**
 * @author wjb
 * @date 2024/6/11 23:10 周二
 */
public class Demo {
    public void demo() {
        // 获取当前class文件所在的路径
        String path = this.getClass().getResource("/").getPath();
        System.out.println("当前加载的文件路径为:" + path);
    }
}
现在问题来了，现在{user.dir}/target/classes 被加入了classpath，如果我们想加载{user.dir}/shared目录下的字节码文件，尝试加载一下。
当然，我们后面会有类似需求，即"把某个文件路径作为class文件的仓库"，所以采取工厂模式，ClassLoaderFactory.createClassLoader(File f)，这个方法会为我们创建一个扫描指定路径下class文件的类加载器。
另外，如果想维护我们自己的类加载器体系，可以仿照java的classloader的亲属关系。
设置当前classloader的parent的方法位于ClassLoader#setParent
1. 自定义类加载器
1. 通过重写loadClass打破双亲委派
  根据类名分派类加载器，如果类的全限定名前缀是我们自己的类，直接调用findClass加载类。否则委派给父类加载。
2. 通过重写findClass 获取类的信息
  在此之前，我们已经构造了url对象，相当于指定了类文件都在哪些文件夹。
  另外，我们通过类的全限定名访问类，那么需要将其转换为操作系统的文件路径，然后加上后缀。
  加载的过程，就是读class文件到内存里的字节数组，然后调用defineClass将其加载即可。
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

    // https://www.cnblogs.com/haitaofeiyang/p/7737360.html
    // 这里资源文件要理解清楚
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new UnsupportedOperationException(
                "This ClassLoader Not Support find Resource from Anywhere" +
                " So it default delegate to its parent ClassLoader, aka AppClassLoader to getResource" +
                " In this implement, this method is forbidden"
        );
    }
}
2. 主程序
注意，我们为什么不能直接把Class.newInstance获取的对象，强转为具体的类型。
比如，既然我们知道加载的类是一个foo.bar.demo.Demo 的类型，为什么不直接强转呢？
注意，对于类路径下的Demo类型，应该由AppClassLoader加载，而我们自己的加载器MyFileClassLoader加载的Demo类型，由于 {Class, ClassLoader}确定唯一的类型，二者不被视为相同类型。
如果要强转，会直接抛出非法类型转换错误。
    // 1. 获取一个扫描项目路径/shared, aka {user.dir}/shared 路径下的classloader
    String contentRoot = System.getProperty("user.dir");
    File[] files = new File[1];
    files[0] = new File(contentRoot + "/shared");
    System.out.println(contentRoot);
    URLClassLoader cl = reflectUtil.of(files, null);
    System.out.println(cl);
    System.out.println("类加载器的路径:" + Arrays.toString(cl.getURLs()));
    System.out.println("资源的路径" + cl.getResource(""));
    // 2. 用上面获取到的类加载器进行加载
    /*
        线程上下文加载器的用法

        主类
        1.1 通过AppClassLoader(或者更高的类加载器) 加载一个类 A
        1.2 主类通过 setContextClassLoader 将类加载器替换为 AppClassLoader之下的一个类加载器cl
        1.3 类A 直接getContextLoader加载类B

        这样，类A原本是只能向 AppClassLoader及其父加载器请求加载B，而这些加载器不会把B所在的路径作为资源仓库。
        而现在类A把加载请求委托给子类型，加载B就可以了。
     */



    Class<?> aClass = cl.loadClass("top.wjb114514.demo.Demo");
    // 不建议这么做，建议反射出方法然后把实例对象传入invoke的第一个参数
    // Demo d = (Demo) aClass.newInstance();

    Object o = aClass.newInstance();
    Method demo = aClass.getMethod("demo");
    demo.invoke(o);


}


我们可能有个疑问，为什么不直接把这个路径加到classpath，然后让AppClassLoader帮我们来加载呢？而是另起炉灶，用一个新的加载器(我们姑且命名为SharedClassLoader)来加载呢？
实际上，tomcat就是这么做的，其好处在于。

---

Java的Resource体系
还有一个比较关键的点，
如果我们希望加载一个外部配置，比如xxx.xml, xxx.propertis, xxx.yml等。这时候谁来加载，在哪里寻找这些东西?

Java提供了两种方式

1. 通过class对象加载
调用方式
this.getClass().getResource[AsStream]("")
this.getClass().getResource[AsStream]("/")
this.getClass().getResource[AsStream]("mapper/UserMapper.xml")
2. 通过classLoader加载。
this.getClass().getClassLoader().getResource[AsStream]("")
this.getClass().getClassLoader().getResource[AsStream]("/")
this.getClass().getClassLoader().getResource[AsStream]("mapper/UserMapper.xml")
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
相对路径:/C:/Users/DELL/IdeaProjects/reflect-test/target/test-classes/top/wjb114514/
绝对路径:/C:/Users/DELL/IdeaProjects/reflect-test/target/test-classes/
相对路径下的某个文件:Null
绝对路径下的文件:Null
=============== 用 ClassLoader对象寻找资源 ================
相对路径:/C:/Users/DELL/IdeaProjects/reflect-test/target/test-classes/
绝对路径:Null
相对路径下的某个文件:Null
绝对路径下的文件:Null
分析
1. 所谓的Resource对象，可以理解为一个非字节码的内容，一般就是独立的配置
2. ResourceAsStream就是把文件读成字节流处理
3. ClassLoader的加载方法 不会解析绝对路径，因此只有相对路径才可以，而且相对路径为classpath
4. Class的加载方法会把 绝对路径解析为classpath，相对路径解析为当前class所在的路径。
所以，如果我们想实现一个XmlClasspathResourceLoader，其实借助Classpath对象的getResource就可以加载配置了。
从磁盘任意地方加载资源并进行解析
前面说了，想加载资源，要么从Classpath，要么在当前class的路径下， 我们如果想在Disk任意位置呢？
我们希望有一个DiskResourceLoaderFactory，可以接受任何URL并提供资源加载能力。
同时，我们会使用Digester库，对磁盘任意位置上的资源进行简单解析。
我们继续对上一步的MyFileClassLoader修改。
观察ClassLoader#getResource方法
public Enumeration<URL> getResources(String name) throws IOException {
    @SuppressWarnings("unchecked")
    Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[2];
    
    // 1. 委托给父加载器，查看(我们的父加载器为AppClassLoader)
    // 父加载器会从classpath里搜寻资源
    // 因此，如果我们传入 ""，父加载器刚好就有一个名为 ""的资源
    // 这个资源我们也知道，就是classpath
    if (parent != null) {
        tmp[0] = parent.getResources(name);
    } else {
        // 否则交给BootstrapClassLoader的getResource去找
        tmp[0] = getBootstrapResources(name);
    }
    
    // 只有都找不到，才自己找
    tmp[1] = findResources(name);

    return new CompoundEnumeration<>(tmp);
}

// 子类应该重写这个方法，定义自己的资源加载逻辑
protected URL findResource(String name) {
    return null;
}
最终完整版
1. 别看岔劈了，注意getResource 和 getResources 结尾差了一个s... 害我debug半天。
2. 在自己的findResource方法里，可以定义怎么找文件，我这里简单的看是不是/开头，如果是的话，直接把整个文件名作为一个全路径名，否则用注册的url和文件名拼一下。
public class MyFileClassLoader extends URLClassLoader {
    public MyFileClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new UnsupportedOperationException(
                "This ClassLoader Not Support find Resources from urls" +
                        " And it default delegate to its parent ClassLoader, aka AppClassLoader to getResource" +
                        " In this implement, this method is forbidden"
        );
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        throw new UnsupportedOperationException(
                "This ClassLoader Not Support find Resources from urls" +
                        " And it default delegate to its parent ClassLoader, aka AppClassLoader to getResource" +
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
        // 如果可以的话，这里也可以hack，比如不经过父亲查找了，直接自己查，可以稍快一些。
        return super.getResource(name);
    }
}
当然，上面可以对文件的格式进行校对，比如我们只加载.xml 资源。
现在我们在{user.dir}/shared下准备好一个config.xml，使用Apache Digester对其简单解析。

---
由于xml具备一定的嵌套结构，所以用栈来表示比较好，因此digester就是大概基于这个工作来做的。
我们简单解析一个三层嵌套的xml，将其转换为java对象，这些对象都由我们自己的类加载器来加载。

- Root: School
  - Layer1:
    ClassRoom
    - Layer2:
      - Student
      - Teacher
    Canteen:
    - Layer2:
      - Chef
      - Waitness

  
  
  
