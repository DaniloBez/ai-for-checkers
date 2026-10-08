package Game;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    private static final int[][] WHITE_MAN_DIRS = {{1, -1}, {1, 1}};
    private static final int[][] BLACK_MAN_DIRS = {{-1, -1}, {-1, 1}};

    private static final int[][] KING_DIRS = {{1, -1}, {1, 1}, {-1, -1}, {-1, 1}};

    private static final int[][] JUMP_DIRS = {{1, -1}, {1, 1}, {-1, -1}, {-1, 1}};

    public static List<Move> getLegalMoves(Board board) {
        List<Move> captures = new ArrayList<>();
        List<Move> moves = new ArrayList<>();

        boolean isWhite = board.isWhiteToMove();

        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Piece piece = board.getPieceAt(row, col);
                if (isOwnPiece(piece, isWhite)) 
                    generateCaptures(board, row, col, piece, captures);
            }
        }

        if (!captures.isEmpty())
            return captures;


        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Piece piece = board.getPieceAt(row, col);
                if (isOwnPiece(piece, isWhite)) 
                    generateMoves(board, row, col, piece, moves);
            }
        }

        return moves;
    }

    private static boolean isOwnPiece(Piece piece, boolean isWhiteToMove) {
        if (piece == Piece.EMPTY) return false;
        if (isWhiteToMove)
            return piece == Piece.WHITE_MAN || piece == Piece.WHITE_KING;
        else
            return piece == Piece.BLACK_MAN || piece == Piece.BLACK_KING;
    }

    private static boolean isEnemy(Piece piece, boolean isWhiteToMove) {
        if (piece == Piece.EMPTY) return false;
        return isWhiteToMove ? (piece == Piece.BLACK_MAN || piece == Piece.BLACK_KING)
                : (piece == Piece.WHITE_MAN || piece == Piece.WHITE_KING);
    }

    public static void generateCaptures(Board board, int row, int col, Piece piece, List<Move> captures) {
        dfsCapture(board, row, col, row, col, piece, 0, false, captures);
    }

    private static void dfsCapture(
            Board board, 
            int initialRow, 
            int initialCol,
            int currentRow, 
            int currentCol, 
            Piece piece,
            int capturedMask, 
            boolean promoted, 
            List<Move> captures
    ) {
        boolean foundFurtherCapture = false;
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
                boolean isAlreadyCaptured = (capturedMask & (1 << squareIndex)) != 0;

                if (p != Piece.EMPTY) {
                    if (isAlreadyCaptured)
                        break;

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
                        foundFurtherCapture = true;
                        int newCapturedMask = capturedMask | (1 << enemyIndex);

                        Piece nextPiece = piece;
                        boolean newlyPromoted = promoted;
                        if (!isKing) {
                            if ((isWhite && landRow == Board.ROWS - 1) || (!isWhite && landRow == 0)) {
                                nextPiece = isWhite ? Piece.WHITE_KING : Piece.BLACK_KING;
                                newlyPromoted = true;
                            }
                        }

                        dfsCapture(board, initialRow, initialCol, landRow, landCol, nextPiece, newCapturedMask, newlyPromoted, captures);

                        if (!isKing) break;
                    } else
                        break;
                }
            }
        }

        if (!foundFurtherCapture && capturedMask != 0) {
            int initialIndex = Board.getBitIndex(initialRow, initialCol);
            int finalIndex = Board.getBitIndex(currentRow, currentCol);

            Move move = new Move(initialIndex, finalIndex, capturedMask, promoted);

            if (!captures.contains(move))
                captures.add(move);
        }
    }

    private static void generateMoves(Board board, int startRow, int startCol, Piece piece, List<Move> moves) {
        boolean isWhite = board.isWhiteToMove();
        boolean isKing = (piece == Piece.WHITE_KING || piece == Piece.BLACK_KING);

        int[][] directions;
        if (isKing)
            directions = KING_DIRS;
        else
            directions = isWhite ? WHITE_MAN_DIRS : BLACK_MAN_DIRS;

        int startIndex = Board.getBitIndex(startRow, startCol);

        for (int[] dir : directions) {
            int row = startRow;
            int col = startCol;

            while (true) {
                row += dir[0];
                col += dir[1];

                if (!Board.isValidPlayableSquare(row, col))
                    break;

                if (board.getPieceAt(row, col) != Piece.EMPTY)
                    break;

                int targetIndex = Board.getBitIndex(row, col);

                boolean isPromotion = false;
                if (!isKing)
                    if ((isWhite && row == Board.ROWS - 1) || (!isWhite && row == 0))
                        isPromotion = true;

                moves.add(new Move(startIndex, targetIndex, 0, isPromotion));

                if (!isKing)
                    break;
            }
        }
    }
}
