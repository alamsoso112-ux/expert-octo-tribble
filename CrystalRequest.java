package com.brother.crystalviz.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor  // <--- 必须添加
@AllArgsConstructor // 建议配套添加
public class CrystalRequest {
    private String latticeType;
    private double a, b, c;
    private double alpha =90.0, beta =90.0, gamma =90.0;
    private List<Atom> customAtoms;
}