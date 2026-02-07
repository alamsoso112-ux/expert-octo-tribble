package com.brother.crystalviz.model;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Bond {
    private Atom start;
    private Atom end;
}