package com.example.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Submit extends Activity {
	private Button back_btn;
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.submit);
		
		back_btn = (Button)findViewById(R.id.start_new);
		back_btn.setOnClickListener(back);
	}
	
	private OnClickListener back = new OnClickListener(){
		public void onClick(View v){
			Intent returnIntent = new Intent();
			setResult(RESULT_OK, returnIntent);     
			finish();
		}
	};
}
