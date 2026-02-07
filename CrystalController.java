package com.brother.crystalviz.controller;

import com.brother.crystalviz.model.Atom;
import com.brother.crystalviz.model.Bond;
import com.brother.crystalviz.model.CrystalRequest;
import com.brother.crystalviz.model.CrystalResponse;
import com.brother.crystalviz.service.CrystalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/crystal")
public class CrystalController {

    @Autowired
    private CrystalService crystalService;

    // 生成晶体结构的接口
    @PostMapping("/generate")
    public CrystalResponse generate(@RequestBody CrystalRequest request) {
        List<Atom> atoms = crystalService.generateStructure(request);
        List<Bond> bonds = crystalService.calculateBonds(atoms);

        // 修复：传入所有 8 个参数
        return new CrystalResponse(
                atoms,
                bonds,
                request.getA(),
                request.getB(),
                request.getC(),
                request.getAlpha(),
                request.getBeta(),
                request.getGamma()
        );
    }

    // 导出 XYZ 文件的接口
    @PostMapping("/export")
    public ResponseEntity<String> export(@RequestBody CrystalRequest request) {
        // 1. 生成原子数据
        List<Atom> atoms = crystalService.generateStructure(request);

        // 2. 转换为 XYZ 格式字符串
        String xyzContent = crystalService.exportToXYZ(atoms);

        // 3. 返回文件流
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"structure.xyz\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(xyzContent);
    }

    @PostMapping("/import")
    public CrystalResponse importFile(@RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes());
        return crystalService.importCifLogic(content);
    }

    // 用于只更新坐标不改变晶胞的操作
    @PostMapping("/update-properties")
    public CrystalResponse updateProperties(@RequestBody CrystalRequest request) {
        // 1. 获取前端传来的原子列表
        List<Atom> atoms = request.getCustomAtoms();

        // 2. 重新根据修改后的坐标计算化学键
        List<Bond> bonds = crystalService.calculateBonds(atoms);

        // 3. 【修复报错】返回完整的 8 个参数，确保前端输入框不会被重置
        return new CrystalResponse(
                atoms,
                bonds,
                request.getA(),
                request.getB(),
                request.getC(),
                request.getAlpha(),
                request.getBeta(),
                request.getGamma()
        );
    }
}