package com.yzx.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class User{
    String name;
    int age;
}
