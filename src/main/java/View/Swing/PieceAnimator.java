package View.Swing;

import Game.Piece;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class PieceAnimator extends JPanel implements ActionListener {
    private Timer timer;
    private long startTime;
    private double progress = 0.0;
    private int totalDuration;

    private List<Point> waypoints;
    private int pieceSize;
    private Piece movingPiece;
    private Runnable onComplete;

    public PieceAnimator() {
        setOpaque(false);
    }

    public void animateMove(Piece piece, List<Point> waypoints, int size, Runnable onComplete) {
        this.movingPiece = piece;
        this.waypoints = waypoints;
        this.pieceSize = size;
        this.onComplete = onComplete;
        this.progress = 0.0;

        this.totalDuration = 200 * (waypoints.size() - 1);

        setVisible(true);
        startTime = System.currentTimeMillis();
        timer = new Timer(16, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long elapsed = System.currentTimeMillis() - startTime;
        progress = (double) elapsed / totalDuration;

        if (progress >= 1.0) {
            progress = 1.0;
            timer.stop();
            setVisible(false);
            if (onComplete != null) onComplete.run();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (progress <= 0 || progress >= 1.0 || movingPiece == null || waypoints == null || waypoints.size() < 2) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int totalSegments = waypoints.size() - 1;
        double scaledProgress = progress * totalSegments;
        int currentSegment = (int) scaledProgress;

        if (currentSegment >= totalSegments) currentSegment = totalSegments - 1;

        double segmentProgress = scaledProgress - currentSegment;

        Point p1 = waypoints.get(currentSegment);
        Point p2 = waypoints.get(currentSegment + 1);

        int currentX = (int) (p1.x + (p2.x - p1.x) * segmentProgress);
        int currentY = (int) (p1.y + (p2.y - p1.y) * segmentProgress);

        boolean isWhite = (movingPiece == Piece.WHITE_MAN || movingPiece == Piece.WHITE_KING);

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillOval(currentX + 5, currentY + 10, pieceSize, pieceSize);

        g2.setColor(isWhite ? Color.WHITE : new Color(45, 45, 45));
        g2.fillOval(currentX, currentY, pieceSize, pieceSize);

        g2.setStroke(new BasicStroke(2));
        g2.setColor(isWhite ? new Color(200, 200, 200) : new Color(20, 20, 20));
        g2.drawOval(currentX, currentY, pieceSize, pieceSize);

        if (movingPiece == Piece.WHITE_KING || movingPiece == Piece.BLACK_KING) {
            g2.setColor(new Color(255, 215, 0));
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(currentX + 15, currentY + 15, pieceSize - 30, pieceSize - 30);
        }
    }
}