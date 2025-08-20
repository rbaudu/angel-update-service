// Admin Interface JavaScript

class AdminInterface {
    constructor() {
        this.currentSection = 'dashboard';
        this.wsConnection = null;
        this.init();
    }

    init() {
        this.setupNavigation();
        this.setupWebSocket();
        this.loadDashboard();
        
        // Auto-refresh every 30 seconds
        setInterval(() => {
            if (this.currentSection === 'dashboard') {
                this.loadDashboard();
            }
        }, 30000);
    }

    setupNavigation() {
        document.querySelectorAll('.list-group-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const href = item.getAttribute('href');
                if (href && href.startsWith('#')) {
                    this.showSection(href.substring(1));
                }
            });
        });
    }

    setupWebSocket() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/ws/admin`;
        
        try {
            this.wsConnection = new WebSocket(wsUrl);
            
            this.wsConnection.onopen = () => {
                console.log('WebSocket connected');
                this.updateConnectionStatus(true);
            };
            
            this.wsConnection.onmessage = (event) => {
                const data = JSON.parse(event.data);
                this.handleWebSocketMessage(data);
            };
            
            this.wsConnection.onclose = () => {
                console.log('WebSocket disconnected');
                this.updateConnectionStatus(false);
                // Reconnect after 5 seconds
                setTimeout(() => this.setupWebSocket(), 5000);
            };
            
            this.wsConnection.onerror = (error) => {
                console.error('WebSocket error:', error);
                this.updateConnectionStatus(false);
            };
        } catch (error) {
            console.error('Failed to establish WebSocket connection:', error);
            this.updateConnectionStatus(false);
        }
    }

    updateConnectionStatus(connected) {
        const statusElement = document.getElementById('status');
        const statusIcon = statusElement.previousElementSibling;
        
        if (connected) {
            statusElement.textContent = 'En ligne';
            statusIcon.className = 'fas fa-circle text-success';
        } else {
            statusElement.textContent = 'Hors ligne';
            statusIcon.className = 'fas fa-circle text-danger';
        }
    }

    handleWebSocketMessage(data) {
        switch (data.type) {
            case 'collector-update':
                this.updateCollectorStatus(data.payload);
                break;
            case 'new-log':
                this.addLogEntry(data.payload);
                break;
            case 'stats-update':
                this.updateStats(data.payload);
                break;
            default:
                console.log('Unknown WebSocket message:', data);
        }
    }

    showSection(sectionName) {
        // Update navigation
        document.querySelectorAll('.list-group-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[href="#${sectionName}"]`).classList.add('active');
        
        // Hide all sections
        document.querySelectorAll('.content-section').forEach(section => {
            section.classList.add('d-none');
        });
        
        // Show selected section
        const targetSection = document.getElementById(`${sectionName}-section`);
        if (targetSection) {
            targetSection.classList.remove('d-none');
            this.currentSection = sectionName;
            
            // Load section content
            switch (sectionName) {
                case 'dashboard':
                    this.loadDashboard();
                    break;
                case 'collectors':
                    this.loadCollectors();
                    break;
                case 'content':
                    this.loadContent();
                    break;
                case 'cache':
                    this.loadCacheStats();
                    break;
                case 'logs':
                    this.loadLogs();
                    break;
            }
        }
    }

    async loadDashboard() {
        try {
            const response = await fetch('/api/v1/admin/stats');
            if (response.ok) {
                const stats = await response.json();
                this.updateDashboardStats(stats);
            }
        } catch (error) {
            console.error('Failed to load dashboard:', error);
        }

        try {
            const response = await fetch('/api/v1/admin/activity');
            if (response.ok) {
                const activity = await response.json();
                this.updateRecentActivity(activity);
            }
        } catch (error) {
            console.error('Failed to load activity:', error);
        }
    }

    updateDashboardStats(stats) {
        document.getElementById('active-collectors').textContent = stats.activeCollectors || '--';
        document.getElementById('total-contents').textContent = stats.totalContents || '--';
        document.getElementById('cache-hit-ratio').textContent = stats.cacheHitRatio ? `${stats.cacheHitRatio}%` : '--';
        document.getElementById('requests-per-hour').textContent = stats.requestsPerHour || '--';
    }

    updateRecentActivity(activities) {
        const container = document.getElementById('recent-activity');
        if (!activities || activities.length === 0) {
            container.innerHTML = '<div class="text-muted">Aucune activité récente</div>';
            return;
        }

        const activityHtml = activities.map(activity => `
            <div class="activity-item ${activity.type}">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <strong>${activity.title}</strong>
                        <div class="text-muted small">${activity.description}</div>
                    </div>
                    <small class="text-muted">${this.formatTime(activity.timestamp)}</small>
                </div>
            </div>
        `).join('');

        container.innerHTML = activityHtml;
    }

    async loadCollectors() {
        try {
            const response = await fetch('/api/v1/admin/collectors');
            if (response.ok) {
                const collectors = await response.json();
                this.updateCollectorsTable(collectors);
            }
        } catch (error) {
            console.error('Failed to load collectors:', error);
            this.showError('Impossible de charger les collecteurs');
        }
    }

    updateCollectorsTable(collectors) {
        const tbody = document.querySelector('#collectors-table tbody');
        
        if (!collectors || collectors.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Aucun collecteur trouvé</td></tr>';
            return;
        }

        const collectorsHtml = collectors.map(collector => `
            <tr>
                <td>
                    <span class="collector-status ${collector.status.toLowerCase()}"></span>
                    ${collector.name}
                </td>
                <td><span class="badge bg-secondary">${collector.type}</span></td>
                <td>
                    <span class="badge status-badge status-${collector.status.toLowerCase()}">
                        ${collector.status}
                    </span>
                </td>
                <td>${collector.lastRun ? this.formatDateTime(collector.lastRun) : 'Jamais'}</td>
                <td>${collector.nextRun ? this.formatDateTime(collector.nextRun) : 'N/A'}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary" onclick="admin.runCollector('${collector.name}')">
                            <i class="fas fa-play"></i>
                        </button>
                        <button class="btn btn-outline-warning" onclick="admin.toggleCollector('${collector.name}', ${collector.enabled})">
                            <i class="fas fa-${collector.enabled ? 'pause' : 'play'}"></i>
                        </button>
                        <button class="btn btn-outline-info" onclick="admin.viewCollectorLogs('${collector.name}')">
                            <i class="fas fa-list-alt"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');

        tbody.innerHTML = collectorsHtml;
    }

    async loadContent() {
        try {
            const response = await fetch('/api/v1/admin/content');
            if (response.ok) {
                const content = await response.json();
                this.updateContentTable(content);
            }
        } catch (error) {
            console.error('Failed to load content:', error);
            this.showError('Impossible de charger le contenu');
        }
    }

    updateContentTable(contents) {
        const tbody = document.querySelector('#content-table tbody');
        
        if (!contents || contents.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Aucun contenu trouvé</td></tr>';
            return;
        }

        const contentsHtml = contents.map(content => `
            <tr>
                <td>${content.id}</td>
                <td><span class="badge bg-info">${content.contentType}</span></td>
                <td>
                    ${content.countryCode}${content.regionCode ? `/${content.regionCode}` : ''}
                </td>
                <td>
                    <span class="badge status-badge status-${content.status.toLowerCase()}">
                        ${content.status}
                    </span>
                </td>
                <td>${this.formatDateTime(content.publishedAt)}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary" onclick="admin.editContent(${content.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-outline-danger" onclick="admin.deleteContent(${content.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');

        tbody.innerHTML = contentsHtml;
    }

    async loadCacheStats() {
        try {
            const response = await fetch('/api/v1/admin/cache/stats');
            if (response.ok) {
                const stats = await response.json();
                this.updateCacheStats(stats);
            }
        } catch (error) {
            console.error('Failed to load cache stats:', error);
            this.showError('Impossible de charger les statistiques du cache');
        }
    }

    updateCacheStats(stats) {
        // Update Caffeine stats
        const caffeineContainer = document.getElementById('caffeine-stats');
        if (stats.caffeine) {
            caffeineContainer.innerHTML = `
                <div class="cache-stat">
                    <span>Entries</span>
                    <span class="cache-stat-value">${stats.caffeine.entries}</span>
                </div>
                <div class="cache-stat">
                    <span>Hit Rate</span>
                    <span class="cache-stat-value">${(stats.caffeine.hitRate * 100).toFixed(1)}%</span>
                </div>
                <div class="cache-stat">
                    <span>Miss Rate</span>
                    <span class="cache-stat-value">${(stats.caffeine.missRate * 100).toFixed(1)}%</span>
                </div>
                <div class="cache-stat">
                    <span>Evictions</span>
                    <span class="cache-stat-value">${stats.caffeine.evictions}</span>
                </div>
            `;
        }

        // Update Redis stats
        const redisContainer = document.getElementById('redis-stats');
        if (stats.redis) {
            redisContainer.innerHTML = `
                <div class="cache-stat">
                    <span>Connected</span>
                    <span class="cache-stat-value">${stats.redis.connected ? 'Oui' : 'Non'}</span>
                </div>
                <div class="cache-stat">
                    <span>Keys</span>
                    <span class="cache-stat-value">${stats.redis.keys}</span>
                </div>
                <div class="cache-stat">
                    <span>Memory</span>
                    <span class="cache-stat-value">${stats.redis.memory}</span>
                </div>
                <div class="cache-stat">
                    <span>Hits</span>
                    <span class="cache-stat-value">${stats.redis.hits}</span>
                </div>
            `;
        }
    }

    async loadLogs() {
        try {
            const level = document.getElementById('log-level').value;
            const response = await fetch(`/api/v1/admin/logs?level=${level}&limit=100`);
            if (response.ok) {
                const logs = await response.json();
                this.updateLogsContainer(logs);
            }
        } catch (error) {
            console.error('Failed to load logs:', error);
            this.showError('Impossible de charger les logs');
        }
    }

    updateLogsContainer(logs) {
        const container = document.getElementById('logs-container');
        
        if (!logs || logs.length === 0) {
            container.innerHTML = '<div class="text-muted">Aucun log trouvé</div>';
            return;
        }

        const logsHtml = logs.map(log => `
            <div class="log-entry ${log.level.toLowerCase()}">
                <span class="text-muted">[${this.formatDateTime(log.timestamp)}]</span>
                <span class="badge bg-${this.getLevelColor(log.level)}">${log.level}</span>
                <span class="text-muted">${log.logger}</span>
                <div>${log.message}</div>
                ${log.exception ? `<div class="text-danger small mt-1">${log.exception}</div>` : ''}
            </div>
        `).join('');

        container.innerHTML = logsHtml;
        container.scrollTop = container.scrollHeight;
    }

    // Action methods
    async runCollector(collectorName) {
        try {
            const response = await fetch(`/api/v1/admin/collectors/${collectorName}/run`, {
                method: 'POST'
            });
            if (response.ok) {
                this.showSuccess(`Collecteur ${collectorName} démarré`);
                this.loadCollectors();
            } else {
                throw new Error('Failed to start collector');
            }
        } catch (error) {
            console.error('Failed to run collector:', error);
            this.showError(`Impossible de démarrer le collecteur ${collectorName}`);
        }
    }

    async toggleCollector(collectorName, currentState) {
        const action = currentState ? 'disable' : 'enable';
        try {
            const response = await fetch(`/api/v1/admin/collectors/${collectorName}/${action}`, {
                method: 'POST'
            });
            if (response.ok) {
                this.showSuccess(`Collecteur ${collectorName} ${currentState ? 'désactivé' : 'activé'}`);
                this.loadCollectors();
            } else {
                throw new Error(`Failed to ${action} collector`);
            }
        } catch (error) {
            console.error(`Failed to ${action} collector:`, error);
            this.showError(`Impossible de ${currentState ? 'désactiver' : 'activer'} le collecteur ${collectorName}`);
        }
    }

    async clearAllCache() {
        if (!confirm('Êtes-vous sûr de vouloir vider tout le cache ?')) {
            return;
        }

        try {
            const response = await fetch('/api/v1/admin/cache/clear', {
                method: 'POST'
            });
            if (response.ok) {
                this.showSuccess('Cache vidé avec succès');
                this.loadCacheStats();
            } else {
                throw new Error('Failed to clear cache');
            }
        } catch (error) {
            console.error('Failed to clear cache:', error);
            this.showError('Impossible de vider le cache');
        }
    }

    // Upload content
    async uploadContent() {
        const form = document.getElementById('uploadForm');
        const formData = new FormData();
        
        // Get form values
        const file = document.getElementById('contentFile').files[0];
        const contentType = document.getElementById('contentType').value;
        const countryCode = document.getElementById('countryCode').value;
        const regionCode = document.getElementById('regionCode').value;
        const tags = document.getElementById('tags').value;
        const priority = document.getElementById('priority').value;
        
        if (!file || !contentType || !countryCode || !priority) {
            this.showError('Veuillez remplir tous les champs obligatoires');
            return;
        }
        
        // Build form data
        formData.append('file', file);
        formData.append('contentType', contentType);
        formData.append('countryCode', countryCode);
        formData.append('regionCode', regionCode);
        formData.append('tags', tags);
        formData.append('priority', priority);
        
        try {
            const response = await fetch('/api/v1/admin/content/upload', {
                method: 'POST',
                body: formData
            });
            
            if (response.ok) {
                this.showSuccess('Contenu uploadé avec succès');
                form.reset();
                bootstrap.Modal.getInstance(document.getElementById('uploadModal')).hide();
                if (this.currentSection === 'content') {
                    this.loadContent();
                }
            } else {
                throw new Error('Upload failed');
            }
        } catch (error) {
            console.error('Failed to upload content:', error);
            this.showError('Impossible d\'uploader le contenu');
        }
    }

    // Utility methods
    formatDateTime(dateString) {
        return new Date(dateString).toLocaleString('fr-FR');
    }

    formatTime(dateString) {
        return new Date(dateString).toLocaleTimeString('fr-FR');
    }

    getLevelColor(level) {
        switch (level.toUpperCase()) {
            case 'ERROR': return 'danger';
            case 'WARN': return 'warning';
            case 'INFO': return 'info';
            case 'DEBUG': return 'secondary';
            default: return 'light';
        }
    }

    showSuccess(message) {
        this.showAlert(message, 'success');
    }

    showError(message) {
        this.showAlert(message, 'danger');
    }

    showAlert(message, type) {
        const alertHtml = `
            <div class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
        
        // Insert at the top of the current section
        const currentSection = document.querySelector('.content-section:not(.d-none)');
        if (currentSection) {
            currentSection.insertAdjacentHTML('afterbegin', alertHtml);
        }
    }
}

// Global functions for onclick handlers
const admin = new AdminInterface();

function refreshDashboard() {
    admin.loadDashboard();
}

function refreshLogs() {
    admin.loadLogs();
}

function filterContent() {
    admin.loadContent();
}

function clearAllCache() {
    admin.clearAllCache();
}

function uploadContent() {
    admin.uploadContent();
}

function startAllCollectors() {
    // Implementation for starting all collectors
    console.log('Starting all collectors...');
}

function stopAllCollectors() {
    // Implementation for stopping all collectors
    console.log('Stopping all collectors...');
}