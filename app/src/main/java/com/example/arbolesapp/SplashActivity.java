package com.example.arbolesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoImage = findViewById(R.id.logoImage);
        TextView titleView = findViewById(R.id.tvTitle);

        Animation fadeInLogo = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation fadeOutLogo = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        Animation fadeInTitle = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation fadeOutTitle = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        fadeInLogo.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                logoImage.startAnimation(fadeOutLogo);
                titleView.startAnimation(fadeOutTitle);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        fadeOutLogo.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        logoImage.startAnimation(fadeInLogo);
        titleView.startAnimation(fadeInTitle);
    }
}