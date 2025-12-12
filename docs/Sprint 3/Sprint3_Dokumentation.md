# Sprint 3 – Kochfeldsteuerung

## Ziel des Sprints

Sprint 3 erweitert die Kochfeldsteuerung um die **Erkennung von Fehlbedienungen** und die **Ausgabe von Warnungen**.  
Im Fokus stehen:

- das Erkennen wiederholter ungültiger Benutzereingaben (z. B. Bedienen gesperrter oder inaktiver Zonen),
- die Ausgabe verständlicher Warnmeldungen auf der Anzeige,
- die Sicherstellung, dass bestehende Funktionen aus Sprint 1 und 2 unverändert korrekt weiterlaufen (Regression).

Der Benutzer soll bei wiederholten Fehlbedienungen klare Rückmeldungen erhalten, ohne dass die Stabilität der Basisfunktionen (Leistungsregelung, Timer, Kindersicherung) beeinträchtigt wird.

---

## Scope Sprint 3 (Requirements)

Folgende Requirements aus der Traceability-Matrix werden in Sprint 3 adressiert:

**Funktionale Anforderungen**

- **F-08**: Fehler- oder Sperrzustände anzeigen (wird um Fehlbedienungswarnungen ergänzt)  
- **F-14**: Fehlbedienungserkennung & Warnung  

**Nicht-funktionale Anforderungen**

- **NF-02**: Keine ungewollte Aktivierung/Deaktivierung  
- **NF-03**: Schutz gegen unbeabsichtigte Eingaben  

Die Zuordnung zu den Software-Design-Komponenten ist in der [Traceability-Matrix](../Traceability-Matrix.md) dokumentiert.  
Sprint 3 ergänzt dort insbesondere die Testabdeckung von **F-14**, sowie die Querverbindung zu **NF-02/NF-03**.

---

## Software-Design-Komponenten Sprint 3

Im Vergleich zu Sprint 2 werden keine ganz neuen Komponenten eingeführt, sondern bestehende Klassen erweitert:

- **cooktopController** (erweitert)  
  - bleibt zentrale Steuerinstanz für Zonen, Leistung und Timer  
  - führt einen **Fehlbedienungszähler pro Zone**  
  - registriert ungültige Eingaben (z. B. Bedienung bei aktiver Kindersicherung oder inaktiver Zone)  
  - löst ab einem Schwellwert (z. B. mehrfachen Fehlversuchen) eine Warnmeldung aus (`showWarning(...)`)

- **hmiOutput** (erweitert)  
  - erhält eine zusätzliche Methode `showWarning(String message)`  
  - kann neben Fehlern und Statusinformationen nun auch **Warnungen** bei Fehlbedienung ausgeben  
  - bleibt zentrale Schnittstelle für alle Benutzerrückmeldungen (Status, Timer, Fehlermeldung, Warnung, Beep)

- **SafetyManager** (unverändert in der Struktur, aber intensiver genutzt)  
  - liefert weiterhin den Sperrstatus (Kindersicherung)  
  - wird vom Controller verwendet, um Eingaben bei Sperre als Fehlbedienung zu klassifizieren

- **ZoneManager / PowerControl / TimerManager** (unverändert)  
  - bleiben für Zonenstatus, Leistungsstufen und Timerverwaltung zuständig  
  - dienen jetzt zusätzlich als Kontext, in dem Fehlbedienungen erkannt werden (z. B. Leistungsänderung an inaktiver Zone)

- **HmiInput** (unverändert)  
  - repräsentiert weiterhin die HMI-Eingaben (Zonenwahl, Leistungsänderung, Timeroperationen, Kindersicherung)  
  - die Erkennung der Fehlbedienung erfolgt bewusst im `CooktopController`, nicht in `HmiInput`

---

## UML-Diagramme Sprint 3

Die UML-Diagramme für Sprint 3 liegen im Ordner `docs/Sprint 3/UML-Diagramme/` und wurden mit PlantUML erzeugt.

**Klassendiagramm – Sprint 3**  
![Klassendiagramm](./UML-Diagramme/Klassendiagramm_Sprint3.png)

- zeigt die erweiterten Methoden von `CooktopController` und `HmiOutput`  
- visualisiert den internen Fehlbedienungszähler im `CooktopController`  
- macht sichtbar, dass die grundlegende Architektur aus Sprint 1 / 2 beibehalten wurde

**Kommunikationsdiagramm – Fehlbedienung**  
![Kommunikationsdiagramm](./UML-Diagramme/Kommunikationsdiagramm_Sprint3.png)

- stellt die Interaktion bei wiederholter Fehlbedienung dar  
  (z. B. Benutzer versucht mehrfach, eine gesperrte Zone zu bedienen)  
- zeigt den Weg von der Eingabe (`HmiInput`) über den `CooktopController` hin zu `HmiOutput.showError(...)` und `showWarning(...)`

**Sequenzdiagramm – Use Case „Wiederholte Fehlbedienung“**  
![Sequenzdiagramm](./UML-Diagramme/Sequenzdiagramm_Sprint3.png)

- beschreibt den Ablauf mehrerer ungültiger Eingaben hintereinander  
- zeigt, wie der Fehlbedienungszähler im `CooktopController` erhöht wird  
- illustriert, ab wann eine Warnung angezeigt wird (Schwellwert)

Die Diagramme erweitern konsistent die in Sprint 1 und 2 eingeführte Architektur und fokussieren sich auf die neue Funktion **Fehlbedienungserkennung & Warnung (F-14)**.

---

# Testfälle – Sprint 3

## 1. Zielsetzung der Testaktivitäten

In **Sprint 3** wurde die Kochfeldsteuerung um eine Logik zur Erkennung von Fehlbedienungen erweitert.
Ziel der Testaktivitäten ist es daher,

- sicherzustellen, dass **ungültige Benutzereingaben zuverlässig erkannt** werden,
- zu prüfen, dass **Warnmeldungen bei wiederholter Fehlbedienung korrekt ausgegeben** werden,
- nachzuweisen, dass bestehende Funktionen aus **Sprint 1 und Sprint 2 unverändert korrekt funktionieren** (Regression).

Die Teststrategie basiert – wie in den vorherigen Sprints – auf zwei Ebenen:

- **Modulebene**: Prüfung der Logik im `CooktopController` (Fehlbedienung, Zählung, Rücksetzen),
- **Integrationsebene**: Prüfung des Zusammenspiels von `HmiInput`, `CooktopController` und `HmiOutput`.

Ein automatisiertes Testframework (z. B. JUnit) wurde im Sprint erprobt, konnte jedoch in der gegebenen
Projekt- und Toolumgebung nicht stabil integriert werden.
Aus diesem Grund wurde eine **eigene, automatisierte Testlogik in Java** implementiert,
die eine eindeutige **PASS/FAIL-Auswertung** pro Testfall erlaubt.

---

## 2. Testfälle auf Modulebene

Auf Modulebene liegt der Fokus in **Sprint 3** auf der erweiterten Fehlbedienungslogik
im `CooktopController`.

Die Tests prüfen dabei insbesondere,

- ob ungültige Aktionen als Fehlbedienung erkannt werden,
- ob wiederholte Fehlbedienungen korrekt verarbeitet werden,
- dass keine Exceptions oder unerwünschten Zustandsänderungen auftreten.

Die folgenden **neuen Modultests** wurden in Sprint 3 ergänzt und automatisiert umgesetzt:

| Test-ID | Modul | Zweck |
|--------:|-------|-------|
| MT-07 | core (CooktopController) | Fehlbedienung bei inaktiver Zone wird erkannt |
| MT-08 | core (CooktopController) | Fehlbedienung bei aktiver Kindersicherung |
| MT-09 | core (CooktopController) | Ungültiger Timerwert (0 / negativ) |

Die Modultests werden über die Datei  
`Test_Sprint3.java` ausgeführt und automatisch als **PASS oder FAIL** bewertet.

---

## 3. Testfälle auf Integrationsebene

Auf Integrationsebene wird geprüft, ob Fehlbedienungen über die HMI ausgelöst,
vom `CooktopController` erkannt und über das `HmiOutput` korrekt gemeldet werden.

Dabei wird das vollständige Zusammenspiel folgender Komponenten getestet:

- `HmiInput`
- `CooktopController`
- `HmiOutput`
- `SafetyManager`

Die folgenden **neuen Integrationstests** wurden in Sprint 3 ergänzt:

| Test-ID | Komponenten | Ziel |
|--------:|------------|------|
| IT-10 | HmiInput ↔ Controller ↔ HmiOutput | Leistungsänderung bei inaktiver Zone |
| IT-11 | HmiInput ↔ Controller ↔ HmiOutput | Eingaben bei aktiver Kindersicherung |
| IT-12 | HmiInput ↔ Controller ↔ HmiOutput | Ungültige Timerwerte (0 / negativ) |

Die Tests prüfen unter anderem,

- dass keine Leistungsänderung oder Aktivierung erfolgt,
- dass Fehlermeldungen korrekt ausgegeben werden,
- dass Warnmeldungen bei Fehlbedienung erscheinen.

Alle Integrationstests werden automatisiert über `Test_Sprint3.java` ausgeführt
und liefern eine eindeutige PASS/FAIL-Ausgabe in der Konsole.

---

## 4. Bezug zur Traceability-Matrix

Die neuen Testfälle aus Sprint 3 sind in der  
[Traceability-Matrix](../Traceability-Matrix.md) mit den entsprechenden Requirements verknüpft.

**Anforderungsauszug (Sprint 3-Relevanz)**

| Requirement | Inhalt                                   | Abgedeckt durch              |
|------------|-------------------------------------------|------------------------------|
| F-08       | Fehler-/Sperrzustände anzeigen            | MT-07, IT-10                 |
| F-14       | Fehlbedienungserkennung & Warnung         | MT-07, MT-08, IT-10, IT-11   |
| NF-02      | Keine ungewollte Aktivierung/Deaktivierung | MT-07, IT-10, IT-11          |
| NF-03      | Schutz gegen unbeabsichtigte Eingaben      | MT-07, MT-08, IT-10, IT-11   |

Damit ist dokumentiert, wie die neue Fehlbedienungslogik die übergeordneten Qualitätsziele unterstützt.

---

## 5. Durchgeführte Testläufe und Dokumentation der Ergebnisse

Für Sprint 3 wurde ein vollständiger Testdurchlauf durchgeführt.

Dabei kamen folgende Testdateien zum Einsatz:

- `Test_Sprint1.java` – automatisierte Tests für Sprint 1 (Regression)
- `Test_Sprint2.java` – automatisierte Tests für Sprint 2 (Regression)
- `Test_Sprint3.java` – neue Tests für Fehlbedienung und Warnlogik

Alle Tests wurden ohne externes Testframework ausgeführt.
Stattdessen wurde eine eigene, automatisierte Testlogik implementiert,
die jeden Testfall eindeutig als **PASS** oder **FAIL** bewertet.

Die Konsolenausgaben aller Testläufe zeigen,
dass sämtliche Testfälle erfolgreich bestanden wurden.
Es traten keine Regressionen in den Funktionen aus Sprint 1 und 2 auf.

---

## 6. Vergleich von Architektur/Design und Implementierung (Sprint 3)

Die geplante Erweiterung um Fehlbedienungserkennung wurde mit der konkreten Implementierung verglichen.

### Übereinstimmungen

- Die Erkennung von Fehlbedienungen wurde wie geplant **zentral im `CooktopController`** realisiert.  
- Es wird **kein zusätzlicher Dienst/Subsystem** eingeführt, sondern die Logik ist eng an die bestehende Steuerung gekoppelt.  
- `HmiOutput` wurde um eine **spezifische Warn-API** (`showWarning(String)`) erweitert, wie im Design vorgesehen.  
- Die bestehende Schichtenarchitektur (HMI → Controller → Module) wird beibehalten; HMI erzeugt nur Eingaben, die Auswertung erfolgt im Controller.

### Konkretisierungen / Designentscheidungen

- Die ursprünglich denkbare separate Komponente (z. B. `MisuseDetector`) wurde zugunsten einer **vereinfachten Lösung im `CooktopController`** nicht eingeführt.  
  - Vorteil: weniger Komplexität, direkter Zugriff auf Zustände und Ergebnisse.  
  - Nachteil: Fehlbedienungslogik ist mit der Steuerlogik gekoppelt, später ggf. auslagerbar.

- Die Fehlbedienungserkennung arbeitet mit einem **einfachen Zähler pro Zone** und einem Schwellwert (z. B. ab 3 Fehlversuchen).  

Insgesamt bleibt die Implementierung im Rahmen der geplanten Architektur und ergänzt diese um eine klar abgegrenzte Verantwortung: **Erkennen und Melden von Fehlbedienungen**.

---

## 7. Erkenntnisse aus Sprint 3 (Retrospektive)

### 7.1 Positiv aufgefallene Punkte

#### Erweiterbarkeit der bestehenden Architektur
Die in Sprint 1 und Sprint 2 etablierte Architektur konnte ohne strukturelle Änderungen
um die Fehlbedienungserkennung erweitert werden.  
Der `CooktopController` blieb die zentrale Instanz für Steuerungsentscheidungen,
wodurch neue Logik (z. B. Fehlbedienungszähler, Warnmeldungen) konsistent
integriert werden konnte.

#### Verbesserte Benutzerführung durch Warnmeldungen
Die zusätzliche Unterscheidung zwischen Fehlermeldungen (`showError(...)`)
und Warnmeldungen (`showWarning(...)`) führte zu einer klareren und besser
nachvollziehbaren Rückmeldung für den Benutzer, insbesondere bei wiederholten
Fehlbedienungen.

#### Automatisierte Tests ohne externes Framework
Obwohl ursprünglich der Einsatz eines klassischen Testframeworks
(z. B. JUnit) vorgesehen war, konnte durch eine selbst implementierte Testlogik
eine zuverlässige automatisierte **PASS/FAIL-Auswertung** realisiert werden.  
Dieses Vorgehen erwies sich als praktikabel und ermöglichte eine konsistente
Testdurchführung über alle drei Sprints hinweg.

#### Erfolgreiche Regressionstests
Die erneute Ausführung der Tests aus Sprint 1 (`Test_Sprint1.java`) und
Sprint 2 (`Test_Sprint2.java`) bestätigte, dass die Erweiterungen aus Sprint 3
keine negativen Auswirkungen auf bestehende Funktionen
(Leistungsregelung, Timer, Kindersicherung) hatten.

### 7.2 Herausforderungen und Verbesserungspotenziale

#### Einsatz eines Testframeworks nicht möglich
Der Versuch, die Tests mit einem etablierten Testframework umzusetzen,
scheiterte an der Projekt- und Toolkonfiguration.  
Dies erforderte eine Umstellung auf eine eigenständige Testimplementierung,
die mit zusätzlichem Entwicklungsaufwand verbunden war.

#### Abhängigkeit von Konsolenausgaben
Obwohl die Tests automatisiert ausgeführt werden, basiert ein Teil der Bewertung
weiterhin auf der Interpretation von Konsolenausgaben
(z. B. Warn- und Fehlmeldungen).  
Diese Vorgehensweise ist weniger robust als formale Assertions auf
Systemzustände oder Rückgabewerte.

#### Einfache Fehlbedienungslogik
Die aktuelle Fehlbedienungserkennung basiert auf einfachen Bedingungen
(z. B. Zone inaktiv, Kindersicherung aktiv) und Zählwerten.  
Für ein reales Produkt wären komplexere Regeln und differenziertere
Auswertungen sinnvoll.

### 7.3 Konsequenzen für zukünftige Erweiterungen

#### Weiterentwicklung der Testautomatisierung
Langfristig ist eine erneute Integration eines Testframeworks sinnvoll,
um Tests noch klarer, robuster und wartbarer zu gestalten.  
Die in Sprint 3 entwickelte eigene Teststruktur bildet jedoch bereits
eine solide Grundlage für automatisierte Regressionstests.

#### Mögliche Modularisierung der Fehlbedienungslogik
Bei wachsender Komplexität könnte die Fehlbedienungserkennung aus dem
`CooktopController` in eine eigene Komponente ausgelagert werden.  
Die aktuelle Lösung ist bewusst einfach gehalten und gut nachvollziehbar.

#### Konsequente Pflege der Traceability
Die enge Verknüpfung von Requirements, Design, Implementierung und Tests
hat sich erneut bewährt.  
Auch zukünftige Erweiterungen sollten konsequent über die
Traceability-Matrix und die Sprint-Dokumentation nachvollziehbar gemacht werden.

