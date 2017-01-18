package calculator;

import javax.swing.SwingUtilities;

public class Runner {
    public static void main(String[] args) {
        CalcInterface calc = new CalcInterface();
        SwingUtilities.invokeLater(calc);
    }
}
