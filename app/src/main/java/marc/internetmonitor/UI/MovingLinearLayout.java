package marc.internetmonitor.UI;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * Created by gilbertm on 29/09/2015.
 */
public class MovingLinearLayout extends LinearLayout {


    public MovingLinearLayout(Context context) {
        super(context);
    }

    public MovingLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    // USED BY THE objectAnimator
    public void setXFraction( float xFraction ){

        setX( getWidth()*xFraction );
    }

    // USED BY THE objectAnimator
    public void setYFraction( float yFraction ){

        setY( getHeight()*yFraction );
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        super.onInterceptTouchEvent(ev);

        return false;
    }



}
