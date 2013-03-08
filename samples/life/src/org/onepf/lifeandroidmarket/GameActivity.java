package org.onepf.lifeandroidmarket;

import org.onepf.lifeandroidmarket.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class GameActivity extends Activity {
	LifeView lifeView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_game);
        lifeView = (LifeView) findViewById(R.id.life_view);
        
        final Button button = (Button) findViewById(R.id.start_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lifeView.setEditMode(!lifeView.getEditMode());
        		Button button = (Button)findViewById(R.id.start_button);
        		if (button!=null) {
        			button.setText(lifeView.getEditMode() ? R.string.start_button : R.string.edit_button);
        		}
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_game, menu);
        return true;
    }
    
	@Override
	public void onResume() {
		super.onResume();
		lifeView.resume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		lifeView.pause();
	}
    
}
