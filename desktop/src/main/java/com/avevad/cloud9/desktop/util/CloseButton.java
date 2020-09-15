package com.avevad.cloud9.desktop.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

public class CloseButton extends JButton {
    private boolean mouseOver = false;
    private boolean mousePressed = false;

    public CloseButton(String text) {
        super(text);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);

        MouseAdapter mouseListener = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent me) {
                if (contains(me.getX(), me.getY())) {
                    mousePressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                mousePressed = false;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent me) {
                mouseOver = false;
                mousePressed = false;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent me) {
                mouseOver = contains(me.getX(), me.getY());
                repaint();
            }
        };

        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }

    private int getDiameter() {
        return Math.min(getWidth(), getHeight());
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getGraphics().getFontMetrics(getFont());
        int minDiameter = (int) (metrics.getHeight() * 0.75);
        return new Dimension(minDiameter, minDiameter);
    }

    @Override
    public boolean contains(int x, int y) {
        int radius = getDiameter() / 2;
        return Point2D.distance(x, y, getWidth() / 2., getHeight() / 2.) < radius;
    }

    @Override
    public void paintComponent(Graphics g) {

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int diameter = getDiameter();
        int radius = diameter / 2;

        if (mousePressed || mouseOver) g.setColor(new Color(0x330000));
        else g.setColor(new Color(0xAA0000));
        g.fillOval(getWidth() / 2 - radius, getHeight() / 2 - radius, diameter, diameter);
    }
}
