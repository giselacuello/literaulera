package com.aluracursos.literalura.principal;

import com.aluracursos.literalura.repository.AutorRepository;
import com.aluracursos.literalura.repository.LibroRepository;
import com.aluracursos.literalura.service.ConsumoAPI;
import com.aluracursos.literalura.service.ConvierteDatos;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Principal {
    private static final String URL_BASE = "https://gutendex.com/books/";
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private Scanner teclado = new Scanner(System.in);

    private LibroRepository libroRepositorio;
    private AutorRepository autorRepositorio;


    public Principal(LibroRepository repository, AutorRepository autorRepository) {
        this.libroRepositorio = repository;
        this.autorRepositorio = autorRepository;
    }

    public void muestraMenu() {
        var opcion = -1;

        while(opcion != 0) {
            var menu = """
                    1 - Buscar libro por titulo
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4-  Lstar autores vivos en un determinado año
                    5-  Listar libros por idioma
                    
                    0 - Salir
                    """;

            System.out.println(menu);
            try {// si se ingresa un valor no numérico
                opcion = teclado.nextInt();
                teclado.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Entrada inválida, por faovr ingrese un número");
                teclado.nextLine();
                continue; // vuelve a mostrar el menu
            }

            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();

                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación");
                    break;
                default:
                    System.out.println("Opcion invalida, ingrese un valor valido");
            }
        }
    }

    private void buscarLibroPorTitulo() {
    }

}
