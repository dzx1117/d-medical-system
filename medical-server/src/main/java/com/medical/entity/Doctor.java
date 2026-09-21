package com.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("doctor")
public class Doctor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long departmentId;

    private String title;

    private String specialty;

    private String introduction;

    private BigDecimal fee;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
