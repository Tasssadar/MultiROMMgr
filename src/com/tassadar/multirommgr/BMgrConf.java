package com.tassadar.multirommgr;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
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
        SetValues();
        LoadConfig();
    }
    
    @Override
    public void onStop()
    {
        String text = null;
        GetValues();
        try {
            FileWriter w = new FileWriter(CONF_PATH, false);
            w.append("timezone = " + String.valueOf(timezone) + "\r\n");
            w.append("timeout = " + String.valueOf(timeout) + "\r\n");
            w.append("show_seconds = " + (show_seconds ? "1" : "0") + "\r\n");
            w.append("touch_ui = " + (touch_ui ? "1" : "0") + "\r\n");
            w.close();
            text = getResources().getString(R.string.conf_w_succes); 
        }
        catch(IOException e) { 
            text = getResources().getString(R.string.conf_w_error); 
        }
        
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();

        super.onStop();
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
                            // Remove whitespace, replaceAll does not work...wtf?
                            String lineS = "";
                            for(int i = 0; i < line.length(); ++i)
                                if(line.charAt(i) != ' ')
                                    lineS += line.charAt(i);
                            

                            split = lineS.split("=");
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
    
    private Handler m_confLoaded = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            SetValues();
        }
    };
    
    private float timezone;
    private byte timeout;
    private boolean touch_ui;
    private boolean show_seconds;
}