package com.medical.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 通用的「名称 - 数值」结构，用于排行类统计 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameValueVO {

    private String name;

    private Long value;
}
