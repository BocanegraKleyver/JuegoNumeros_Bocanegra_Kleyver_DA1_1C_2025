package com.example.juegodelosnumeros;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.content.Intent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Random;

public class InicioActivity extends AppCompatActivity {

    private Spinner spinnerJugadores;
    private Button btnNuevoJugador, btnNuevaPartida, btnContinuarPartida;

    private ArrayList<Jugador> listaJugadores = new ArrayList<>();
    private ArrayAdapter<String> adapterNombres;
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    private static final String PREF_JUGADORES = "jugadores_guardados";
    private Switch switchRepetidos;

    private TextView txtEstadisticas;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        spinnerJugadores = findViewById(R.id.spinnerJugadores);
        btnNuevoJugador = findViewById(R.id.btnNuevoJugador);
        btnNuevaPartida = findViewById(R.id.btnNuevaPartida);
        btnContinuarPartida = findViewById(R.id.btnContinuarPartida);
        switchRepetidos = findViewById(R.id.switchRepetidos);
        txtEstadisticas = findViewById(R.id.txtEstadisticas);


        prefs = getSharedPreferences("mis_prefs", MODE_PRIVATE);

        cargarJugadores();
        configurarSpinner();


        boolean hayPartida = prefs.getBoolean("juego_enCurso", false);
        btnContinuarPartida.setEnabled(hayPartida);


        if (!listaJugadores.isEmpty()) {
            spinnerJugadores.setSelection(0); // fuerza el evento de selección
        }

        btnNuevoJugador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoNuevoJugador();
            }
        });

        btnNuevaPartida.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = spinnerJugadores.getSelectedItemPosition();
                if (index >= 0 && index < listaJugadores.size()) {
                    Jugador jugadorSeleccionado = listaJugadores.get(index);

                    // Más adelante le pasamos datos por Intent
                    Intent intent = new Intent(InicioActivity.this, JuegoActivity.class);
                    intent.putExtra("nombreJugador", jugadorSeleccionado.getNombre());
                    intent.putExtra("permitirRepetidos", switchRepetidos.isChecked());

                    // Guardar estado inicial de la partida
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("juego_jugador", jugadorSeleccionado.getNombre());
                    editor.putString("juego_numero", generarNumeroSecretoInternamente(switchRepetidos.isChecked()));
                    editor.putInt("juego_intento", 1);
                    editor.putBoolean("juego_enCurso", true);
                    editor.putBoolean("juego_repetidos", switchRepetidos.isChecked()); // ✅ Guardamos la elección del switch
                    editor.apply();


                    startActivity(intent);

                } else {
                    Toast.makeText(InicioActivity.this, "Seleccioná un jugador", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnContinuarPartida.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InicioActivity.this, JuegoActivity.class);
                intent.putExtra("continuar", true); // importante para saber que es modo continuación
                startActivity(intent);
            }
        });


    }

    private void cargarJugadores() {
        String json = prefs.getString(PREF_JUGADORES, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Jugador>>() {}.getType();
            listaJugadores = gson.fromJson(json, type);
        }
    }

    private void guardarJugadores() {
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(listaJugadores);
        editor.putString(PREF_JUGADORES, json);
        editor.apply();
    }

    private void configurarSpinner() {
        ArrayList<String> nombres = new ArrayList<>();
        for (Jugador j : listaJugadores) {
            nombres.add(j.getNombre());
        }

        adapterNombres = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
        adapterNombres.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerJugadores.setAdapter(adapterNombres);

        // Listener para mostrar estadísticas del jugador
        spinnerJugadores.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < listaJugadores.size()) {
                    Jugador jugador = listaJugadores.get(position);

                    String resumen = "Ganadas: " + jugador.getPartidasGanadas() +
                            " | Perdidas: " + jugador.getPartidasPerdidas();

                    String jugadorActivo = prefs.getString("juego_jugador", null);
                    boolean enCurso = prefs.getBoolean("juego_enCurso", false);

                    if (enCurso && jugador.getNombre().equals(jugadorActivo)) {
                        resumen += "\n📌 Partida activa";
                    }

                    txtEstadisticas.setText(resumen);


                } else {
                    txtEstadisticas.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                txtEstadisticas.setText("");
            }
        });

        btnNuevaPartida.setEnabled(!listaJugadores.isEmpty());
        btnContinuarPartida.setEnabled(!listaJugadores.isEmpty());
    }


    private void mostrarDialogoNuevoJugador() {
        if (listaJugadores.size() >= 3) {
            Toast.makeText(this, "Máximo de 3 jugadores alcanzado", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nuevo Jugador");
        builder.setMessage("Ingrese el nombre del jugador:");
        builder.setView(input);

        builder.setPositiveButton("Crear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nombre = input.getText().toString().trim();
                if (!nombre.isEmpty()) {
                    for (Jugador j : listaJugadores) {
                        if (j.getNombre().equalsIgnoreCase(nombre)) {
                            Toast.makeText(InicioActivity.this, "Ya existe un jugador con ese nombre", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    listaJugadores.add(new Jugador(nombre));
                    guardarJugadores();
                    configurarSpinner();
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private String generarNumeroSecretoInternamente(boolean permitirRepetidos) {
        Random rand = new Random();
        ArrayList<Integer> digitos = new ArrayList<>();

        while (digitos.size() < 4) {
            int n = rand.nextInt(10);
            if (permitirRepetidos || !digitos.contains(n)) {
                digitos.add(n);
            }
        }

        StringBuilder numero = new StringBuilder();
        for (int d : digitos) {
            numero.append(d);
        }

        return numero.toString();
    }


}
