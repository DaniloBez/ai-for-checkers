package View.Swing;

import Game.Board;
import Game.Move;
import Game.Piece;

import java.util.ArrayList;
import java.util.List;

public class PathDecoder {
    private static final int[][] JUMP_DIRS = {{1, -1}, {1, 1}, {-1, -1}, {-1, 1}};

    public static List<Integer> reconstructPath(Board board, Move move) {
        if (move.capturedMask() == 0)
            return List.of(move.toIndex());

        List<Integer> resultPath = new ArrayList<>();
        int startRow = Board.getRow(move.fromIndex());
        int startCol = Board.getColumn(move.fromIndex());
        Piece piece = board.getPieceAt(startRow, startCol);

        dfs(board, startRow, startCol, startRow, startCol, piece, 0, false, new ArrayList<>(), move, resultPath);

        return resultPath;
    }

    private static boolean dfs(
            Board board,
            int initialRow,
            int initialCol,
            int currentRow,
            int currentCol,
            Piece piece,
            int currentMask,
            boolean promoted,
            List<Integer> currentPath,
            Move targetMove,
            List<Integer> resultPath) {

        if (currentMask == targetMove.capturedMask() && Board.getBitIndex(currentRow, currentCol) == targetMove.toIndex()) {
            resultPath.addAll(currentPath);
            return true;
        }

        boolean isWhite = board.isWhiteToMove();
        boolean isKing = (piece == Piece.WHITE_KING || piece == Piece.BLACK_KING);

        for (int[] dir : JUMP_DIRS) {
            int row = currentRow;
            int col = currentCol;

            int enemyRow = -1;
            int enemyCol = -1;
            int enemyIndex = -1;

            while (true) {
                row += dir[0];
                col += dir[1];

                if (!Board.isValidPlayableSquare(row, col)) break;

                int squareIndex = Board.getBitIndex(row, col);
                Piece p = board.getPieceAt(row, col);
                boolean isAlreadyCaptured = (currentMask & (1 << squareIndex)) != 0;

                if (p != Piece.EMPTY) {
                    if (isAlreadyCaptured) break;
                    if (isEnemy(p, isWhite)) {
                        enemyRow = row;
                        enemyCol = col;
                        enemyIndex = squareIndex;
                    }
                    break;
                }
                if (!isKing) break;
            }

            if (enemyRow != -1) {
                int landRow = enemyRow;
                int landCol = enemyCol;

                while (true) {
                    landRow += dir[0];
                    landCol += dir[1];

                    if (!Board.isValidPlayableSquare(landRow, landCol)) break;

                    Piece landPiece = board.getPieceAt(landRow, landCol);
                    boolean isEmpty = (landPiece == Piece.EMPTY) || (landRow == initialRow && landCol == initialCol);

                    if (isEmpty) {
                        int newMask = currentMask | (1 << enemyIndex);

                        Piece nextPiece = piece;
                        boolean newlyPromoted = promoted;
                        if (!isKing) {
                            if ((isWhite && landRow == Board.ROWS - 1) || (!isWhite && landRow == 0)) {
                                nextPiece = isWhite ? Piece.WHITE_KING : Piece.BLACK_KING;
                                newlyPromoted = true;
                            }
                        }

                        currentPath.add(Board.getBitIndex(landRow, landCol));

                        if (dfs(board, initialRow, initialCol, landRow, landCol, nextPiece, newMask, newlyPromoted, currentPath, targetMove, resultPath)) {
                            return true;
                        }

                        currentPath.removeLast();

                        if (!isKing) break;
                    } else
                        break;
                }
            }
        }

        return false;
    }

    private static boolean isEnemy(Piece piece, boolean isWhiteToMove) {
        if (piece == Piece.EMPTY) return false;
        return isWhiteToMove ? (piece == Piece.BLACK_MAN || piece == Piece.BLACK_KING)
                : (piece == Piece.WHITE_MAN || piece == Piece.WHITE_KING);
    }
}