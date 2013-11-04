package com.tassadar.multirommgr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.fima.cardsui.objects.Card;


public class StatusCard extends Card {

    @Override
    public View getCardContent(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.status_card, null);
        StatusAsyncTask.instance().setStatusCardLayout(view);
        return view;
    }
}
