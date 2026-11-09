package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (padres, abuelos... en Ahnentafel)
 * y sus DESCENDIENTES (hijos, nietos... en d'Aboville). Ver la clase Posicion.
 *
 * Son en realidad dos árboles unidos en la raíz:
 *
 *     ascendientes:  árbol BINARIO. Cada persona tiene como mucho un padre (2n) y una madre (2n + 1).
 *     descendientes: árbol GENERAL. Cada persona tiene una lista de hijos (1.1, 1.2, ...) ordenada por número.
 *
 * El árbol solo guarda a los miembros que EXISTEN: no hay "huecos" ni personas vacías. Puede faltar el padre
 * de alguien, o el hijo 1.1 aunque exista el 1.2.
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 2)
 * ------------------------------------------
 * 1. Implemente el árbol con sus PROPIOS NODOS: una clase interna "Nodo" con la Persona, el nodo del
 *    padre, el nodo de la madre y la lista de hijos. NO se permite usar Map ni Set (las pruebas lo
 *    revisan): para buscar una posición recorra el árbol siguiendo su camino
 *      - Ahnentafel: los bits de n después del primer 1 dicen el camino (0 = padre, 1 = madre).
 *        Ej.: 5 = 101 -> desde el sujeto: padre (0), luego madre (1).
 *      - D'Aboville: cada número de la posición dice a qué hijo bajar. Ej.: 1.2.3 -> hijo 2, luego su hijo 3.
 *    Se permite usar List/ArrayList para la lista de hijos.
 * 2. Todos los errores de las reglas se informan con {@link ArbolInvalidoException} (un mensaje claro).
 *    Los datos inválidos de una Persona los detecta Persona.validar() (IllegalArgumentException).
 * 3. NO modifique las pruebas (src/test).
 *
 * REGLAS DEL ÁRBOL
 *   - La posición "1" (el sujeto) existe siempre y no se puede eliminar.
 *   - Una posición no puede repetirse.
 *   - Un ascendiente n solo se puede agregar si ya existe su hijo n / 2, y debe haber NACIDO ANTES
 *     (año de nacimiento menor) que ese hijo.
 *   - Un descendiente solo se puede agregar si ya existe su progenitor, y debe haber NACIDO DESPUÉS
 *     (año de nacimiento mayor) que su progenitor.
 */
public class ArbolGenealogico {

    /**
     * Un miembro del árbol. Los ascendientes usan "padre" y "madre" (árbol binario) y los descendientes
     * usan "hijos" (árbol general, ordenado por número de orden). El sujeto usa las tres cosas.
     */
    private static class Nodo {
        final Persona persona;
        Nodo padre;
        Nodo madre;
        final List<Nodo> hijos = new ArrayList<>();

        Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    /** El nodo del sujeto (posición "1"). */
    private final Nodo raiz;
    /** Cantidad de miembros (el sujeto incluido). */
    private int tamano;

    /**
     * Crea un árbol que solo tiene al sujeto.
     *
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si su posición no es "1"
     */
    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new ArbolInvalidoException("El sujeto es obligatorio.");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1, no en la " + sujeto.getPosicion() + ".");
        }
        this.raiz = new Nodo(sujeto);
        this.tamano = 1;
    }

    /** @return el sujeto (la persona de la posición "1") */
    public Persona sujeto() {
        return raiz.persona;
    }

    /** @return cuántos miembros tiene el árbol (el sujeto cuenta; un árbol nuevo tiene 1) */
    public int tamano() {
        return tamano;
    }

    // =====================================================================
    // Búsqueda: se recorre el camino que indica la posición (sin Map ni Set)
    // =====================================================================

    /** @return el nodo de esa posición, o null si no existe o si la posición no es válida */
    private Nodo buscarNodo(String posicion) {
        if (Posicion.esAhnentafel(posicion)) {
            // Los bits de n después del primer 1 indican el camino: 0 = padre, 1 = madre.
            int n = Integer.parseInt(posicion);
            Nodo actual = raiz;
            for (int bit = Posicion.generacion(posicion) - 1; bit >= 0 && actual != null; bit--) {
                actual = ((n >> bit) & 1) == 0 ? actual.padre : actual.madre;
            }
            return actual;
        }
        if (Posicion.esDAboville(posicion)) {
            // Cada número después del "1" dice a qué hijo bajar.
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length && actual != null; i++) {
                actual = buscarHijo(actual, Integer.parseInt(partes[i]));
            }
            return actual;
        }
        return null;
    }

    /** @return el hijo de ese nodo con ese número de orden, o null si no lo tiene */
    private static Nodo buscarHijo(Nodo progenitor, int orden) {
        for (Nodo hijo : progenitor.hijos) {
            int ordenHijo = Posicion.orden(hijo.persona.getPosicion());
            if (ordenHijo == orden) {
                return hijo;
            }
            if (ordenHijo > orden) {
                return null;   // la lista está ordenada: ya no puede aparecer
            }
        }
        return null;
    }

    /**
     * @return la persona de esa posición, o null si no existe en el árbol o si la posición no es válida
     */
    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo == null ? null : nodo.persona;
    }

    /** @return true si hay alguien en esa posición (false también si la posición no es válida) */
    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    // =====================================================================
    // Agregar
    // =====================================================================

    /**
     * Agrega una persona en la posición que ella misma trae (la usa el constructor del árbol a partir del texto).
     * El orden de llamada importa: primero debe existir la persona de la que depende (ver REGLAS DEL ÁRBOL).
     *
     * @return la misma persona
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si la posición ya existe (o es "1"), si falta la persona de la que depende,
     *         o si los años de nacimiento no cumplen las reglas
     */
    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new ArbolInvalidoException("La persona es obligatoria.");
        }
        persona.validar();
        String posicion = persona.getPosicion();
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("La posición 1 (el sujeto) ya existe.");
        }
        if (buscarNodo(posicion) != null) {
            throw new ArbolInvalidoException("La posición " + posicion + " está repetida.");
        }

        if (Posicion.esAscendiente(posicion)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(posicion);
            Nodo hijo = buscarNodo(posHijo);
            if (hijo == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + " porque no existe su hijo/a (posición " + posHijo + ").");
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La persona de la posición " + posicion + " (nacida en "
                        + persona.getNacimiento() + ") debe haber nacido antes que su hijo/a de la posición "
                        + posHijo + " (nacido/a en " + hijo.persona.getNacimiento() + ").");
            }
            Nodo nuevo = new Nodo(persona);
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
        } else {
            // Persona.validar ya garantizó que la posición es válida: si no es ascendiente, es descendiente.
            String posProgenitor = Posicion.posicionDelProgenitor(posicion);
            Nodo progenitor = buscarNodo(posProgenitor);
            if (progenitor == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + " porque no existe su progenitor (posición " + posProgenitor + ").");
            }
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La persona de la posición " + posicion + " (nacida en "
                        + persona.getNacimiento() + ") debe haber nacido después que su progenitor de la posición "
                        + posProgenitor + " (nacido/a en " + progenitor.persona.getNacimiento() + ").");
            }
            insertarHijoOrdenado(progenitor, new Nodo(persona));
        }
        tamano++;
        return persona;
    }

    /** Inserta el hijo en la lista del progenitor manteniéndola ordenada por número de orden. */
    private static void insertarHijoOrdenado(Nodo progenitor, Nodo hijo) {
        int orden = Posicion.orden(hijo.persona.getPosicion());
        int i = 0;
        while (i < progenitor.hijos.size() && Posicion.orden(progenitor.hijos.get(i).persona.getPosicion()) < orden) {
            i++;
        }
        progenitor.hijos.add(i, hijo);
    }

    /**
     * Agrega el PADRE de la persona de esa posición, en la posición 2n (ver Posicion.posicionDelPadre).
     * Solo se pueden agregar padres al sujeto y a sus ascendientes.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un ascendiente, si ese padre
     *         ya existe o si no cumple las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        revisarQuePuedeTenerPadres(posicion);
        return agregarAscendiente(Posicion.posicionDelPadre(posicion), nombre, nacimiento, fallecimiento);
    }

    /**
     * Igual que agregarPadre, pero agrega la MADRE (posición 2n + 1).
     */
    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        revisarQuePuedeTenerPadres(posicion);
        return agregarAscendiente(Posicion.posicionDeLaMadre(posicion), nombre, nacimiento, fallecimiento);
    }

    /** Lanza ArbolInvalidoException si la posición no existe o si no es el sujeto ni un ascendiente. */
    private void revisarQuePuedeTenerPadres(String posicion) {
        if (buscarNodo(posicion) == null) {
            throw new ArbolInvalidoException("No existe nadie en la posición " + posicion + ".");
        }
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes ("
                    + posicion + " es un descendiente).");
        }
    }

    private Persona agregarAscendiente(String nuevaPosicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esAhnentafel(nuevaPosicion)) {
            throw new ArbolInvalidoException("No se pueden agregar más generaciones de ascendientes (la posición "
                    + nuevaPosicion + " pasa de " + Posicion.MAX_AHNENTAFEL + ").");
        }
        return agregar(new Persona(nuevaPosicion, nombre, nacimiento, fallecimiento));
    }

    /**
     * Agrega un HIJO a la persona de esa posición. Solo se pueden agregar hijos al sujeto y a sus descendientes.
     * Su posición es la del progenitor + "." + el número siguiente al MAYOR número de hijo que ya exista
     * (o 1 si todavía no tiene hijos). Ej.: si "1" tiene los hijos 1.1 y 1.3, el nuevo es 1.4.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un descendiente, o si no cumple
     *         las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo progenitor = buscarNodo(posicion);
        if (progenitor == null) {
            throw new ArbolInvalidoException("No existe nadie en la posición " + posicion + ".");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes ("
                    + posicion + " es un ascendiente).");
        }
        int siguiente = 1;
        if (!progenitor.hijos.isEmpty()) {
            Nodo ultimo = progenitor.hijos.get(progenitor.hijos.size() - 1);   // el de mayor número
            siguiente = Posicion.orden(ultimo.persona.getPosicion()) + 1;
        }
        String nuevaPosicion = posicion + "." + siguiente;
        if (!Posicion.esDAboville(nuevaPosicion)) {
            throw new ArbolInvalidoException("No se pueden agregar más hijos a la posición " + posicion + ".");
        }
        return agregar(new Persona(nuevaPosicion, nombre, nacimiento, fallecimiento));
    }

    // =====================================================================
    // Eliminar
    // =====================================================================

    /**
     * Elimina a la persona de esa posición y TODO lo que depende de ella:
     *   - si es un ascendiente: ella y todos sus propios ascendientes (sus padres, abuelos...);
     *   - si es un descendiente: ella y todos sus propios descendientes (hijos, nietos...).
     * Las demás posiciones NO cambian de número.
     *
     * @return cuántas personas se eliminaron (ella incluida)
     * @throws ArbolInvalidoException si la posición es "1" (el sujeto no se puede eliminar) o si no existe
     */
    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto (posición 1) no se puede eliminar.");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("No existe nadie en la posición " + posicion + ".");
        }
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (hijo.padre == nodo) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
        } else {
            Nodo progenitor = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            progenitor.hijos.remove(nodo);
        }
        int eliminados = contar(nodo);
        tamano -= eliminados;
        return eliminados;
    }

    /** @return cuántos nodos hay en el subárbol que empieza en ese nodo (él incluido) */
    private static int contar(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        int total = 1 + contar(nodo.padre) + contar(nodo.madre);
        for (Nodo hijo : nodo.hijos) {
            total += contar(hijo);
        }
        return total;
    }

    // =====================================================================
    // Recorridos
    // =====================================================================

    /**
     * @return todos los miembros en este orden: el sujeto; luego los ascendientes por número Ahnentafel
     *         ascendente (2, 3, 4, 5...); luego los descendientes en PREORDEN (cada persona y después sus hijos:
     *         1.1, 1.1.1, 1.1.2, 1.2, ...). La lista es una copia nueva: modificarla no cambia el árbol.
     *         Este orden garantiza que cada persona aparece DESPUÉS de la que necesita (útil para guardar y cargar).
     */
    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    /** @return solo los ascendientes, por número Ahnentafel ascendente (sin el sujeto) */
    public List<Persona> ascendientes() {
        // Recorrido por niveles (en anchura): visitando primero al padre y luego a la madre, los números
        // Ahnentafel salen en orden ascendente (2, 3, 4, 5, 6, 7...).
        List<Persona> lista = new ArrayList<>();
        List<Nodo> cola = new ArrayList<>();
        cola.add(raiz);
        for (int i = 0; i < cola.size(); i++) {
            Nodo actual = cola.get(i);
            if (actual != raiz) {
                lista.add(actual.persona);
            }
            if (actual.padre != null) {
                cola.add(actual.padre);
            }
            if (actual.madre != null) {
                cola.add(actual.madre);
            }
        }
        return lista;
    }

    /** @return solo los descendientes, en preorden (sin el sujeto) */
    public List<Persona> descendientes() {
        List<Persona> lista = new ArrayList<>();
        for (Nodo hijo : raiz.hijos) {
            preorden(hijo, lista);
        }
        return lista;
    }

    private static void preorden(Nodo nodo, List<Persona> lista) {
        lista.add(nodo.persona);
        for (Nodo hijo : nodo.hijos) {
            preorden(hijo, lista);
        }
    }
}
