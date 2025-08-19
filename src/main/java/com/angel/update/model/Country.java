package com.angel.update.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant un pays
 */
@Entity
@Table(name = "countries")
@Data
@EqualsAndHashCode(exclude = "regions")
@ToString(exclude = "regions")
public class Country {
    
    @Id
    @Column(length = 2)
    private String code; // ISO 3166-1 alpha-2
    
    @Column(nullable = false)
    private String name;
    
    private String nativeName;
    
    @Column(nullable = false)
    private String languageCode;
    
    private String timezone;
    
    private String currency;
    
    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Region> regions = new ArrayList<>();
    
    private String capital;
    
    private String continent;
    
    private Integer population;
    
    private Boolean active = true;
    
    @Column(name = "phone_code")
    private String phoneCode;
    
    @ElementCollection
    @CollectionTable(name = "country_languages")
    private List<String> officialLanguages = new ArrayList<>();
}
