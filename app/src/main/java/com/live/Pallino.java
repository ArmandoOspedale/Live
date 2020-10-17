package com.live;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class Pallino extends View {

    Paint p;
    Animation anim;

    public Pallino(Context c) {
        super(c);
        p = new Paint();
        p.setColor(Color.TRANSPARENT);
        animazione();
    }

    public Pallino(Context c, AttributeSet a) {
        super(c, a);
        p = new Paint();
        p.setColor(Color.TRANSPARENT);
        animazione();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(getLeft() + ((getRight() - getLeft()) / 2), getTop() + ((getBottom() - getTop()) / 2), 10, p);
    }

    public void setColor (int c) {
        p.setColor(c);
        /*if (c == Color.GREEN) {
            setAnimation(anim);
        } else {
            clearAnimation();
        }*/
        invalidate();
    }

    private void animazione () {
        anim = new AlphaAnimation(0.2f, 1.0f);
        anim.setDuration(800);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
    }
}
