import javax.swing.JFrame;

public class Arrancador extends JFrame {

    public Arrancador() {
        add(new TableroSnake());
        setResizable(false);
        pack();
        setTitle("Snake DAM 1 - Version Cero");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        JFrame juego = new Arrancador();
        juego.setVisible(true);
    }
}