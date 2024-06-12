package top.wjb114514;

import com.sun.org.apache.xml.internal.resolver.tools.ResolvingXMLReader;
import org.apache.commons.digester3.Digester;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import top.wjb114514.Pojo.ClassRoom;
import top.wjb114514.Pojo.School;
import top.wjb114514.Pojo.Teacher;
import top.wjb114514.utils.MyFileClassLoader;
import top.wjb114514.utils.reflectUtil;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.concurrent.Future;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception {
//        // App#main作为一个没有main方法的类的执行容器
//
//        // 这个Class.forName 相当于
//
//        // 1. 尝试在classpath中寻找全限定名为className的字节码
//        // 如果存在，则加入。如果不存在，则在类的加载阶段会扔一个ClassNotFoundException
//        Class<?> demoClass = Class.forName("top.wjb114514.demo.Demo");
//
//        // 2. 尝试访问字节码里的一个方法，根据方法的名字(SimpleName)和参数信息(NameAndType)访问
//        // 如果对应字节码不存在相关符号信息，在验证阶段会抛出NoSuchMethodException异常
//        Method demo = demoClass.getMethod("demo");
//
//        // 3. 尝试根据类元信息，创建一个实例对象
//        // 如果不能实例化(如果是字节码的类型信息为基本类型，数组类型，或者抽象类或者接口)，则扔一个InstantiationException
//        // 如果不能访问，比如构造器私有，则扔一个IllegalAccessException异常
//        Object o = demoClass.newInstance();
//
//        // 4. InvocationTargetException 方法对象本身的异常会被封装成这个类型然后扔出去。
//        demo.invoke(o);



        // 1. 获取一个扫描项目路径/shared, aka {user.dir}/shared 路径下的classloader
        String contentRoot = System.getProperty("user.dir");
        File[] files = new File[1];
        files[0] = new File(contentRoot + "/shared");
        System.out.println(contentRoot);
        URLClassLoader cl = reflectUtil.of(files, null);
//        URL resource = cl.getResource("config.xml");
        InputStream ras = cl.getResourceAsStream("config.xml");
        // 使用Apache-Digester消费xml文件

        // 1. 创建一个xml文件的读取器
        XMLReader xmlReader = new ResolvingXMLReader();
//        xmlReader.parse(new InputSource(ras));
        Digester digester = new Digester(xmlReader);
        digester.setClassLoader(cl);

        // 当遇到<school>标签时，就创建一个对象并压栈
        digester.addObjectCreate("school", School.class);  // top of stack: School

        // 怎么把classRoom类设置为school的成员? 注意，这里使用了大量约定俗成的规定和反射

        // 1. 根据标签创建对象
        digester.addObjectCreate("school/classRoom", ClassRoom.class); // stack: school, classRoom <= top
        // 2. 解析标签属性，反射获取set方法，然后对栈顶的实例进行收集并填充相关属性
        digester.addSetProperties("school/classRoom");
        // 3. 调用栈顶下一个元素的set方法，把栈顶元素设置为该元素的成员
        // 这里注意下，我们可以提供add/remove方法，就可以操作集合元素了
        digester.addSetNext("school/classRoom", "setClassRoom");

        // 对于classRoom同理


        digester.addObjectCreate("school/classRoom/teacher", Teacher.class);
        digester.addSetProperties("school/classRoom/teacher");
        // 此时栈为
        // teacher <= top
        // classRoom <= addSetNext操作的实体
        // teacher
        digester.addSetNext("school/classRoom/teacher", "addTeacher");

        // TODO: school/classRoom/student, school/canteen, ...

        Object parse = digester.parse(ras);

        // 验证结果对不对，其他的属性仿照上面三步，建对象(实例化)，初始化(反射set)，填充进行即可
        School school = (School) parse;
        System.out.println(school);
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
}
