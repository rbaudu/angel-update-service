package com.angel.update.model;

import lombok.Data;

/**
 * Classe utilitaire pour représenter les métadonnées d'un pays
 * Facilite la manipulation des données JSONB du champ metadata
 */
@Data
public class CountryMetadata {
    
    private Integer population;
    private String currency;
    private String capital;
    private String phoneCode;
    private String flagUrl;
    private String callingCode;
    private Double area; // en km²
    private String[] borders; // codes des pays frontaliers
    private String region; // continent détaillé
    private String subregion;
    private String[] tld; // top level domains
    
    /**
     * Constructeur par défaut
     */
    public CountryMetadata() {}
    
    /**
     * Constructeur avec les données essentielles
     */
    public CountryMetadata(Integer population, String currency, String capital, String phoneCode) {
        this.population = population;
        this.currency = currency;
        this.capital = capital;
        this.phoneCode = phoneCode;
    }
}