package com.brother.crystalviz.model;
import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrystalResponse {
    private List<Atom> atoms;
    private List<Bond> bonds;
    private double a;
    private double b;
    private double c;
    private double alpha;
    private double beta;
    private double gamma;
}