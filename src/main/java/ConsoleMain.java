import Game.Board;
import Game.Move;
import Game.MoveGenerator;
import View.Console.ConsoleView;
import View.GameView;

void main() {
    Board board = new Board();
    GameView view = new ConsoleView();
    Scanner scanner = new Scanner(System.in);

    while (true) {
        view.displayBoard(board);
        List<Move> moves = MoveGenerator.getLegalMoves(board);

        int winner = board.checkWinner(!moves.isEmpty());
        if (winner != 0) {
            if (winner == 1) System.out.println("The Whites Won!");
            else if (winner == -1) System.out.println("The Blacks Won!");
            else System.out.println("It's a tie!");
            break;
        }

        String playerColor = board.isWhiteToMove() ? "white" : "black";
        System.out.println("Move " + playerColor + ". Available moves:");

        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);

            int startRow = Board.getRow(m.fromIndex());
            int startColumn = Board.getColumn(m.fromIndex());
            int endRow = Board.getRow(m.toIndex());
            int endColumn = Board.getColumn(m.toIndex());

            String action = m.capturedMask() != 0 ? " [Capture!]" : "";
            String promo = m.isPromotion() ? " [King!]" : "";

            System.out.printf("[%d] (%d, %d) -> (%d, %d)%s%s\n", i, startRow, startColumn, endRow, endColumn, action, promo);
        }

        int choice = -1;
        while (choice < 0 || choice >= moves.size()) {
            System.out.print("Select a move number: ");
            if (scanner.hasNextInt())
                choice = scanner.nextInt();
            else
                scanner.next();
        }

        board.makeMove(moves.get(choice));
        System.out.println("--------------------------------------------------");
    }

    scanner.close();
}