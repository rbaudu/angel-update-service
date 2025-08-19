# Interface Web d'Administration - ANGEL Update Service

## Vue d'Ensemble

L'interface web d'administration permet de gérer manuellement les contenus, monitorer le service et configurer les collecteurs de données.

## Fonctionnalités Principales

### 1. Dashboard Principal

```
┌──────────────────────────────────────────────────────┐
│                    ANGEL Update Service                 │
├──────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐│
│  │  Stats   │  │ Uploads  │  │Collectors│  │ Monitor  ││
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘│
├──────────────────────────────────────────────────────┤
│                                                         │
│  Statistiques en Temps Réel:                          │
│  • Requêtes/min: 245                                  │
│  • Cache Hit Rate: 87%                                │
│  • Active Clients: 1,234                              │
│  • Dernière MAJ: Il y a 5 min                         │
│                                                         │
│  [Graphique de charge]                                │
│                                                         │
└──────────────────────────────────────────────────────┘
```

### 2. Upload Manuel de Contenus

#### Interface d'Upload

```html
<!-- upload.html -->
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Upload de Contenus - ANGEL</title>
    <link rel="stylesheet" href="/css/admin.css">
</head>
<body>
    <div class="container">
        <h1>Upload de Contenus</h1>
        
        <form id="uploadForm" class="upload-form">
            <!-- Sélection du type de contenu -->
            <div class="form-group">
                <label>Type de Contenu:</label>
                <select id="contentType" required>
                    <option value="">-- Sélectionner --</option>
                    <optgroup label="Exercices">
                        <option value="exercises/gym">Gym</option>
                        <option value="exercises/stretching">Stretching</option>
                    </optgroup>
                    <optgroup label="Divertissement">
                        <option value="music">Musique</option>
                        <option value="poetry">Poésie</option>
                    </optgroup>
                    <optgroup label="Histoires">
                        <option value="stories/funny">Drôles</option>
                        <option value="stories/legends">Légendes</option>
                        <option value="stories/myths">Mythes</option>
                    </optgroup>
                    <optgroup label="Énigmes">
                        <option value="riddles/charades">Charades</option>
                        <option value="riddles/devinettes">Devinettes</option>
                    </optgroup>
                </select>
            </div>
            
            <!-- Sélection Pays/Région -->
            <div class="form-group">
                <label>Pays:</label>
                <select id="countryCode" required>
                    <option value="FR">France</option>
                    <option value="EN">England</option>
                    <option value="ES">España</option>
                    <option value="DE">Deutschland</option>
                </select>
            </div>
            
            <div class="form-group">
                <label>Région (optionnel):</label>
                <select id="regionCode">
                    <option value="">Toutes</option>
                    <option value="IDF">Île-de-France</option>
                    <option value="PACA">PACA</option>
                    <option value="BRE">Bretagne</option>
                </select>
            </div>
            
            <!-- Zone de texte ou upload fichier -->
            <div class="form-group">
                <label>Contenu:</label>
                <div class="upload-options">
                    <input type="radio" name="uploadType" value="text" checked>
                    <label>Texte direct</label>
                    <input type="radio" name="uploadType" value="file">
                    <label>Fichier</label>
                </div>
                
                <textarea id="contentText" class="content-input" 
                          placeholder="Entrez votre contenu ici..."></textarea>
                
                <input type="file" id="contentFile" 
                       accept=".txt,.md" style="display:none;">
            </div>
            
            <!-- Métadonnées -->
            <div class="form-group">
                <label>Tags (séparés par virgules):</label>
                <input type="text" id="tags" 
                       placeholder="relaxation, bien-être, santé">
            </div>
            
            <div class="form-group">
                <label>Priorité:</label>
                <select id="priority">
                    <option value="LOW">Basse</option>
                    <option value="NORMAL" selected>Normale</option>
                    <option value="HIGH">Haute</option>
                </select>
            </div>
            
            <!-- Boutons d'action -->
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">
                    <span class="icon">📤</span> Uploader
                </button>
                <button type="button" class="btn btn-secondary" 
                        onclick="previewContent()">
                    <span class="icon">👁️</span> Prévisualiser
                </button>
                <button type="reset" class="btn btn-danger">
                    <span class="icon">🗑️</span> Réinitialiser
                </button>
            </div>
        </form>
        
        <!-- Zone de prévisualisation -->
        <div id="preview" class="preview-area" style="display:none;">
            <h3>Prévisualisation</h3>
            <div id="previewContent"></div>
        </div>
        
        <!-- Historique des uploads -->
        <div class="upload-history">
            <h3>Uploads Récents</h3>
            <table class="history-table">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Type</th>
                        <th>Pays/Région</th>
                        <th>Statut</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="historyTable">
                    <!-- Chargé dynamiquement -->
                </tbody>
            </table>
        </div>
    </div>
    
    <script src="/js/upload.js"></script>
</body>
</html>
```

### 3. Gestion des Collecteurs

#### Interface de Configuration

```javascript
// dashboard.js - Gestion des Collecteurs
class CollectorManager {
    constructor() {
        this.collectors = [];
        this.loadCollectors();
    }
    
    async loadCollectors() {
        const response = await fetch('/api/v1/admin/collectors');
        this.collectors = await response.json();
        this.renderCollectors();
    }
    
    renderCollectors() {
        const container = document.getElementById('collectorsGrid');
        container.innerHTML = this.collectors.map(collector => `
            <div class="collector-card ${collector.enabled ? 'active' : 'inactive'}">
                <div class="collector-header">
                    <h3>${collector.name}</h3>
                    <label class="switch">
                        <input type="checkbox" 
                               ${collector.enabled ? 'checked' : ''}
                               onchange="toggleCollector('${collector.id}')">
                        <span class="slider"></span>
                    </label>
                </div>
                <div class="collector-info">
                    <p>Type: ${collector.type}</p>
                    <p>Schedule: ${collector.schedule}</p>
                    <p>Dernière exécution: ${this.formatDate(collector.lastRun)}</p>
                    <p>Prochaine: ${this.formatDate(collector.nextRun)}</p>
                    <p>Statut: <span class="status ${collector.status}">
                        ${collector.status}</span></p>
                </div>
                <div class="collector-actions">
                    <button onclick="runCollector('${collector.id}')" 
                            class="btn-small btn-primary">▶️ Exécuter</button>
                    <button onclick="configureCollector('${collector.id}')" 
                            class="btn-small btn-secondary">⚙️ Configurer</button>
                    <button onclick="viewLogs('${collector.id}')" 
                            class="btn-small btn-info">📋 Logs</button>
                </div>
            </div>
        `).join('');
    }
    
    async toggleCollector(id) {
        await fetch(`/api/v1/admin/collectors/${id}/toggle`, {
            method: 'POST'
        });
        this.loadCollectors();
    }
    
    async runCollector(id) {
        const btn = event.target;
        btn.disabled = true;
        btn.textContent = '⏳ En cours...';
        
        try {
            const response = await fetch(`/api/v1/admin/collectors/${id}/run`, {
                method: 'POST'
            });
            const result = await response.json();
            
            if (result.success) {
                this.showNotification('Collecteur exécuté avec succès', 'success');
            } else {
                this.showNotification(`Erreur: ${result.error}`, 'error');
            }
        } finally {
            btn.disabled = false;
            btn.textContent = '▶️ Exécuter';
            this.loadCollectors();
        }
    }
}
```

### 4. Monitoring en Temps Réel

#### Dashboard de Monitoring

```html
<!-- monitoring.html -->
<div class="monitoring-dashboard">
    <!-- Métriques en temps réel -->
    <div class="metrics-grid">
        <div class="metric-card">
            <h4>Requêtes/sec</h4>
            <div class="metric-value" id="rps">0</div>
            <canvas id="rpsChart"></canvas>
        </div>
        
        <div class="metric-card">
            <h4>Latence (ms)</h4>
            <div class="metric-value" id="latency">0</div>
            <canvas id="latencyChart"></canvas>
        </div>
        
        <div class="metric-card">
            <h4>Cache Hit Rate</h4>
            <div class="metric-value" id="cacheHitRate">0%</div>
            <div class="progress-bar">
                <div class="progress-fill" id="cacheProgress"></div>
            </div>
        </div>
        
        <div class="metric-card">
            <h4>Erreurs/min</h4>
            <div class="metric-value error" id="errorRate">0</div>
            <div class="error-list" id="recentErrors"></div>
        </div>
    </div>
    
    <!-- Logs en temps réel -->
    <div class="logs-container">
        <div class="logs-header">
            <h3>Logs en Direct</h3>
            <div class="log-filters">
                <select id="logLevel">
                    <option value="ALL">Tous</option>
                    <option value="ERROR">Erreurs</option>
                    <option value="WARN">Warnings</option>
                    <option value="INFO">Info</option>
                    <option value="DEBUG">Debug</option>
                </select>
                <input type="text" id="logSearch" 
                       placeholder="Rechercher...">
                <button onclick="clearLogs()">🗑️ Vider</button>
            </div>
        </div>
        <div class="logs-content" id="logsContent">
            <!-- Logs streaming via WebSocket -->
        </div>
    </div>
</div>
```

### 5. Gestion des Versions

```javascript
// version-manager.js
class VersionManager {
    async getVersionHistory() {
        const response = await fetch('/api/v1/admin/versions');
        return await response.json();
    }
    
    renderVersionTable(versions) {
        return `
            <table class="version-table">
                <thead>
                    <tr>
                        <th>Version</th>
                        <th>Date</th>
                        <th>Pays/Région</th>
                        <th>Type de Contenu</th>
                        <th>Changements</th>
                        <th>Taille</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${versions.map(v => `
                        <tr>
                            <td><code>${v.version}</code></td>
                            <td>${this.formatDate(v.createdAt)}</td>
                            <td>${v.countryCode}/${v.regionCode || '*'}</td>
                            <td>${v.contentTypes.join(', ')}</td>
                            <td>${v.changeCount} fichiers</td>
                            <td>${this.formatSize(v.size)}</td>
                            <td>
                                <button onclick="downloadVersion('${v.id}')">
                                    📥 Télécharger
                                </button>
                                <button onclick="rollbackVersion('${v.id}')" 
                                        class="btn-danger">
                                    ⏪ Rollback
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    }
}
```

### 6. CSS Styles

```css
/* admin.css */
:root {
    --primary-color: #4CAF50;
    --secondary-color: #2196F3;
    --danger-color: #f44336;
    --warning-color: #ff9800;
    --bg-color: #f5f5f5;
    --card-bg: white;
    --text-color: #333;
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}

/* Cards */
.collector-card {
    background: var(--card-bg);
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    transition: transform 0.3s;
}

.collector-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}

.collector-card.active {
    border-left: 4px solid var(--primary-color);
}

.collector-card.inactive {
    opacity: 0.7;
    border-left: 4px solid var(--warning-color);
}

/* Forms */
.upload-form {
    background: var(--card-bg);
    padding: 30px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.form-group {
    margin-bottom: 20px;
}

.form-group label {
    display: block;
    margin-bottom: 5px;
    font-weight: 600;
    color: var(--text-color);
}

.form-group input,
.form-group select,
.form-group textarea {
    width: 100%;
    padding: 10px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
}

.form-group textarea {
    min-height: 200px;
    resize: vertical;
}

/* Buttons */
.btn {
    padding: 10px 20px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.3s;
}

.btn-primary {
    background: var(--primary-color);
    color: white;
}

.btn-primary:hover {
    background: #45a049;
}

.btn-secondary {
    background: var(--secondary-color);
    color: white;
}

.btn-danger {
    background: var(--danger-color);
    color: white;
}

/* Metrics */
.metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
}

.metric-card {
    background: var(--card-bg);
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.metric-value {
    font-size: 36px;
    font-weight: bold;
    color: var(--primary-color);
    margin: 10px 0;
}

.metric-value.error {
    color: var(--danger-color);
}

/* Progress Bar */
.progress-bar {
    width: 100%;
    height: 20px;
    background: #e0e0e0;
    border-radius: 10px;
    overflow: hidden;
}

.progress-fill {
    height: 100%;
    background: var(--primary-color);
    transition: width 0.3s ease;
}

/* Logs */
.logs-container {
    background: var(--card-bg);
    border-radius: 8px;
    padding: 20px;
    max-height: 500px;
    overflow-y: auto;
}

.logs-content {
    font-family: 'Courier New', monospace;
    font-size: 12px;
    background: #1e1e1e;
    color: #d4d4d4;
    padding: 10px;
    border-radius: 4px;
    height: 400px;
    overflow-y: auto;
}

.log-entry {
    padding: 2px 0;
    border-bottom: 1px solid #333;
}

.log-entry.error {
    color: #f48771;
}

.log-entry.warn {
    color: #dcdcaa;
}

.log-entry.info {
    color: #9cdcfe;
}

/* Switch Toggle */
.switch {
    position: relative;
    display: inline-block;
    width: 50px;
    height: 24px;
}

.switch input {
    opacity: 0;
    width: 0;
    height: 0;
}

.slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: .4s;
    border-radius: 24px;
}

.slider:before {
    position: absolute;
    content: "";
    height: 18px;
    width: 18px;
    left: 3px;
    bottom: 3px;
    background-color: white;
    transition: .4s;
    border-radius: 50%;
}

input:checked + .slider {
    background-color: var(--primary-color);
}

input:checked + .slider:before {
    transform: translateX(26px);
}

/* Responsive */
@media (max-width: 768px) {
    .metrics-grid {
        grid-template-columns: 1fr;
    }
    
    .container {
        padding: 10px;
    }
}
```

### 7. WebSocket pour Temps Réel

```javascript
// websocket-client.js
class WebSocketClient {
    constructor() {
        this.connect();
    }
    
    connect() {
        this.ws = new WebSocket('ws://localhost:8080/ws/admin');
        
        this.ws.onopen = () => {
            console.log('WebSocket connecté');
            this.subscribe(['metrics', 'logs', 'alerts']);
        };
        
        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.handleMessage(data);
        };
        
        this.ws.onerror = (error) => {
            console.error('WebSocket erreur:', error);
        };
        
        this.ws.onclose = () => {
            console.log('WebSocket fermé, reconnexion dans 5s...');
            setTimeout(() => this.connect(), 5000);
        };
    }
    
    subscribe(channels) {
        this.ws.send(JSON.stringify({
            type: 'subscribe',
            channels: channels
        }));
    }
    
    handleMessage(data) {
        switch(data.type) {
            case 'metrics':
                this.updateMetrics(data.payload);
                break;
            case 'log':
                this.appendLog(data.payload);
                break;
            case 'alert':
                this.showAlert(data.payload);
                break;
        }
    }
    
    updateMetrics(metrics) {
        document.getElementById('rps').textContent = metrics.requestsPerSecond;
        document.getElementById('latency').textContent = metrics.avgLatency + 'ms';
        document.getElementById('cacheHitRate').textContent = metrics.cacheHitRate + '%';
        document.getElementById('errorRate').textContent = metrics.errorsPerMinute;
        
        // Mise à jour des graphiques
        updateChart('rpsChart', metrics.requestsPerSecond);
        updateChart('latencyChart', metrics.avgLatency);
    }
    
    appendLog(log) {
        const logsContent = document.getElementById('logsContent');
        const logEntry = document.createElement('div');
        logEntry.className = `log-entry ${log.level.toLowerCase()}`;
        logEntry.textContent = `[${log.timestamp}] [${log.level}] ${log.message}`;
        logsContent.appendChild(logEntry);
        
        // Auto-scroll
        logsContent.scrollTop = logsContent.scrollHeight;
    }
}

// Initialisation
document.addEventListener('DOMContentLoaded', () => {
    new WebSocketClient();
    new CollectorManager();
    new VersionManager();
});
```

## Sécurité de l'Interface

### Authentication

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/update/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(LogoutConfigurer::permitAll);
        
        return http.build();
    }
}
```

## API Endpoints Admin

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/v1/admin/upload` | POST | Upload manuel de contenu |
| `/api/v1/admin/collectors` | GET | Liste des collecteurs |
| `/api/v1/admin/collectors/{id}/toggle` | POST | Activer/Désactiver collecteur |
| `/api/v1/admin/collectors/{id}/run` | POST | Exécuter collecteur manuellement |
| `/api/v1/admin/metrics` | GET | Métriques en temps réel |
| `/api/v1/admin/versions` | GET | Historique des versions |
| `/api/v1/admin/cache/clear` | POST | Vider le cache |
| `/ws/admin` | WebSocket | Stream temps réel |

---

📝 **Note**: L'interface d'administration doit être sécurisée et accessible uniquement aux utilisateurs autorisés.
