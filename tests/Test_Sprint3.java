import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import core.CooktopController;
import hmi.HmiInput;
import hmi.HmiOutput;
import misuse.MisuseDetector;
import safety.SafetyManager;
import util.Types.ZoneID;

/**
 * Sprint 3 – JUnit Tests
 *
 * Enthält NUR die neuen Sprint-3-Testfälle:
 *  - MT-07..MT-09 (MisuseDetector)
 *  - IT-10..IT-12 (Fehlbedienung End-to-End / Integration)
 *
 * Hinweis:
 * - Diese Tests prüfen erwartete FEHLER/WARNUNG-Ausgaben über die Konsole (HmiOutput).
 * - Dafür wird System.out abgefangen und ausgewertet.
 */
public class Test_Sprint3 {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outBuffer;

    @BeforeEach
    void setup() {
        outBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer));

        // Test-Isolation: Lock immer aus
        SafetyManager.getInstance().unlockInput();
    }

    @AfterEach
    void teardown() {
        SafetyManager.getInstance().unlockInput();
        System.setOut(originalOut);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private String console() {
        return outBuffer.toString();
    }

    private static class SystemUnderTest {
        final HmiOutput out;
        final CooktopController ctl;
        final HmiInput hmi;

        SystemUnderTest(HmiOutput out, CooktopController ctl, HmiInput hmi) {
            this.out = out;
            this.ctl = ctl;
            this.hmi = hmi;
        }
    }

    private SystemUnderTest createSystem() {
        HmiOutput out = new HmiOutput();

        // Erwartung: CooktopController nutzt MisuseDetector intern ODER bekommt ihn injected.
        // Falls dein CooktopController einen Konstruktor (HmiOutput, MisuseDetector) hat, dann hier umstellen.
        CooktopController ctl = new CooktopController(out);

        HmiInput hmi = new HmiInput(ctl);
        return new SystemUnderTest(out, ctl, hmi);
    }

    private void assertContains(String text, String needle) {
        assertTrue(text.contains(needle), "Erwartet, dass Konsole enthält: " + needle + "\n--- Konsole ---\n" + text);
    }

    private void assertNotContains(String text, String needle) {
        assertFalse(text.contains(needle), "Erwartet, dass Konsole NICHT enthält: " + needle + "\n--- Konsole ---\n" + text);
    }

    // ---------------------------------------------------------
    // MT-07 – MisuseDetector: Zone nicht aktiv
    // ---------------------------------------------------------
    @Test
    void MT07_misuseDetector_register_zoneNotActive() {
        MisuseDetector md = new MisuseDetector();

        assertDoesNotThrow(() ->
                md.registerInvalidOperation("ZONE_NOT_ACTIVE", ZoneID.FRONT_RIGHT),
                "registerInvalidOperation sollte keine Exception werfen");

        // Optional/empfohlen: MisuseDetector sollte intern zählbar sein.
        // Wenn du eine API wie getCount(reason) hast, kannst du hier hart prüfen.
        // (Diese Zeilen erst aktivieren, wenn vorhanden.)
        //
        // assertEquals(1, md.getCount("ZONE_NOT_ACTIVE"));
    }

    // ---------------------------------------------------------
    // MT-08 – MisuseDetector: Eingabe gesperrt
    // ---------------------------------------------------------
    @Test
    void MT08_misuseDetector_register_lockedInput() {
        MisuseDetector md = new MisuseDetector();

        assertDoesNotThrow(() ->
                md.registerInvalidOperation("LOCKED_INPUT", ZoneID.FRONT_LEFT),
                "registerInvalidOperation sollte keine Exception werfen");

        // Optional/empfohlen:
        // assertEquals(1, md.getCount("LOCKED_INPUT"));
    }

    // ---------------------------------------------------------
    // MT-09 – MisuseDetector: Ungültige Timerdauer
    // ---------------------------------------------------------
    @Test
    void MT09_misuseDetector_register_invalidTimerValue() {
        MisuseDetector md = new MisuseDetector();

        assertDoesNotThrow(() ->
                md.registerInvalidOperation("INVALID_TIMER_VALUE", ZoneID.FRONT_LEFT),
                "registerInvalidOperation sollte keine Exception werfen");

        // Optional/empfohlen:
        // assertEquals(1, md.getCount("INVALID_TIMER_VALUE"));
    }

    // ---------------------------------------------------------
    // IT-10 – Fehlbedienung: Leistungsänderung bei inaktiver Zone
    // ---------------------------------------------------------
    @Test
    void IT10_increasePower_onInactiveZone_isMisuse_andNoChange() {
        SystemUnderTest sut = createSystem();

        // Vorbedingung: Kochfeld initialisiert, FRONT_RIGHT inaktiv, lock aus
        SafetyManager.getInstance().unlockInput();

        // Aktion
        sut.hmi.increasePower(ZoneID.FRONT_RIGHT);

        String c = console();

        // Erwartet: Warnung (Fehlbedienung) und KEINE Leistungsänderung
        // (Passe Strings an deine tatsächlichen Texte an: showWarning(...) etc.)
        assertContains(c, "WARN");                 // z.B. "[HMI] WARNUNG: ..."
        assertContains(c, "Fehlbedien");           // z.B. "Fehlbedienung erkannt"
        assertNotContains(c, "Zone FRONT_RIGHT -> Leistungsstufe 1");
    }

    // ---------------------------------------------------------
    // IT-11 – Fehlbedienung: Eingaben bei aktiver Kindersicherung
    // ---------------------------------------------------------
    @Test
    void IT11_actionWhileLocked_isError_andMisuseWarning() {
        SystemUnderTest sut = createSystem();

        // Vorbedingung: FRONT_LEFT aktiv, lock = true
        sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);
        SafetyManager.getInstance().lockInput();

        // Aktion: z.B. Leistung erhöhen (alternativ setTimer)
        sut.hmi.increasePower(ZoneID.FRONT_LEFT);

        String c = console();

        // Erwartet: Fehler + Lock-Anzeige + Warnung (Fehlbedienung)
        assertContains(c, "FEHLER");
        assertContains(c, "Bedienung gesperrt");
        assertContains(c, "Kindersicherung");
        assertContains(c, "AKTIV");
        assertContains(c, "WARN");
        assertContains(c, "Fehlbedien");
    }

    // ---------------------------------------------------------
    // IT-12 – Fehlbedienung: Ungültige Timerdauer
    // ---------------------------------------------------------
    @Test
    void IT12_setTimer_invalidValue_isError_andNoTimerCreated() {
        SystemUnderTest sut = createSystem();

        // Vorbedingung: FRONT_LEFT aktiv, lock aus
        sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);
        SafetyManager.getInstance().unlockInput();

        // Aktion: ungültiger Wert (0 oder negativ)
        sut.hmi.setTimer(ZoneID.FRONT_LEFT, 0);

        String c = console();

        // Erwartet: Fehler + Warnung
        assertContains(c, "FEHLER");
        assertContains(c, "Timerdauer");
        assertContains(c, ">");
        assertContains(c, "WARN");
        assertContains(c, "Fehlbedien");

        // Nachbedingung: Kein Timer angelegt (keine "5s verbleibend" o.ä.)
        assertNotContains(c, "Timer FRONT_LEFT: 5s verbleibend");
        assertNotContains(c, "Timer FRONT_LEFT: 1s verbleibend");
    }
}
