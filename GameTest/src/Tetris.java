import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * TETRIS — Java puro (JDK 21), sin librerías externas.
 * Compilar y ejecutar:  javac Tetris.java && java Tetris
 * O ejecución directa:  java Tetris.java
 */
public final class Tetris extends JPanel {

    // ---------------- Constantes ----------------
    private static final int COLS = 10, ROWS = 20;
    private static final int CELL = 32;                 // tamaño de celda (px)
    private static final int PREVIEW_CELL = 22;
    private static final int PAD = 14;
    private static final int SIDE_W = 200;
    private static final int BOARD_PX_W = COLS * CELL;
    private static final int BOARD_PX_H = ROWS * CELL;

    private static final Color BG       = new Color(0x101018);
    private static final Color BOARD_BG = new Color(0x181822);
    private static final Color GRID     = new Color(255, 255, 255, 26);
    private static final Color TEXT     = new Color(0xE8E8F0);
    private static final Color SUBTEXT  = new Color(0x9A9AB0);

    private static final int[] LINE_SCORES = {0, 100, 300, 500, 800};

    private static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    private static final Font FONT_LABEL = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font FONT_VALUE = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private static final Font FONT_HELP  = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font FONT_OVER  = new Font(Font.SANS_SERIF, Font.BOLD, 34);
    private static final Font FONT_SUB   = new Font(Font.SANS_SERIF, Font.PLAIN, 15);

    private static final String HELP = """
            ← / →      mover
            ↓          caída suave
            ↑ / X      rotar →
            Z          rotar ←
            ESPACIO    caída dura
            P / ESC    pausa
            R          reiniciar""";

    // ---------------- Piezas ----------------
    private enum Piece {
        I(new int[][]{{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}}, new Color(0x35C1E8)),
        O(new int[][]{{1,1},{1,1}},                             new Color(0xF2C230)),
        T(new int[][]{{0,1,0},{1,1,1},{0,0,0}},                 new Color(0xB45BE0)),
        S(new int[][]{{0,1,1},{1,1,0},{0,0,0}},                 new Color(0x4FC94F)),
        Z(new int[][]{{1,1,0},{0,1,1},{0,0,0}},                 new Color(0xE85050)),
        J(new int[][]{{1,0,0},{1,1,1},{0,0,0}},                 new Color(0x5578E8)),
        L(new int[][]{{0,0,1},{1,1,1},{0,0,0}},                 new Color(0xF09030));

        final int[][] shape;
        final Color color;
        Piece(int[][] shape, Color color) { this.shape = shape; this.color = color; }
    }

    // ---------------- Estado ----------------
    private final Color[][] board = new Color[ROWS][COLS];   // null = celda vacía
    private final Random rng = new Random();
    private final ArrayDeque<Piece> bag = new ArrayDeque<>(); // randomizador "7-bag"
    private final Timer gravity;

    private Piece cur, next;
    private int[][] curShape;
    private int curX, curY;

    private long score;
    private int lines, level;
    private boolean paused, gameOver;

    // ---------------- Constructor ----------------
    private Tetris() {
        setPreferredSize(new Dimension(PAD * 3 + BOARD_PX_W + SIDE_W, PAD * 2 + BOARD_PX_H));
        setBackground(BG);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKey(e.getKeyCode()); }
        });

        gravity = new Timer(520, e -> tick());
        restart();
        gravity.start();
    }

    // ---------------- Lógica de juego ----------------
    private int dropDelay() {
        return Math.max(80, 520 - (level - 1) * 42);
    }

    private void tick() {
        if (paused || gameOver) return;
        if (!collides(curShape, curX, curY + 1)) curY++;
        else lockPiece();
        repaint();
    }

    private void restart() {
        for (Color[] row : board) Arrays.fill(row, null);
        bag.clear();
        score = 0; lines = 0; level = 1;
        paused = false; gameOver = false;
        next = fromBag();
        spawn();
        gravity.setDelay(dropDelay());
        repaint();
    }

    private Piece fromBag() {
        if (bag.isEmpty()) {
            var pieces = new ArrayList<>(List.of(Piece.values()));
            Collections.shuffle(pieces, rng);
            bag.addAll(pieces);
        }
        return bag.poll();
    }

    private void spawn() {
        cur = next;
        next = fromBag();
        curShape = cur.shape;
        curX = (COLS - curShape[0].length) / 2;
        curY = 0;
        if (collides(curShape, curX, curY)) gameOver = true;
    }

    private boolean collides(int[][] shape, int px, int py) {
        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] == 0) continue;
                int bx = px + x, by = py + y;
                if (bx < 0 || bx >= COLS || by >= ROWS) return true;
                if (by >= 0 && board[by][bx] != null) return true;
            }
        }
        return false;
    }

    private void lockPiece() {
        boolean topOut = false;
        for (int y = 0; y < curShape.length; y++) {
            for (int x = 0; x < curShape[y].length; x++) {
                if (curShape[y][x] == 0) continue;
                int by = curY + y, bx = curX + x;
                if (by < 0) { topOut = true; continue; }
                board[by][bx] = cur.color;
            }
        }
        if (topOut) { gameOver = true; return; }
        clearLines();
        spawn();
    }

    private void clearLines() {
        int cleared = 0;
        for (int y = ROWS - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < COLS; x++) {
                if (board[y][x] == null) { full = false; break; }
            }
            if (!full) continue;
            cleared++;
            for (int row = y; row > 0; row--) board[row] = board[row - 1];
            board[0] = new Color[COLS];
            y++; // vuelve a comprobar la misma fila tras el desplazamiento
        }
        if (cleared == 0) return;
        score += (long) LINE_SCORES[cleared] * level;
        lines += cleared;
        int newLevel = lines / 10 + 1;
        if (newLevel != level) {
            level = newLevel;
            gravity.setDelay(dropDelay());
        }
    }

    // ---------------- Entrada ----------------
    private void onKey(int code) {
        switch (code) {
            case KeyEvent.VK_R -> { restart(); return; }
            case KeyEvent.VK_P, KeyEvent.VK_ESCAPE -> {
                if (!gameOver) paused = !paused;
                repaint();
                return;
            }
            default -> { }
        }
        if (paused || gameOver) return;
        switch (code) {
            case KeyEvent.VK_LEFT  -> moveHorizontal(-1);
            case KeyEvent.VK_RIGHT -> moveHorizontal(1);
            case KeyEvent.VK_DOWN  -> softDrop();
            case KeyEvent.VK_UP, KeyEvent.VK_X -> rotate(true);
            case KeyEvent.VK_Z     -> rotate(false);
            case KeyEvent.VK_SPACE -> hardDrop();
            default -> { }
        }
        repaint();
    }

    private void moveHorizontal(int dx) {
        if (!collides(curShape, curX + dx, curY)) curX += dx;
    }

    private void softDrop() {
        if (!collides(curShape, curX, curY + 1)) { curY++; score++; }
    }

    private void hardDrop() {
        int dist = 0;
        while (!collides(curShape, curX, curY + 1)) { curY++; dist++; }
        score += 2L * dist;
        lockPiece();
    }

    private void rotate(boolean clockwise) {
        if (cur == Piece.O) return;
        int[][] rotated = clockwise ? rotateCW(curShape) : rotateCCW(curShape);
        // Patadas de pared sencillas: intenta 0, ±1, ±2 (y una fila arriba si hace falta)
        for (int dx : new int[]{0, -1, 1, -2, 2}) {
            if (!collides(rotated, curX + dx, curY)) {
                curShape = rotated; curX += dx; return;
            }
            if (!collides(rotated, curX + dx, curY - 1)) {
                curShape = rotated; curX += dx; curY--; return;
            }
        }
    }

    private static int[][] rotateCW(int[][] m) {
        int rows = m.length, cols = m[0].length;
        int[][] r = new int[cols][rows];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                r[x][rows - 1 - y] = m[y][x];
        return r;
    }

    private static int[][] rotateCCW(int[][] m) {
        int rows = m.length, cols = m[0].length;
        int[][] r = new int[cols][rows];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                r[cols - 1 - x][y] = m[y][x];
        return r;
    }

    // ---------------- Dibujo ----------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int bx = PAD, by = PAD;
        drawBoardBackground(g2, bx, by);
        drawStack(g2, bx, by);
        if (!gameOver) {
            drawGhost(g2, bx, by);
            drawCurrent(g2, bx, by);
        }
        drawSidePanel(g2, PAD * 2 + BOARD_PX_W, PAD);
        drawOverlays(g2, bx, by);
    }

    private void drawBoardBackground(Graphics2D g2, int bx, int by) {
        g2.setColor(BOARD_BG);
        g2.fillRect(bx, by, BOARD_PX_W, BOARD_PX_H);
        g2.setColor(GRID);
        for (int x = 1; x < COLS; x++) g2.drawLine(bx + x * CELL, by, bx + x * CELL, by + BOARD_PX_H);
        for (int y = 1; y < ROWS; y++) g2.drawLine(bx, by + y * CELL, bx + BOARD_PX_W, by + y * CELL);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRect(bx, by, BOARD_PX_W, BOARD_PX_H);
    }

    private void drawStack(Graphics2D g2, int bx, int by) {
        for (int y = 0; y < ROWS; y++)
            for (int x = 0; x < COLS; x++) {
                Color c = board[y][x];
                if (c != null) drawCell(g2, bx + x * CELL, by + y * CELL, CELL, c, false);
            }
    }

    private void drawGhost(Graphics2D g2, int bx, int by) {
        int gy = curY;
        while (!collides(curShape, curX, gy + 1)) gy++;
        for (int y = 0; y < curShape.length; y++)
            for (int x = 0; x < curShape[y].length; x++)
                if (curShape[y][x] != 0 && gy + y >= 0)
                    drawCell(g2, bx + (curX + x) * CELL, by + (gy + y) * CELL, CELL, cur.color, true);
    }

    private void drawCurrent(Graphics2D g2, int bx, int by) {
        for (int y = 0; y < curShape.length; y++)
            for (int x = 0; x < curShape[y].length; x++)
                if (curShape[y][x] != 0 && curY + y >= 0)
                    drawCell(g2, bx + (curX + x) * CELL, by + (curY + y) * CELL, CELL, cur.color, false);
    }

    private void drawCell(Graphics2D g2, int px, int py, int size, Color c, boolean ghost) {
        if (ghost) {
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 55));
            g2.fillRect(px + 2, py + 2, size - 4, size - 4);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
            g2.drawRect(px + 2, py + 2, size - 5, size - 5);
            return;
        }
        g2.setColor(c);
        g2.fillRect(px + 1, py + 1, size - 2, size - 2);
        g2.setColor(c.brighter());
        g2.drawLine(px + 1, py + 1, px + size - 2, py + 1);
        g2.drawLine(px + 1, py + 1, px + 1, py + size - 2);
        g2.setColor(c.darker());
        g2.drawLine(px + size - 2, py + 1, px + size - 2, py + size - 2);
        g2.drawLine(px + 1, py + size - 2, px + size - 2, py + size - 2);
    }

    private void drawSidePanel(Graphics2D g2, int sx, int sy) {
        g2.setFont(FONT_TITLE);
        g2.setColor(TEXT);
        g2.drawString("TETRIS", sx, sy + 24);

        g2.setFont(FONT_LABEL);
        g2.setColor(SUBTEXT);
        g2.drawString("SIGUIENTE", sx, sy + 64);

        int boxY = sy + 76, boxH = 4 * PREVIEW_CELL + 20;
        g2.setColor(BOARD_BG);
        g2.fillRoundRect(sx, boxY, SIDE_W, boxH, 12, 12);
        drawPreview(g2, next, sx + SIDE_W / 2, boxY + boxH / 2);

        int y = boxY + boxH + 38;
        g2.setFont(FONT_LABEL);
        g2.setColor(SUBTEXT);
        g2.drawString("PUNTOS", sx, y);
        g2.setFont(FONT_VALUE);
        g2.setColor(TEXT);
        g2.drawString(String.format("%,d", score), sx, y + 26);

        g2.setFont(FONT_LABEL);
        g2.setColor(SUBTEXT);
        g2.drawString("NIVEL", sx, y + 64);
        g2.drawString("LÍNEAS", sx + 90, y + 64);
        g2.setFont(FONT_VALUE);
        g2.setColor(TEXT);
        g2.drawString(String.valueOf(level), sx, y + 90);
        g2.drawString(String.valueOf(lines), sx + 90, y + 90);

        g2.setFont(FONT_HELP);
        g2.setColor(SUBTEXT);
        String[] helpLines = HELP.split("\n");
        int hy = sy + BOARD_PX_H - helpLines.length * 17;
        for (String line : helpLines) { g2.drawString(line, sx, hy); hy += 17; }
    }

    private void drawPreview(Graphics2D g2, Piece p, int cx, int cy) {
        int[][] m = p.shape;
        int minX = 99, minY = 99, maxX = -1, maxY = -1;
        for (int y = 0; y < m.length; y++)
            for (int x = 0; x < m[y].length; x++)
                if (m[y][x] != 0) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                }
        int w = (maxX - minX + 1) * PREVIEW_CELL;
        int h = (maxY - minY + 1) * PREVIEW_CELL;
        int ox = cx - w / 2, oy = cy - h / 2;
        for (int y = 0; y < m.length; y++)
            for (int x = 0; x < m[y].length; x++)
                if (m[y][x] != 0)
                    drawCell(g2, ox + (x - minX) * PREVIEW_CELL, oy + (y - minY) * PREVIEW_CELL,
                            PREVIEW_CELL, p.color, false);
    }

    private void drawOverlays(Graphics2D g2, int bx, int by) {
        if (!paused && !gameOver) return;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(bx, by, BOARD_PX_W, BOARD_PX_H);

        String title = gameOver ? "GAME OVER" : "PAUSA";
        g2.setFont(FONT_OVER);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(TEXT);
        g2.drawString(title, bx + (BOARD_PX_W - fm.stringWidth(title)) / 2, by + BOARD_PX_H / 2 - 10);

        String sub = gameOver ? "Pulsa R para reiniciar" : "Pulsa P para continuar";
        g2.setFont(FONT_SUB);
        fm = g2.getFontMetrics();
        g2.setColor(SUBTEXT);
        g2.drawString(sub, bx + (BOARD_PX_W - fm.stringWidth(sub)) / 2, by + BOARD_PX_H / 2 + 24);
    }

    // ---------------- Arranque ----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris · Java 21");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            Tetris game = new Tetris();
            frame.setContentPane(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}