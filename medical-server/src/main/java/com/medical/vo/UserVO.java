package com.medical.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String realName;

    private String phone;

    private String email;

    private String idCard;

    private Integer userType;

    private Integer status;

    private LocalDateTime createTime;
}
