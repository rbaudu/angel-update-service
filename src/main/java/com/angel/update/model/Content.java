package com.angel.update.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entité représentant un contenu
 */
@Entity
@Table(name = "contents")
@Data
@EqualsAndHashCode(exclude = {"region"})
@ToString(exclude = {"region"})
public class Content {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String contentType;
    
    @Column(nullable = false)
    private String languageCode;
    
    @Column(nullable = false)
    private String countryCode;
    
    private String regionCode;
    
    @Column(nullable = false)
    private String filePath;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String version;
    
    @ElementCollection
    @CollectionTable(name = "content_tags")
    private Set<String> tags = new HashSet<>();
    
    @Enumerated(EnumType.STRING)
    private ContentPriority priority = ContentPriority.NORMAL;
    
    @Enumerated(EnumType.STRING)
    private ContentStatus status = ContentStatus.ACTIVE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime publishedAt;
    
    private LocalDateTime expiresAt;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "checksum")
    private String checksum;
    
    public enum ContentPriority {
        LOW, NORMAL, HIGH, URGENT
    }
    
    public enum ContentStatus {
        DRAFT, ACTIVE, ARCHIVED, DELETED
    }
}
