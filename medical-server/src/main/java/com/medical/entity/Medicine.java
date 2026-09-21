package com.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medicine")
public class Medicine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String category;

    private String spec;

    private String unit;

    private BigDecimal price;

    private Integer stock;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
