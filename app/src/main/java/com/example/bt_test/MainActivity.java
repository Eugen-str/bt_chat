package com.example.bt_test;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    // UUID mora biti isti na clientu i na serveru
    String UUID = "b4f80654-4f28-4b71-bf2d-0fa5e7d065c6";
    String btAktivan = "Bluetooth aktiviran.";
    String btNeaktivan = "Bluetooth nije aktiviran.";
    TextView tvBtAktiviran, tvUpareni, tvDebug;
    Button btnStart, btnPostaviIme, btnUpareni, btnServer, btnCli, btnOdaberiServer;
    ImageButton btnSend;
    EditText etMsgBox, etUnosIme;
    BluetoothAdapter bluetoothAdapter;
    LinearLayout layoutPoruke;

    int boja_moje_poruke = Color.argb(255, 230, 242, 255);
    int boja_dobivene_poruke = Color.argb(255, 255, 230, 255);
    int boja_server_poruke = Color.argb(255, 255, 204, 204);
    //Privremena BluetoothDevice varijabla za moj mobitel, za provjeravanje rada aplikacije
    BluetoothDevice odabraniServer;

    int BLUETOOTH_ON_REQUEST = 1;
    int BLUETOOTH_VISIBLE_REQUEST = 2;
    //int PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 21;
    //int PERMISSIONS_REQUEST_BLUETOOTH_SCAN = 22;
    String TAG = "aplikacija_log";
    final String[] ime = {""};


    btClient klijent;
    btServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);


        btnStart = findViewById(R.id.btnStart);
        btnPostaviIme = findViewById(R.id.btnPostaviIme);
        btnUpareni = findViewById(R.id.btnPrintUpareno);
        btnServer = findViewById(R.id.btnServer);
        btnCli = findViewById(R.id.btnClient);
        btnSend = findViewById(R.id.btnPosalji);
        btnOdaberiServer = findViewById(R.id.btnOdaberiServer);

        etMsgBox = findViewById(R.id.msgBox);

        tvBtAktiviran = findViewById(R.id.textView);
        tvUpareni = findViewById(R.id.tvUpareno);
        tvUpareni.setText("");
        tvDebug = findViewById(R.id.tvDebug);

        layoutPoruke = findViewById(R.id.layoutPoruke);
        //tvPoruke = findViewById(R.id.tvPoruke);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Uređaj ne podržava BlueTooth.", Toast.LENGTH_SHORT).show();
        }

        if (bluetoothAdapter.isEnabled()) {
            tvBtAktiviran.setText(btAktivan);
        } else {
            tvBtAktiviran.setText(btNeaktivan);
        }

        btnStart.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE);
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN);
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, BLUETOOTH_ON_REQUEST);
                }
            }
        });

        btnPostaviIme.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                View imeView = getLayoutInflater().inflate(R.layout.layout_unos_ime, null);

                etUnosIme = imeView.findViewById(R.id.etKorisnikIme);

                builder.setTitle("Unesi korisničko ime")
                        .setPositiveButton("Odaberi", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ime[0] = etUnosIme.getText().toString();
                                Toast.makeText(MainActivity.this, "Ime: " + ime[0], Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setView(imeView);

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        btnOdaberiServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int duljina = bluetoothAdapter.getBondedDevices().toArray().length;

                final String[] odabrano = {new String()};
                final int[] odabrano_index = new int[1];

                String []odabiri = new String[duljina];
                BluetoothDevice []uredaji_niz = new BluetoothDevice[duljina];

                try {
                    Set<BluetoothDevice> uredaji = bluetoothAdapter.getBondedDevices();
                    int i = 0;
                    for(BluetoothDevice uredaj : uredaji){
                        odabiri[i] = uredaj.getName();
                        uredaji_niz[i] = uredaj;
                        i++;
                    }
                } catch (Exception e){
                    Log.i(TAG, "ERROR: " + e);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Odaberi uređaji na koji se spajaš")
                        .setSingleChoiceItems(odabiri, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                odabrano[0] = odabiri[i];
                                odabrano_index[0] = i;
                            }
                        })
                        .setPositiveButton("Odaberi", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // TODO: postavljanje servera na odabrani uređaj --- NEISPROBANO ---
                                Toast.makeText(MainActivity.this, "Odabrani uređaj: " + odabrano[0], Toast.LENGTH_SHORT).show();
                                odabraniServer = uredaji_niz[odabrano_index[0]];
                            }
                        });

                try {
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }catch (Exception e){
                    Log.i(TAG, "ERROR: alert dialog:" + e);
                }
            }
        });

        btnUpareni.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tvUpareni.getVisibility() == View.VISIBLE) {
                    tvUpareni.setVisibility(View.INVISIBLE);
                } else {
                    if (bluetoothAdapter.isEnabled()) {
                        tvUpareni.setVisibility(View.VISIBLE);
                        if (!provjeraBluetoothDostupno()) {
                            return;
                        }
                        tvUpareni.setText("Upareni uređaji:");
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        Set<BluetoothDevice> uredaji = bluetoothAdapter.getBondedDevices();

                        for (BluetoothDevice uredaj : uredaji) {
                            if (Objects.equals(uredaj.getName(), "A54 korisnika Eugen")) {
                                // TODO: ako ne radi odabraniServer - debug opcija
                                //odabraniServer = uredaj;
                                tvDebug.setText("IP servera: " + odabraniServer.getAddress());
                            }
                            tvUpareni.append("\nUredaj " + uredaj.getName() + ", " + uredaj);
                        }
                    }
                }
            }
        });

        btnServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(server != null){
                    Toast.makeText(MainActivity.this, "Server već napravljen.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(klijent != null){
                    Toast.makeText(MainActivity.this, "Klijent već napravljan. Nemoguće napraviti server.", Toast.LENGTH_SHORT).show();
                }
                try{
                    server = new btServer();
                    server.start();

                    btnSend.setVisibility(View.VISIBLE);
                    etMsgBox.setVisibility(View.VISIBLE);

                    btnServer.setVisibility(View.INVISIBLE);
                    btnCli.setVisibility(View.INVISIBLE);
                    btnOdaberiServer.setVisibility(View.INVISIBLE);
                    btnUpareni.setVisibility(View.INVISIBLE);
                    tvBtAktiviran.setVisibility(View.INVISIBLE);
                    tvUpareni.setVisibility(View.INVISIBLE);

                    btnStart.setVisibility(View.INVISIBLE);
                    btnPostaviIme.setVisibility(View.INVISIBLE);
                } catch (Exception e){
                    Log.i(TAG, "ERROR: btnServer: " + e);
                }
            }
        });

        btnCli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(klijent != null){
                    Toast.makeText(MainActivity.this, "Klijent već napravljen", Toast.LENGTH_SHORT).show();
                }
                if(server != null){
                    Toast.makeText(MainActivity.this, "Server već napravljan. Nemoguće napraviti klijent.", Toast.LENGTH_SHORT).show();
                }
                if(provjeriBluetoothDozvole()){
                    try {
                        klijent = new btClient(odabraniServer, ime[0]);
                        klijent.start();

                        btnSend.setVisibility(View.VISIBLE);
                        etMsgBox.setVisibility(View.VISIBLE);

                        btnServer.setVisibility(View.INVISIBLE);
                        btnCli.setVisibility(View.INVISIBLE);
                        btnOdaberiServer.setVisibility(View.INVISIBLE);
                        btnUpareni.setVisibility(View.INVISIBLE);
                        tvBtAktiviran.setVisibility(View.INVISIBLE);
                        tvUpareni.setVisibility(View.INVISIBLE);

                        btnStart.setVisibility(View.INVISIBLE);
                        btnPostaviIme.setVisibility(View.INVISIBLE);
                    } catch (Exception e){
                        Log.i(TAG, "ERROR: btnCli: " + e);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Uređaj nema bluetooth dozvole potrebne za spajanje", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(klijent != null){
                    try{
                        String poruka = etMsgBox.getText().toString();
                        klijent.posaljiPorukuServeru(poruka, false);
                        etMsgBox.setText("");
                    } catch (Exception e) {
                        Log.i(TAG, "ERROR: btnSend: " + e);
                    }
                }
                if(server != null){
                    try{
                        String poruka = etMsgBox.getText().toString();
                        server.posalji_svima(poruka);
                        etMsgBox.setText("");
                    } catch (Exception e){
                        Log.i(TAG, "btnSend: server: " + e);
                    }
                }
            }
        });
    }

    // ---- Server -----  nasljeđuje Thread klasu, za radnju sa više konekcija istovremeno.
    class btServer extends Thread {
        private BluetoothServerSocket serverSocket;

        public ArrayList<BluetoothSocket>spojeni_klijenti = new ArrayList<BluetoothSocket>();

        public btServer() {
            BluetoothServerSocket tmp = null;
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("bt_test", java.util.UUID.fromString(UUID));
            } catch (IOException e) {
                Log.i(TAG, "listen() greška", e);
            }
            Log.i(TAG, "upješno napravljen server");
            serverSocket = tmp;
        }

        // Override na run() u Thread
        public void run() {
            BluetoothSocket socket;
            threadTV(tvDebug, "server radi");

            while (true) {
                try {
                    //Toast.makeText(MainActivity.this, "primanje konekcija...", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "server prima uređaje...");
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "greška", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "accept() greška", e);
                    break;
                }

                // Uspostavljena konekcija
                if (socket != null) {
                    Log.i(TAG, "server upravlja konekcijom: " + socket);
                    spojeni_klijenti.add(socket);
                    upravljajKonekcijomServer(socket);

                    /*
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }*/
                    break;
                }
            }
        }

        public void posalji_svima(String poruka){
            // TODO: -- prikaz poruke ? -- mislim da ovo ide ovdje -- nisam siguran
            prikazi_poruku("Ti: " + poruka, boja_moje_poruke);

            for(BluetoothSocket socket : this.spojeni_klijenti){
                try{
                    OutputStream outputStream = socket.getOutputStream();
                    Log.i(TAG, "output stream = " + outputStream.toString());

                    // slanje poruke
                    outputStream.write((ime[0] + ": " + poruka).getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();

                    // log
                    Log.i(TAG, "Poruka poslana serveru: " + poruka);
                }
                catch (IOException e){
                    Log.i(TAG, "ERROR: nije moguce poslati poruku: " + e);
                    //throw new RuntimeException(e);
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    //za mijenjanje TextView stringa unutar threada
    private void threadTV(final TextView textView, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    // funkcija za prikazivanje poruke
    // mora biti u runOnUiThread jer
    public void prikazi_poruku(String poruka, int boja_poruke){
        try{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView poruka_ui = new TextView(MainActivity.this);
                    poruka_ui.setTextSize(30);
                    poruka_ui.setBackgroundColor(boja_poruke);
                    poruka_ui.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    poruka_ui.setPadding(5, 2, 5, 2);
                    poruka_ui.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.tipka));
                    poruka_ui.setText(poruka);

                    layoutPoruke.addView(poruka_ui);
                }
            });
        } catch (Exception e){
            Log.i(TAG, "ERROR: prikazi_poruku " + e);
        }
    }

    // ----- Client ----- Uređaj koji se spaja na server
    class btClient extends Thread {
        private BluetoothSocket btSocket;
        private final BluetoothDevice btUredaj;
        public String ime;
        public btClient(BluetoothDevice uredaj, String ime) {
            BluetoothSocket tmp = null;
            btUredaj = uredaj;
            this.ime = ime;

            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Ne radi!", Toast.LENGTH_SHORT).show();
                    return;
                }
                tmp = btUredaj.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));
            } catch (IOException e) {
                Log.e(TAG, "Greška u btClient konstruktoru", e);
            }

            btSocket = tmp;
        }

        public void run() {
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Ne radi!", Toast.LENGTH_SHORT).show();
                    return;
                }
                btSocket.connect();
                threadTV(tvDebug, "konekcija uspješna");
            } catch (IOException connectException) {
                try {
                    btSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Nije bilo moguće zatvoriti konekciju", closeException);
                }
                return;
            }

            // zasad radi
            this.posaljiPorukuServeru("Korisnik " + ime + " se spojio u razgovor", true);

            // primanje poruke od servera do klijenta
            upravljajKonekcijomClient(btSocket);
        }

        public void posaljiPorukuServeru(String poruka, boolean server){
            try{
                OutputStream outputStream = btSocket.getOutputStream();
                Log.i(TAG, "output stream = " + outputStream.toString());

                // slanje poruke
                outputStream.write((ime + ": " + poruka).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                // prikaz poruke
                if(server){
                    prikazi_poruku(poruka, boja_server_poruke);
                }else{
                    prikazi_poruku(poruka, boja_moje_poruke);
                }

                // log
                Log.i(TAG, "Poruka poslana serveru: " + poruka);
            }
            catch (IOException e){
                Log.i(TAG, "ERROR: nije moguce poslati poruku: " + e);
                throw new RuntimeException(e);
            }
        }

        // Zatvara client i thread
        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Nije bilo moguće zatvoriti konekciju", e);
            }
        }
    }

    private boolean provjeriBluetoothDozvole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(
                    MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        else{
            Toast.makeText(this, "Uređaj ne dozvoljava Bluetooth.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    void upravljajKonekcijomServer(BluetoothSocket s) {
        threadTV(tvDebug, "upravljajKonekcijomServer: " + s.toString());
        InputStream is = null;

        try {
            is = s.getInputStream();
        } catch (IOException e) {
            Log.i(TAG,"ERROR: nije moguce dobiti inputStream: "+ e);
        }


        byte []msgBuffer = new byte[1024];
        int numBytes;

        while(true){
            try{
                numBytes = is.read(msgBuffer);

                String porukaDek = new String(msgBuffer, StandardCharsets.UTF_8).substring(0, numBytes);

                Log.i(TAG, "numBytes = " + numBytes);
                Log.i(TAG, "PORUKA: " + porukaDek);

                prikazi_poruku(porukaDek, boja_dobivene_poruke);
                threadTV(tvDebug, "PORUKA:"+ porukaDek);
            } catch(IOException e){
                // ništa nije dobiveno, ne želimo ugasiti apliaciju nego čekati da dođe poruka
            }
        }
    }

    void upravljajKonekcijomClient(BluetoothSocket s) {
        threadTV(tvDebug, s.toString());
        InputStream is = null;

        try {
            is = s.getInputStream();
        } catch (IOException e) {
            Log.i(TAG,"ERROR: nije moguce dobiti inputStream: "+ e);
        }

        byte []msgBuffer = new byte[1024];
        int numBytes;

        while(true){
            try{
                numBytes = is.read(msgBuffer);

                String porukaDek = new String(msgBuffer, StandardCharsets.UTF_8).substring(0, numBytes);

                Log.i(TAG, "numBytes = " + numBytes);
                Log.i(TAG, "PORUKA: " + porukaDek);

                prikazi_poruku(porukaDek, boja_dobivene_poruke);
                threadTV(tvDebug, "PORUKA:"+ porukaDek);
            } catch(IOException e){
                // ništa nije dobiveno, ne želimo ugasiti apliaciju nego čekati da dođe poruka
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ON_REQUEST) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, btAktivan, Toast.LENGTH_SHORT).show();
                tvBtAktiviran.setText(btAktivan);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Bluetooth nije aktiviran", Toast.LENGTH_SHORT).show();
            }
        }

        else if (resultCode == BLUETOOTH_VISIBLE_REQUEST){
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Uređaj je sada vidljiv drugim uređajima", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Greška", Toast.LENGTH_SHORT).show();
            }
        }
    }

    boolean provjeraBluetoothDostupno(){
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return false;
            }
        }
        return true;
    }


    public ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "NIL", Toast.LENGTH_SHORT).show();
                }
            });
}