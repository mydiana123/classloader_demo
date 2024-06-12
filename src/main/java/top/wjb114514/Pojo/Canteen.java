package top.wjb114514.Pojo;

import java.util.List;

/**
 * @author wjb
 * @date 2024/6/12 16:00 周三
 */
public class Canteen {
    List<Chef> chefs;
    List<Service> services;

    public List<Chef> getChefs() {
        return chefs;
    }

    public void setChefs(List<Chef> chefs) {
        this.chefs = chefs;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }
}
