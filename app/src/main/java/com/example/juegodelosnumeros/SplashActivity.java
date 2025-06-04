package com.example.juegodelosnumeros;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashLayout = findViewById(R.id.splashLayout);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashLayout.startAnimation(fadeIn);



        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, InicioActivity.class));
                finish();
            }
        }, 4500);
    }
}
