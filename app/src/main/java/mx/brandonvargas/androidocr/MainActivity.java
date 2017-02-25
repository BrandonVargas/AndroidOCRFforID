package mx.brandonvargas.androidocr;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import mx.brandonvargas.ocrforid.OcrIdActivity;

public class MainActivity extends AppCompatActivity {

    private final int OCR_REQUEST = 1;
    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button)findViewById(R.id.button);
        textView = (TextView)findViewById(R.id.tv_result);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, OcrIdActivity.class),OCR_REQUEST);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle extras = data.getExtras();
        if(resultCode == Activity.RESULT_OK && requestCode == OCR_REQUEST && extras!=null){
            Boolean is_ine = extras.getBoolean("IS_INE");
            String name = extras.getString("NAME");
            String lastName = extras.getString("LAST_NAME");
            String motherLastName = extras.getString("M_LAST_NAME");
            String curp = extras.getString("CURP");
            String address = extras.getString("ADDRESS");
            String elector = extras.getString("ELECTOR");
            String state = extras.getString("STATE");
            String town = extras.getString("TOWN");
            String section = extras.getString("SECTION");
            String id = extras.getString("ID");
            Uri uri1 = Uri.parse(extras.getString("URI1"));
            Uri uri2 = Uri.parse(extras.getString("URI2"));
            String showResult = "Nombre: "+name+
                    "\nApellido Paterno: "+lastName+
                    "\nApellido Materno: "+motherLastName+
                    "\nCurp: "+curp+
                    "\nDirección: "+address+
                    "\nClave de elector: "+elector+
                    "\nEstado: "+state+
                    "\nMunicipio: "+town+
                    "\nSección: "+section+
                    "\nOCR: "+id;
            showResult(showResult);
        }
    }

    private void showResult(String showResult) {
        textView.setText(showResult);
    }
}
