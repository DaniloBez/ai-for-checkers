package View.Console;

import Game.Board;
import Game.Piece;
import View.GameView;


public class ConsoleView implements GameView {
    @Override
    public void displayBoard(Board board) {
        System.out.println("\n  0 1 2 3 4 5 6 7");

        for (int row = Board.ROWS - 1; row >= 0; row--) {
            System.out.print(row + " ");

            for (int col = 0; col < Board.COLS; col++) {
                Piece piece = board.getPieceAt(row, col);

                switch (piece) {
                    case WHITE_MAN -> System.out.print("w ");
                    case WHITE_KING -> System.out.print("W ");
                    case BLACK_MAN -> System.out.print("b ");
                    case BLACK_KING -> System.out.print("B ");
                    default -> {
                        if (Board.isValidPlayableSquare(row, col))
                            System.out.print(". ");
                        else
                            System.out.print("  ");
                    }
                }
            }
            System.out.println();
        }
        System.out.println();
    }
}
