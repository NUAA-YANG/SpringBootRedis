package com.yzx.service;

import com.yzx.pojo.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    public User getUserByName(String name){
        ArrayList<User> list = new ArrayList<>();
        list.add(new User("yzx",24));
        list.add(new User("yzx1",21));
        list.add(new User("yzx2",22));
        list.add(new User("yzx3",23));
        list.add(new User("nuaa",25));
        list.add(new User("yzx4",26));
        list.add(new User("yzx5",27));
        for (User user:list){
            if (user.getName().equals(name)){
                return user;
            }
        }
        return null;
    }

}
