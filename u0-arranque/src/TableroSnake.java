import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TableroSnake extends JPanel implements ActionListener {
    private final int ANCHO = 300;
    private final int ALTO = 300;
    private final int TAMANO_PUNTO = 10;
    private final int PUNTOS_TOTALES = 900;
    private final int RETARDO = 140;

    private final int x[] = new int[PUNTOS_TOTALES];
    private final int y[] = new int[PUNTOS_TOTALES];

    private int longitudSerpiente;
    private int manzana_x;
    private int manzana_y;

    private boolean izquierda = false;
    private boolean derecha = true;
    private boolean arriba = false;
    private boolean abajo = false;
    private boolean enJuego = true;

    private Timer timer;

    public TableroSnake() {
        setBackground(Color.black);
        setFocusable(true);
        setPreferredSize(new Dimension(ANCHO, ALTO));
        addKeyListener(new ControlTeclas());
        iniciarJuego();
    }

    private void iniciarJuego() {
        longitudSerpiente = 3;
        for (int z = 0; z < longitudSerpiente; z++) {
            x[z] = 50 - z * 10;
            y[z] = 50;
        }
        generarManzana();
        timer = new Timer(RETARDO, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (enJuego) {
            g.setColor(Color.red);
            g.fillRect(manzana_x, manzana_y, TAMANO_PUNTO, TAMANO_PUNTO);

            for (int z = 0; z < longitudSerpiente; z++) {
                if (z == 0) {
                    g.setColor(Color.green); // Cabeza
                } else {
                    g.setColor(new Color(45, 180, 0)); // Cuerpo
                }
                g.fillRect(x[z], y[z], TAMANO_PUNTO, TAMANO_PUNTO);
            }
            Toolkit.getDefaultToolkit().sync();
        } else {
            String msg = "Fin del Juego";
            Font fuente = new Font("Helvetica", Font.BOLD, 14);
            FontMetrics fm = getFontMetrics(fuente);
            g.setColor(Color.white);
            g.setFont(fuente);
            g.drawString(msg, (ANCHO - fm.stringWidth(msg)) / 2, ALTO / 2);
        }
    }

    private void generarManzana() {
        int r = (int) (Math.random() * 29);
        manzana_x = r * TAMANO_PUNTO;
        r = (int) (Math.random() * 29);
        manzana_y = r * TAMANO_PUNTO;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (enJuego) {
            // Comprobar si come la manzana
            if ((x[0] == manzana_x) && (y[0] == manzana_y)) {
                longitudSerpiente++;
                generarManzana();
            }

            // Comprobar colisiones
            for (int z = longitudSerpiente; z > 0; z--) {
                if ((z > 4) && (x[0] == x[z]) && (y[0] == y[z])) enJuego = false;
            }
            if (y[0] >= ALTO || y[0] < 0 || x[0] >= ANCHO || x[0] < 0) enJuego = false;
            if (!enJuego) timer.stop();

            // Mover
            for (int z = longitudSerpiente; z > 0; z--) {
                x[z] = x[(z - 1)];
                y[z] = y[(z - 1)];
            }
            if (izquierda) x[0] -= TAMANO_PUNTO;
            if (derecha) x[0] += TAMANO_PUNTO;
            if (arriba) y[0] -= TAMANO_PUNTO;
            if (abajo) y[0] += TAMANO_PUNTO;
        }
        repaint();
    }

    private class ControlTeclas extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if ((key == KeyEvent.VK_LEFT) && (!derecha)) { izquierda = true; arriba = false; abajo = false; }
            if ((key == KeyEvent.VK_RIGHT) && (!izquierda)) { derecha = true; arriba = false; abajo = false; }
            if ((key == KeyEvent.VK_UP) && (!abajo)) { arriba = true; derecha = false; izquierda = false; }
            if ((key == KeyEvent.VK_DOWN) && (!arriba)) { abajo = true; derecha = false; izquierda = false; }
        }
    }
}