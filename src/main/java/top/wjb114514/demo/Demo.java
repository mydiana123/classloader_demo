package top.wjb114514.demo;

/**
 * @author wjb
 * @date 2024/6/11 23:10 周二
 */
public class Demo {
    public void demo() {
        System.out.println("谁加载了我?" + this.getClass().getClassLoader());

        System.out.println(this.getClass().getClassLoader().getResource("/config.xml"));
    }
}
