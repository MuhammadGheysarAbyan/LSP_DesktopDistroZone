package util;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InputValidator {

    /**
     * Restricts input to numbers only.
     */
    public static void restrictToNumbers(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    // Optional: beep or flash field
                }
            }
        });
    }

    /**
     * Restricts input to letters and spaces only.
     */
    public static void restrictToText(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                // Allow letters, space, backspace, delete, and common punctuation like '.- if
                // needed for names?
                // User said "huruf gabisa dikasi angka". So strictly letters?
                // Names usually contain letters, spaces, maybe ' or .
                if (!Character.isLetter(c) && !Character.isWhitespace(c)
                        && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE
                        && c != '\'' && c != '.') {
                    e.consume();
                }
            }
        });
    }

    /**
     * Limits the length of the input.
     */
    public static void limitLength(JTextField field, int limit) {
        field.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if (str == null)
                    return;
                if ((getLength() + str.length()) <= limit) {
                    super.insertString(offs, str, a);
                }
            }
        });
    }

    /**
     * Combined: Numbers only AND Max Length (e.g., NIK, Phone).
     * Note: Setting document overwrites previous document listener/filter.
     * So we need a custom document that handles both or apply key listener + doc
     * filter.
     * Simpler approach: Use verify logic or custom document for everything.
     * But for existing code simple KeyListener is less intrusive.
     * 
     * Let's use a DocumentFilter approach for robustness if possible,
     * but strictly KeyListener is requested for "can't type".
     * 
     * We will use a mixed approach: KeyAdapter for "consume" (can't type)
     * and DocumentFilter for Length (stop typing).
     */
    public static void restrictToNumbersAndLength(JTextField field, int limit) {
        restrictToNumbers(field);
        limitLength(field, limit);
    }

    /**
     * Checks if email contains '@'.
     * Returns true if valid or empty (optional check), false otherwise.
     */
    public static boolean isValidEmail(String email) {
        return email.contains("@");
    }

    /**
     * Restricts input to time format HH:MM (numbers and colon only).
     * Used for operational hours input fields.
     */
    public static void restrictToTimeFormat(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                String text = field.getText();

                // Allow digits, colon, backspace, and delete
                if (!Character.isDigit(c) && c != ':'
                        && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    return;
                }

                // Limit to 5 characters (HH:MM)
                if (text.length() >= 5 && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    return;
                }

                // Only allow one colon
                if (c == ':' && text.contains(":")) {
                    e.consume();
                    return;
                }

                // Colon only at position 2
                if (c == ':' && text.length() != 2) {
                    e.consume();
                }
            }
        });
    }

    /**
     * Validates time format HH:MM (00:00 - 23:59).
     * Returns true if valid, false otherwise.
     */
    public static boolean isValidTimeFormat(String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}")) {
            return false;
        }
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
        } catch (Exception e) {
            return false;
        }
    }
}
