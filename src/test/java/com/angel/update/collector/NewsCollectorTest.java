package com.angel.update.collector;

import com.angel.update.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour NewsCollector
 */
@ExtendWith(MockitoExtension.class)
class NewsCollectorTest {
    
    @Mock
    private CacheService cacheService;
    
    private NewsCollector newsCollector;
    
    @BeforeEach
    void setUp() {
        newsCollector = new NewsCollector(cacheService);
        ReflectionTestUtils.setField(newsCollector, "mockMode", true);
        ReflectionTestUtils.setField(newsCollector, "globalEnabled", true);
    }
    
    @Test
    void getCollectorName_ShouldReturnCorrectName() {
        // When
        String name = newsCollector.getCollectorName();
        
        // Then
        assertEquals("NewsCollector", name);
    }
    
    @Test
    void getContentType_ShouldReturnNews() {
        // When
        String type = newsCollector.getContentType();
        
        // Then
        assertEquals("news", type);
    }
    
    @Test
    void isEnabled_WhenGlobalEnabledIsTrue_ShouldReturnTrue() {
        // Given
        ReflectionTestUtils.setField(newsCollector, "globalEnabled", true);
        
        // When
        boolean enabled = newsCollector.isEnabled();
        
        // Then
        assertTrue(enabled);
    }
    
    @Test
    void isEnabled_WhenGlobalEnabledIsFalse_ShouldReturnFalse() {
        // Given
        ReflectionTestUtils.setField(newsCollector, "globalEnabled", false);
        
        // When
        boolean enabled = newsCollector.isEnabled();
        
        // Then
        assertFalse(enabled);
    }
    
    @Test
    void collectNews_WhenEnabled_ShouldCacheNewsForMultipleCountries() {
        // Given
        ReflectionTestUtils.setField(newsCollector, "globalEnabled", true);
        ReflectionTestUtils.setField(newsCollector, "mockMode", true);
        
        // When
        newsCollector.collectNews();
        
        // Then
        // Vérifier que cacheNews a été appelé pour les différents pays
        verify(cacheService, atLeast(4)).cacheNews(anyString(), any(), any());
        verify(cacheService).cacheNews(eq("FR"), eq(null), any());
        verify(cacheService).cacheNews(eq("FR"), eq("IDF"), any());
        verify(cacheService).cacheNews(eq("US"), eq(null), any());
        verify(cacheService).cacheNews(eq("GB"), eq(null), any());
        verify(cacheService).cacheNews(eq("DE"), eq(null), any());
    }
    
    @Test
    void collectNews_WhenDisabled_ShouldNotCollect() {
        // Given
        ReflectionTestUtils.setField(newsCollector, "globalEnabled", false);
        
        // When
        newsCollector.collectNews();
        
        // Then
        verifyNoInteractions(cacheService);
    }
    
    @Test
    void validateConfiguration_ShouldReturnTrue() {
        // When
        boolean valid = newsCollector.validateConfiguration();
        
        // Then
        assertTrue(valid);
    }
}