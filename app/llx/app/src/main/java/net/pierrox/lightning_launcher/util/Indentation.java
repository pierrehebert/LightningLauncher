package net.pierrox.lightning_launcher.util;

import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Pair;
import android.widget.EditText;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Class that [INSERT DESCRIPTION HERE]
 */
public class Indentation implements TextWatcher {
    private static final int INDENT_SIZE = 2;
    
    public Indentation(EditText editText) {
        this.editText = editText;
    }
    
    
    // ------------ textwatcher ----------------
    private String spanNewline = "spanNewline";
    private String spanEndBracket = "spanEndBracket";
    private boolean editing = false;
    private EditText editText;
    
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }
    
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(editing) return;
        
        if(count == 1 && s.charAt(start) == '\n'){
            // newline inserted
            editText.getEditableText().setSpan(spanNewline,start,start,0);
        }
        if(count == 1 && s.charAt(start) == '}'){
            // end bracket inserted
            editText.getEditableText().setSpan(spanEndBracket, start, start, 0);
        }
    }
    
    @Override
    public void afterTextChanged(Editable editable) {
        editing = true;
        int spanPos;
        
        // check newline
        spanPos = editable.getSpanStart(spanNewline);
        editable.removeSpan(spanNewline);
        if (spanPos != -1 && editable.charAt(spanPos) == '\n')
            onNewLine(spanPos, editable);
        
        // check endbracket
        spanPos = editable.getSpanStart(spanEndBracket);
        editable.removeSpan(spanEndBracket);
        if (spanPos != -1 && editable.charAt(spanPos) == '}')
            onEndBracket(spanPos, editable);
        
        editing = false;
    }
    
    
    /**
     * Called when an ending bracket is inserted. Decreases the indentation.
     * @param posBracket where the endbracket was
     * @param editable where to unindent
     */
    private void onEndBracket(int posBracket, Editable editable){
        
        // check if beginning of line
        if( posBracket == getLineIndent(posBracket, editable).second ){
            // decrease indent
            decreaseIndent( posBracket, editable );
        }
    }
    
    /**
     * Called when a newline is inserted. Indents it with the same indentation as the previous line (if any)
     * @param posEnter position of the newline char
     * @param editable where to indent
     */
    private void onNewLine(int posEnter, Editable editable){
        
        int n = getLineIndent(posEnter, editable).first;
        StringBuilder indent = new StringBuilder();
        for(int i=0;i<n;++i){
            indent.append(" ");
        }
        
        // do if previous line ends in open bracket
        if(posEnter > 0 && editable.charAt(posEnter - 1) == '{'){
            
            // add newline if also following close bracket
            if(posEnter < editable.length() - 1 && editable.charAt(posEnter + 1) == '}'){
                editable.insert(posEnter + 2, "\n" + indent.toString() + "}");
                // this avoids moving the cursor
                editable.replace(posEnter + 1, posEnter + 2, "");
            }
            
            // add indent size
            for(int i=0;i<INDENT_SIZE;++i){
                indent.append(" ");
            }
        }
        
        // write indent
        editable.insert(posEnter + 1, indent.toString());
    }
    
    // ------------ functions -----------------
    
    /**
     * Returns the size of the indent in the current line (spaces at the left) and the position of the first non-space char
     * @param currentpos pos of current line (any char)
     * @param editable where to search
     * @return length of indent (number of spaces) and position of first non-space char (can be end of file)
     */
    private static Pair<Integer, Integer> getLineIndent(int currentpos, Editable editable){
        // goto beginning of line
        if(currentpos != 0) {
            do{
                currentpos--;
            }while (currentpos >= 0 && editable.charAt(currentpos) != '\n');
            currentpos++;
        }
        
        // find indent size
        int n = 0;
        boolean cont = true;
        while(cont && currentpos < editable.length()){
            switch (editable.charAt(currentpos)){
                case ' ':
                    n++;
                    currentpos++;
                    break;
                case '\t':
                    n+=INDENT_SIZE;
                    currentpos++;
                    break;
                //case '\n':
                default:
                    cont = false;
            }
        }
        return new Pair<>(n, currentpos);
    }
    
    /**
     * Changes the indentation of all the lines selected
     * @param posLeft start of selection
     * @param posRight end of selection
     * @param increase if true increase indent, decrease otherwise
     * @param editable where to apply the indentation
     * @return the new selection (may have changed due to the indentation changes)
     */
    public static Pair<Integer, Integer> modifyIndent(int posLeft, int posRight, boolean increase, Editable editable){
        String span = "modifyIntent";
        editable.setSpan(span, posLeft, posRight, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        
        Deque<Integer> positions = new ArrayDeque<>();
        while(posLeft <= posRight && posLeft <= editable.length()){
            // mark position to indent
            positions.push(posLeft);
            
            // find next line (next newline char)
            posLeft++;
            while(posLeft <= posRight && posLeft <= editable.length() && editable.charAt(posLeft - 1) != '\n')
                posLeft++;
        }
        
        for (Integer position : positions) {
            // indent the lines in reverse order
            if(increase)
                increaseIndent(position, editable);
            else
                decreaseIndent(position, editable);
        }
        
        //restore span
        return new Pair<>(editable.getSpanStart(span), editable.getSpanEnd(span));
    }
    
    /**
     * Increases the indentation of a single line
     * @param posCursor position of a character in the line that will be indented
     * @param editable where to apply the indentation
     */
    private static void increaseIndent(int posCursor, Editable editable){
        
        Pair<Integer, Integer> n_beg = getLineIndent(posCursor, editable);
        int beg = n_beg.second;
        
        // increase indent adding spaces
        for(int i=0; i< INDENT_SIZE; i++) editable.insert(beg, " ");
        
    }
    
    /**
     * Decreases the indentation of a single line
     * @param posCursor position of a character in the line that will be indented
     * @param editable where to apply the indentation
     */
    private static void decreaseIndent(int posCursor, Editable editable){
        
        Pair<Integer, Integer> n_beg = getLineIndent(posCursor, editable);
        int n = n_beg.first;
        int beg = n_beg.second;
        
        if ( n >= INDENT_SIZE ){
            // enough intent to remove, remove the first tab, or all the spaces if no tabs found
            int p = 1;
            while (p <= INDENT_SIZE) {
                if (editable.charAt(beg - p) == '\t') {
                    //tab found, remove
                    editable.delete(beg - p, beg - p + 1);
                    return;
                }
                p++;
            }
            // no tabs found, only spaces, remove them
            editable.delete(beg - INDENT_SIZE, beg);
        }else{
            // not enough indent (so no tabs), just remove all the previous spaces
            editable.delete(beg - n, beg);
        }
    }
}
