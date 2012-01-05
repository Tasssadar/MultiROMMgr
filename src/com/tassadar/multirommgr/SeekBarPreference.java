package com.tassadar.multirommgr;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference
{
    public SeekBarPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setLayoutResource(R.layout.seek_bar_preference);
    }
    
    @Override
    protected void onBindView (View view)
    {
    	bar = (SeekBar)view.findViewById(R.id.brightSeekBar);
    	bar.setMax(100);
    	bar.setOnSeekBarChangeListener(seekBarChanged);
    	pctText = (TextView)view.findViewById(R.id.brightPctVal);
    	setPctValue(value);
    }
    
    public void setPctValue(int pct)
    {
    	value = pct;
    	if(bar == null)
    		return;
    	bar.setProgress(pct);
    	pctText.setText(pct + "%");
    }
    
    public int getPctValue()
    {
    	return value;
    }
    
    private final OnSeekBarChangeListener seekBarChanged = new OnSeekBarChangeListener()
    {

		@Override
		public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
			pctText.setText(progress + "%");
			value = progress;
		}

		@Override
		public void onStartTrackingTouch(SeekBar arg0) { }
		@Override
		public void onStopTrackingTouch(SeekBar arg0) { }
    };
    
    SeekBar bar;
    TextView pctText;
    int value;

}