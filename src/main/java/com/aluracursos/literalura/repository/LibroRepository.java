package com.aluracursos.literalura.repository;

import com.aluracursos.literalura.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends JpaRepository <Libro, Long>{
    Optional<Libro> findByTituloIgnoreCase(String titulo);
    List<Libro> findByIdiomaIgnoreCase(String idioma);

    @Query("SELECT l FROM Libro l JOIN FETCH l.autores") // // Necesario para cargar autores por ser una relación LAZY (ManyToMany)
    List<Libro> findAllConAutores();

    @Query("SELECT l FROM Libro l JOIN FETCH l.autores WHERE LOWER(l.idioma) = LOWER(:idioma)")
    List<Libro> findByIdiomaWithAutores(@Param("idioma") String idioma);

}
