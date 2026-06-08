package com.example.netscope.ui.main;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.netscope.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Cargamos el Radar al iniciar la app
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new RadarFragment()).commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_radar) {
                selectedFragment = new RadarFragment();
            }
            // CUANDO TUS COMPAÑEROS HAGAN ESTOS FRAGMENTOS, QUITAS LOS COMENTARIOS
            /*
            else if (itemId == R.id.nav_tools) {
                selectedFragment = new ToolsFragment();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }
            */

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            }
            return true;
        });
    }
}