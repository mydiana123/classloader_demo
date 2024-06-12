package top.wjb114514.Pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wjb
 * @date 2024/6/12 16:00 周三
 */
public class ClassRoom {
    List<Teacher> teachers;
    List<Student> students;

    public ClassRoom() {
        teachers = new ArrayList<>();
        students = new ArrayList<>();
    }

    public List<Teacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<Teacher> teachers) {
        this.teachers = teachers;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    // 支持集合类型
    public void addTeacher(Teacher t) {
        teachers.add(t);
    }

    public void addStudent(Student s) {
        students.add(s);
    }

    @Override
    public String toString() {
        return "ClassRoom{" +
                "teachers=" + teachers +
                ", students=" + students +
                '}';
    }
}
