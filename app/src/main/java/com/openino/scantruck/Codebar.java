package com.openino.scantruck;

public class Codebar {
    String code;
    String name;
    int Quantite;

    public Codebar(String nam, String cod, int Quant){
        this.name = nam;
        this.code = cod;
        this.Quantite = Quant;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public void setQuantite(int quantite) {
        Quantite = quantite;
    }
    public int getQuantite() {
        return Quantite;
    }

}
