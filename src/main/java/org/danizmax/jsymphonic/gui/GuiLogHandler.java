/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.danizmax.jsymphonic.gui;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javax.swing.JTextArea;

/**
 *
 * @author danizmax
 */
public class GuiLogHandler extends Handler{

    private JTextArea textArea;

    public GuiLogHandler(){
    }

    @Override
    public void publish(LogRecord arg0) {
        if(getTextArea() != null){
            getTextArea().append(arg0.getMessage() + "\n");
            getTextArea().setCaretPosition(getTextArea().getText().length() - 1);
        }

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {
        
    }

    /**
     * @return the textArea
     */
    public JTextArea getTextArea() {
        return textArea;
    }

    /**
     * @param textArea the textArea to set
     */
    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;
    }
}
