package calculator;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JButton;

import org.scilab.forge.jlatexmath.TeXIcon;

public class CalcButton extends JButton {

  /**
   * Subclass of JButton, allows for custom background color for default,
   * mouse hover, and mouse click.
   */

  private Color hoverBackgroundColor;
  private Color pressedBackgroundColor;
 
  public CalcButton(String text) {
    super(text);
    super.setContentAreaFilled(false);
  }
  
  public CalcButton(TeXIcon icon) {
    super(icon);
    super.setContentAreaFilled(false);
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (getModel().isPressed()) {
      g.setColor(pressedBackgroundColor);
    } else if (getModel().isRollover()) {
      g.setColor(hoverBackgroundColor);
    } else {
      g.setColor(getBackground());
    }
    g.fillRect(0, 0, getWidth(), getHeight());
    super.paintComponent(g);
  }

  @Override
  public void setContentAreaFilled(boolean b) {
  }

  public Color getHoverBackgroundColor() {
    return hoverBackgroundColor;
  }

  public void setHoverBackgroundColor(Color hoverBackgroundColor) {
    this.hoverBackgroundColor = hoverBackgroundColor;
  }

  public Color getPressedBackgroundColor() {
    return pressedBackgroundColor;
  }

  public void setPressedBackgroundColor(Color pressedBackgroundColor) {
    this.pressedBackgroundColor = pressedBackgroundColor;
  }
}
