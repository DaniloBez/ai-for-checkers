package AI;

import Game.Move;

public class TranspositionTable {
    public static final int SIZE = 1 << 22;
    private static final int MASK = SIZE - 1;

    private static final long[] keys = new long[SIZE];
    private static final int[] scores = new int[SIZE];
    private static final byte[] depths = new byte[SIZE];
    private static final byte[] flags = new byte[SIZE];
    private static final Move[] bestMoves = new Move[SIZE];

    public static final byte EXACT = 0;
    public static final byte ALPHA = 1;
    public static final byte BETA = 2;

    public static final int UNKNOWN = 2_000_000;

    public static void store(long key, int depth, int score, byte flag, Move bestMove) {
        int index = (int) (key & MASK);

        if (keys[index] != key || depths[index] <= depth) {
            keys[index] = key;
            scores[index] = score;
            depths[index] = (byte) depth;
            flags[index] = flag;
            bestMoves[index] = bestMove;
        }
    }

    public static int probeScore(long key, int depth, int alpha, int beta) {
        int index = (int) (key & MASK);

        if (keys[index] == key && depths[index] >= depth) {
            int score = scores[index];
            byte flag = flags[index];

            if (flag == EXACT) return score;
            if (flag == ALPHA && score <= alpha) return score;
            if (flag == BETA && score >= beta) return score;
        }
        return UNKNOWN;
    }

    public static Move probeMove(long key) {
        int index = (int) (key & MASK);
        if (keys[index] == key)
            return bestMoves[index];

        return null;
    }
}
