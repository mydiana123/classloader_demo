package top.wjb114514.Pojo;

/**
 * @author wjb
 * @date 2024/6/12 16:00 周三
 * 需要get/set方法
 */
public class Teacher {
    private String name;
    private String age;
    private String subject;

    @Override
    public String toString() {
        return "Teacher{" +
                "name='" + name + '\'' +
                ", age='" + age + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
