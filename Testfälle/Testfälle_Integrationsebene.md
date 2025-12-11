## Testfälle - Integrationsebene

### **IT-01 – HmiInput ↔ CooktopController (Zone aktivieren)**
**Ziel:** Prüfen, ob der Aufruf von `HmiInput` korrekt an den `CooktopController` weitergegeben wird.

|  | Beschreibung |
|-------|--------------|
| **Komponenten** | HmiInput, CooktopController |
| **Vorbedingung** | Ein `CooktopController` ist erstellt und an ein `HmiInput`-Objekt übergeben. Die Zone `FRONT_LEFT` ist zu Beginn inaktiv. |
| **Aktion** | Aufruf von `hmiInput.selectZone(ZoneID.FRONT_LEFT, true)` |
| **Erwartete Reaktion** | `HmiInput` ruft intern `cooktopController.setZoneActive(ZoneID.FRONT_LEFT, true)` auf. Die Zone wird im Controller aktiv gesetzt. |
| **Nachbedingung** | `ZoneManager.isActive(ZoneID.FRONT_LEFT)` meldet **aktiv** |
| **Ergebnis** | Bestanden - Ausgabe in Konsole bestätigt Aktivierung |

---

### **IT-02 – CooktopController ↔ PowerControl (Leistungsstufe erhöhen)**
**Ziel:** Prüfen, ob der Controller die Leistung korrekt über `PowerControl` ändert.

|  | Beschreibung |
|-------|--------------|
| **Komponenten** | CooktopController, PowerControl |
| **Vorbedingung** | `CooktopController` ist instanziiert und besitzt ein internes `PowerControl`. Zone `FRONT_LEFT` ist aktiv, Leistungsstufe = 0. |
| **Aktion** | Aufruf von `cooktopController.increasePower(ZoneID.FRONT_LEFT)` |
| **Erwartete Reaktion** | Der Controller ruft intern `powerControl.increaseLevel(ZoneID.FRONT_LEFT)` auf. Leistungsstufe steigt auf **1**. |
| **Nachbedingung** | `powerControl.getLevel(ZoneID.FRONT_LEFT)` liefert **1** |
| **Ergebnis** | Bestanden - Anzeige zeigt korrekt "Leistungsstufe 1" |

---

### **IT-03 – CooktopController ↔ SafetyManager (Kindersicherung blockiert Änderungen)**
**Ziel:** Prüfen, ob der Controller die Kindersicherung über `SafetyManager` korrekt abfragt und Aktionen blockiert.

|  | Beschreibung |
|-------|--------------|
| **Komponenten** | CooktopController, SafetyManager |
| **Vorbedingung** | `CooktopController` ist instanziiert. `SafetyManager.getInstance()` existiert. Zone `FRONT_LEFT` aktiv, Leistungsstufe = 0. Kindersicherung = aus. |
| **Aktion** | 1. `cooktopController.toggleChildLock()` → ruft `SafetyManager.lockInput()` auf. <br>2. Danach `cooktopController.increasePower(ZoneID.FRONT_LEFT)` |
| **Erwartete Reaktion** | 1. `toggleChildLock()` aktiviert Sperre über `lockInput()`. <br>2. Beim Erhöhen prüft der Controller `isLocked()` und **bricht ab** – kein `increaseLevel()` mehr. |
| **Nachbedingung** | `SafetyManager.isLocked()` ist **true**. Leistungsstufe bleibt **0**. |
| **Ergebnis** | Bestanden - Konsole zeigt Blockierung + Fehlermeldung |

---

### IT-04 – HmiInput ↔ CooktopController ↔ TimerManager (Timer setzen & anzeigen)

**Ziel:** Prüfen, ob das Setzen eines Timers vom Benutzer bis in den `TimerManager` durchgereicht wird und korrekt auf der Anzeige erscheint.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Komponenten        | `HmiInput`, `CooktopController`, `TimerManager`, `HmiOutput`                 |
| Vorbedingung       | Kochfeld ist initialisiert. Zone `FRONT_LEFT` ist aktiv. Kein Timer gesetzt. |
| Aktion             | Aufruf von `hmi.setTimer(FRONT_LEFT, 5)`.                                   |
| Erwartete Reaktion | `HmiInput` ruft `CooktopController.setTimer(FRONT_LEFT, 5)` auf. Der Controller delegiert an `TimerManager.startTimer(...)` und aktualisiert über `HmiOutput.showTimer(FRONT_LEFT, 5)` die Anzeige. |
| Nachbedingung      | Timer für `FRONT_LEFT` ist im `TimerManager` mit Restzeit 5 registriert; Anzeige zeigt den gesetzten Timerwert. |
| Ergebnis           | Bestanden |

---

### IT-05 – CooktopController ↔ TimerManager ↔ ZoneManager/HmiOutput (Timerablauf)

**Ziel:** Prüfen, ob beim Ablauf des Timers die Zone deaktiviert und der Benutzer informiert wird.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Komponenten        | `CooktopController`, `TimerManager`, `ZoneManager`, `HmiOutput`              |
| Vorbedingung       | Zone `FRONT_LEFT` ist aktiv, Leistungsstufe > 0, Timer für diese Zone läuft mit Restzeit 3. Kindersicherung ist **aus**. |
| Aktion             | 3× `handleTimerTick()` im Controller aufrufen (z. B. via `hmi.tickTimer()`). |
| Erwartete Reaktion | Der `TimerManager` meldet Ablauf für `FRONT_LEFT`. Der `CooktopController` setzt `ZoneManager.setActive(FRONT_LEFT, false)`, setzt die Leistungsstufe auf 0 zurück und ruft `HmiOutput.showTimerExpired(FRONT_LEFT)` sowie `HmiOutput.beep()` auf. |
| Nachbedingung      | Zone `FRONT_LEFT` ist inaktiv, Leistungsstufe = 0; Anzeige zeigt abgelaufenen Timer, akustisches Signal wurde ausgegeben. |
| Ergebnis           | Bestanden |

---

### IT-06 – HmiInput ↔ CooktopController ↔ TimerManager (Timer ändern & abbrechen)

**Ziel:** Prüfen, ob Änderungen und Abbruch eines Timers korrekt verarbeitet werden.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Komponenten        | `HmiInput`, `CooktopController`, `TimerManager`, `HmiOutput`                 |
| Vorbedingung       | Zone `BACK_LEFT` ist aktiv. Timer wurde mit `setTimer(BACK_LEFT, 10)` gesetzt. |
| Aktion             | 1. `hmi.changeTimer(BACK_LEFT, 3)` aufrufen. 2. 2× `hmi.tickTimer()` ausführen. 3. `hmi.cancelTimer(BACK_LEFT)` aufrufen. 4. Weitere 5× `hmi.tickTimer()` ausführen. |
| Erwartete Reaktion | 1. Restzeit im `TimerManager` wird auf 3 gesetzt und über `showTimer(...)` angezeigt. 2. Nach 2 Ticks ist Restzeit = 1. 3. Nach `cancelTimer(...)` wird der Timer gelöscht; Anzeige kann aktualisiert werden (z. B. kein Timer mehr). 4. Es tritt **kein** Ablauf-Ereignis und kein `showTimerExpired(...)`/`beep()` für `BACK_LEFT` auf. |
| Nachbedingung      | Timer für `BACK_LEFT` ist inaktiv; keine Deaktivierung der Zone aufgrund des abgebrochenen Timers. |
| Ergebnis           | Bestanden |

---

### IT-07 – Auto-Deaktivierung nach Timerablauf (End-to-End)

**Ziel:** Prüfen, ob nach Ablauf des Timers die Kochzone automatisch deaktiviert wird, wenn der Timer über `HmiInput` gesetzt wurde.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| **Komponenten**    | `HmiInput`, `CooktopController`, `TimerManager`, `ZoneManager`, `HmiOutput` |
| **Vorbedingung**   | Zone `FRONT_LEFT` ist aktiv, Timer ist noch **nicht** gesetzt. Kindersicherung ist **aus**. |
| **Aktion**         | 1. `hmi.selectZone(FRONT_LEFT, true)` (falls noch nicht aktiv). <br>2. `hmi.setTimer(FRONT_LEFT, 2)` aufrufen. <br>3. 2× `hmi.tickTimer()` ausführen. |
| **Erwartete Reaktion** | Nach dem letzten Tick meldet der `TimerManager` den Ablauf. Der `CooktopController` setzt `ZoneManager.setActive(FRONT_LEFT, false)` und die Leistungsstufe auf 0 zurück; die Statusanzeige wird aktualisiert (z. B. AUS/Restwärme). |
| **Nachbedingung**  | Zone `FRONT_LEFT` ist inaktiv, Leistungsstufe = 0; HMI zeigt den deaktivierten Zustand nach Timerablauf. |
| **Ergebnis**       | Bestanden |

---

### IT-08 – Visuelle & akustische Rückmeldung nach Timerablauf

**Ziel:** Prüfen, ob nach Ablauf des Timers eine visuelle und akustische Rückmeldung über `HmiOutput` erfolgt.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| **Komponenten**    | `HmiInput`, `CooktopController`, `TimerManager`, `HmiOutput`                 |
| **Vorbedingung**   | Zone `FRONT_LEFT` ist aktiv, Timer noch nicht gesetzt, Kindersicherung = aus. |
| **Aktion**         | 1. `hmi.setTimer(FRONT_LEFT, 1)` aufrufen. <br>2. 1× `hmi.tickTimer()` ausführen. |
| **Erwartete Reaktion** | Beim Ablauf ruft der Controller `HmiOutput.showTimerExpired(FRONT_LEFT)` und anschließend `HmiOutput.beep()` auf. |
| **Nachbedingung**  | In der Konsole ist eine Meldung zum abgelaufenen Timer sichtbar; zusätzlich wurde ein akustisches Signal (beep) ausgegeben. |
| **Ergebnis**       | Bestanden |

---

### IT-09 – Timeränderung / Abbruch über HmiInput (End-to-End)

**Ziel:** Prüfen, ob ein bereits gesetzter Timer über `HmiInput` geändert und anschließend abgebrochen werden kann, ohne dass ein Ablaufereignis ausgelöst wird.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| **Komponenten**    | `HmiInput`, `CooktopController`, `TimerManager`, `HmiOutput`                 |
| **Vorbedingung**   | Zone `FRONT_LEFT` ist aktiv. Es ist **kein** Timer gesetzt. Kindersicherung = aus. |
| **Aktion**         | 1. `hmi.setTimer(FRONT_LEFT, 10)` aufrufen. <br>2. `hmi.changeTimer(FRONT_LEFT, 5)` aufrufen. <br>3. 1× `hmi.tickTimer()` ausführen. <br>4. `hmi.cancelTimer(FRONT_LEFT)` aufrufen. <br>5. Mehrfach `hmi.tickTimer()` ausführen (z. B. 5×). |
| **Erwartete Reaktion** | 1. Timer wird mit 10 s angelegt und auf der Anzeige dargestellt. <br>2. Restzeit wird auf 5 s gesetzt und aktualisiert angezeigt. <br>3. Nach einem Tick ist die Restzeit reduziert (z. B. 4 s). <br>4. Nach `cancelTimer(...)` wird der Timer im `TimerManager` entfernt; weitere Ticks führen **nicht** zu `showTimerExpired(...)` oder `beep()`. |
| **Nachbedingung**  | Für `FRONT_LEFT` existiert kein aktiver Timer mehr; die Zone bleibt weiterhin aktiv, es gab kein Timerablauf-Ereignis. |
| **Ergebnis**       | Bestanden |

---

### IT-10 – Fehlbedienung: Leistungsänderung bei inaktiver Zone

**Ziel:** Prüfen, ob eine Leistungsänderung auf einer **inaktiven** Zone als Fehlbedienung erkannt und als Warnung ausgegeben wird.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| **Komponenten**    | `HmiInput`, `CooktopController`, `ZoneManager`, `MisuseDetector`, `HmiOutput` |
| **Vorbedingung**   | Kochfeld initialisiert. Zone `FRONT_RIGHT` ist **inaktiv**, Leistungsstufe = 0, Kindersicherung = aus. |
| **Aktion**         | Aufruf von `hmi.increasePower(FRONT_RIGHT)`. |
| **Erwartete Reaktion** | Der `CooktopController` erkennt, dass die Zone nicht aktiv ist, ruft `MisuseDetector.registerInvalidOperation(...)` auf und `HmiOutput.showWarning(...)` meldet eine Fehlbedienung. Es erfolgt keine Änderung an `ZoneManager` oder `PowerControl`. |
| **Nachbedingung**  | `ZoneManager.isActive(FRONT_RIGHT)` bleibt **false**, Leistungsstufe bleibt 0; in der Konsole ist eine Warnmeldung zur Fehlbedienung sichtbar. |
| **Ergebnis**       | – |

---

### IT-11 – Fehlbedienung: Eingaben bei aktiver Kindersicherung

**Ziel:** Prüfen, ob Benutzeraktionen bei aktiver Kindersicherung als Fehlbedienung gewertet und sowohl Fehler- als auch Warnmeldungen ausgelöst werden.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| **Komponenten**    | `HmiInput`, `CooktopController`, `SafetyManager`, `MisuseDetector`, `HmiOutput` |
| **Vorbedingung**   | Zone `FRONT_LEFT` ist aktiv, Leistungsstufe = 0. `SafetyManager.isLocked() = true` (Kindersicherung EIN). Kein Timer gesetzt. |
| **Aktion**         | Aufruf von `hmi.setTimer(FRONT_LEFT, 5)` oder `hmi.increasePower(FRONT_LEFT)`. |
| **Erwartete Reaktion** | Der Controller prüft `SafetyManager.isLocked()` und blockiert die Aktion. `HmiOutput.showError("Bedienung gesperrt")` und `HmiOutput.showLock(true)` werden aufgerufen. Zusätzlich registriert der `MisuseDetector` die Fehlbedienung und löst `HmiOutput.showWarning(...)` aus. |
| **Nachbedingung**  | Leistungsstufe bleibt 0, es wird **kein** Timer gesetzt; Sperrzustand bleibt aktiv; Konsole zeigt Fehler- und Warnmeldung. |
| **Ergebnis**       | – |

---

### IT-12 – Fehlbedienung: Ungültige Timerdauer

**Ziel:** Prüfen, ob eine ungültige Timerdauer (z. B. 0 oder negativer Wert) als Fehlbedienung erkannt und sauber abgefangen wird.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| **Komponenten**    | `HmiInput`, `CooktopController`, `TimerManager`, `MisuseDetector`, `HmiOutput` |
| **Vorbedingung**   | Zone `FRONT_LEFT` ist aktiv, Kindersicherung = aus, es ist kein Timer gesetzt. |
| **Aktion**         | Aufruf von `hmi.setTimer(FRONT_LEFT, 0)` (alternativ ein negativer Wert). |
| **Erwartete Reaktion** | Der `CooktopController` erkennt die ungültige Dauer, ruft `HmiOutput.showError("Timerdauer muss > 0 sein")` auf und registriert die Aktion im `MisuseDetector`, der eine Warnung über `HmiOutput.showWarning(...)` ausgibt. Im `TimerManager` wird **kein** Timer angelegt. |
| **Nachbedingung**  | Für `FRONT_LEFT` existiert weiterhin **kein** aktiver Timer; der Zonenstatus bleibt unverändert; Konsole zeigt Fehler- und Warnmeldung. |
| **Ergebnis**       | – |
