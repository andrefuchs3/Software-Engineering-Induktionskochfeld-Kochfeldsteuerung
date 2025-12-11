package core;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import hmi.HmiOutput;
import power.PowerControl;
import safety.SafetyManager;
import util.Types.ZoneID;

/**
 * Zentrale Steuerlogik:
 * - prüft Kindersicherung
 * - delegiert an ZoneManager / PowerControl / TimerManager
 * - aktualisiert die Anzeige
 * - erkennt wiederholte Fehlbedienungen (Sprint 3, F-14)
 */
public class CooktopController {

    private final ZoneManager zones = new ZoneManager();
    private final PowerControl power = new PowerControl();
    private final TimerManager timers = new TimerManager();   // Timerverwaltung (Sprint 2)
    private final HmiOutput out;

    // Fehlbedienungserkennung (Sprint 3, F-14):
    // zählt pro Zone, wie oft eine ungültige Aktion versucht wurde
    private final Map<ZoneID, Integer> invalidOperationCount =
            new EnumMap<ZoneID, Integer>(ZoneID.class);

    public CooktopController(HmiOutput out) {
        this.out = out;
    }

    // -------------------------------------------------------------------------
    // Sprint 1 – bestehende Funktionen
    // -------------------------------------------------------------------------

    /** Zonenaktivierung/-deaktivierung (F-01, F-02, F-06) */
    public void setZoneActive(ZoneID z, boolean active) {
        if (SafetyManager.getInstance().isLocked()) {
            out.showError("Bedienung gesperrt");
            out.showLock(true);
            registerInvalidOperation(z, "Zone kann nicht aktiviert/deaktiviert werden (Kindersicherung aktiv).");
            return;
        }

        zones.setActive(z, active);
        out.showActiveZone(z, active);

        // Bei erfolgreicher, gültiger Bedienung Fehlbedienungszähler für diese Zone zurücksetzen
        resetInvalidOperationCount(z);

        // Leistungsstufe anzeigen, wenn Zone aktiv ist (F-07)
        if (active) {
            out.showPowerLevel(z, power.getLevel(z));
        } else {
            // Timer beim Deaktivieren zurücksetzen (F-09/F-12)
            timers.cancelTimer(z);
            out.showTimer(z, 0);
        }
    }

    /** Leistungsstufe + (F-03/F-04, F-07) */
    public void increasePower(ZoneID z) {
        if (!preCheck(z)) return;
        power.increaseLevel(z);
        out.showPowerLevel(z, power.getLevel(z));
        resetInvalidOperationCount(z);
    }

    /** Leistungsstufe - (F-03/F-04, F-07) */
    public void decreasePower(ZoneID z) {
        if (!preCheck(z)) return;
        power.decreaseLevel(z);
        out.showPowerLevel(z, power.getLevel(z));
        resetInvalidOperationCount(z);
    }

    /** Direkte Stufe setzen (intern/optional) */
    public void setPowerLevel(ZoneID z, int level) {
        if (!preCheck(z)) return;
        power.setLevel(z, level);
        out.showPowerLevel(z, power.getLevel(z));
        resetInvalidOperationCount(z);
    }

    /** Kindersicherung toggeln (F-13, NF-02/NF-03) */
    public void toggleChildLock() {
        SafetyManager sm = SafetyManager.getInstance();
        if (sm.isLocked()) {
            sm.unlockInput();
        } else {
            sm.lockInput();
        }
        out.showLock(sm.isLocked());
    }

    /**
     * Basisprüfung:
     * - Kindersicherung
     * - Zone aktiv
     * - Fehlbedienungserkennung bei Verstößen (F-14)
     */
    private boolean preCheck(ZoneID z) {
        if (SafetyManager.getInstance().isLocked()) {
            out.showError("Bedienung gesperrt");
            out.showLock(true);
            registerInvalidOperation(z, "Eingabe bei aktiver Kindersicherung.");
            return false;
        }
        if (!zones.isActive(z)) {
            out.showError("Zone nicht aktiv");
            registerInvalidOperation(z, "Leistungsänderung an inaktiver Zone.");
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Sprint 2 – Timerfunktionen (F-09, F-10, F-11, F-12, F-08)
    // -------------------------------------------------------------------------

    /** Timer für eine aktive Zone setzen oder ändern. */
    public void setTimer(ZoneID z, int seconds) {
        if (!preCheck(z)) return;
        if (seconds <= 0) {
            out.showError("Timerdauer muss > 0 sein");
            registerInvalidOperation(z, "Timerdauer <= 0 angegeben.");
            return;
        }
        timers.startTimer(z, seconds);
        out.showTimer(z, seconds);
        resetInvalidOperationCount(z);
    }

    /** Timeränderung ist intern dasselbe wie setzen. */
    public void changeTimer(ZoneID z, int seconds) {
        setTimer(z, seconds);
    }

    /** Timer für eine Zone abbrechen. */
    public void cancelTimer(ZoneID z) {
        timers.cancelTimer(z);
        out.showTimer(z, 0);
        resetInvalidOperationCount(z);
    }

    /**
     * Simulierter Zeit-Tick (z. B. 1 Sekunde).
     * Wird z. B. von HmiInput.tickTimer() aufgerufen.
     */
    public void handleTimerTick() {
        List<ZoneID> expired = timers.tick();

        for (ZoneID z : expired) {
            // Auto-Deaktivierung der Zone (F-10)
            zones.setActive(z, false);
            power.setLevel(z, 0);

            out.showActiveZone(z, false);
            out.showPowerLevel(z, 0);

            // Visuelle & "akustische" Rückmeldung (F-11)
            out.showTimerExpired(z);
            out.beep();
        }
    }

    // -------------------------------------------------------------------------
    // Sprint 3 – Hilfsmethoden für Fehlbedienungserkennung (F-14)
    // -------------------------------------------------------------------------

    /**
     * Registriert eine Fehlbedienung für eine Zone und gibt ab einem
     * Schwellwert eine Warnung über HMI aus.
     */
    private void registerInvalidOperation(ZoneID zone, String reason) {
        if (zone == null) {
            // allgemeine Warnung ohne konkrete Zone möglich
            out.showWarning(reason);
            return;
        }

        Integer current = invalidOperationCount.get(zone);
        if (current == null) {
            current = Integer.valueOf(0);
        }
        current = Integer.valueOf(current.intValue() + 1);
        invalidOperationCount.put(zone, current);

        // Schwellwert für "Fehlbedienungserkennung" (z. B. ab 3 Versuchen)
        if (current.intValue() >= 3) {
            out.showWarning("Mehrfache Fehlbedienung an Zone " + zone + ": " + reason);
        }
    }

    /** Setzt den Fehlbedienungszähler für eine Zone zurück (bei erfolgreicher Bedienung). */
    private void resetInvalidOperationCount(ZoneID zone) {
        if (zone != null) {
            invalidOperationCount.remove(zone);
        }
    }
}
