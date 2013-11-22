package com.fima.cardsui.objects;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tassadar.multirommgr.R;
import com.fima.cardsui.StackAdapter;
import com.fima.cardsui.SwipeDismissTouchListener;
import com.fima.cardsui.SwipeDismissTouchListener.OnDismissCallback;
import com.fima.cardsui.Utils;
/*import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ObjectAnimator;*/

public class CardStack extends AbstractCard {
    private static final float _12F = 12f;
    private static final float _45F = 45f;
    private static final String NINE_OLD_TRANSLATION_Y = "translationY";
    private static final int SWIPE_UNCHANGED = 0;
    private static final int SWIPE_ENABLE = 1;
    private static final int SWIPE_DISABLE = 2;
    private ArrayList<Card> cards;
    private String title;

    private StackAdapter mAdapter;
    private int mPosition;
    private Context mContext;
    private CardStack mStack;
    private boolean mSlideIn;
    private int mSwipable;

    public CardStack() {
        cards = new ArrayList<Card>();
        mStack = this;
        mSlideIn = true;
        mSwipable = SWIPE_UNCHANGED;
    }

    public CardStack(boolean slideIn) {
        cards = new ArrayList<Card>();
        mStack = this;
        mSlideIn = slideIn;
        mSwipable = SWIPE_UNCHANGED;
    }

    public CardStack(boolean slideIn, boolean swipable) {
        cards = new ArrayList<Card>();
        mStack = this;
        mSlideIn = slideIn;
        mSwipable = swipable ? SWIPE_ENABLE : SWIPE_DISABLE;
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public void add(Card newCard) {
        cards.add(newCard);

    }

    @Override
    public View getView(Context context) {
        return getView(context, false);
    }

    @Override
    public View getView(Context context, boolean swipable) {

        if(mSwipable != SWIPE_UNCHANGED)
                swipable = (mSwipable == SWIPE_ENABLE);

        mContext = context;

        final View view = LayoutInflater.from(context).inflate(
                R.layout.item_stack, null);

        final RelativeLayout container = (RelativeLayout) view
                .findViewById(R.id.stackContainer);
        final TextView title = (TextView) view.findViewById(R.id.stackTitle);

        if (!TextUtils.isEmpty(this.title)) {
            title.setText(this.title);
            title.setVisibility(View.VISIBLE);
        }

        final int cardsArraySize = cards.size();
        final int lastCardPosition = cardsArraySize - 1;

        Card card;
        View cardView;
        for (int i = 0; i < cardsArraySize; i++) {
            card = cards.get(i);
            cardView = null;
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);

            int topPx = 0;

            if (lastCardPosition == i) {
                // last card
                cardView = card.getViewLast(context);
                cardView.setOnClickListener(card.getClickListener());
            } else {
                if (0 == i) {
                    // first card
                    cardView = card.getViewFirst(context);

                } else {
                    // any other card
                    cardView = card.getView(context);

                }

                cardView.setOnClickListener(getClickListener(this, container, i));

            }

            if (i > 0) {
                float dp = (_45F * i) - _12F;
                topPx = Utils.convertDpToPixelInt(context, dp);
            }

            lp.setMargins(0, topPx, 0, 0);

            cardView.setLayoutParams(lp);
            
            if (swipable){
                cardView.setOnTouchListener(new SwipeDismissTouchListener(
                        cardView, card, new OnDismissCallback() {
                            
                            @Override
                            public void onDismiss(View view, Object token) {
                                Card c = (Card) token;
                                // call onCardSwiped() listener
                                c.OnSwipeCard();
                                cards.remove(c);
                                

                                mAdapter.setItems(mStack, getPosition());

                                // refresh();
                                mAdapter.notifyDataSetChanged();
                                
                            }
                        }));
            }

            container.addView(cardView);
        }

        if(mSlideIn) {
            Animation a = AnimationUtils.loadAnimation(context, R.anim.card_slide_in);
            view.startAnimation(a);
            mSlideIn = false;
        }

        return view;
    }

    public Card remove(int index) {
        return cards.remove(index);
    }

    public Card get(int i) {
        return cards.get(i);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private OnClickListener getClickListener(final CardStack cardStack,
            final RelativeLayout container, final int index) {
        return new OnClickListener() {

            @Override
            public void onClick(View v) {

                // init views array
                View[] views = new View[container.getChildCount()];

                for (int i = 0; i < views.length; i++) {
                    views[i] = container.getChildAt(i);

                }

                int last = views.length - 1;

                if (index != last) {

                    if (index == 0) {
                        onClickFirstCard(cardStack, container, index, views);
                    } else if (index < last) {
                        onClickOtherCard(cardStack, container, index, views,
                                last);
                    }

                }

            }

            public void onClickFirstCard(final CardStack cardStack,
                    final RelativeLayout frameLayout, final int index, View[] views) {
                // run through all the cards
                /*for (int i = 0; i < views.length; i++) {
                    ObjectAnimator anim = null;

                    if (i == 0) {
                        // the first goes all the way down
                        float downFactor = 0;
                        if (views.length > 2) {
                            downFactor = convertDpToPixel((_45F)
                                    * (views.length - 1) - 1);
                        } else {
                            downFactor = convertDpToPixel(_45F);
                        }

                        anim = ObjectAnimator.ofFloat(views[i],
                                NINE_OLD_TRANSLATION_Y, 0, downFactor);
                        anim.addListener(getAnimationListener(cardStack,
                                frameLayout, index, views[index]));

                    } else if (i == 1) {
                        // the second goes up just a bit

                        float upFactor = convertDpToPixel(-17f);
                        anim = ObjectAnimator.ofFloat(views[i],
                                NINE_OLD_TRANSLATION_Y, 0, upFactor);

                    } else {
                        // the rest go up by one card
                        float upFactor = convertDpToPixel(-1 * _45F);
                        anim = ObjectAnimator.ofFloat(views[i],
                                NINE_OLD_TRANSLATION_Y, 0, upFactor);
                    }

                    if (anim != null)
                        anim.start();

                }*/
            }

            public void onClickOtherCard(final CardStack cardStack,
                    final RelativeLayout frameLayout, final int index,
                    View[] views, int last) {
                // if clicked card is in middle
                /*for (int i = index; i <= last; i++) {
                    // run through the cards from the clicked position
                    // and on until the end
                    ObjectAnimator anim = null;

                    if (i == index) {
                        // the selected card goes all the way down
                        float downFactor = convertDpToPixel(_45F * (last - i)
                                + _12F);
                        anim = ObjectAnimator.ofFloat(views[i],
                                NINE_OLD_TRANSLATION_Y, 0, downFactor);
                        anim.addListener(getAnimationListener(cardStack,
                                frameLayout, index, views[index]));
                    } else {
                        // the rest go up by one
                        float upFactor = convertDpToPixel(_45F * -1);
                        anim = ObjectAnimator.ofFloat(views[i],
                                NINE_OLD_TRANSLATION_Y, 0, upFactor);
                    }

                    if (anim != null)
                        anim.start();
                }*/
            }
        };
    }

    protected float convertDpToPixel(float dp) {

        return Utils.convertDpToPixel(mContext, dp);
    }

    /*private AnimatorListener getAnimationListener(final CardStack cardStack,
            final RelativeLayout frameLayout, final int index,
            final View clickedCard) {
        return new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                if (index == 0) {

                    View newFirstCard = frameLayout.getChildAt(1);
                    handleFirstCard(newFirstCard);

                    // clickedCard.setBackgroundResource(com.fima.cardsui.R.drawable.);

                    // newFirstCard.setBackgroundResource(com.fima.cardsui.R.drawable.card_background);
                    // FrameLayout.LayoutParams lp = new
                    // FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    // FrameLayout.LayoutParams.WRAP_CONTENT);
                    //
                    // if (index > 0) {
                    // top = convertDpToPixelInt(8) + (convertDpToPixelInt(36f)
                    // * index);
                    // bottom = 24;
                    //
                    // }
                    //
                    // else if (0 == index) {
                    // top = 2 * convertDpToPixelInt(8) +
                    // convertDpToPixelInt(1);
                    // bottom = convertDpToPixelInt(12);
                    // }
                    //
                    // lp.setMargins(0, top, 0, bottom);
                    //
                    // clickedCard.setLayoutParams(lp);
                    // clickedCard.setPadding(0, convertDpToPixelInt(20), 0, 0);

                } else {
                    clickedCard
                            .setBackgroundResource(com.tassadar.multirommgr.R.drawable.card_background);
                }
                frameLayout.removeView(clickedCard);
                frameLayout.addView(clickedCard);

            }

            private void handleFirstCard(View newFirstCard) {
                newFirstCard
                        .setBackgroundResource(com.tassadar.multirommgr.R.drawable.card_background);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

                int top = 0;
                int bottom = 0;

                top = 2 * Utils.convertDpToPixelInt(mContext, 8)
                        + Utils.convertDpToPixelInt(mContext, 1);
                bottom = Utils.convertDpToPixelInt(mContext, 12);

                lp.setMargins(0, top, 0, bottom);
                newFirstCard.setLayoutParams(lp);
                newFirstCard.setPadding(0,
                        Utils.convertDpToPixelInt(mContext, 8), 0, 0);

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                Card card = cardStack.remove(index);
                cardStack.add(card);

                mAdapter.setItems(cardStack, cardStack.getPosition());

                // refresh();
                mAdapter.notifyDataSetChanged();
                Log.v("CardsUI", "Notify Adapter");

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // TODO Auto-generated method stub

            }
        };
    }*/

    public void setAdapter(StackAdapter stackAdapter) {
        mAdapter = stackAdapter;

    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public int getPosition() {
        return mPosition;
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        super.saveInstanceState(outState);
        for(int i = 0; i < cards.size(); ++i)
            cards.get(i).saveInstanceState(outState);
    }
}
