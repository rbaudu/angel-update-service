package com.angel.update.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entité représentant une région
 */
@Entity
@Table(name = "regions")
@Data
@EqualsAndHashCode(exclude = {"country", "contents"})
@ToString(exclude = {"country", "contents"})
public class Region {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code; // ex: "FR-IDF"
    
    @Column(nullable = false)
    private String languageCode; // ex: "fr"
    
    @Column(nullable = false)
    private String countryCode; // ex: "FR"
    
    @Column(nullable = false)
    private String regionCode; // ex: "IDF"
    
    @Column(nullable = false)
    private String name;
    
    private String nativeName;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;
    
    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL)
    private List<Content> contents = new ArrayList<>();
    
    private String timezone;
    
    @ElementCollection
    @CollectionTable(name = "region_cities")
    private List<String> majorCities = new ArrayList<>();
    
    private Integer population;
    
    private Boolean active = true;
}
