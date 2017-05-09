package DetiInteract.Mobile;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class HelpActivity extends Activity
{
	private String[] items= {"Incline o seu telemóvel para a esquerda ou para a direita para mudar de página. Inclinando para a esquerda selecciona a página à esquerda, e inclinando para a direita selecciona a página à direita.",
					 "Scroll: Deslize o dedo no ecrã do telemóvel para navegar na lista de docentes.",
					 "Fling: Deslize o dedo no ecrã do telemóvel para navegar na lista de docentes ou para alterar o horário apresentando.",
					 "Press: Faça um toque prolongado no ecrã para iniciar interacção com o Google Earth ou Visualizador 3D."};
	private ArrayAdapter<String> HelpItems;
	private boolean canExit = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		//setup the window
		setContentView(R.layout.help_dialog);
		
		ListView HelpListView = (ListView) findViewById(R.id.help_list);
		HelpListView.setAdapter(new IconAdapter());
		
		setResult(RESULT_OK);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
			canExit = true;
		
		return super.onKeyDown(keyCode, event);
	}

	class IconAdapter extends ArrayAdapter
	{
		IconAdapter() {
			super(HelpActivity.this, R.layout.row, items);
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row=inflater.inflate(R.layout.row, parent, false); 
		      TextView label=(TextView)row.findViewById(R.id.label); 
		 
		      label.setText(items[position]); 
		 
		      ImageView icon=(ImageView)row.findViewById(R.id.icon); 
		 
		      switch (position) {
		      case 0:
		    	  icon.setImageResource(R.drawable.tilt);
		    	  break;
		      case 1:
		    	  icon.setImageResource(R.drawable.scroll);
		    	  break;
		      case 2:
		    	  icon.setImageResource(R.drawable.fling);
		    	  break;
		      case 3:
		    	  icon.setImageResource(R.drawable.press);
		    	  break;
		      }
		      
		      return(row);
		}
	}
}
