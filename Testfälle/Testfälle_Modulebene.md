## Testfälle - Modulebene


### **MT-01 – Modul: power (PowerControl)**
**Zweck:** Algorithmische Korrektheit der Leistungsstufen-Logik  

|  | Beschreibung |
|-------|---------------|
| **Modul** | power |
| **Vorbedingung** | Modul *power* ist initialisierbar; `PowerControl` erstellt; Leistungsstufe von `FRONT_LEFT = 5` |
| **Aktion** | `increaseLevel(ZoneID.FRONT_LEFT)` |
| **Erwartete Reaktion** | Stufe steigt auf **6** |
| **Nachbedingung** | `getLevel(FRONT_LEFT)` liefert **6** |
| **Ergebnis** |  Bestanden  |

---

### **MT-02 – Modul: safety (SafetyManager)**
**Zweck:** Korrektes Sperren/Entsperren im Singleton-Modul  

|  | Beschreibung |
|-------|---------------|
| **Modul** | safety |
| **Vorbedingung** | `SafetyManager.getInstance()` existiert; Kindersicherung deaktiviert |
| **Aktion** | `lockInput()` danach `isLocked()` |
| **Erwartete Reaktion** | Rückgabe = **true** |
| **Nachbedingung** | Sperre aktiv; später durch `unlockInput()` wieder deaktivierbar |
| **Ergebnis** |  Bestanden  |

---

### **MT-03 – Modul: util (Types / Enums)**
**Zweck:** Korrekte Auflistung und Existenz aller `ZoneID`-Werte  

|  | Beschreibung |
|-------|---------------|
| **Modul** | util |
| **Vorbedingung** | `Types.ZoneID.values()` existieren |
| **Aktion** | Abfrage aller Enum-Werte |
| **Erwartete Reaktion** | Werte: `FRONT_LEFT`, `FRONT_RIGHT`, `BACK_LEFT`, `BACK_RIGHT` |
| **Nachbedingung** | Enum unverändert und valide |
| **Ergebnis** |  Bestanden - Manuelle Sichtprüfung korrekt  |

---

### MT-04 – Modul: TimerManager (Timer starten & runterzählen)

**Zweck:** Algorithmische Korrektheit beim Starten eines Timers und beim Herunterzählen per `tick()`.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Modul              | timer (TimerManager)                                                         |
| Vorbedingung       | `TimerManager` ist initialisiert. Für Zone `FRONT_LEFT` ist kein Timer aktiv. |
| Aktion             | `startTimer(FRONT_LEFT, 5)` aufrufen, danach 5× `tick()` ausführen.          |
| Erwartete Reaktion | Interne Restzeit wird von 5 auf 0 heruntergezählt. Beim Übergang auf 0 wird ein Ablauf-Ereignis für `FRONT_LEFT` erzeugt (z. B. Rückgabe/Callback). |
| Nachbedingung      | Kein positiver Restwert mehr für `FRONT_LEFT`; Timer für diese Zone gilt als **abgelaufen**. |
| Ergebnis           | Bestanden |

---

### MT-05 – Modul: TimerManager (Timer ändern)

**Zweck:** Änderung eines laufenden Timers (Restzeit anpassen).

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Modul              | timer (TimerManager)                                                         |
| Vorbedingung       | `TimerManager` ist initialisiert. Timer für `FRONT_RIGHT` wurde mit `startTimer(FRONT_RIGHT, 10)` gestartet. Restzeit ist noch ≥ 7 Sekunden. |
| Aktion             | Aufruf von `changeTimer(FRONT_RIGHT, 3)`. Anschließend 3× `tick()` ausführen. |
| Erwartete Reaktion | Restzeit wird unmittelbar auf 3 gesetzt. Nach 3 weiteren `tick()` läuft der Timer ab und erzeugt ein Ablauf-Ereignis für `FRONT_RIGHT`. |
| Nachbedingung      | Timer für `FRONT_RIGHT` ist nach genau 3 Ticks abgelaufen. Kein weiterer Ablauf für diese Zone nach zusätzlichen Ticks. |
| Ergebnis           | Bestanden |

---

### MT-06 – Modul: TimerManager (Timer abbrechen)

**Zweck:** Sicherstellen, dass ein abgebrochener Timer nicht mehr abläuft.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Modul              | timer (TimerManager)                                                         |
| Vorbedingung       | `TimerManager` ist initialisiert. Timer für `BACK_LEFT` wurde mit `startTimer(BACK_LEFT, 8)` gestartet. |
| Aktion             | 3× `tick()` ausführen, danach `cancelTimer(BACK_LEFT)` aufrufen und anschließend weitere 10× `tick()` ausführen. |
| Erwartete Reaktion | Während der ersten 3 Ticks wird die Restzeit reduziert. Nach `cancelTimer(...)` tritt **kein** Ablauf-Ereignis für `BACK_LEFT` mehr auf. |
| Nachbedingung      | Timer für `BACK_LEFT` gilt als **inaktiv**; kein Ablauf-Ereignis wurde nach dem Cancel registriert. |
| Ergebnis           | Bestanden |

---

### MT-07 – Modul: misuse (MisuseDetector) – Fehlbedienung „Zone nicht aktiv“

**Zweck:** Sicherstellen, dass eine Fehlbedienung bei Leistungsänderung auf einer inaktiven Zone als Fehlbedienung erkannt und registriert wird.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Modul              | misuse (MisuseDetector)                                                      |
| Vorbedingung       | `MisuseDetector` ist initialisiert. Es liegt noch **keine** Fehlbedienung für `FRONT_RIGHT` vor. |
| Aktion             | Aufruf von `registerInvalidOperation("ZONE_NOT_ACTIVE", …)` für Zone `FRONT_RIGHT`. |
| Erwartete Reaktion | Die Methode akzeptiert die Fehlbedienung und kennzeichnet sie intern (z. B. Zähler/Log). Optional wird eine Warnung vorbereitet. |
| Nachbedingung      | Die Fehlbedienung „Zone nicht aktiv“ ist im `MisuseDetector` erfasst (z. B. über internen Status oder Statistik). |
| Ergebnis           | Bestanden |

---

### MT-08 – Modul: misuse (MisuseDetector) – Fehlbedienung „Eingabe gesperrt“

**Zweck:** Prüfen, ob Eingaben bei aktiver Kindersicherung als eigene Fehlbedienungskategorie behandelt werden.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Modul              | misuse (MisuseDetector)                                                      |
| Vorbedingung       | `MisuseDetector` ist initialisiert. Ein Reason-Typ für gesperrte Eingaben (z. B. `"LOCKED_INPUT"`) ist definiert/verwendbar. |
| Aktion             | Aufruf von `registerInvalidOperation("LOCKED_INPUT", …)` bei aktiver Kindersicherung. |
| Erwartete Reaktion | Die Fehlbedienung wird registriert; der MisuseDetector unterscheidet sie intern von anderen Gründen (z. B. separater Eintrag/Code). |
| Nachbedingung      | Die Kategorie „LOCKED_INPUT“ ist intern hinterlegt; spätere Auswertungen können zwischen verschiedenen Fehlbedienungen differenzieren. |
| Ergebnis           | Bestanden |

---

### MT-09 – Modul: misuse (MisuseDetector) – Fehlbedienung „Ungültige Timerdauer“

**Zweck:** Sicherstellen, dass ungültige Parameter (z. B. Timerdauer ≤ 0) als Fehlbedienung erfasst werden.

| Punkt              | Beschreibung                                                                 |
|--------------------|------------------------------------------------------------------------------|
| Modul              | misuse (MisuseDetector)                                                      |
| Vorbedingung       | `MisuseDetector` ist initialisiert. Es ist kein Eintrag für „INVALID_TIMER_VALUE“ vorhanden. |
| Aktion             | Aufruf von `registerInvalidOperation("INVALID_TIMER_VALUE", …)` im Kontext eines fehlerhaften Timer-Setzens. |
| Erwartete Reaktion | Die Fehlbedienung wird akzeptiert und intern dokumentiert; es werden keine Ausnahmen geworfen. |
| Nachbedingung      | Die Fehlbedienung „INVALID_TIMER_VALUE“ ist im `MisuseDetector` hinterlegt und kann für Logging/Auswertung genutzt werden. |
| Ergebnis           | Bestanden |
