package com.tassadar.multirommgr;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;


public class BMgrConf extends PreferenceActivity
{
    private static final String CONF_PATH = "/sdcard/multirom.txt";
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);  
        
        // Default values
        //timezone = 0;
        timeout = 3;
        touch_ui = true;
        show_seconds = false;
        
        addPreferencesFromResource(R.xml.bmgr_config);
        m_tetris_max = (TetrisMaxPreference)findPreference("tetris_max");
        m_tetris_max.setHandler(m_confLoaded);
        getListView().setEnabled(false);
        getListView().setClickable(false);
        SetValues();
        LoadConfig();
    }
    
    @Override
    public void onStop()
    {
        super.onStop();
        if(!getListView().isEnabled())
            return;
        
        String text = null;
        GetValues();
        try {
            FileWriter w = new FileWriter(CONF_PATH, false);
            w.append("timezone = " + String.valueOf(timezone) + "\r\n");
            w.append("timeout = " + String.valueOf(timeout) + "\r\n");
            w.append("show_seconds = " + (show_seconds ? "1" : "0") + "\r\n");
            w.append("touch_ui = " + (touch_ui ? "1" : "0") + "\r\n");
            w.append("tetris_max_score = " + String.valueOf(tetris_max_score) + "\r\n");
            w.close();
            text = getResources().getString(R.string.conf_w_succes); 
        }
        catch(IOException e) { 
            text = getResources().getString(R.string.conf_w_error); 
        }
        
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    
    private void SetValues()
    {
        CheckBoxPreference c = (CheckBoxPreference)findPreference("conf_show_seconds");
        c.setChecked(show_seconds);
        c = (CheckBoxPreference)findPreference("conf_touch_ui");
        c.setChecked(touch_ui);
        
        EditTextPreference e = (EditTextPreference)findPreference("conf_timezone");
        e.setText(String.valueOf(timezone));
        e = (EditTextPreference)findPreference("conf_timeout");
        e.setText(String.valueOf(timeout));
        
        m_tetris_max.setSummary(getResources().getString(R.string.tetris_max_sum) + " " + tetris_max_score);
    }
    
    private void GetValues()
    {
        CheckBoxPreference c = (CheckBoxPreference)findPreference("conf_show_seconds");
        show_seconds = c.isChecked();
        c.setChecked(show_seconds);
        c = (CheckBoxPreference)findPreference("conf_touch_ui");
        touch_ui = c.isChecked();
        
        EditTextPreference e = (EditTextPreference)findPreference("conf_timezone");
        try { timezone = Float.valueOf(e.getText()); }
        catch(NumberFormatException ex) { }
        
        e = (EditTextPreference)findPreference("conf_timeout");
        try { timeout = Byte.valueOf(e.getText()); }
        catch(NumberFormatException ex) { }
    }
    
    private void LoadConfig()
    {
        new Thread(new Runnable() {
            public void run() {
                try {
                    FileInputStream file = new FileInputStream(CONF_PATH);
                    if(file != null)
                    {
                        InputStreamReader inputreader = new InputStreamReader(file);
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        
                        String line;
                        String split[];
                        while(((line = buffreader.readLine()) != null))
                        {
                            line = line.replaceAll(" ", "");
                            
                            split = line.split("=");
                            if(split.length != 2)
                                continue;
                            
                            if(split[0].startsWith("timezone"))
                            {
                                float tmp = timezone;
                                try { tmp = Float.valueOf(split[1]); }
                                catch(NumberFormatException e) { }
                                
                                if(tmp < 25 && tmp > -25)
                                    timezone = tmp;
                            }
                            else if(split[0].startsWith("timeout"))
                            {
                                byte tmp = timeout;
                                try { tmp = Byte.valueOf(split[1]); }
                                catch(NumberFormatException e) { }

                                if(tmp >= -1 && tmp <= 127)
                                    timeout = tmp;
                            }
                            else if(split[0].startsWith("show_seconds"))
                            {
                                byte tmp = (byte) (show_seconds ? 1 : 0);
                                try { tmp = Byte.valueOf(split[1]); }
                                catch(NumberFormatException e) { }

                                if(tmp == 0 || tmp == 1)
                                    show_seconds = tmp == 1 ? true : false; // Fuck java IWontCastAnything
                            }
                            else if(split[0].startsWith("touch_ui"))
                            {
                                byte tmp = (byte) (touch_ui ? 1 : 0);
                                try { tmp = Byte.valueOf(split[1]); }
                                catch(NumberFormatException e) { }

                                if(tmp == 0 || tmp == 1)
                                    touch_ui = tmp == 1 ? true : false;
                            }
                            else if(split[0].startsWith("tetris_max_score"))
                            {
                                int tmp = tetris_max_score;
                                try { tmp = Integer.valueOf(split[1]); }
                                catch(NumberFormatException e) { }
                                tetris_max_score = tmp;
                            }
                        }
                        buffreader.close();
                        inputreader.close();
                        file.close();
                    }    
                }
                catch(FileNotFoundException e) { }
                catch(IOException e) { }
                m_confLoaded.sendEmptyMessage(0);
            }
          }).start();
    }
    
    private void displayTetrisDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.reset_high_score));
        builder.setMessage(getResources().getString(R.string.tetris_dialog));
        builder.setPositiveButton(getResources().getString(R.string.reset), m_click_reset);
        builder.setNegativeButton(getResources().getString(R.string.no_thanks), null);
        builder.setCancelable(true);
        builder.create().show();
    }
    
    private Handler m_confLoaded = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == 1)
            {
                displayTetrisDialog();
                return;
            }
            SetValues();
            getListView().setEnabled(true);
            getListView().setClickable(true);
        }
    };
    
    private OnClickListener m_click_reset = new OnClickListener()
    {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            tetris_max_score = 0;
            SetValues();
        }
    };
    
    
    private float timezone;
    private byte timeout;
    private boolean touch_ui;
    private boolean show_seconds;
    private int tetris_max_score;
    private TetrisMaxPreference m_tetris_max;
}