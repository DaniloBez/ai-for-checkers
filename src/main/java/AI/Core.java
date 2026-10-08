package AI;

import Game.Board;
import Game.Move;
import Game.MoveGenerator;

import java.util.List;

public class Core {
    private static int nodesEvaluated = 0;

    private static final int WIN_SCORE = 100_000;

    private static boolean stopSearch = false;
    private static int MAX_TIME_THINKING_MS = 500;

    private static final int[] WHITE_BONUS = new int[32];
    private static final int[] BLACK_BONUS = new int[32];

    static {
        for (int i = 0; i < 32; i++) {
            int row = Board.getRow(i);
            int col = Board.getColumn(i);

            int whiteBonus = 0;
            int blackBonus = 0;

            whiteBonus += row * 2;
            blackBonus += (7 - row) * 2;

            if (col == 0 || col == 7) {
                whiteBonus += 2;
                blackBonus += 2;
            }

            if (row >= 3 && row <= 4 && col >= 2 && col <= 5) {
                whiteBonus += 4;
                blackBonus += 4;
            }

            if (row == 0) whiteBonus += 4;
            if (row == 7) blackBonus += 4;

            WHITE_BONUS[i] = whiteBonus;
            BLACK_BONUS[i] = blackBonus;
        }
    }

    public static int evaluate(Board board) {
        int whitePieces = board.getWhitePieces();
        int blackPieces = board.getBlackPieces();
        int kings = board.getKings();

        int whiteScore = 0;
        int blackScore = 0;

        int whiteTemp = whitePieces;
        while (whiteTemp != 0) {
            int index = Integer.numberOfTrailingZeros(whiteTemp);
            boolean isKing = (kings & (1 << index)) != 0;

            if (isKing)
                whiteScore += 300;
            else
                whiteScore += 100 + WHITE_BONUS[index];

            whiteTemp &= (whiteTemp - 1);
        }

        int blackTemp = blackPieces;
        while (blackTemp != 0) {
            int index = Integer.numberOfTrailingZeros(blackTemp);
            boolean isKing = (kings & (1 << index)) != 0;

            if (isKing)
                blackScore += 300;
            else
                blackScore += 100 + BLACK_BONUS[index];

            blackTemp &= (blackTemp - 1);
        }

        return whiteScore - blackScore;
    }

    public static Move findBestMove(Board board) {
        List<Move> legalMoves = MoveGenerator.getLegalMoves(board);
        if (legalMoves.isEmpty()) return null;

        if (legalMoves.size() == 1) {
            System.out.println("AI: Only one possible move (Required). The response is instantaneous.");
            return legalMoves.getFirst();
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + MAX_TIME_THINKING_MS;

        nodesEvaluated = 0;
        stopSearch = false;

        Move globalBestMove = null;

        boolean isWhite = board.isWhiteToMove();
        Board tempBoard = new Board();

        int currentDepth = 1;
        int MAX_SAFE_DEPTH = 64;

        int globalBestScore = 0;

        while (System.currentTimeMillis() < endTime && currentDepth <= MAX_SAFE_DEPTH) {
            Move bestMoveThisDepth = null;
            int bestScore = isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            int alpha = Integer.MIN_VALUE;
            int beta = Integer.MAX_VALUE;

            if (globalBestMove != null) {
                int index = legalMoves.indexOf(globalBestMove);
                if (index > 0) {
                    Move temp = legalMoves.getFirst();
                    legalMoves.set(0, globalBestMove);
                    legalMoves.set(index, temp);
                }
            }

            for (Move move : legalMoves) {
                tempBoard.copyState(board);
                tempBoard.makeMove(move);

                int score = minimax(tempBoard, currentDepth - 1, alpha, beta, endTime);

                if (stopSearch) break;

                if (isWhite) {
                    if (score > bestScore) {
                        bestScore = score;
                        bestMoveThisDepth = move;
                    }
                    if (bestScore > alpha) alpha = bestScore;
                } else {
                    if (score < bestScore) {
                        bestScore = score;
                        bestMoveThisDepth = move;
                    }
                    if (bestScore < beta) beta = bestScore;
                }
            }

            if (!stopSearch && bestMoveThisDepth != null) {
                globalBestMove = bestMoveThisDepth;
                globalBestScore = bestScore;

                if (Math.abs(bestScore) > 0.95 * WIN_SCORE) {
                    System.out.println("AI: I've found a guaranteed way to end the game! I'm stopping the search.");
                    break;
                }
            }

            if (stopSearch) {
                break;
            }
            currentDepth++;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("AI thought: %d ms | Nodes evaluated: %d | Depth: %d | Best Score: %d%n",
                elapsed, nodesEvaluated, currentDepth - 1, isWhite ? globalBestScore : -globalBestScore);

        return globalBestMove != null ? globalBestMove : legalMoves.getFirst();
    }

    private static int minimax(Board board, int depth, int alpha, int beta, long endTime) {
        if ((++nodesEvaluated & 2047) == 0) {
            if (System.currentTimeMillis() >= endTime) {
                stopSearch = true;
                return 0;
            }
        }
        if (stopSearch) return 0;

        long zobristKey = board.getZobristKey();
        int ttScore = TranspositionTable.probeScore(zobristKey, depth, alpha, beta);
        if (ttScore != TranspositionTable.UNKNOWN)
            return ttScore;

        List<Move> legalMoves = MoveGenerator.getLegalMoves(board);

        int winner = board.checkWinner(!legalMoves.isEmpty());
        if (winner == 1) return WIN_SCORE + depth;
        if (winner == -1) return -WIN_SCORE - depth;
        if (winner == 2) return 0;

        if (depth == 0) return quiescence(board, alpha, beta, endTime);

        Move ttMove = TranspositionTable.probeMove(zobristKey);
        if (ttMove != null) {
            int index = legalMoves.indexOf(ttMove);
            if (index > 0) {
                Move temp = legalMoves.getFirst();
                legalMoves.set(0, ttMove);
                legalMoves.set(index, temp);
            }
        }

        boolean isWhite = board.isWhiteToMove();
        int bestScore = isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        int originalAlpha = alpha;
        int originalBeta = beta;
        Move bestMoveThisNode = null;

        Board tempBoard = new Board();

        for (Move move : legalMoves) {
            tempBoard.copyState(board);
            tempBoard.makeMove(move);

            int score = minimax(tempBoard, depth - 1, alpha, beta, endTime);
            if (stopSearch) return 0;

            if (isWhite) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMoveThisNode = move;
                }
                alpha = Math.max(alpha, bestScore);
                if (beta <= alpha) break;
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMoveThisNode = move;
                }
                beta = Math.min(beta, bestScore);
                if (beta <= alpha) break;
            }
        }

        if (!stopSearch) {
            byte flag = TranspositionTable.EXACT;
            if (bestScore <= originalAlpha)
                flag = TranspositionTable.ALPHA;
            else if (bestScore >= originalBeta)
                flag = TranspositionTable.BETA;

            if (Math.abs(bestScore) < 0.9 * WIN_SCORE)
                TranspositionTable.store(zobristKey, depth, bestScore, flag, bestMoveThisNode);
        }

        return bestScore;
    }

    private static int quiescence(Board board, int alpha, int beta, long endTime) {
        if ((++nodesEvaluated & 2047) == 0) {
            if (System.currentTimeMillis() >= endTime) {
                stopSearch = true;
                return 0;
            }
        }
        if (stopSearch) return 0;

        List<Move> legalMoves = MoveGenerator.getLegalMoves(board);

        int winner = board.checkWinner(!legalMoves.isEmpty());
        if (winner == 1) return WIN_SCORE;
        if (winner == -1) return -WIN_SCORE;
        if (winner == 2) return 0;

        if (legalMoves.isEmpty() || legalMoves.getFirst().capturedMask() == 0)
            return evaluate(board);

        boolean isWhite = board.isWhiteToMove();
        int bestScore = isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Board tempBoard = new Board();

        for (Move move : legalMoves) {
            tempBoard.copyState(board);
            tempBoard.makeMove(move);

            int score = quiescence(tempBoard, alpha, beta, endTime);
            if (stopSearch) return 0;

            if (isWhite) {
                if (score > bestScore) bestScore = score;
                alpha = Math.max(alpha, bestScore);
                if (beta <= alpha) break;
            } else {
                if (score < bestScore) bestScore = score;
                beta = Math.min(beta, bestScore);
                if (beta <= alpha) break;
            }
        }

        return bestScore;
    }
}
