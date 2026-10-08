package View.Swing;

import AI.Core;
import Game.Board;
import Game.Move;
import Game.MoveGenerator;
import Game.Piece;
import View.GameView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SwingView extends JFrame implements GameView {
    private final Board board;
    private List<Move> currentLegalMoves;

    private final boolean isPvE;
    private final boolean humanIsWhite;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private int hoveredRow = -1;
    private int hoveredCol = -1;

    private final JLabel turnLabel;
    private final JLabel scoreLabel;
    private final JPanel boardPanel;
    private final SquarePanel[][] squares = new SquarePanel[Board.ROWS][Board.COLS];

    private final PieceAnimator animator;
    private Move animatingMove = null;

    private final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private final Color DARK_SQUARE = new Color(181, 136, 99);
    private final Color HIGHLIGHT_SELECTED = new Color(123, 97, 255, 150);
    private final Color HIGHLIGHT_TARGET = new Color(106, 226, 138);
    private final Color HIGHLIGHT_HOVER = new Color(255, 255, 255, 60);
    private final Color HIGHLIGHT_MOVABLE = new Color(106, 226, 138, 60);

    public SwingView(Board board, boolean isPvE, boolean humanIsWhite) {
        this.board = board;
        this.isPvE = isPvE;
        this.humanIsWhite = humanIsWhite;
        this.currentLegalMoves = MoveGenerator.getLegalMoves(board);

        setTitle("Шашки");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        topPanel.setPreferredSize(new Dimension(600, 50));
        topPanel.setBackground(new Color(40, 40, 40));

        turnLabel = new JLabel("", SwingConstants.CENTER);
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        scoreLabel = new JLabel("", SwingConstants.CENTER);
        scoreLabel.setForeground(new Color(200, 200, 200));
        scoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));

        topPanel.add(turnLabel);
        topPanel.add(scoreLabel);
        add(topPanel, BorderLayout.NORTH);

        boardPanel = new JPanel(new GridLayout(Board.ROWS, Board.COLS));
        boardPanel.setPreferredSize(new Dimension(600, 600));

        for (int row = Board.ROWS - 1; row >= 0; row--) {
            for (int col = 0; col < Board.COLS; col++) {
                SquarePanel square = new SquarePanel(row, col);
                squares[row][col] = square;
                boardPanel.add(square);
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        animator = new PieceAnimator();
        setGlassPane(animator);

        updateLabels();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        triggerAITurnIfNeeded();
    }

    @Override
    public void displayBoard(Board board) {
        boardPanel.repaint();
    }

    private void updateLabels() {
        if (board.isWhiteToMove())
            turnLabel.setText("Хід: Білі");
        else
            turnLabel.setText("Хід: Чорні");

        int[] material = board.getMaterialCount();
        int whiteCount = material[0];
        int blackCount = material[1];
        int capturedByWhite = 12 - blackCount;
        int capturedByBlack = 12 - whiteCount;

        scoreLabel.setText(String.format("З'їдено: ⚪ %d | ⚫ %d", capturedByWhite, capturedByBlack));
    }

    private void triggerAITurnIfNeeded() {
        if (!isPvE) return;
        if (board.isWhiteToMove() == humanIsWhite) return;

        turnLabel.setText(turnLabel.getText() + " (ШІ думає...)");

        new Thread(() -> {
            Move bestMove = Core.findBestMove(board);

            SwingUtilities.invokeLater(() -> {
                if (bestMove != null)
                    executeMoveWithAnimation(bestMove);
            });
        }).start();
    }

    private void executeMoveWithAnimation(Move moveToExecute) {
        animatingMove = moveToExecute;

        int startR = Board.getRow(moveToExecute.fromIndex());
        int startC = Board.getColumn(moveToExecute.fromIndex());
        Piece pieceToMove = board.getPieceAt(startR, startC);

        int pieceSize = squares[0][0].getWidth() - 20;

        List<Integer> pathIndices = PathDecoder.reconstructPath(board, moveToExecute);
        List<Point> waypoints = new java.util.ArrayList<>();

        SquarePanel startPanel = squares[startR][startC];
        waypoints.add(SwingUtilities.convertPoint(startPanel, 10, 10, animator));

        for (int stepIndex : pathIndices) {
            int r = Board.getRow(stepIndex);
            int c = Board.getColumn(stepIndex);
            SquarePanel p = squares[r][c];
            waypoints.add(SwingUtilities.convertPoint(p, 10, 10, animator));
        }

        selectedRow = -1;
        selectedCol = -1;
        displayBoard(board);

        animator.animateMove(pieceToMove, waypoints, pieceSize, () -> {
            board.makeMove(moveToExecute);
            animatingMove = null;

            currentLegalMoves = MoveGenerator.getLegalMoves(board);
            updateLabels();

            int winner = board.checkWinner(!currentLegalMoves.isEmpty());
            if (winner != 0) {
                displayBoard(board);
                String msg = winner == 1 ? "Перемогли Білі!" : (winner == -1 ? "Перемогли Чорні!" : "Нічия!");
                JOptionPane.showMessageDialog(this, msg, "Кінець гри", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            } else {
                displayBoard(board);
                triggerAITurnIfNeeded();
            }
        });
    }

    private void handleSquareClick(int row, int col) {
        if (animator.isVisible()) return;
        if (isPvE && (board.isWhiteToMove() != humanIsWhite)) return;

        int clickedIndex = Board.isValidPlayableSquare(row, col) ? Board.getBitIndex(row, col) : -1;

        if (selectedRow == -1) {
            boolean hasMoves = currentLegalMoves.stream().anyMatch(m -> m.fromIndex() == clickedIndex);
            if (hasMoves) {
                selectedRow = row;
                selectedCol = col;
            }
        } else {
            int fromIndex = Board.getBitIndex(selectedRow, selectedCol);
            Move moveToExecute = currentLegalMoves.stream()
                    .filter(m -> m.fromIndex() == fromIndex && m.toIndex() == clickedIndex)
                    .findFirst()
                    .orElse(null);

            if (moveToExecute != null) {
                executeMoveWithAnimation(moveToExecute);
            } else {
                boolean isAnotherValidPiece = currentLegalMoves.stream().anyMatch(m -> m.fromIndex() == clickedIndex);
                if (isAnotherValidPiece) {
                    selectedRow = row;
                    selectedCol = col;
                } else {
                    selectedRow = -1;
                    selectedCol = -1;
                }
            }
        }
        displayBoard(board);
    }

    private class SquarePanel extends JPanel {
        private final int row;
        private final int col;

        public SquarePanel(int row, int col) {
            this.row = row;
            this.col = col;

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleSquareClick(row, col);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hoveredRow = row;
                    hoveredCol = col;
                    boardPanel.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredRow = -1;
                    hoveredCol = -1;
                    boardPanel.repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (Board.isValidPlayableSquare(row, col)) {
                g2.setColor(DARK_SQUARE);
            } else {
                g2.setColor(LIGHT_SQUARE);
            }
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (!Board.isValidPlayableSquare(row, col)) return;
            int bitIndex = Board.getBitIndex(row, col);

            boolean isHumanTurn = !isPvE || (board.isWhiteToMove() == humanIsWhite);

            if (isHumanTurn && selectedRow == -1) {
                boolean hasMoves = currentLegalMoves.stream().anyMatch(m -> m.fromIndex() == bitIndex);
                if (hasMoves) {
                    g2.setColor(HIGHLIGHT_MOVABLE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
            }

            if (row == selectedRow && col == selectedCol) {
                g2.setColor(HIGHLIGHT_SELECTED);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            if (isHumanTurn && selectedRow != -1) {
                int selectedIndex = Board.getBitIndex(selectedRow, selectedCol);
                List<Move> movesForPiece = currentLegalMoves.stream()
                        .filter(m -> m.fromIndex() == selectedIndex)
                        .toList();

                for (Move m : movesForPiece) {
                    if (m.toIndex() == bitIndex) {
                        g2.setColor(HIGHLIGHT_TARGET);
                        int dotSize = 20;
                        g2.fillOval((getWidth() - dotSize) / 2, (getHeight() - dotSize) / 2, dotSize, dotSize);
                    } else if (m.capturedMask() != 0) {
                        List<Integer> path = PathDecoder.reconstructPath(board, m);
                        if (path.contains(bitIndex)) {
                            g2.setColor(new Color(101, 101, 101, 150));
                            int dotSize = 14;
                            g2.fillOval((getWidth() - dotSize) / 2, (getHeight() - dotSize) / 2, dotSize, dotSize);
                        }
                    }
                }
            }

            if (isHumanTurn && row == hoveredRow && col == hoveredCol) {
                g2.setColor(HIGHLIGHT_HOVER);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            if (animatingMove != null)
                if (bitIndex == animatingMove.fromIndex()) return;

            Piece piece = board.getPieceAt(row, col);
            if (piece != Piece.EMPTY) {
                int margin = 10;
                int size = getWidth() - (margin * 2);

                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillOval(margin + 2, margin + 4, size, size);

                boolean isWhite = (piece == Piece.WHITE_MAN || piece == Piece.WHITE_KING);
                g2.setColor(isWhite ? Color.WHITE : new Color(45, 45, 45));
                g2.fillOval(margin, margin, size, size);

                g2.setStroke(new BasicStroke(2));
                g2.setColor(isWhite ? new Color(200, 200, 200) : new Color(20, 20, 20));
                g2.drawOval(margin, margin, size, size);

                if (piece == Piece.WHITE_KING || piece == Piece.BLACK_KING) {
                    g2.setColor(new Color(255, 215, 0));
                    g2.setStroke(new BasicStroke(4));
                    g2.drawOval(margin + 15, margin + 15, size - 30, size - 30);
                }
            }
        }
    }
}