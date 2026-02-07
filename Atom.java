package com.brother.crystalviz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // 必须导入这个

@Data
@AllArgsConstructor // 生成全参构造函数 (Service里用到了)
@NoArgsConstructor  // <--- 【关键修复】必须加这个，否则前端传JSON后端会报400
public class Atom {
    private String element;
    private double x;
    private double y;
    private double z;
    private String color;
    private double radius;
}