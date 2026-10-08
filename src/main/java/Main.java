import Game.Board;
import View.Swing.SwingView;

import javax.swing.*;

void main() {
    SwingUtilities.invokeLater(() -> {
        Object[] options = {"Грати проти комп'ютера", "Грати удвох"};
        int choice = JOptionPane.showOptionDialog(null,
                "Оберіть режим гри:", "Шашки",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        boolean isPvE = (choice == 0);
        boolean humanIsWhite = true;

        if (isPvE) {
            humanIsWhite = Math.random() < 0.5;
            String msg = humanIsWhite ? "Ви граєте БІЛИМИ!\nВаш хід перший." : "Ви граєте ЧОРНИМИ!\nКомп'ютер ходить першим.";
            JOptionPane.showMessageDialog(null, msg, "Жеребкування", JOptionPane.INFORMATION_MESSAGE);
        }

        Board board = new Board();
        board.reset();

        new SwingView(board, isPvE, humanIsWhite);
    });
}