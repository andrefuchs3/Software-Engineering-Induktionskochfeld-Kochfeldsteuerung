import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import core.CooktopController;
import hmi.HmiInput;
import hmi.HmiOutput;
import safety.SafetyManager;
import util.Types.ZoneID;

/**
 * Sprint 3 – Manuelle Tests (ohne JUnit)
 * Enthält:
 *  - MT-07..MT-09 (MisuseDetector indirekt über Konsole / keine Exceptions)
 *  - IT-10..IT-12 (End-to-End über HMI/Controller)
 *
 * Annahme: MisuseDetector löst am Ende HmiOutput.showWarning(...) aus.
 * Wenn deine Warnungs-Texte anders heißen, passe die contains()-Strings unten an.
 */
public class Test_Sprint3 {

    // -------- Mini Test Framework --------
    private static int passed = 0;
    private static int failed = 0;

    private static void pass(String id, String msg) {
        passed++;
        System.out.printf("[PASS] %s: %s%n", id, msg);
    }

    private static void fail(String id, String msg) {
        failed++;
        System.out.printf("[FAIL] %s: %s%n", id, msg);
    }

    private static void assertTrue(String id, boolean cond, String ok, String bad) {
        if (cond) pass(id, ok);
        else fail(id, bad);
    }

    private static void assertContains(String id, String haystack, String needle, String ok, String bad) {
        assertTrue(id, haystack.contains(needle), ok, bad + " (missing: \"" + needle + "\")");
    }

    // -------- Helper: Console capture --------
    private static class ConsoleCapture implements AutoCloseable {
        private final PrintStream originalOut = System.out;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        ConsoleCapture() {
            System.setOut(new PrintStream(buffer));
        }

        String text() {
            return buffer.toString();
        }

        @Override
        public void close() {
            System.setOut(originalOut);
        }
    }

    // -------- Test Setup --------
    private static class Sut {
        final HmiOutput out;
        final CooktopController ctl;
        final HmiInput hmi;

        Sut() {
            out = new HmiOutput();
            ctl = new CooktopController(out);
            hmi = new HmiInput(ctl);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Sprint 3 Tests (ohne JUnit) ===");

        // Isolation: Lock immer definiert setzen
        SafetyManager.getInstance().unlockInput();

        test_IT10_increasePower_onInactiveZone();
        test_IT11_actionWhileLocked();
        test_IT12_setTimer_invalidValue();

        // MT-07..MT-09: ohne direkten Zugriff auf MisuseDetector:
        // Wir prüfen: keine Exception + Warnung/Fehlbedienung erscheint.
        // (Wenn du MisuseDetector direkt testen willst, sag mir kurz sein public API,
        // dann schreibe ich dir 1:1 direkte MT-Tests.)
        test_MT07_zoneNotActive_isRegistered();
        test_MT08_lockedInput_isRegistered();
        test_MT09_invalidTimerValue_isRegistered();

        System.out.println("----------------------------------");
        System.out.printf("Ergebnis: %d PASS, %d FAIL%n", passed, failed);

        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------
    // IT-10 – Fehlbedienung: Leistungsänderung bei inaktiver Zone
    // ------------------------------------------------------------
    private static void test_IT10_increasePower_onInactiveZone() {
        final String ID = "IT-10";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();

            // Vorbedingung: FRONT_RIGHT inaktiv (Default)
            // Aktion:
            sut.hmi.increasePower(ZoneID.FRONT_RIGHT);

            String out = cc.text();

            // Erwartung: Warnung + keine Leistungsänderung
            // -> Wir können Leistung nicht direkt auslesen (PowerControl ist intern),
            //    daher prüfen wir indirekt über Ausgabe.
            // Passe diese Strings an deine echten Texte an.
            assertContains(ID, out, "Warn", "Warnung wurde ausgegeben", "Keine Warnung ausgegeben");
            assertContains(ID, out, "Zone", "Meldung bezieht sich auf Zone", "Keine zonenbezogene Meldung");
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // IT-11 – Fehlbedienung: Eingaben bei aktiver Kindersicherung
    // ------------------------------------------------------------
    private static void test_IT11_actionWhileLocked() {
        final String ID = "IT-11";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            // Zone aktivieren (Lock aus), dann Lock an
            SafetyManager.getInstance().unlockInput();
            sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);

            SafetyManager.getInstance().lockInput();

            // Aktion: z.B. Timer setzen oder Power erhöhen
            sut.hmi.increasePower(ZoneID.FRONT_LEFT);

            String out = cc.text();

            // Erwartung: Fehler "Bedienung gesperrt" + Warnung (Misuse)
            assertContains(ID, out, "Bedienung gesperrt",
                    "Fehlermeldung 'Bedienung gesperrt' angezeigt",
                    "Fehlermeldung 'Bedienung gesperrt' fehlt");

            assertContains(ID, out, "Warn",
                    "Warnung zur Fehlbedienung ausgegeben",
                    "Warnung zur Fehlbedienung fehlt");
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getMessage());
        } finally {
            SafetyManager.getInstance().unlockInput();
        }
    }

    // ------------------------------------------------------------
    // IT-12 – Fehlbedienung: Ungültige Timerdauer (0/negativ)
    // ------------------------------------------------------------
    private static void test_IT12_setTimer_invalidValue() {
        final String ID = "IT-12";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();
            sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);

            // Aktion: Timer = 0
            sut.hmi.setTimer(ZoneID.FRONT_LEFT, 0);

            String out = cc.text();

            // Erwartung: Fehler + Warnung; kein Timer angelegt (prüfen wir indirekt: keine "Timer ...: 0s verbleibend" o.ä. als Erfolg)
            assertContains(ID, out, "Timer",
                    "Timer-Fehlerpfad wurde ausgelöst",
                    "Keine Timer-bezogene Ausgabe");
            assertContains(ID, out, "muss",
                    "Fehlermeldung enthält Constraint",
                    "Fehlermeldung ist nicht spezifisch genug");

            assertContains(ID, out, "Warn",
                    "Warnung zur Fehlbedienung ausgegeben",
                    "Warnung zur Fehlbedienung fehlt");
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // MT-07 – MisuseDetector: Zone nicht aktiv (indirekt)
    // ------------------------------------------------------------
    private static void test_MT07_zoneNotActive_isRegistered() {
        final String ID = "MT-07";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();

            // Aktion: Power ändern auf inaktiver Zone -> sollte Misuse registrieren
            sut.hmi.increasePower(ZoneID.FRONT_RIGHT);

            String out = cc.text();

            // Erwartung: Warnung/Fehlbedienung (indirekt)
            assertContains(ID, out, "Warn",
                    "Misuse (Warnung) wurde registriert/ausgegeben",
                    "Misuse-Warnung fehlt");
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // MT-08 – MisuseDetector: Eingabe gesperrt (indirekt)
    // ------------------------------------------------------------
    private static void test_MT08_lockedInput_isRegistered() {
        final String ID = "MT-08";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();
            sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);

            SafetyManager.getInstance().lockInput();

            sut.hmi.increasePower(ZoneID.FRONT_LEFT);

            String out = cc.text();

            assertContains(ID, out, "Bedienung gesperrt",
                    "Sperr-Fehler wurde ausgegeben",
                    "Sperr-Fehler fehlt");

            assertContains(ID, out, "Warn",
                    "Misuse (Warnung) wurde registriert/ausgegeben",
                    "Misuse-Warnung fehlt");
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getMessage());
        } finally {
            SafetyManager.getInstance().unlockInput();
        }
    }

    // ------------------------------------------------------------
    // MT-09 – MisuseDetector: Ungültiger Timerwert (indirekt)
    // ------------------------------------------------------------
    private static void test_MT09_invalidTimerValue_isRegistered() {
        final String ID = "MT-09";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();
            sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);

            sut.hmi.setTimer(ZoneID.FRONT_LEFT, -1);

            String out = cc.text();

            assertContains(ID, out, "Timer",
                    "Timer-Fehlerpfad wurde ausgelöst",
                    "Keine Timer-bezogene Ausgabe");

            assertContains(ID, out, "Warn",
                    "Misuse (Warnung) wurde registriert/ausgegeben",
                    "Misuse-Warnung fehlt");
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getMessage());
        }
    }
}
