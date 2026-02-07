package com.brother.crystalviz.service;

import com.brother.crystalviz.model.Atom;
import com.brother.crystalviz.model.Bond;
import com.brother.crystalviz.model.CrystalRequest;
import com.brother.crystalviz.model.CrystalResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.regex.*;

@Service
public class CrystalService {

    // 元素颜色映射 [cite: 2]
    private static final Map<String, String> ELEMENT_COLORS = new HashMap<>();
    // 原子半径映射 [cite: 2]
    private static final Map<String, Double> ATOMIC_RADII = new HashMap<>();

    static {
        ELEMENT_COLORS.put("Fe", "#FF0000"); // Red
        ELEMENT_COLORS.put("C", "#888888");
        ELEMENT_COLORS.put("Cu", "#FFA500"); // Orange
        ELEMENT_COLORS.put("Al", "#0000FF"); // Blue
        ELEMENT_COLORS.put("Cl", "#00FF00"); // Green
        ELEMENT_COLORS.put("Na", "#800080"); // Purple
        ELEMENT_COLORS.put("O", "#FF4500");
        ELEMENT_COLORS.put("H", "#FFFFFF"); // 白色
        ELEMENT_COLORS.put("N", "#3050F8"); // 深蓝色

        ATOMIC_RADII.put("Fe", 1.26);
        ATOMIC_RADII.put("C", 0.77);
        ATOMIC_RADII.put("O", 0.6);
        ATOMIC_RADII.put("Cu", 1.28);
        ATOMIC_RADII.put("Al", 1.43);
        ATOMIC_RADII.put("Cl", 1.00);
        ATOMIC_RADII.put("Na", 1.50);
        ATOMIC_RADII.put("H", 0.37);
        ATOMIC_RADII.put("N", 0.75);
    }

    /**
     * 生成晶体结构数据
     * 对应 Python 源码中的 generate_lattice 方法
     */
    public List<Atom> generateStructure(CrystalRequest req) {
        if (req.getCustomAtoms() != null && !req.getCustomAtoms().isEmpty()) {
            List<Atom> updatedList = req.getCustomAtoms();
            // 给修改后的原子补上颜色和半径（因为前端传回来的可能只有坐标）
            for (Atom atom : updatedList) {
                atom.setColor(ELEMENT_COLORS.getOrDefault(atom.getElement(), "#808080"));
                atom.setRadius(ATOMIC_RADII.getOrDefault(atom.getElement(), 1.0));
            }
            return updatedList; // 直接返回修改后的，不走下面的生成逻辑
        }

        List<Atom> atoms = new ArrayList<>();
        String type = req.getLatticeType();
        double a = req.getA();
        double b = req.getB();
        double c = req.getC();

        // 基础分数坐标 (Fractional Coordinates)
        List<double[]> fracCoords = new ArrayList<>();
        List<String> elements = new ArrayList<>();

        // 根据晶系生成原胞 [cite: 6, 7, 8]
        switch (type) {
            case "SC": // 简单立方
                fracCoords.add(new double[]{0, 0, 0});
                elements.add("Polonium"); // 示例
                break;
            case "BCC": // 体心立方 [cite: 8]
                fracCoords.add(new double[]{0, 0, 0});
                fracCoords.add(new double[]{0.5, 0.5, 0.5});
                elements.add("Fe");
                elements.add("Fe");
                break;
            case "FCC": // 面心立方 [cite: 7]
                fracCoords.add(new double[]{0, 0, 0});
                fracCoords.add(new double[]{0.5, 0.5, 0});
                fracCoords.add(new double[]{0.5, 0, 0.5});
                fracCoords.add(new double[]{0, 0.5, 0.5});
                // 模拟 NaCl 结构或者纯金属
                elements.add("Cu"); elements.add("Cu"); elements.add("Cu"); elements.add("Cu");
                break;
            case "NaCl": // 岩盐结构示例
                // Na
                fracCoords.add(new double[]{0, 0, 0});
                fracCoords.add(new double[]{0.5, 0.5, 0});
                fracCoords.add(new double[]{0.5, 0, 0.5});
                fracCoords.add(new double[]{0, 0.5, 0.5});
                // Cl (位移 0.5, 0.5, 0.5)
                fracCoords.add(new double[]{0.5, 0.5, 0.5});
                fracCoords.add(new double[]{0.5, 0, 0}); // 简化版，实际会有周期性
                fracCoords.add(new double[]{0, 0.5, 0});
                fracCoords.add(new double[]{0, 0, 0.5});

                for(int i=0; i<4; i++) elements.add("Na");
                for(int i=0; i<4; i++) elements.add("Cl");
                break;
            case "HEX": // 六方晶系
                // 原子1 (0, 0, 0)
                fracCoords.add(new double[]{0, 0, 0});
                elements.add("Mg");
                // 原子2 (2/3, 1/3, 1/2)
                fracCoords.add(new double[]{2.0/3.0, 1.0/3.0, 0.5});
                elements.add("Mg");

                // 坐标转换时，gamma 强制为 120 度
                double gammaRad = Math.toRadians(120);
                // x = a*u + b*v*cos(120)
                // y = b*v*sin(120)
                // z = c*w
                break;
            default: // 默认简单立方
                fracCoords.add(new double[]{0, 0, 0});
                elements.add("Fe");
        }

        // 坐标转换：分数坐标 -> 笛卡尔坐标 [cite: 10]
        // Cartesian = Fractional * Lattice Matrix
        double alphaRad = Math.toRadians(req.getAlpha());
        double betaRad = Math.toRadians(req.getBeta());
        double gammaRad = Math.toRadians(req.getGamma());

// 计算晶格矢量矩阵 (用于处理非 90 度的晶系)
        double ax = req.getA();
        double ay = 0;
        double az = 0;

        double bx = req.getB() * Math.cos(gammaRad);
        double by = req.getB() * Math.sin(gammaRad);
        double bz = 0;

        double cx = req.getC() * Math.cos(betaRad);
        double cy = req.getC() * (Math.cos(alphaRad) - Math.cos(betaRad) * Math.cos(gammaRad)) / Math.sin(gammaRad);
        double cz = Math.sqrt(Math.pow(req.getC(), 2) - cx * cx - cy * cy);

        for (int i = 0; i < fracCoords.size(); i++) {
            double[] f = fracCoords.get(i);
            String elem = elements.get(i);

            // 将分数坐标转换为真实的笛卡尔坐标
            double x = f[0] * ax + f[1] * bx + f[2] * cx;
            double y = f[0] * ay + f[1] * by + f[2] * cy;
            double z = f[0] * az + f[1] * bz + f[2] * cz;

            atoms.add(new Atom(
                    elem,
                    x, y, z,
                    ELEMENT_COLORS.getOrDefault(elem, "#808080"),
                    ATOMIC_RADII.getOrDefault(elem, 1.0)
            ));
        }
        return atoms;
    }

    // 1. 自动计算化学键逻辑
    public List<Bond> calculateBonds(List<Atom> atoms) {
        List<Bond> bonds = new ArrayList<>();
        // 设置键长阈值（例如 1.0Å 到 5.0Å 之间判定为有键）
        double minLen = 1.0;
        double maxLen = 5.0;

        for (int i = 0; i < atoms.size(); i++) {
            for (int j = i + 1; j < atoms.size(); j++) {
                Atom a1 = atoms.get(i);
                Atom a2 = atoms.get(j);
                double dist = Math.sqrt(Math.pow(a1.getX()-a2.getX(), 2) +
                        Math.pow(a1.getY()-a2.getY(), 2) +
                        Math.pow(a1.getZ()-a2.getZ(), 2));
                if (dist >= minLen && dist <= maxLen) {
                    bonds.add(new Bond(a1, a2));
                }
            }
        }
        return bonds;
    }

    // 1. 导出 XYZ 字符串功能
    public String exportToXYZ(List<Atom> atoms) {
        StringBuilder sb = new StringBuilder();
        sb.append(atoms.size()).append("\n");
        sb.append("Generated by CrystalViz Platform\n");
        for (Atom atom : atoms) {
            sb.append(String.format("%s %.6f %.6f %.6f\n",
                    atom.getElement(), atom.getX(), atom.getY(), atom.getZ()));
        }
        return sb.toString();
    }

    // 2. 错误检测：重叠原子检查
    public List<String> checkErrors(List<Atom> atoms) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < atoms.size(); i++) {
            for (int j = i + 1; j < atoms.size(); j++) {
                double dist = calculateDist(atoms.get(i), atoms.get(j));
                if (dist < 0.5) { // 如果两个原子距离小于0.5埃，判定为异常重叠
                    errors.add("警告: 发现原子重叠于坐标 (" + atoms.get(i).getX() + ", " + atoms.get(i).getY() + ")");
                }
            }
        }
        return errors;
    }

    private double calculateDist(Atom a, Atom b) {
        return Math.sqrt(Math.pow(a.getX()-b.getX(),2) + Math.pow(a.getY()-b.getY(),2) + Math.pow(a.getZ()-b.getZ(),2));
    }

    // [新增] 解析 CIF 文件并返回原子列表
    public List<Atom> parseCIF(String content) {
        List<Atom> atoms = new ArrayList<>();
        // 简单的正则表达式或行扫描来获取晶格常数和原子坐标
        // 实际生产建议使用专门的解析库，这里展示核心逻辑
        double a = 1.0, b = 1.0, c = 1.0;

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            // 解析晶格参数
            if (line.startsWith("_cell_length_a")) a = Double.parseDouble(line.split("\\s+")[1].split("\\(")[0]);
            if (line.startsWith("_cell_length_b")) b = Double.parseDouble(line.split("\\s+")[1].split("\\(")[0]);
            if (line.startsWith("_cell_length_c")) c = Double.parseDouble(line.split("\\s+")[1].split("\\(")[0]);

            // 匹配原子坐标行 (例如: Fe1 Fe 0.0000 0.0000 0.0000)
            if (line.matches("^[a-zA-Z]+\\d*\\s+[a-zA-Z]+\\s+[\\d\\.-]+\\s+[\\d\\.-]+\\s+[\\d\\.-]+.*")) {
                String[] parts = line.split("\\s+");
                String symbol = parts[1];
                double fx = Double.parseDouble(parts[2].split("\\(")[0]);
                double fy = Double.parseDouble(parts[3].split("\\(")[0]);
                double fz = Double.parseDouble(parts[4].split("\\(")[0]);

                // 转换为笛卡尔坐标 (此处简化为正交，建议复用之前的变换矩阵逻辑)
                atoms.add(new Atom(symbol, fx * a, fy * b, fz * c,
                        ELEMENT_COLORS.getOrDefault(symbol, "#808080"),
                        ATOMIC_RADII.getOrDefault(symbol, 1.0)));
            }
        }
        return atoms;
    }

    // 修改返回类型，或者创建一个包装类。这里我们假设直接在 Service 里处理
    public CrystalResponse importCifLogic(String content) {
        CrystalResponse response = new CrystalResponse();
        List<Atom> atoms = new ArrayList<>();

        // 1. 预处理：去除注释，将所有空白字符（空格、换行、制表符）统一替换为空格
        // 这样 _cell_length_a 和 2.456 就变成了相邻的两个单词，不用管换行了
        String cleanContent = content.replaceAll("#.*", ""); // 去除 # 后的注释
        // 将内容按空白字符分割成 Token 流
        String[] tokens = cleanContent.split("\\s+");
        List<String> tokenList = new ArrayList<>();
        for (String t : tokens) {
            if (!t.trim().isEmpty()) {
                tokenList.add(t);
            }
        }

        // 默认值
        double a = 1.0, b = 1.0, c = 1.0;
        double alpha = 90.0, beta = 90.0, gamma = 90.0;

        // 2. 遍历 Token 解析参数
        // 我们需要记录 atom_site loop 的列定义顺序
        List<String> atomHeaders = new ArrayList<>();
        boolean inAtomLoop = false;

        for (int i = 0; i < tokenList.size(); i++) {
            String token = tokenList.get(i);

            try {
                switch (token) {
                    case "_cell_length_a":
                        a = Double.parseDouble(tokenList.get(++i));
                        break;
                    case "_cell_length_b":
                        b = Double.parseDouble(tokenList.get(++i));
                        break;
                    case "_cell_length_c":
                        c = Double.parseDouble(tokenList.get(++i));
                        break;
                    case "_cell_angle_alpha":
                        alpha = Double.parseDouble(tokenList.get(++i));
                        break;
                    case "_cell_angle_beta":
                        beta = Double.parseDouble(tokenList.get(++i));
                        break;
                    case "_cell_angle_gamma":
                        gamma = Double.parseDouble(tokenList.get(++i));
                        break;
                    case "loop_":
                        // 检查下一个 token 是否是原子位点定义
                        if (i + 1 < tokenList.size() && tokenList.get(i+1).startsWith("_atom_site_")) {
                            inAtomLoop = true;
                            atomHeaders.clear();
                        } else {
                            inAtomLoop = false;
                        }
                        break;
                    default:
                        if (inAtomLoop) {
                            if (token.startsWith("_atom_site_")) {
                                atomHeaders.add(token);
                            } else {
                                // 遇到了非 header 的内容，说明开始是数据了
                                // 必须确保 atomHeaders 不为空
                                if (!atomHeaders.isEmpty()) {
                                    // 这是一个数据行的开始，我们需要根据 headers 的长度读取一组数据
                                    int stride = atomHeaders.size();
                                    // 回退当前 token (因为它是数据的一部分)
                                    i--;

                                    // 循环读取原子直到遇到非数据或结束
                                    while (i + stride < tokenList.size()) {
                                        // 预判下一个 token，如果是新的 key (以 _ 开头) 或 loop_ 则跳出
                                        String nextCheck = tokenList.get(i + 1);
                                        if (nextCheck.startsWith("_") || nextCheck.equals("loop_")) {
                                            inAtomLoop = false;
                                            break;
                                        }

                                        // 读取一行原子数据
                                        Atom atom = new Atom();
                                        // 给个默认半径和颜色
                                        atom.setRadius(0.5);
                                        atom.setColor("#808080");

                                        for (int k = 0; k < stride; k++) {
                                            String val = tokenList.get(++i); // 移动主指针
                                            String header = atomHeaders.get(k);

                                            // 简单去除括号 (有些CIF会有 0.123(5))
                                            val = val.replaceAll("\\(.*\\)", "");

                                            if (header.contains("_label") || header.contains("_symbol")) {
                                                // 提取元素符号 (比如 C1 -> C)
                                                String el = val.replaceAll("[0-9+-]", "");
                                                atom.setElement(el);
                                                assignAtomProperties(atom); // 复用你原有的设置颜色方法
                                            } else if (header.contains("_fract_x")) {
                                                atom.setX(Double.parseDouble(val));
                                            } else if (header.contains("_fract_y")) {
                                                atom.setY(Double.parseDouble(val));
                                            } else if (header.contains("_fract_z")) {
                                                atom.setZ(Double.parseDouble(val));
                                            }
                                        }

                                        // 坐标转换：CIF 是分数坐标，需要转为笛卡尔坐标
                                        // 简化的正交转换 (对于 Alpha=Beta=Gamma=90 适用)
                                        // 如果要严谨，需要根据 alpha/beta/gamma 做矩阵变换，这里先做简单版
                                        atom.setX(atom.getX() * a);
                                        atom.setY(atom.getY() * b);
                                        atom.setZ(atom.getZ() * c);

                                        atoms.add(atom);
                                    }
                                    inAtomLoop = false; // 读完这个 loop
                                }
                            }
                        }
                        break;
                }
            } catch (Exception e) {
                // 忽略解析错误的单个 token，继续尝试
                System.err.println("Token parsing error: " + token + " -> " + e.getMessage());
            }
        }

        response.setA(a);
        response.setB(b);
        response.setC(c);
        response.setAlpha(alpha);
        response.setBeta(beta);
        response.setGamma(gamma);
        response.setAtoms(atoms);

        response.setBonds(calculateBonds(atoms));

        return response;
    }

    // 辅助方法（确保在类中定义）
    private double extractCifNum(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) return 1.0;
        return Double.parseDouble(parts[1].split("\\(")[0]);
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d*\\.?\\d+");
    }


    private double parseCifValue(String line) {
        return Double.parseDouble(line.split("\\s+")[1].split("\\(")[0]);
    }

    // 这个方法负责给解析出来的原子“穿衣服”（上色、定半径）
    private void assignAtomProperties(Atom atom) {
        String element = atom.getElement();

        // 从你类里定义的 ELEMENT_COLORS 映射中取颜色，没取到默认白色
        atom.setColor(ELEMENT_COLORS.getOrDefault(element, "#FFFFFF"));

        // 从 ATOMIC_RADII 映射中取半径，没取到默认 1.0
        atom.setRadius(ATOMIC_RADII.getOrDefault(element, 1.0));
    }
}