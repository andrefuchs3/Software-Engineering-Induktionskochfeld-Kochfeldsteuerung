# Sprint 3 – Implementierung

## 1. Implementierungsumgebung

Die Implementierungsumgebung entspricht Sprint 2:

- **Programmiersprache:** Java 17  
- **JDK:** Temurin / OpenJDK 17  
- **IDE:** Visual Studio Code  
- **Kompilierung & Ausführung:** integrierte Java-Unterstützung in VS Code  

Die Modul- und Paketstruktur wurde um die Fehlbedienungserkennung erweitert:

```text

/src/
├─ core
│ ├─ CooktopController.java (erweitert: Fehlbedienungslogik)
│ ├─ ZoneManager.java
│ └─ TimerManager.java
├─ power
│ └─ PowerControl.java
├─ safety
│ ├─ SafetyManager.java
│ └─ MisuseDetector.java (neu)
├─ hmi
│ ├─ HmiInput.java
│ └─ HmiOutput.java (erweitert: Warnmeldungen)
└─ util
└─ Types.java
```


---

## 2. Bezug zur Traceability-Matrix

Die in Sprint 3 implementierten Funktionen decken folgende Requirements ab:

| Req-ID | Inhalt                                    | Abgedeckt durch |
|--------|--------------------------------------------|------------------|
| **F-08** | Fehler-/Sperrzustände anzeigen             | HmiOutput.showError, SafetyManager, CooktopController |
| **F-14** | Fehlbedienungserkennung & Warnung          | MisuseDetector, CooktopController, HmiOutput |
| **NF-02** | Keine ungewollte Aktivierung/Deaktivierung | SafetyManager, MisuseDetector |
| **NF-03** | Schutz vor unbeabsichtigten Eingaben       | SafetyManager, MisuseDetector |

---

## 3. Implementierungsüberblick

Die Fehlbedienungserkennung wurde vollständig in die bestehende Architektur integriert und erweitert das System um eine konsistente Warn- und Fehlerbehandlung.

### Ablauf der Fehlbedienungs-Interaktion

#### **1. Benutzerinteraktion über HmiInput**
Eingaben wie:
- Leistungsänderung  
- Timer setzen  
- Zonenwahl  

#### **2. Validierung im CooktopController**
Der Controller prüft nun zusätzlich:

- Ist die Zone aktiv?  
- Ist die Bedienung gesperrt?  
- Ist die Eingabe gültig?  

Bei ungültiger Aktion wird der **MisuseDetector** aufgerufen.

#### **3. Fehlbedienungserkennung durch MisuseDetector (neu)**

- registriert ungültige Bedienung  
- protokolliert Grund und betroffene Zone  
- meldet Warnung über `HmiOutput.showWarning(...)`  

#### **4. Systemreaktion bei Fehlbedienung**

- keine Statusänderung  
- keine Leistungsänderung  
- Benutzer erhält Warnung, System bleibt stabil  

---

## Beispiele für erkannte Fehlbedienungen

- Leistungsänderung ohne aktive Zone  
- Timer setzen bei aktiver Kindersicherung  
- Eingaben auf einer nicht existierenden Zone  
- Versuche zur Steuerung während Sperrzustand  

Die Logik arbeitet unabhängig von den Timerfunktionen und ergänzt das System um robuste Fehlerbehandlung.

