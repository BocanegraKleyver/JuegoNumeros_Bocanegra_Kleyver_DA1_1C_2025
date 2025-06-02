package com.example.juegodelosnumeros;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;




public class JuegoActivity extends AppCompatActivity {

    private TextView txtJugador, txtIntento, txtResultado;
    private EditText inputNumero;
    private Button btnVerificar;

    private String numeroSecreto;
    private int intentoActual = 1;
    private final int MAX_INTENTOS = 10;

    private SharedPreferences prefs;
    private Gson gson = new Gson();
    private ArrayList<Jugador> listaJugadores;
    private String nombreJugador;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego);

        // Vincular vistas
        txtJugador = findViewById(R.id.txtJugador);
        txtIntento = findViewById(R.id.txtIntento);
        txtResultado = findViewById(R.id.txtResultado);
        inputNumero = findViewById(R.id.inputNumero);
        btnVerificar = findViewById(R.id.btnVerificar);

        // Inicializar SharedPreferences y Gson
        prefs = getSharedPreferences("mis_prefs", MODE_PRIVATE);
        gson = new Gson();

        // Obtener datos del intent
        nombreJugador = getIntent().getStringExtra("nombreJugador");
        boolean permitirRepetidos = getIntent().getBooleanExtra("permitirRepetidos", false);

        // Mostrar nombre y cargar jugadores
        txtJugador.setText("Jugador: " + nombreJugador);
        listaJugadores = cargarJugadores();

        // Generar número secreto
        numeroSecreto = generarNumeroSecreto(permitirRepetidos);
        System.out.println("DEBUG - Número secreto: " + numeroSecreto);

        // Configurar botón
        btnVerificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String intento = inputNumero.getText().toString().trim();

                if (intento.length() != 4) {
                    Toast.makeText(JuegoActivity.this, "Ingresá 4 cifras", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!intento.matches("\\d{4}")) {
                    Toast.makeText(JuegoActivity.this, "Solo se permiten números", Toast.LENGTH_SHORT).show();
                    return;
                }

                String resultado = evaluarIntento(intento);
                txtResultado.setText("Resultado: " + resultado);

                if (resultado.equals("4B 0R 0M")) {
                    mostrarDialogoFinal(true);
                } else if (intentoActual == MAX_INTENTOS) {
                    mostrarDialogoFinal(false);
                } else {
                    intentoActual++;
                    txtIntento.setText("Intento " + intentoActual + " de " + MAX_INTENTOS);
                    inputNumero.setText("");
                }
            }
        });

        txtIntento.setText("Intento 1 de " + MAX_INTENTOS);
    }


    private String generarNumeroSecreto(boolean permitirRepetidos) {
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

    private String evaluarIntento(String intento) {
        int bien = 0, regular = 0, mal = 0;

        for (int i = 0; i < 4; i++) {
            char c = intento.charAt(i);
            if (c == numeroSecreto.charAt(i)) {
                bien++;
            } else if (numeroSecreto.contains(String.valueOf(c))) {
                regular++;
            } else {
                mal++;
            }
        }

        return bien + "B " + regular + "R " + mal + "M";
    }

    private void mostrarDialogoFinal(boolean gano) {
        String mensaje = gano ?
                "¡Felicitaciones! Adivinaste el número." :
                "Lo siento, el número era: " + numeroSecreto;

        // Buscar jugador actual y actualizar
        for (Jugador j : listaJugadores) {
            if (j.getNombre().equals(nombreJugador)) {
                if (gano) {
                    j.ganarPartida();
                } else {
                    j.perderPartida();
                }
                break;
            }
        }

        // Guardar ranking actualizado
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(listaJugadores);
        editor.putString("jugadores_guardados", json);
        editor.apply();

        // Mostrar mensaje final
        new AlertDialog.Builder(this)
                .setTitle(gano ? "Ganaste" : "Perdiste")
                .setMessage(mensaje)
                .setCancelable(false)
                .setPositiveButton("Volver al inicio", (dialog, which) -> {
                    Intent intent = new Intent(JuegoActivity.this, InicioActivity.class);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private ArrayList<Jugador> cargarJugadores() {
        String json = prefs.getString("jugadores_guardados", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Jugador>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }


}
