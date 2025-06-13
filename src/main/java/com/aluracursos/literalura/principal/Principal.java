package com.aluracursos.literalura.principal;

import com.aluracursos.literalura.model.*;
import com.aluracursos.literalura.repository.AutorRepository;
import com.aluracursos.literalura.repository.LibroRepository;
import com.aluracursos.literalura.service.ConsumoAPI;
import com.aluracursos.literalura.service.ConvierteDatos;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Principal {
    private static final String URL_BASE = "https://gutendex.com/books/";
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private Scanner teclado = new Scanner(System.in);

    private LibroRepository libroRepositorio;
    private AutorRepository autorRepositorio;

    public Principal(LibroRepository libroRepository, AutorRepository autorRepository) {
        this.libroRepositorio = libroRepository;
        this.autorRepositorio = autorRepository;
    }


    public void muestraMenu() {
        var opcion = -1;

        while(opcion != 0) {
            var menu = """
                    
                    ---- Bienvenido a Literalura ----
                        
                        Ingrese una opción
                        
                    1 - Buscar libro por titulo
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4-  Listar autores vivos en un determinado año
                    5-  Listar libros por idioma
                    
                    0 - Salir
                    """;

            System.out.println(menu);
            try {// si se ingresa un valor no numérico
                opcion = teclado.nextInt();
                teclado.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Entrada inválida, por favor ingrese un número");
                teclado.nextLine();
                continue; // vuelve a mostrar el menu
            }

            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    listarLibrosRegistrados();
                    break;
                case 3:
                    listarAutoresRegistrados();
                    break;
                case 4:
                    listarAutoresVivosEnAnio();
                    break;
                case 5:
                    listarLibrosPorIdioma();
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
        System.out.println("Ingrese el nombre del libro a buscar");
        var tituloLibro = teclado.nextLine();
        var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" +tituloLibro.replace(" ", "+"));

        // Manejo de JSON vacío o nulo
        if (json == null || json.isEmpty() || json.equals("{}")) {
            System.out.println("No se obtuvieron datos de la API. Verifique la conexión o el título.");
            return;
        }

        var datos = conversor.obtenerDatos(json, Datos.class);

        // Verificar si la lista de resultados está vacía
        if (datos == null || datos.resultados().isEmpty()) {
            System.out.println("No se encontraron libros en la API de Gutendex con ese título.");
            return;
        }

        //Busca libro pero se queda sólo con el primer resultado que encuentra
        Optional<DatosLibro> libroBuscado = datos.resultados().stream()
                .filter(l -> l.titulo().toUpperCase().contains(tituloLibro.toUpperCase()))
                .findFirst();

        if(libroBuscado.isPresent()) {
            DatosLibro datosLibro = libroBuscado.get();
            System.out.println("Libro encontrado en la API:");
            System.out.println("Título: " + datosLibro.titulo());

            //Imprime el/los autor/es del libro encontrado
            datosLibro.autores().forEach(a -> System.out.println("Autor: " + a.autor()));

            // Asegurarse de que la lista de idiomas no esté vacía antes de acceder
            if (datosLibro.idiomas() != null && !datosLibro.idiomas().isEmpty()) {
                System.out.println("Idiomas: " + String.join(", ", datosLibro.idiomas()));
            } else {
                System.out.println("Idiomas: Desconocido");
            }
            System.out.println("Número de descargas: " + datosLibro.numeroDescargas());

            // --- Lógica de persistencia ---
            Optional<Libro> libroExistente = libroRepositorio.findByTituloIgnoreCase(datosLibro.titulo());

            if (libroExistente.isPresent()) {
                System.out.println("¡Este libro ya está registrado en la base de datos!\n");
            } else {
                // Crear y guardar el nuevo libro y sus autores
                Libro nuevoLibro = new Libro();
                nuevoLibro.setTitulo(datosLibro.titulo());
                nuevoLibro.setNumeroDescargas(datosLibro.numeroDescargas());
                if (datosLibro.idiomas() != null && !datosLibro.idiomas().isEmpty()) {
                    nuevoLibro.setIdioma(datosLibro.idiomas().get(0)); // Tomar el primer idioma disponible
                } else {
                    nuevoLibro.setIdioma("Desconocido"); // Asignar un valor por defecto si no hay idioma
                }

                List<Autor> autoresEntidad = datosLibro.autores().stream()
                        .map(datosAutor -> {
                            Optional<Autor> autorExistente = autorRepositorio.findByNombreAutor(datosAutor.autor());
                            if (autorExistente.isPresent()) {
                                return autorExistente.get();
                            } else {
                                Autor nuevoAutor = new Autor();
                                nuevoAutor.setNombreAutor(datosAutor.autor());
                                nuevoAutor.setFechaNacimiento(datosAutor.fechaDeNacimiento());
                                nuevoAutor.setFechaFallecimiento(datosAutor.fechaDeFallecimiento());
                                return autorRepositorio.save(nuevoAutor);
                            }
                        })
                        .collect(Collectors.toList());

                nuevoLibro.setAutores(autoresEntidad);
                libroRepositorio.save(nuevoLibro);
                System.out.println("Libro y autores guardados exitosamente en la base de datos.");
            }
        } else {
            System.out.println("No se encontró un libro que coincida exactamente con el título en los resultados de la API.");
            System.out.println("Posibles resultados en Gutendex:");
            datos.resultados().forEach(dl -> System.out.println("- " + dl.titulo() + " por " + dl.autores().stream().map(DatosAutor::autor).collect(Collectors.joining(", "))));
        }
    }

    private void listarLibrosRegistrados() {
        List<Libro> libros = libroRepositorio.findAllConAutores(); //obtiene todos los libros de la BD

        if (libros.isEmpty()) {
            System.out.println("No hay libros registrados en la base de datos.");
        } else {
            System.out.println("\n--- Libros Registrados ---");
            libros.forEach(libro -> {
                System.out.println("Título: " + libro.getTitulo());

                // obtener los autores de la BD y unirlos
                String nombresAutores = libro.getAutores().stream()
                        .map(Autor::getNombreAutor)
                        .collect(Collectors.joining(", "));
                System.out.println("Autores: " + (nombresAutores.isEmpty() ? "Desconocido" : nombresAutores));

                // mostrar idioma
                System.out.println("Idioma: " + (libro.getIdioma() == null || libro.getIdioma().isEmpty() ? "Desconocido" : libro.getIdioma()));

                // mostrar numero de descargas
                System.out.println("Número de descargas: " + (libro.getNumeroDescargas() != null ? libro.getNumeroDescargas() : "N/A"));
                System.out.println("-------------------------\n"); // Added a newline for better readability
            });
        }
    }
    
    private void listarAutoresRegistrados() {
        List<Autor> autores = autorRepositorio.findAllConLibros();

        if (autores.isEmpty()) {
            System.out.println("No hay autores registrados en la base de datos");
        } else {
            System.out.println("\n-- Autores Registrados --");
            autores.forEach(autor -> {
                System.out.println("Nombre: " + autor.getNombreAutor());

                //Mostrar fecha de nacimiento (si existe)
                if (autor.getFechaNacimiento() != null) {
                    System.out.println("Fecha de nacimiento: " + autor.getFechaNacimiento());
                }

                //Mostrar fecha de fallecimiento (si existe)
                if (autor.getFechaFallecimiento() != null) {
                    System.out.println("Fecha de fallecimiento: " + autor.getFechaFallecimiento());
                }

                //Mostrar libros
                List<Libro> libros = autor.getLibros();
                if (libros != null && !libros.isEmpty()) {
                    System.out.println("Libros");
                    libros.forEach(libro -> System.out.println(" - " + libro.getTitulo()));
                } else {
                    System.out.println("Libros: Ninguno registrado");
                }

                System.out.println("-------------------------\n");
            });
        }
    }

    private void listarAutoresVivosEnAnio() {
        System.out.println("Ingrese el año para listar autores vivos: ");
        int anio;

        try {
            anio = teclado.nextInt();
            teclado.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("Entrada inválida, por favos ingrese un número");
            teclado.nextLine();
            return;
        }

        //Busca todos los autores
        List<Autor> autores = autorRepositorio.findAllConLibros();

        //Filtra los autores vivos en el año ingresado
        List<Autor> autoresVivos = autores.stream()
                .filter(autor -> autor.getFechaNacimiento() != null &&
                        autor.getFechaNacimiento() <= anio &&
                        (autor.getFechaFallecimiento() == null || autor.getFechaFallecimiento() > anio))
                .toList();

        //Muestra los autores y los libros
        if (autoresVivos.isEmpty()) {
            System.out.println("No se encontraron autores vivos en el año " + anio);
        } else {
            System.out.println("\n---Autores vivos en el año " + anio + "---");
            autoresVivos.forEach(autor -> {
                System.out.println("Nombre: " + autor.getNombreAutor());
                System.out.println("Fecha de nacimiento: " + autor.getFechaNacimiento());
                System.out.println("Fecha de fallecimiento: " + autor.getFechaFallecimiento());

                List<Libro> libros = autor.getLibros();
                if (libros != null && !libros.isEmpty()) {
                    System.out.println("Libros:");
                    libros.forEach(libro -> System.out.println(" - " + libro.getTitulo()));
                } else {
                    System.out.println("Libros: Ninguno registrado");
                }
                System.out.println("-------------------------\n");
            });
        }
    }

    private void listarLibrosPorIdioma() {
        System.out.println(""" 
                Ingrese el idioma para buscar los libros,, las opciones son: 
                    - es (Español)
                    - en (Inglés)
                    - fr (Francés)
                    - pt (Portugués)
                """);

        String idioma = teclado.nextLine().trim().toLowerCase();

        // Validar idioma
        if (!idioma.equals("es") && !idioma.equals("en") &&
                !idioma.equals("fr") && !idioma.equals("pt")) {
            System.out.println("Idioma inválido. Debe ingresar uno de: es, en, fr, pt.");
            return;
        }

        //trae los libros con el idioma
        List<Libro> libros = libroRepositorio.findByIdiomaWithAutores(idioma);

        if (libros.isEmpty()) {
            System.out.println("No se encontraron libros en el idioma");
        } else {
            System.out.println("\n---Libros en idioma: " +idioma+ "---");
            libros.forEach(libro -> {
                System.out.println("Título: " + libro.getTitulo());

                //Autores
                String nombresAutores = libro.getAutores().stream()
                        .map(Autor::getNombreAutor)
                        .collect(Collectors.joining(", "));
                System.out.println("Autores: " + (nombresAutores.isEmpty() ? "Desconocido" : nombresAutores));

                //idioma
                System.out.println("Idioma: " + libro.getIdioma());

                //Número de descargas
                System.out.println("Numero de descargas: " +
                        (libro.getNumeroDescargas() != null ? libro.getNumeroDescargas() : "N/A"));
                System.out.println("--------------------");
            });


        }

    }

}
