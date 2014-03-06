package org.onepf.store;

import org.onepf.store.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class StoreActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView textInformation = (TextView) findViewById(R.id.textInformation);
        
        
    }
}
