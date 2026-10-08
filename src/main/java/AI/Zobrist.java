package AI;

import Game.Board;

import java.util.Random;

public class Zobrist {
    public static final long[][] PIECES = new long[4][32];
    public static final long BLACK_TO_MOVE;

    static {
        Random random = new Random(123456789L);

        for (int piece = 0; piece < 4; piece++)
            for (int square = 0; square < 32; square++)
                PIECES[piece][square] = random.nextLong();

        BLACK_TO_MOVE = random.nextLong();
    }

    public static long computeHash(Board board) {
        long hash = 0;
        int whiteMen = board.getWhitePieces() & ~board.getKings();
        int whiteKings = board.getWhitePieces() & board.getKings();
        int blackMen = board.getBlackPieces() & ~board.getKings();
        int blackKings = board.getBlackPieces() & board.getKings();

        hash ^= hashBits(whiteMen, 0);
        hash ^= hashBits(whiteKings, 1);
        hash ^= hashBits(blackMen, 2);
        hash ^= hashBits(blackKings, 3);

        if (!board.isWhiteToMove())
            hash ^= BLACK_TO_MOVE;

        return hash;
    }

    private static long hashBits(int bitboard, int pieceType) {
        long hash = 0;
        int temp = bitboard;
        while (temp != 0) {
            int index = Integer.numberOfTrailingZeros(temp);
            hash ^= PIECES[pieceType][index];
            temp &= (temp - 1);
        }
        return hash;
    }
}
