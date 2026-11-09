package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *
 *     posicion;nombre;nacimiento;fallecimiento
 *
 * Ejemplo (el fallecimiento puede quedar vacío):
 *
 *     1;John Doe;2002;
 *     2;Henry Doe;1920;1990
 *     3;Maria Doe;1921;
 *     1.1;Timmy Doe;2021;
 *     1.2;Amy Doe;2026;
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 3)
 * ------------------------------------------
 * Reemplace el "throw new UnsupportedOperationException" por su implementación. Esta clase usa lo que ya
 * hizo en las etapas anteriores (Posicion, Persona y ArbolGenealogico). NO modifique las pruebas (src/test).
 *
 * REGLAS
 *   1. Si el texto es null o no tiene ninguna línea con contenido: ArbolInvalidoException (mensaje: "El texto está vacío...").
 *   2. Las líneas pueden terminar en \n o en \r\n. Las líneas en blanco se ignoran, pero los números de línea
 *      que se informan cuentan TODAS las líneas del texto (la primera es la línea 1).
 *   3. Cada línea tiene 4 campos separados por ';' (el cuarto, el fallecimiento, puede estar vacío). Se acepta
 *      también la línea con solo 3 campos y sin el último ';'. Los campos se recortan (trim).
 *      Si hay menos de 3 o más de 4 campos: error.
 *   4. El nacimiento y el fallecimiento deben ser números enteros (el fallecimiento vacío es "no tiene").
 *   5. Cada persona debe ser válida (Persona.validar) y no puede haber dos líneas con la misma posición.
 *   6. Debe existir la persona "1" (el sujeto), pero puede estar en cualquier línea.
 *   7. Las líneas pueden venir en CUALQUIER orden. El constructor debe agregarlas al árbol en un orden en el
 *      que cada persona ya tenga a la que necesita (por ejemplo, primero los ascendientes por número Ahnentafel
 *      ascendente y los descendientes por generación). Las reglas del árbol (ver ArbolGenealogico) las valida el árbol.
 *   8. Todos los errores asociados a una línea se informan con ArbolInvalidoException y un mensaje que EMPIEZA con
 *      "Línea N: " (N es el número de línea de la persona con el problema), por ejemplo:
 *          "Línea 3: la posición 2 está repetida."
 *      Los errores que no son de una línea (texto vacío, falta el sujeto) no llevan ese prefijo.
 *   9. Solo se muestran los miembros que están en el texto: no se crean personas vacías para los que faltan.
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    /** Una persona leída del texto junto con el número de la línea en la que estaba. */
    private record Linea(int numero, Persona persona) {
    }

    /**
     * Convierte el texto en un árbol (ver las reglas arriba).
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        // 1. Leer y validar cada línea (en el orden del texto).
        List<Linea> lineas = leerLineas(texto);
        if (lineas.isEmpty()) {
            throw new ArbolInvalidoException("El texto está vacío: escriba al menos al sujeto (por ejemplo: 1;John Doe;2002;).");
        }

        // 2. Buscar al sujeto (puede estar en cualquier línea).
        Linea sujeto = buscarSujeto(lineas);
        if (sujeto == null) {
            throw new ArbolInvalidoException("Falta el sujeto: debe haber una línea con la posición 1.");
        }

        // 3. Ordenar para que cada persona se agregue después de la que necesita:
        //    primero los ascendientes por número Ahnentafel y luego los descendientes por generación.
        List<Linea> resto = new ArrayList<>(lineas);
        resto.remove(sujeto);
        resto.sort(Comparator.comparingInt(ConstructorArbol::ordenDeInsercion));

        // 4. Construir el árbol. Las reglas del árbol las valida ArbolGenealogico; aquí solo se agrega la línea.
        ArbolGenealogico arbol = conLinea(sujeto.numero(), () -> new ArbolGenealogico(sujeto.persona()));
        for (Linea l : resto) {
            conLinea(l.numero(), () -> arbol.agregar(l.persona()));
        }
        return arbol;
    }

    /** @return la línea del sujeto (posición "1"), o null si no está */
    private static Linea buscarSujeto(List<Linea> lineas) {
        for (Linea l : lineas) {
            if (Posicion.esSujeto(l.persona().getPosicion())) {
                return l;
            }
        }
        return null;
    }

    /**
     * Ascendientes: su número Ahnentafel (2, 3, 4...). Descendientes: después de todos los ascendientes, por
     * generación (1.1 y 1.2 antes que 1.1.1). Como el sort es estable, los empates conservan el orden del texto.
     */
    private static int ordenDeInsercion(Linea l) {
        String posicion = l.persona().getPosicion();
        if (Posicion.esAscendiente(posicion)) {
            return Integer.parseInt(posicion);
        }
        return Posicion.MAX_AHNENTAFEL + Posicion.generacion(posicion);
    }

    /** Lee todas las líneas con contenido. Cualquier error se informa con el número de línea. */
    private static List<Linea> leerLineas(String texto) {
        List<Linea> lineas = new ArrayList<>();
        if (texto == null) {
            return lineas;
        }
        String[] partes = texto.split("\n", -1);   // el \r de Windows se quita con el strip/trim
        for (int i = 0; i < partes.length; i++) {
            if (partes[i].isBlank()) {
                continue;   // las líneas en blanco se ignoran, pero sí cuentan para el número de línea
            }
            int numero = i + 1;
            Persona persona = leerPersona(numero, partes[i]);
            for (Linea anterior : lineas) {
                if (anterior.persona().getPosicion().equals(persona.getPosicion())) {
                    throw errorEnLinea(numero, "la posición " + persona.getPosicion()
                            + " está repetida (ya aparece en la línea " + anterior.numero() + ").");
                }
            }
            lineas.add(new Linea(numero, persona));
        }
        return lineas;
    }

    /** Convierte una línea "posicion;nombre;nacimiento;fallecimiento" en una Persona válida. */
    private static Persona leerPersona(int numero, String linea) {
        String[] campos = linea.split(";", -1);
        if (campos.length < 3 || campos.length > 4) {
            throw errorEnLinea(numero, "debe tener el formato posicion;nombre;nacimiento;fallecimiento (tiene "
                    + campos.length + " campo" + (campos.length == 1 ? "" : "s") + ").");
        }
        String posicion = campos[0].trim();
        String nombre = campos[1].trim();
        int nacimiento = leerAnio(numero, campos[2].trim(), "nacimiento");
        String textoFallecimiento = campos.length == 4 ? campos[3].trim() : "";
        Integer fallecimiento = textoFallecimiento.isEmpty() ? null : leerAnio(numero, textoFallecimiento, "fallecimiento");

        Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
        try {
            persona.validar();
        } catch (IllegalArgumentException e) {
            throw errorEnLinea(numero, e.getMessage());
        }
        return persona;
    }

    private static int leerAnio(int numero, String valor, String campo) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw errorEnLinea(numero, "el año de " + campo + " debe ser un número entero (se encontró \"" + valor + "\").");
        }
    }

    /** Ejecuta la acción y, si el árbol la rechaza, agrega el prefijo "Línea N: " al mensaje. */
    private static <T> T conLinea(int numero, java.util.function.Supplier<T> accion) {
        try {
            return accion.get();
        } catch (IllegalArgumentException e) {   // incluye ArbolInvalidoException
            throw errorEnLinea(numero, e.getMessage());
        }
    }

    private static ArbolInvalidoException errorEnLinea(int numero, String mensaje) {
        String texto = mensaje == null || mensaje.isEmpty() ? "dato inválido."
                : Character.toLowerCase(mensaje.charAt(0)) + mensaje.substring(1);
        return new ArbolInvalidoException("Línea " + numero + ": " + texto);
    }
}
