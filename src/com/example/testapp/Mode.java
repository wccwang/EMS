package com.example.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Mode extends Activity {
	private ListView lView;
	private String lv_items[] = { "Fire", "Flood", "Blizzard"};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.mode);
		lView = (ListView) findViewById(R.id.ListView01);
		
		//Set option as Single Choice
		lView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, lv_items));
		lView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		lView.setOnItemClickListener(choose);
	}
	private OnItemClickListener choose = new OnItemClickListener(){
		public void onItemClick(AdapterView<?> parent, View view, int position, long id){
			Intent returnIntent = new Intent();
			returnIntent.putExtra("result", ((TextView) view).getText().toString());
			setResult(RESULT_OK, returnIntent);     
			finish();
		}
	};
}
