public class DevoradorCPU {
    public static void main(String[] args) {
        System.out.println("Iniciando proceso...");
        System.out.println("Abre tu administrador de tareas o ejecuta 'top'.");
        System.out.println("Busca un proceso de Java consumiendo CPU.");
        System.out.println("Para matarlo, usa el botón rojo (Stop) en IntelliJ.");

        long contador = 0;
        // Bucle infinito: fetch-decode-execute sin pausa
        while (true) {
            contador++;
            if (contador % 2_000_000_000L == 0) {
                System.out.println("Sigo vivo y devorando ciclos de reloj...");
            }
        }
    }
}