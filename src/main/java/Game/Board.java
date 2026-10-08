package Game;

import AI.Zobrist;

public class Board {
    public static final int ROWS = 8;
    public static final int COLS = 8;

    private int whitePieces;
    private int blackPieces;
    private int kings;

    private boolean isWhiteToMove;

    private int drawMoveCounter;
    private static final int MAX_DRAW_MOVES = 30;

    private long zobristKey;

    public Board() {
        reset();
    }

    public void reset() {
        this.whitePieces = 0x00000FFF;
        this.blackPieces = 0xFFF00000;
        this.kings = 0;
        this.isWhiteToMove = true;
        this.drawMoveCounter = 0;
        zobristKey = Zobrist.computeHash(this);
    }

    public int getWhitePieces() {
        return whitePieces;
    }

    public int getBlackPieces() {
        return blackPieces;
    }

    public int getKings() {
        return kings;
    }

    public long getZobristKey() {
        return zobristKey;
    }

    public boolean isWhiteToMove() {
        return isWhiteToMove;
    }

    public static boolean isValidPlayableSquare(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS)
            return false;

        return (row + col) % 2 == 0;
    }

    public static int getBitIndex(int row, int col) {
        if (!isValidPlayableSquare(row, col))
            throw new IllegalArgumentException("Клітинка (" + row + ", " + col + ") біла або за межами!");

        return (row * 4) + (col / 2);
    }

    public static int getRow(int bitIndex) {
        return bitIndex / 4;
    }

    public static int getColumn(int bitIndex) {
        int row = getRow(bitIndex);
        int offset = bitIndex % 4;

        return (offset * 2) + (row % 2);
    }

    public Piece getPieceAt(int row, int col) {
        if (!isValidPlayableSquare(row, col))
            return Piece.EMPTY;

        int bitIndex = getBitIndex(row, col);
        int mask = 1 << bitIndex;

        boolean isWhite = (whitePieces & mask) != 0;
        boolean isBlack = (blackPieces & mask) != 0;
        boolean isKing = (kings & mask) != 0;

        if (isWhite)
            return isKing ? Piece.WHITE_KING : Piece.WHITE_MAN;
        else if (isBlack)
            return isKing ? Piece.BLACK_KING : Piece.BLACK_MAN;

        return Piece.EMPTY;
    }

    public void makeMove(Move move) {
        int fromMask = 1 << move.fromIndex();
        int toMask = 1 << move.toIndex();

        boolean isWhite = (whitePieces & fromMask) != 0;
        boolean isKing = (kings & fromMask) != 0;

        boolean isCapture = (move.capturedMask() != 0);

        if (isCapture || !isKing)
            drawMoveCounter = 0;
        else
            drawMoveCounter++;

        int pieceType = isWhite ? (isKing ? 1 : 0) : (isKing ? 3 : 2);
        zobristKey ^= Zobrist.PIECES[pieceType][move.fromIndex()];

        if (isWhite)
            whitePieces &= ~fromMask;
        else
            blackPieces &= ~fromMask;

        if (isKing) kings &= ~fromMask;

        if (isWhite)
            whitePieces |= toMask;
        else
            blackPieces |= toMask;

        if (isKing || move.isPromotion())
            kings |= toMask;

        boolean isKingNow = isKing || move.isPromotion();
        int newPieceType = isWhite ? (isKingNow ? 1 : 0) : (isKingNow ? 3 : 2);
        zobristKey ^= Zobrist.PIECES[newPieceType][move.toIndex()];

        if (isCapture) {
            int capt = move.capturedMask();
            int tempCapt = capt;
            while (tempCapt != 0) {
                int capturedIndex = Integer.numberOfTrailingZeros(tempCapt);

                boolean capturedIsWhite = (whitePieces & (1 << capturedIndex)) != 0;
                boolean capturedIsKing = (kings & (1 << capturedIndex)) != 0;
                int capturedPieceType = capturedIsWhite ? (capturedIsKing ? 1 : 0) : (capturedIsKing ? 3 : 2);

                zobristKey ^= Zobrist.PIECES[capturedPieceType][capturedIndex];

                tempCapt &= (tempCapt - 1);
            }

            whitePieces &= ~capt;
            blackPieces &= ~capt;
            kings &= ~capt;
        }

        isWhiteToMove = !isWhiteToMove;
        zobristKey ^= Zobrist.BLACK_TO_MOVE;
    }

    public void copyState(Board other) {
        this.whitePieces = other.whitePieces;
        this.blackPieces = other.blackPieces;
        this.kings = other.kings;
        this.isWhiteToMove = other.isWhiteToMove;
        this.drawMoveCounter = other.drawMoveCounter;
        this.zobristKey = other.zobristKey;
    }

    public int[] getMaterialCount() {
        int whiteCount = Integer.bitCount(whitePieces);
        int blackCount = Integer.bitCount(blackPieces);

        int whiteKingsCount = Integer.bitCount(whitePieces & kings);
        int blackKingsCount = Integer.bitCount(blackPieces & kings);

        return new int[]{whiteCount, blackCount, whiteKingsCount, blackKingsCount};
    }

    public int checkWinner(boolean hasLegalMoves) {
        if (whitePieces == 0) return -1;
        if (blackPieces == 0) return 1;

        if (!hasLegalMoves)
            return isWhiteToMove ? -1 : 1;

        if (drawMoveCounter >= MAX_DRAW_MOVES)
            return 2;

        int whiteCount = Integer.bitCount(whitePieces);
        int blackCount = Integer.bitCount(blackPieces);
        int whiteKings = Integer.bitCount(whitePieces & kings);
        int blackKings = Integer.bitCount(blackPieces & kings);

        if (whiteCount == 1 && blackCount == 1 && whiteKings == 1 && blackKings == 1)
            return 2;

        return 0;
    }
}
