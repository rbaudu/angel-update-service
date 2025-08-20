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
    
    private String continent;
    
    private Boolean active = true;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();
    
    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Region> regions = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "country_languages", joinColumns = @JoinColumn(name = "country_code"))
    @Column(name = "official_languages")
    private List<String> officialLanguages = new ArrayList<>();
}
