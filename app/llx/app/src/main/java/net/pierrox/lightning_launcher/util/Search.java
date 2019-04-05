package net.pierrox.lightning_launcher.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import net.pierrox.lightning_launcher_extreme.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.xgouchet.texteditor.ui.AdvancedEditText;

/**
 * Class that Manages a searchable dialog (currently for the script editor).
 */
public class Search {
    private Activity mCntx;
    private AdvancedEditText mEditText;
    
    private AlertDialog mDialog;
    private EditText mEdTxt;
    private CheckBox mChkBackwards;
    private CheckBox mChkCase;
    private CheckBox mChkRegexp;
    
    public Search(Activity cntx, AdvancedEditText editText) {
        mCntx = cntx;
        mEditText = editText;
    
        // The dialog is saved and created on first use
        mDialog = null;
    }
    
    /**
     * Shows the dialog to search.
     */
    public void showDialog(){
        if(mDialog == null)
            createDialog();
        
        mDialog.show();
    }
    
    /**
     * Performs a search as defined in the dialog.
     * If the search fails the dialog is shown.
     */
    public void searchNext() {
        if(mDialog == null)
            createDialog();
    
        String searchText = mEdTxt.getText().toString();
    
        if(searchText.isEmpty()) {
            // no text to search
            showDialog();
            return;
        }
    
        String text = mEditText.getText().toString();
    
        int flags = 0;
        if(!mChkCase.isChecked()) flags |= Pattern.CASE_INSENSITIVE;
        if(!mChkRegexp.isChecked()) flags |= Pattern.LITERAL;
        
        Pattern pattern = Pattern.compile(searchText, flags);
        int start = -1;
        int end = -1;
    
        Matcher matcher = pattern.matcher(text);
        if(!mChkBackwards.isChecked()) {
            // search fordwards
            
            if( matcher.find(mEditText.getSelectionStart() + 1) || matcher.find(0)){
                // found one just after the selection or from the beginning
                start = matcher.start();
                end = matcher.end();
            }
        }else{
            // search backwards
            
            int until = mEditText.getSelectionEnd();
            while( matcher.find() && matcher.end() < until){
                // found match before cursor, save
                start = matcher.start();
                end = matcher.end();
            }
            
            if(start == -1){
                // not found, continue to find last one
                while( matcher.find() ){
                    // found match, save
                    start = matcher.start();
                    end = matcher.end();
                }
            }
        }
        
        if( start != -1) {
            // found, set selection
            mEditText.setSelection(start, end);
        }else{
            // nothing found
            showDialog();
        }
    }
    
    
    /**
     * Private: creates the dialog
     */
    private void createDialog(){
        
        View views = mCntx.getLayoutInflater().inflate(R.layout.dialog_search, null);
        
        
        mEdTxt = views.findViewById(R.id.srch_text);
        mChkBackwards = views.findViewById(R.id.srch_back);
        mChkCase = views.findViewById(R.id.srch_case);
        mChkRegexp = views.findViewById(R.id.srch_regexp);
    
        //TODO: extract string resources (here and in R.layout.dialog_search )
        mDialog = new AlertDialog.Builder(mCntx)
                .setTitle("Search text")
                .setView(views)
                .setCancelable(true)
                .setPositiveButton("search", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // After this code a 'dialog.dismiss()' will be called, so the dialog won't be shown again if necessary.
                        // By posting a runnable for the next iteration, the code runs after the dismiss call and the dialog will be shown again if required.
                        // Other possible solutions require to override the button listener after the call to show (but the dialog won't 'fade out/in')
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                searchNext();
                            }
                        });
                    }
                })
                .setNegativeButton("cancel", null)
                .create();
    }
}
