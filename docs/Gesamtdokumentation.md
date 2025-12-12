# Gesamtdokumentation – Induktionskochfeld: Kochfeldsteuerung

Diese Gesamtdokumentation beschreibt den aktuellen Stand der Softwareentwicklung des Projekts **Induktionskochfeld – Kochfeldsteuerung**.  
Das Dokument fasst alle bisher erarbeiteten Konzepte, Anforderungen, architektonischen Strukturen und Modellierungen zusammen und dient als zentraler Überblick über den Entwicklungsfortschritt.  
Es wird fortlaufend mit jedem Sprint aktualisiert und bildet somit die Hauptreferenz für das gesamte Projekt.

---

## 1. Projektüberblick

Ziel des Projekts ist die Entwicklung einer modularen, wartbaren und erweiterbaren Softwarearchitektur für die Steuerung eines Induktionskochfeldes.  
Das System soll die wichtigsten Funktionen eines modernen Kochfelds abbilden – von der Kochzonenaktivierung und Leistungssteuerung bis hin zu Timer-, Sicherheits- und Energieverwaltungsfunktionen.

Die Softwareentwicklung orientiert sich am **agilen Vorgehensmodell**, bei dem die Funktionalität schrittweise in mehreren **Sprints** umgesetzt wird.  
Jeder Sprint liefert ein lauffähiges Inkrement der Software und wird in einer eigenen Unterdokumentation beschrieben.

---

## 2. Anforderungsanalyse und Requirement Engineering

Die Anforderungsanalyse und das Requirement Engineering bilden die Grundlage für die gesamte Softwareentwicklung.  
Hier wurden alle **funktionalen** und **nicht-funktionalen Requirements** spezifiziert, strukturiert und priorisiert.

📄 [Requirement_Engineering.md](https://github.com/andrefuchs3/Software-Engineering-Induktionskochfeld-Kochfeldsteuerung/blob/main/docs/Requirement_Engineering.md)  

📄 [Requirements.md](https://github.com/andrefuchs3/Software-Engineering-Induktionskochfeld-Kochfeldsteuerung/blob/main/docs/Requirements.md)

Die Anforderungen umfassen u. a.:

- **Funktionale Requirements**
  - Aktivierung und Deaktivierung einzelner Kochzonen  
  - Leistungsstufeneinstellung und -anzeige  
  - Timerfunktion und automatische Abschaltung  
  - Kindersicherung und Fehlbedienungserkennung  
  - Akustisches und visuelles Feedback  

- **Nicht-funktionale Requirements**
  - Reaktionszeit ≤ 200 ms  
  - Energieverbrauch im Standby ≤ 1 W  
  - Normenkonformität (IEC 60335)  
  - Robustheit und Lesbarkeit der Benutzeroberfläche  
  - Sicherheit gegen unbeabsichtigte Eingaben  

Die vollständige Liste aller Requirements mit Zuordnung zu funktionalen Gruppen ist im verlinkten Dokument ersichtlich.

---

## 3. Architektur, Schnittstellen und Traceability

Im Anschluss an die Anforderungsanalyse wurde eine erste Softwarearchitektur entworfen, die alle zentralen Systemfunktionen abbildet.  
Die Architektur folgt einem **schichtenbasierten Modell**, bestehend aus:

- **Eingabeschicht** (Benutzerschnittstelle / HMI-Input)  
- **Steuerungsschicht** (Kochfeldlogik, Sicherheits- und Zeitsteuerung)  
- **Ausgabeschicht** (Anzeige- und Feedback-Komponenten)  
- **Hardwareabstraktionsschicht** (Kommunikation zu Sensoren und Heizelementen)  

Darüber hinaus wurden die Kommunikationsschnittstellen zwischen den Modulen definiert und alle Anforderungen über eine Traceability-Matrix mit Komponenten und Testfällen verknüpft.

📄 [Dokumentation – Architektur, Schnittstellen und Traceability](https://github.com/andrefuchs3/Software-Engineering-Induktionskochfeld-Kochfeldsteuerung/blob/main/docs/Architektur_Schnittstellen_Dokumentation.md)

📊 [Traceability-Matrix](https://github.com/andrefuchs3/Software-Engineering-Induktionskochfeld-Kochfeldsteuerung/blob/main/docs/Traceability-Matrix.md)

Die Architektur definiert bereits alle Hauptkomponenten des Systems, z. B.:

| Komponente | Beschreibung |
|:--|:--|
| `hmiInput` | Erfassung von Touch-Eingaben (Zone, Plus/Minus, Sperre, Timer) |
| `hmiOutput` | Anzeige und Feedback (Display, LEDs, akustische Signale) |
| `cooktopController` | Zentrale Steuerlogik des Systems |
| `powerControl` | Leistungsstufenverwaltung und Regelung |
| `zoneManager` | Verwaltung aktiver/inaktiver Kochzonen |
| `timerManager` | Zeitsteuerung (Start, Ablauf, Änderung) |
| `safetyManager` | Kindersicherung und Fehlbedienungsschutz |
| `energyManager` | Energieüberwachung und Standby-Steuerung |
| `hardwareAbstraction` | Schnittstelle zu Sensoren, Heizelementen, Buzzer |

Diese Komponenten bilden die Grundlage für die objektorientierte Modellierung im weiteren Verlauf.

---

## 4. Objektorientiertes Design

Auf Basis der zuvor definierten Architektur wurde das System in **Software-Design-Komponenten**
und **UML-Modelle** überführt.  
Im ersten Sprint wurde die **Basisfunktionalität** modelliert, die folgende Requirements abdeckt:

- F-01 – Kochzonenaktivierung über Touch  
- F-02 – Anzeige aktiver Kochzonen  
- F-03 – Leistungsstufeneinstellung  
- F-04 – Leistungsänderung über Plus/Minus  
- F-07 – Leistungsanzeige  
- F-13 – Kindersicherung  
- NF-01 – Reaktionszeit ≤ 200 ms  

Diese Anforderungen bilden den Kern der Steuerungslogik und stellen eine lauffähige Grundversion des Systems dar.

Die Modellierung erfolgte mithilfe von **UML-Klassendiagrammen** und
**Kommunikationsdiagrammen**, um sowohl die statische Struktur als auch das dynamische
Zusammenspiel der Komponenten darzustellen.

Zentrale Entwurfsentscheidungen waren:

- klare Trennung zwischen Eingabe, Steuerung und Ausgabe  
- zentrale Koordination über den `CooktopController`  
- lose Kopplung der Module  
- hohe Erweiterbarkeit für spätere Sprints  

📄 Die vollständigen UML-Diagramme sind in der jeweiligen Sprint-Dokumentation enthalten.

---

## 5. Implementierung nach Sprints

Die Implementierung erfolgte iterativ in drei aufeinander aufbauenden Sprints.
Jeder Sprint erweitert das System funktional und strukturell, ohne bestehende Funktionalität zu beeinträchtigen.

### 5.1 Sprint 1 – Basisfunktionalität

Sprint 1 konzentrierte sich auf die grundlegenden Steuerungsfunktionen des Kochfeldes.

**Umgesetzte Schwerpunkte:**
- Aktivierung und Deaktivierung von Kochzonen  
- Leistungsstufenverwaltung (1–9)  
- Anzeige des aktuellen Systemzustands  
- Kindersicherung (Sperren/Entsperren)  

**Zentrale Klassen:**
- `HmiInput`
- `CooktopController`
- `PowerControl`
- `ZoneManager`
- `SafetyManager`
- `HmiOutput`

Sprint 1 stellt ein vollständig lauffähiges Basissystem dar und bildet die Grundlage
für alle weiteren Erweiterungen.

---

### 5.2 Sprint 2 – Zeitsteuerung und Feedback

In Sprint 2 wurde das System um **Zeitsteuerungsfunktionen** erweitert.

**Umgesetzte Schwerpunkte:**
- Timerfunktion pro Kochzone  
- Automatische Deaktivierung nach Timer-Ablauf  
- Visuelles und akustisches Feedback  
- Erweiterung der HMI-Ausgabe  

**Neue / erweiterte Komponenten:**
- `TimerManager`
- Erweiterungen in `CooktopController`
- Erweiterungen in `HmiOutput`

Die Timerlogik wurde vollständig in die bestehende Architektur integriert,
ohne bestehende Module zu verändern.

Sprint 2 erweitert das System funktional erheblich und erhöht die Benutzerfreundlichkeit.

---

### 5.3 Sprint 3 – Fehlbedienungserkennung und Robustheit

Sprint 3 fokussierte sich auf **Systemrobustheit, Sicherheit und Fehlbedienungserkennung**.

**Umgesetzte Schwerpunkte:**
- Erkennung ungültiger Bedienaktionen  
- Unterscheidung zwischen Fehler- und Warnmeldungen  
- Schutz vor unbeabsichtigten Eingaben  
- Erweiterte Sicherheitslogik  

**Neue / erweiterte Komponenten:**
- `MisuseDetector` (neu)
- Erweiterungen in `CooktopController`
- Erweiterungen in `HmiOutput`
- Erweiterungen in `SafetyManager`

Die Fehlbedienungserkennung ergänzt die bestehende Logik, ohne den normalen
Betriebsfluss zu stören, und erhöht die Stabilität des Gesamtsystems deutlich.

---

## 6. Testkonzept und Qualitätssicherung

Zur Sicherstellung der funktionalen und technischen Qualität wurden strukturierte Tests
auf mehreren Ebenen durchgeführt.

### 6.1 Tests auf Modulebene
Auf der Modulebene (Unit-Tests) wurde die algorithmische Korrektheit einzelner Komponenten geprüft,
z. B.:

- Leistungsstufenlogik (`PowerControl`)
- Kindersicherung (`SafetyManager`)
- Fehlbedienungserkennung (`MisuseDetector`)
- Timerverwaltung (`TimerManager`)

📄 Dokumentation: `Testfälle/Testfälle_Modulebene.md`

---

### 6.2 Integrationstests
Integrationstests prüfen das Zusammenspiel mehrerer Komponenten, insbesondere
die korrekte Aufrufreihenfolge und Datenübergabe.

Typische Integrationen:
- `HmiInput ↔ CooktopController`
- `CooktopController ↔ PowerControl`
- `CooktopController ↔ TimerManager`
- `CooktopController ↔ SafetyManager`
- `CooktopController ↔ MisuseDetector`

📄 Dokumentation: `Testfälle/Testfälle_Integrationsebene.md`

---

### 6.3 Regressionstests
Nach jedem Sprint wurden alle bestehenden Tests erneut ausgeführt,
um sicherzustellen, dass neue Funktionen keine bestehenden beeinträchtigen.

Die erfolgreichen Regressionstests bestätigen die Stabilität der Architektur.

---

## 7. Traceability und Nachverfolgbarkeit

Alle Requirements sind eindeutig mit:

- Architekturkomponenten  
- Software-Design-Komponenten  
- Implementierung (Module)  
- Testfällen  

verknüpft.

📊 Die vollständige Nachverfolgbarkeit ist in der **Traceability-Matrix** dokumentiert:

📄 `Traceability-Matrix.md`

Diese Matrix stellt sicher, dass:
- jede Anforderung implementiert ist,
- jede Implementierung getestet wird,
- jede Änderung nachvollziehbar bleibt.

---

## 8. Zusammenfassung und Ausblick

Das Projekt **Induktionskochfeld – Kochfeldsteuerung** zeigt eine schrittweise,
strukturierte Entwicklung einer komplexen Steuerungssoftware.

**Erreichte Ziele:**
- modulare und erweiterbare Architektur  
- klare Trennung von Zuständigkeiten  
- vollständige Abdeckung zentraler Kochfeldfunktionen  
- robuste Sicherheits- und Fehlbedienungslogik  
- durchgängige Test- und Traceability-Struktur  

**Ausblick auf mögliche Erweiterungen:**
- Integration eines vollwertigen Testframeworks (z. B. JUnit)
- Erweiterte Fehlbedienungslogik mit adaptiven Regeln
- Energieoptimierung und Standby-Management
- Erweiterte HMI-Funktionalität (z. B. Restwärmeanzeige, Mehrzonensteuerung)

Die bestehende Architektur bietet hierfür eine stabile und gut nachvollziehbare Grundlage.


