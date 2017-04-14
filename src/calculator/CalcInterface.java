package calculator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.math.BigInteger;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.text.TextAction;

import org.scilab.forge.jlatexmath.*;

public class CalcInterface implements Runnable {
  private JFrame mainFrame;
  private JTextField resultDisplay;
  private JTextField numDigitsDisplay;
  private JTextField listDisplay;
  
  private final static Color DISPLAY_BG_COLOR = new Color(68, 68, 68);
  private final static Color ERROR_BG_COLOR = new Color(100, 40, 40);
  
  private CalcButton[] numbers;
  private CalcButton[] unaryOperators;
  private CalcButton[] binaryOperators;
  private CalcButton[] listOperators;
  private CalcButton equals;
  private CalcButton clear;
  private CalcButton copy;
  private CalcButton paste;
  private CalcButton about;
  
  private BigInteger curDisplay = BigInteger.ZERO;
  private BigInteger operand1 = BigInteger.ZERO;
  private BigInteger operand2 = BigInteger.ZERO;
  private int curOp = -1;
  private boolean inOp = false;
  private boolean newOp = true;
  private boolean cleared = false;
  private static CalcFunctions c = new CalcFunctions();
  
  private static CalcButton makeButton(String text, int type) {
      Color fg = Color.BLACK, bg = Color.WHITE, hover = Color.GRAY, pressed = Color.GRAY;
      Color highlight = Color.WHITE, shadow = Color.BLACK;
      int size = 20;
    if (type == 0) {
      // Number buttons
      bg = new Color(234, 234, 234);
      hover = new Color(243, 243, 243);
      pressed = new Color(224, 224, 224);
      shadow = new Color(100, 100, 100);
      size = 24;
    } else if (type == 1) {
      // Normal operators
      bg = new Color(205, 205, 205);
      hover = new Color(218, 218, 218);
      pressed = new Color(195, 195, 195);
      highlight = new Color(240, 240, 240);
      shadow = new Color(80, 80, 80);
      size = 18;
    } else if (type == 2) {
      // Orange operators
      fg = new Color(242, 242, 242);
      bg = new Color(229, 120, 11); 
      hover = new Color(239, 139, 28);
      pressed = new Color(219, 118, 17);
      highlight = new Color(234, 154, 79);
      shadow = new Color(140, 82, 35);
      size = 30;
    } else if (type == 3) {
      // Menu buttons
      bg = new Color(238, 238, 238);
      hover = new Color(245, 245, 245);
      pressed = new Color(230, 230, 230);
    }
    CalcButton button;
    if (type == 3) {
      button = new CalcButton(text);
      button.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(120, 120, 120)));
        button.setPreferredSize(new Dimension(60, 20));
    } else {
      TeXFormula formula = new TeXFormula(text);
        TeXIcon icon = formula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_DISPLAY)
          .setSize(size)
          .setWidth(TeXConstants.UNIT_PIXEL, 256f, TeXConstants.ALIGN_CENTER)
          .setIsMaxWidth(true).setInterLineSpacing(TeXConstants.UNIT_PIXEL, 20f)
          .build();
      button = new CalcButton(icon);
      button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, highlight, shadow));
      button.setPreferredSize(new Dimension(80, 50));
    }
    button.setBackground(bg);
    button.setForeground(fg);
    button.setHoverBackgroundColor(hover);
    button.setPressedBackgroundColor(pressed);
    return button;
  }
  
  private static JTextField makeTextField(String text, int size) {
    JTextField field = new JTextField(text);
    field.setEditable(false);
    field.setFont(new Font(Font.MONOSPACED, 1, size));
    field.setBackground(DISPLAY_BG_COLOR);
    field.setForeground(new Color(242, 242, 242));
    field.setBorder(new BevelBorder(BevelBorder.LOWERED));
    return field;
  }
  
  /**
   * Unary (one argument) operator interface
   */
  private interface unaryOperator {
    BigInteger compute(BigInteger x);
  }
  
  /**
   * Binary (two arguments) operator interface
   */
  private interface binaryOperator {
    BigInteger compute(BigInteger x, BigInteger y);
  }
  
  /**
   * List operator interface, takes one argument and displays result in bottom left corner
   */
  private interface listOperator {
    String evaluate(BigInteger x);
  }
  
  // All button labels use LaTeX, all tooltips use HTML
  private enum UnaryOperation implements unaryOperator {
    NEGATE("\\pm",
        "Negate",
        9, 2, x -> x.negate()),
    PRIMES("\\pi(n)",
        "<html>Primes less than n<br><b>Limit:</b> 7 digits</html>",
        4, 2, x -> c.sieveOfAtkin(x)),
    ISQRT("\\lfloor{\\sqrt{n}}\\rfloor",
        "<html>Integer square root<br><b>Limit:</b> 2000 digits</html>",
        0, 6, x-> c.isqrt(x)),
    FACTORIAL("n!",
        "<html>Factorial<br><b>Limit:</b> 4 digits</html>",
        6, 2, x -> c.factorial(x)),
    DOUBLE_FACTORIAL("n!!",
        "<html>Double factorial<br><b>Limit:</b> 4 digits</html>",
        7, 2, x -> c.doubleFactorial(x)),
    DERANGEMENTS("!n",
        "<html>Derangements<br><b>Limit:</b> 4 digits</html>",
        6, 3, x -> c.derangement(x)),
    CATALAN("C_n",
        "<html>Catalan number<br><b>Limit:</b> 4 digits</html>",
        6, 4, x -> c.catalan(x)),
    FIBONACCI("F_n",
        "<html>Fibonacci number<br><b>Limit:</b> 4 digits</html>",
        5, 5, x -> c.fibonacci(x, BigInteger.ZERO, BigInteger.ONE)),
    LUCAS("L_n",
        "<html>Lucas number<br><b>Limit:</b> 4 digits</html>",
        5, 6, x -> c.fibonacci(x, BigInteger.valueOf(2), BigInteger.ONE)),
    INT_PARTITION_1("p(n)",
        "<html>Partitions<br><b>Limit:</b> 3 digits</html>",
        6, 5, x -> c.intPartition(x)),
    SET_PARTITION_1("B_n",
        "<html>Bell number<br><b>Limit:</b> 600</html>",
        6, 6, x -> c.setPartition(x)),
    NUM_DIVISORS("\\sigma_0(n)",
        "<html>Number of divisors<br><b>Limit:</b> 12 digits</html>",
        3, 4, x -> c.sumDivisors(BigInteger.ZERO, x)),
    SUM_DIVISORS("\\sigma_1(n)",
        "<html>Sum of divisors<br><b>Limit:</b> 12 digits</html>",
        4, 4, x -> c.sumDivisors(BigInteger.ONE, x)),
    EULER_TOTIENT("\\phi(n)",
        "<html>Euler's totient function<br><b>Limit:</b> 12 digits</html>",
        4, 3, x -> c.jordanTotient(x, BigInteger.ONE)),
    MOBIUS("\\mu(n)",
        "<html>Mobius function<br><b>Limit:</b> 12 digits</html>",
        3, 3, x -> c.mobius(x)),
    CARMICHAEL("\\lambda(n)",
        "<html>Carmichael function<br><b>Limit:</b> 12 digits</html>",
        0, 5, x -> c.carmichael(x)),
    PRIMORIAL("n\\#",
        "<html>Primorial<br><b>Limit:</b> 4 digits</html>",
        5, 2, x -> c.primorial(x)),
    SQUARED("n^2",
        "Square",
        1, 5, x -> x.pow(2)),
    CUBED("n^3",
        "Cube",
        2, 5, x -> x.pow(3)),
    TWO_TO_THE_N("2^n",
        "<html>Power of 2<br><b>Limit:</b> 4 digits</html>",
        1, 6, x -> c.newPow(BigInteger.valueOf(2), x)),
    THREE_TO_THE_N("3^n",
        "<html>Power of 3<br><b>Limit:</b> 4 digits</html>",
        2, 6, x -> c.newPow(BigInteger.valueOf(3), x)),
    LITTLE_OMEGA("\\omega(n)",
        "<html>Number of distinct prime factors<br><b>Limit:</b> 12 digits</html>",
        1, 4, x -> c.littleOmega(x)),
    BIG_OMEGA("\\Omega(n)",
        "<html>Sum of prime factor powers<br><b>Limit:</b> 12 digits</html>",
        2, 4, x -> c.bigOmega(x)),
    BACKSPACE("\\leftarrow", null,
        10, 6, x -> x.divide(BigInteger.TEN));
  
    private final String symbol, toolTip;
    private final unaryOperator equation;
    private final int xPos, yPos;

    UnaryOperation(String symbol, String toolTip, int xPos, int yPos, unaryOperator equation) {
      this.symbol = symbol;
      this.toolTip = toolTip;
      this.equation = equation;
      this.xPos = xPos;
      this.yPos = yPos;
    }
    
    @Override
    public BigInteger compute(BigInteger x) {
      return equation.compute(x);
    }

    @Override
    public String toString() {
      return symbol;
    }
  }

  private enum BinaryOperation implements binaryOperator {
    ADD("+",
        "Add",
        11, 5, (x, y) -> x.add(y)),
    SUBTRACT("-",
        "Subtract",
        11, 4,(x, y) -> x.subtract(y)),
    MULTIPLY("\\times",
        "Multiply",
        11, 3, (x, y) -> x.multiply(y)),
    DIVIDE("\\div",
        "Divide",
        11, 2, (x, y) -> c.newDivide(x, y)),
    MOD("\\text{mod}",
        "Modulo",
        10, 2, (x, y) -> c.newMod(x, y)),
    POLY("p(s,n)",
        "s-gonal number",
        4, 5, (x, y) -> c.polygon(x, y)),
    POLY_CENTERED("p_c(s,n)",
        "Centered s-gonal number",
        4, 6, (x, y) -> c.polygonCentered(x, y)),
    BINOM_COEFF("\\binom{n}{k}",
        "<html>Binomial coefficient<br><b>Limit:</b> 4 digits</html>",
        7, 4, (x, y) -> c.binomialCoefficient(x, y)),
    PERMUTATION("P(n,k)",
        "<html>k-Permutation<br><b>Limit:</b> 4 digits</html>",
        7, 3, (x, y) -> c.permutation(x, y)),
    INT_PARTITION_2("p_k(n)",
        "<html>Partition of size k<br><b>Limit:</b> 3 digits</html>",
        7, 5, (x, y) -> c.intPartition(x, y)),
    SET_PARTITION_2("S(n,k)",
        "<html>Stirling number of the second kind<br><b>Limit:</b> 600</html>",
        7, 6, (x, y) -> c.setPartition(x, y)),
    SUM_DIVISORS_GENERAL("\\sigma_k(n)",
        "Sum of divisors each raised to power k",
        5, 4, (x, y) -> c.sumDivisors(x, y)),
    JACOBI("\\left(\\frac{a}{n}\\right)",
        "Jacobi symbol",
        0, 3, (x, y) -> c.jacobi(x, y)),
    JORDAN_TOTIENT("J_k(n)",
        "Jordan's Totient Function",
        5, 3, (x, y) -> c.jordanTotient(x, y)),
    MOD_INVERSE("a_m^{-1}",
        "Modular multiplicative inverse",
        0, 4, (x, y) -> c.newModInverse(x, y)),
    LCM("\\text{lcm}(a,b)",
        "Least common multiple",
        2, 3, (x, y) -> c.lcm(x,y)),
    GCD("\\text{gcd}(a,b)",
        "Greatest common divisor",
        1, 3, (x, y) -> x.gcd(y)),
    X_TO_THE_Y("x^y",
        "<html>Power of x<br><b>Limit:</b> 4 digits for exponent</html>",
        3, 5, (x, y) -> c.newPow(x,y)),
    Y_TO_THE_X("y^x",
        "<html>Power of y<br><b>Limit:</b> 4 digits for exponent</html>",
        3, 6, (x, y) -> c.newPow(y,x));
    
    private final String symbol, toolTip;
    private final binaryOperator equation;
    private final int xPos, yPos;

    BinaryOperation(String symbol, String toolTip, int xPos, int yPos, binaryOperator equation) {
      this.symbol = symbol;
      this.toolTip = toolTip;
      this.equation = equation;
      this.xPos = xPos;
      this.yPos = yPos;
    }
    
    @Override
    public BigInteger compute(BigInteger x, BigInteger y) {
      return equation.compute(x, y);
    }

    @Override
    public String toString() {
      return symbol;
    }
  }
  
  private enum ListOperation implements listOperator {
    IS_PRIME("\\text{Prime?}",
      "<html>Deterministic primality test<br><b>Limit:</b> 24 digits, 1000 digits for Mersenne numbers<html>",
      3, 2, x -> c.stringifyPrime(x)),
    FACTOR("\\text{Factors}",
      "<html>Prime factorization<br><b>Limit:</b> 12 digits</html>",
      1, 2, x -> c.stringifyFactors(x)),
    DIVISORS("\\text{Divisors}",
      "<html>List of divisors<br><b>Limit:</b> 12 digits</html>",
      2, 2, x -> c.stringifyDivisors(x)),
    QUAD_RESIDUES("\\text{Q.R.}",
      "<html>Quadratic residues<br><b>Limit:</b> 5 digits</html>",
      0, 2, x -> c.stringifyQuadResidue(x));
    
    private final String symbol, toolTip;
    private final listOperator func;
    private final int xPos, yPos;
    
    ListOperation(String symbol, String toolTip, int xPos, int yPos, listOperator func) {
          this.symbol = symbol;
          this.toolTip = toolTip;
          this.func = func;
          this.xPos = xPos;
          this.yPos = yPos;
    }
    
    @Override
    public String evaluate(BigInteger x) {
      return func.evaluate(x);
    }
    
    @Override
    public String toString() {
      return symbol;
    } 
  }
  
  /**
   * Verify that string is an integer, i.e. only digits and optionally a minus sign at the start.
   * Limits integer length from 1 to 10000 digits.
   * @param s String to be verified
   * @return true if string represents an integer
   */
  private boolean verifyInteger(String s) {
    if (!s.isEmpty() && s.length() <= 10000 + (s.charAt(0) == '-' ? 1 : 0)) {
      for (int i = 0; i < s.length(); i++) {
        if (i == 0 && s.charAt(i) == '-') {
          if (s.length() == 1)
            return false;
          else
            continue;
        }
        if (Character.digit(s.charAt(i), 10) < 0)
          return false;
      }
      return true;
    }
    else
      return false;
  }
  
  /**
   * Copy contents of resultsDisplay to clipboard
   */
  private void copyContents() {
    StringSelection selection = new StringSelection(resultDisplay.getText());
    Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
    c.setContents(selection, selection);
  }
  
  /**
   * Paste contents of clipboard to results display if it is a valid integer
   */
  private void pasteContents() {
    try {
      String clipStr = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
      if (verifyInteger(clipStr)) {
        BigInteger clipNum = new BigInteger(clipStr);
        if (!inOp) {
          operand1 = clipNum;
          curDisplay = operand1;
        } else {
          operand2 = clipNum;
          curDisplay = operand2;
        }
        changeDisplay();
      }
    } catch (HeadlessException e) {
      e.printStackTrace();
    } catch (UnsupportedFlavorException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Update resultDisplay and numDigitsDisplay based on the curDisplay variable
   */
  private void changeDisplay() {
    String displayText = String.valueOf(curDisplay);
    resultDisplay.setText(displayText);
    resultDisplay.setBackground(DISPLAY_BG_COLOR);
    int numLen = (displayText.length() - (displayText.charAt(0) == '-' ? 1 : 0));
    numDigitsDisplay.setText(numLen + (numLen == 1 ? " digit " : " digits"));
  }
  
  /**
   * Action for digit (0 - 9) press
   * @param i The pressed digit
   */
  private void pressNumber(int i) {
    if (newOp) {
      curDisplay = BigInteger.ZERO;
      newOp = false;
    }
    if (curDisplay.signum() != -1) {
      curDisplay = curDisplay.multiply(BigInteger.TEN).add(BigInteger.valueOf(i));
    } else {
      curDisplay = curDisplay.multiply(BigInteger.TEN).subtract(BigInteger.valueOf(i));
    }
    cleared = false;
  }
  
  /**
   * Action for equals button press or enter press, evaluate the given binary operation
   */
  private void evaluate() {
    if (curOp != -1) {
      BinaryOperation op = BinaryOperation.values()[curOp];
      BigInteger res;
      if (inOp)
        res = op.compute(operand1, curDisplay); // Pressing = button after a new operation
      else
        res = op.compute(operand1, operand2);     // Repressing = button
      if (res != null) {
        if (inOp) {
          operand2 = curDisplay;
          inOp = false;
        }
        curDisplay = res;
        operand1 = res;
        changeDisplay();
        cleared = false;
        newOp = true;
      } else {
        resultDisplay.setBackground(ERROR_BG_COLOR);
      }
    }
  }

  /**
   * Action for clear button press
   */
  private void clearContents() {
    if (cleared) { // If pressed twice in a row, clear last operation and last operand
      operand2 = BigInteger.ZERO;
      curOp = -1;
    }
    operand1 = BigInteger.ZERO;
    curDisplay = BigInteger.ZERO;
    changeDisplay();
    listDisplay.setText("");
    cleared = true;
    inOp = false;
  }

  @Override
  public void run() {
    mainFrame = new JFrame();
    mainFrame.setLayout(new GridBagLayout());
    
    // Sets look and feel to system default. Not especially important,
    // as most of the buttons have a custom appearance.
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } 
    catch (UnsupportedLookAndFeelException e) {
      e.printStackTrace();
    }
    catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    catch (InstantiationException e) {
      e.printStackTrace();
    }
    catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setFocusable(true);
    mainFrame.setFocusTraversalKeysEnabled(false);
    mainFrame.setResizable(false);
    mainFrame.setTitle("Tyler's Number Theory Calculator");
    JRootPane rootPane = mainFrame.getRootPane();
    
    // Keyboard input for digits
    for (Integer i = 0; i < 10; i++) {
      String a = "num" + i + "Action";
      final int innerI = i;
      rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
          .put(KeyStroke.getKeyStroke(i.toString()), a);
      Action action = new TextAction(a) {
        public void actionPerformed(ActionEvent e) {
          pressNumber(innerI);
          changeDisplay();
        };
      };
      rootPane.getActionMap().put(a, action);
    }
    
    // Keyboard input for backspace
    String a = "backspaceAction";
    rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), a);
    Action action = new TextAction(a) {
      public void actionPerformed(ActionEvent e) {
          curDisplay = UnaryOperation.BACKSPACE.compute(curDisplay);
          changeDisplay();
      };
    };
    rootPane.getActionMap().put(a, action);
    
    // Keyboard action for enter
    a = "enterAction";
    rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), a);
    action = new TextAction(a) {
      public void actionPerformed(ActionEvent e) {
        evaluate();
      };
    };
    rootPane.getActionMap().put(a, action);
    
    // Keyboard action for copy (ctrl+c)
    a = "copyAction";
    rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control C"), a);
    action = new TextAction(a) {
      public void actionPerformed(ActionEvent e) {
        copyContents();
      };
    };
    rootPane.getActionMap().put(a, action);

    // Keyboard action for paste (ctrl+v)
    a = "pasteAction";
    rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control V"), a);
    action = new TextAction(a) {
      public void actionPerformed(ActionEvent e) {
        pasteContents();
      };
    };
    rootPane.getActionMap().put(a, action);     
    
    GridBagConstraints gbc = new GridBagConstraints();

    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    // Menu buttons
    about = makeButton("About", 3);
    about.addActionListener(e -> {
      JOptionPane.showMessageDialog(null,
          "<html><h2>Tyler's Number Theory Calculator</h2>"
          + "<p>Version 1.0.1</p>"
          + "<p>Written by Tyler Sontag</p>"
          + "<p><a href='http://www.tylersontag.com'>http://www.tylersontag.com</a></p></html>",
          "About", JOptionPane.PLAIN_MESSAGE);
    });
    gbc.gridx = 0;
    gbc.gridy = 0;
    mainFrame.add(about, gbc);
    
    copy = makeButton("Copy", 3);
    copy.addActionListener(e -> {
      copyContents();
    });
    gbc.gridx = 1;
    gbc.gridy = 0;
    mainFrame.add(copy, gbc);
    
    paste = makeButton("Paste", 3);
    paste.addActionListener(e -> {
      pasteContents();
    });
    gbc.gridx = 2;
    gbc.gridy = 0;
    mainFrame.add(paste, gbc);
    
    // Text fields
    resultDisplay = makeTextField("0", 32);
    resultDisplay.setPreferredSize(new Dimension(720, 50));
    resultDisplay.setHorizontalAlignment(JTextField.RIGHT);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 12;
    mainFrame.add(resultDisplay, gbc);
    
    numDigitsDisplay = makeTextField("1 digit ", 20);
    numDigitsDisplay.setPreferredSize(new Dimension(240, 30));
    numDigitsDisplay.setHorizontalAlignment(JTextField.RIGHT);
    gbc.gridx = 8;
    gbc.gridy = 7;
    gbc.gridwidth = 4;
    mainFrame.add(numDigitsDisplay, gbc);
    
    listDisplay = makeTextField("", 20);
    listDisplay.setPreferredSize(new Dimension(480, 30));
    gbc.gridx = 0;
    gbc.gridy = 7;
    gbc.gridwidth = 8;
    mainFrame.add(listDisplay, gbc);
    
    // Instantiation of all other buttons
    numbers = new CalcButton[10];
    binaryOperators = new CalcButton[BinaryOperation.values().length];
    unaryOperators = new CalcButton[UnaryOperation.values().length];
    listOperators = new CalcButton[ListOperation.values().length];

    // Number buttons
    for (int i = 0; i < 10; i++) {
      final int innerI = i;
      numbers[i] = makeButton(String.valueOf(i), 0);
      numbers[i].addActionListener(e -> {
        pressNumber(innerI);
          changeDisplay();
      });
      if (i == 0) {
        gbc.gridx = 8;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
      } else {
        gbc.gridx = ((i - 1) % 3) + 8;
        gbc.gridy = 5 - ((i - 1) / 3);
        gbc.gridwidth = 1;
      }
      mainFrame.add(numbers[i], gbc);
    }

    // Binary operator buttons
    for (int i = 0; i < binaryOperators.length; i++) {
      final int innerI = i;
      BinaryOperation op = BinaryOperation.values()[i];
      if (i <= 3) {
        binaryOperators[i] = makeButton(op.toString(), 2);
      } else {
        binaryOperators[i] = makeButton(op.toString(), 1);
      }
      binaryOperators[i].addActionListener(e -> {
        if (inOp) {
          // Chaining binary operators
          if (!newOp) {
            BinaryOperation oldOp = BinaryOperation.values()[curOp];
            BigInteger res = oldOp.compute(operand1, curDisplay);
            if (res != null) {
              operand2 = curDisplay;
              curDisplay = res;
              changeDisplay();
              operand1 = curDisplay;
              newOp = true;
              cleared = false;
            } else {
              resultDisplay.setBackground(ERROR_BG_COLOR);
            }
          }
        } else {
          // New binary operator
          operand1 = curDisplay;
          curDisplay = BigInteger.ZERO;
          inOp = true;
          newOp = true;
          cleared = false;
        }
        curOp = innerI;
      });
      if (op.toolTip != null)
        binaryOperators[i].setToolTipText(op.toolTip);
      gbc.gridx = op.xPos;
      gbc.gridy = op.yPos;
      mainFrame.add(binaryOperators[i], gbc);
    }
    
    // Unary operator buttons
    for (int i = 0; i < unaryOperators.length; i++) {
      UnaryOperation op = UnaryOperation.values()[i];
      unaryOperators[i] = makeButton(op.toString(), 1);
      unaryOperators[i].addActionListener(e -> {
        BigInteger res = op.compute(curDisplay);
        if (res != null) {
          curDisplay = res;
          if (!inOp)
            operand1 = res;
          else
            operand2 = res;
          changeDisplay();
          cleared = false;
          newOp = true;
        } else {
          resultDisplay.setBackground(ERROR_BG_COLOR);
        }
      });
      if (op.toolTip != null)
        unaryOperators[i].setToolTipText(op.toolTip);
      gbc.gridx = op.xPos;
      gbc.gridy = op.yPos;
      mainFrame.add(unaryOperators[i], gbc);
    }
    
    // List operator buttons
    for (int i = 0; i < listOperators.length; i++) {
      ListOperation op = ListOperation.values()[i];
      listOperators[i] = makeButton(op.toString(), 1);
      listOperators[i].addActionListener(e -> {
        String displayStr = op.evaluate(curDisplay);
        listDisplay.setText(displayStr);
      });
      if (op.toolTip != null)
        listOperators[i].setToolTipText(op.toolTip);
      gbc.gridx = op.xPos;
      gbc.gridy = op.yPos;
      mainFrame.add(listOperators[i], gbc);
    }
    
    equals = makeButton("=", 2);
    gbc.gridx = 11;
    gbc.gridy = 6;
    equals.addActionListener(e -> {
      evaluate();         
    });
    mainFrame.add(equals, gbc);

    clear = makeButton("\\text{C}", 1);
    gbc.gridx = 8;
    gbc.gridy = 2;
    clear.addActionListener(e -> {
      clearContents();
    });
    mainFrame.add(clear, gbc);
    
    mainFrame.setVisible(true);
    mainFrame.pack();
  }
}
