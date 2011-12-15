package de.matdue.isk;

import de.matdue.isk.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class StartActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start);

		Button pilotsButton = (Button) findViewById(R.id.pilots);
		pilotsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(StartActivity.this,
						PilotsActivity.class));
			}

		});
	}

}
