package top.wjb114514.Pojo;

/**
 * @author wjb
 * @date 2024/6/12 16:00 周三
 */
public class School {
    // TODO: List<ClassRoom>
    ClassRoom classRoom;
    Canteen canteen;

    public ClassRoom getClassRoom() {
        return classRoom;
    }

    public void setClassRoom(ClassRoom classRoom) {
        this.classRoom = classRoom;
    }

    public Canteen getCanteen() {
        return canteen;
    }

    public void setCanteen(Canteen canteen) {
        this.canteen = canteen;
    }

    @Override
    public String toString() {
        return "School{" +
                "classRoom=" + classRoom +
                ", canteen=" + canteen +
                '}';
    }
}
