package com.angel.update.service;

import com.angel.update.collector.BaseCollector;
import com.angel.update.model.CollectorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * Service de gestion des collecteurs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectorService {
    
    private final List<BaseCollector> collectors;
    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    private final Map<String, CollectorStatus> collectorStatuses = new HashMap<>();
    
    @PostConstruct
    public void initializeCollectors() {
        for (BaseCollector collector : collectors) {
            String id = collector.getId();
            
            // Initialiser le statut
            CollectorStatus status = CollectorStatus.builder()
                    .id(id)
                    .name(collector.getName())
                    .type(collector.getType())
                    .enabled(collector.isEnabled())
                    .schedule(collector.getSchedule())
                    .status(CollectorStatus.Status.IDLE)
                    .successCount(0)
                    .errorCount(0)
                    .build();
            
            collectorStatuses.put(id, status);
            
            // Planifier si activé
            if (collector.isEnabled()) {
                scheduleCollector(collector);
            }
        }
        
        log.info("Initialized {} collectors", collectors.size());
    }
    
    /**
     * Active/Désactive un collecteur
     */
    public boolean toggleCollector(String id) {
        BaseCollector collector = findCollectorById(id);
        if (collector == null) {
            throw new IllegalArgumentException("Collector not found: " + id);
        }
        
        boolean newState = !collector.isEnabled();
        collector.setEnabled(newState);
        
        CollectorStatus status = collectorStatuses.get(id);
        status.setEnabled(newState);
        
        if (newState) {
            scheduleCollector(collector);
            status.setStatus(CollectorStatus.Status.IDLE);
        } else {
            cancelScheduledTask(id);
            status.setStatus(CollectorStatus.Status.DISABLED);
        }
        
        log.info("Collector {} {}", id, newState ? "enabled" : "disabled");
        
        return newState;
    }
    
    /**
     * Exécute un collecteur immédiatement
     */
    public void runCollectorNow(String id) {
        BaseCollector collector = findCollectorById(id);
        if (collector == null) {
            throw new IllegalArgumentException("Collector not found: " + id);
        }
        
        CollectorStatus status = collectorStatuses.get(id);
        status.setStatus(CollectorStatus.Status.RUNNING);
        status.setLastRun(LocalDateTime.now());
        
        taskScheduler.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                collector.collect();
                long executionTime = System.currentTimeMillis() - startTime;
                
                status.setStatus(CollectorStatus.Status.SUCCESS);
                status.setSuccessCount(status.getSuccessCount() + 1);
                status.setLastExecutionTime(executionTime);
                status.setLastError(null);
                
                log.info("Collector {} executed successfully in {}ms", id, executionTime);
                
            } catch (Exception e) {
                status.setStatus(CollectorStatus.Status.ERROR);
                status.setErrorCount(status.getErrorCount() + 1);
                status.setLastError(e.getMessage());
                
                log.error("Error executing collector {}", id, e);
            }
        });
    }
    
    /**
     * Récupère le statut de tous les collecteurs
     */
    public List<CollectorStatus> getAllCollectorStatus() {
        return new ArrayList<>(collectorStatuses.values());
    }
    
    /**
     * Récupère le statut d'un collecteur
     */
    public CollectorStatus getCollectorStatus(String id) {
        return collectorStatuses.get(id);
    }
    
    private void scheduleCollector(BaseCollector collector) {
        String schedule = collector.getSchedule();
        if (schedule == null || schedule.isEmpty()) {
            return;
        }
        
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> runCollectorNow(collector.getId()),
                new CronTrigger(schedule)
        );
        
        scheduledTasks.put(collector.getId(), future);
        
        log.info("Scheduled collector {} with cron: {}", collector.getId(), schedule);
    }
    
    private void cancelScheduledTask(String id) {
        ScheduledFuture<?> future = scheduledTasks.get(id);
        if (future != null) {
            future.cancel(false);
            scheduledTasks.remove(id);
            log.info("Cancelled scheduled task for collector {}", id);
        }
    }
    
    private BaseCollector findCollectorById(String id) {
        return collectors.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
