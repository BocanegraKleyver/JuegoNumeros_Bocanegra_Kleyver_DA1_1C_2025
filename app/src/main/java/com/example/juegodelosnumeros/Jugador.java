package com.example.juegodelosnumeros;


public class Jugador {
    private String nombre;
    private int partidasGanadas;
    private int partidasPerdidas;

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.partidasGanadas = 0;
        this.partidasPerdidas = 0;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPartidasGanadas() {
        return partidasGanadas;
    }

    public int getPartidasPerdidas() {
        return partidasPerdidas;
    }

    public void ganarPartida() {
        partidasGanadas++;
    }

    public void perderPartida() {
        partidasPerdidas++;
    }
}
