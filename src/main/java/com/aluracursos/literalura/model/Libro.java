package com.aluracursos.literalura.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity // Marca esta clase como una entidad JPA
@Table(name = "libros") // Nombre de la tabla en la base de datos
public class Libro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;

    private List<Autor> autores = new ArrayList<>();

    private List<Idioma> idiomas = new ArrayList<>();
    private Long numeroDescargas;

}