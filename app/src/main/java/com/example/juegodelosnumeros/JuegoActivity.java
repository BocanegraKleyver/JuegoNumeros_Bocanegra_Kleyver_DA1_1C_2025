package com.example.juegodelosnumeros;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

    private Button btnVolverMenu;
    private Button btnReiniciar;
    private Button btnFinalizar;

    private String numeroSecreto;
    private int intentoActual = 1;
    private final int MAX_INTENTOS = 10;

    private SharedPreferences prefs;
    private Gson gson = new Gson();
    private ArrayList<Jugador> listaJugadores;
    private String nombreJugador;

    private LinearLayout layoutHistorial;
    private ArrayList<String> historialIntentos = new ArrayList<>();

    private Button btnAyuda;
    private int ayudasRestantes = 3;


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
        btnAyuda = findViewById(R.id.btnAyuda);
        layoutHistorial = findViewById(R.id.layoutHistorial);
        btnVolverMenu = findViewById(R.id.btnVolverMenu);
        btnReiniciar = findViewById(R.id.btnReiniciar);
        btnFinalizar = findViewById(R.id.btnFinalizar);


        // Inicializar preferencias y Gson
        prefs = getSharedPreferences("mis_prefs", MODE_PRIVATE);
        gson = new Gson();
        listaJugadores = cargarJugadores();

        boolean continuar = getIntent().getBooleanExtra("continuar", false);

        if (continuar) {
            // Cargar partida guardada
            nombreJugador = prefs.getString("juego_jugador", "Jugador");
            numeroSecreto = prefs.getString("juego_numero", "");
            intentoActual = prefs.getInt("juego_intento", 1);

            String jsonHistorial = prefs.getString("juego_historial", null);
            if (jsonHistorial != null) {
                Type type = new TypeToken<ArrayList<String>>() {}.getType();
                historialIntentos = gson.fromJson(jsonHistorial, type);

                for (String entrada : historialIntentos) {
                    agregarEntradaHistorial(entrada);
                }
            }

        } else {
            // Nueva partida
            nombreJugador = getIntent().getStringExtra("nombreJugador");
            boolean permitirRepetidos = getIntent().getBooleanExtra("permitirRepetidos", false);
            numeroSecreto = generarNumeroSecreto(permitirRepetidos);
            intentoActual = 1;

            // Guardar datos iniciales
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("juego_jugador", nombreJugador);
            editor.putString("juego_numero", numeroSecreto);
            editor.putInt("juego_intento", intentoActual);
            editor.putBoolean("juego_enCurso", true);
            editor.apply();
        }

        txtJugador.setText("Jugador: " + nombreJugador);
        txtIntento.setText("Intento " + intentoActual + " de " + MAX_INTENTOS);
        System.out.println("DEBUG - Número secreto: " + numeroSecreto);


        btnVerificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String intento = inputNumero.getText().toString().trim();

                if (intento.length() != 4 || !intento.matches("\\d{4}")) {
                    Toast.makeText(JuegoActivity.this, "Ingresá 4 cifras válidas", Toast.LENGTH_SHORT).show();
                    return;
                }

                String resultado = evaluarIntento(intento);
                txtResultado.setText("Resultado: " + resultado);

                // 📋 Guardar intento en historial
                String entrada = "Intento " + intentoActual + ": " + intento + " → " + resultado;
                historialIntentos.add(entrada);
                agregarEntradaHistorial(entrada);

                if (resultado.equals("4B 0R 0M")) {
                    mostrarDialogoFinal(true);
                } else if (intentoActual == MAX_INTENTOS) {
                    mostrarDialogoFinal(false);
                } else {
                    intentoActual++;
                    txtIntento.setText("Intento " + intentoActual + " de " + MAX_INTENTOS);
                    inputNumero.setText("");

                    // Guardar avance
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("juego_intento", intentoActual);

                    // 💾 También guardamos historial
                    String jsonHistorial = gson.toJson(historialIntentos);
                    editor.putString("juego_historial", jsonHistorial);

                    editor.apply();
                }
            }
        });

        btnVolverMenu.setOnClickListener(v -> {
            finish(); // vuelve atrás, al menú
        });

        btnReiniciar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reiniciar partida")
                    .setMessage("¿Querés reiniciar la partida actual desde cero?")
                    .setPositiveButton("Sí, reiniciar", (dialog, which) -> {
                        Intent intent = new Intent(JuegoActivity.this, JuegoActivity.class);
                        intent.putExtra("nombreJugador", nombreJugador);
                        boolean permitirRepetidos = prefs.getBoolean("juego_repetidos", false);
                        intent.putExtra("permitirRepetidos", permitirRepetidos);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnFinalizar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Finalizar partida")
                    .setMessage("¿Querés finalizar esta partida?")
                    .setPositiveButton("Sí", (dialog, which) -> mostrarDialogoFinal(false))
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnAyuda.setOnClickListener(v -> {
            if (ayudasRestantes <= 0) {
                Toast.makeText(this, "Ya usaste las 3 ayudas", Toast.LENGTH_SHORT).show();
                return;
            }

            String intento = inputNumero.getText().toString().trim();

            if (intento.length() != 4 || !intento.matches("\\d{4}")) {
                Toast.makeText(this, "Ingresá un número válido antes de pedir ayuda", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("🆘 Ayuda")
                    .setMessage("¿Querés usar una ayuda? Solo podés usarla 3 veces por partida.")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        StringBuilder pistas = new StringBuilder();
                        for (int i = 0; i < 4; i++) {
                            if (intento.charAt(i) == numeroSecreto.charAt(i)) {
                                pistas.append("✔️ La cifra en la posición ").append(i + 1).append(" es correcta.\n");
                            } else {
                                pistas.append("❌ La cifra en la posición ").append(i + 1).append(" es incorrecta.\n");
                            }
                        }

                        ayudasRestantes--;

                        new AlertDialog.Builder(this)
                                .setTitle("Resultado de la Ayuda")
                                .setMessage(pistas.toString() + "\nAyudas restantes: " + ayudasRestantes)
                                .setPositiveButton("OK", null)
                                .show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });


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

        SharedPreferences.Editor clear = prefs.edit();
        clear.remove("juego_jugador");
        clear.remove("juego_numero");
        clear.remove("juego_intento");
        clear.putBoolean("juego_enCurso", false);
        clear.apply();


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
                .setNegativeButton("Jugar otra vez", (dialog, which) -> {
                    Intent intent = new Intent(JuegoActivity.this, JuegoActivity.class);
                    intent.putExtra("nombreJugador", nombreJugador);

                    boolean permitirRepetidos = prefs.getBoolean("juego_repetidos", false);
                    intent.putExtra("permitirRepetidos", permitirRepetidos);


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


    private void agregarEntradaHistorial(String texto) {
        TextView tv = new TextView(this);
        tv.setTextSize(16); // 🔠 Tamaño un poco más grande

        // 🎨 Preparar texto con colores por letra
        SpannableString spannable = new SpannableString(texto);

        int verde = getResources().getColor(android.R.color.holo_green_dark);
        int naranja = getResources().getColor(android.R.color.holo_orange_dark);
        int rojo = getResources().getColor(android.R.color.holo_red_dark);

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == 'B') {
                spannable.setSpan(new ForegroundColorSpan(verde), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (c == 'R') {
                spannable.setSpan(new ForegroundColorSpan(naranja), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (c == 'M') {
                spannable.setSpan(new ForegroundColorSpan(rojo), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        tv.setText(spannable);
        layoutHistorial.addView(tv);
    }




}
