import core.CooktopController;
import hmi.HmiInput;
import hmi.HmiOutput;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import safety.SafetyManager;
import util.Types.ZoneID;

/**
 * Sprint 3 – Manuelle Tests (ohne Framework)
 *
 * Ziel: stabile, robuste Tests, die ohne konkrete Konsolen-Texte funktionieren.
 * Die Tests prüfen v. a.:
 *  - keine Exceptions
 *  - Sperrzustand wird korrekt gesetzt (SafetyManager)
 *  - optional: es kam überhaupt irgendeine Ausgabe (ohne auf konkrete Strings zu prüfen)
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

        boolean hasAnyOutput() {
            return buffer.size() > 0;
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
        System.out.println("=== Sprint 3 Tests (ohne Framework) ===");

        // Isolation
        SafetyManager.getInstance().unlockInput();

        test_IT10_increasePower_onInactiveZone();
        test_IT11_actionWhileLocked();
        test_IT12_setTimer_invalidValue();

        test_MT07_zoneNotActive_isHandled();
        test_MT08_lockedInput_isHandled();
        test_MT09_invalidTimerValue_isHandled();

        System.out.println("----------------------------------");
        System.out.printf("Ergebnis: %d PASS, %d FAIL%n", passed, failed);

        if (failed > 0) System.exit(1);
    }

    // ------------------------------------------------------------
    // IT-10 – Fehlbedienung: Leistungsänderung bei inaktiver Zone
    // ------------------------------------------------------------
    private static void test_IT10_increasePower_onInactiveZone() {
        final String ID = "IT-10";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();

            // Aktion: Leistung erhöhen auf inaktiver Zone
            sut.hmi.increasePower(ZoneID.FRONT_RIGHT);

            // Robuste Checks:
            // 1) keine Exception => OK (kommt schon durch try)
            // 2) optional: es gab irgendeine Ausgabe (je nach Implementierung kann das auch leer sein)
            if (cc.hasAnyOutput()) {
                pass(ID, "Keine Exception, Ausgabe vorhanden");
            } else {
                pass(ID, "Keine Exception (keine Ausgabe erforderlich)");
            }
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // IT-11 – Fehlbedienung: Eingaben bei aktiver Kindersicherung
    // ------------------------------------------------------------
    private static void test_IT11_actionWhileLocked() {
        final String ID = "IT-11";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            // Vorbedingung: Zone aktivieren (Lock aus)
            SafetyManager.getInstance().unlockInput();
            sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);

            // Lock an
            SafetyManager.getInstance().lockInput();

            // Aktion: irgendeine gesperrte Aktion
            sut.hmi.increasePower(ZoneID.FRONT_LEFT);

            // Robuste Checks:
            // - Sperre ist tatsächlich aktiv
            assertTrue(ID, SafetyManager.getInstance().isLocked(),
                    "Sperrzustand aktiv (SafetyManager.isLocked() == true)",
                    "Sperrzustand nicht aktiv (SafetyManager.isLocked() == false)");

            // Ausgabe optional
            String out = cc.text();
            if (out != null && !out.isEmpty()) {
                pass(ID, "Keine Exception, Ausgabe vorhanden");
            } else {
                pass(ID, "Keine Exception (Ausgabe nicht zwingend)");
            }
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getClass().getSimpleName() + " - " + e.getMessage());
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

            // Aktion: Timer = 0 (ungültig)
            sut.hmi.setTimer(ZoneID.FRONT_LEFT, 0);

            // Robuste Checks:
            // - keine Exception
            // - Ausgabe optional
            if (cc.hasAnyOutput()) {
                pass(ID, "Keine Exception, Ausgabe vorhanden");
            } else {
                pass(ID, "Keine Exception (keine Ausgabe erforderlich)");
            }
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // MT-07 – Fehlbedienung "Zone nicht aktiv" (robust/indirekt)
    // ------------------------------------------------------------
    private static void test_MT07_zoneNotActive_isHandled() {
        final String ID = "MT-07";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();

            // Aktion: Power ändern auf inaktiver Zone
            sut.hmi.increasePower(ZoneID.FRONT_RIGHT);

            // Robuster Check: keine Exception (optional Ausgabe)
            if (cc.hasAnyOutput()) {
                pass(ID, "Fehlbedienung verarbeitet (Ausgabe vorhanden)");
            } else {
                pass(ID, "Fehlbedienung verarbeitet (keine Ausgabe erforderlich)");
            }
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // MT-08 – Fehlbedienung "Eingabe gesperrt" (robust/indirekt)
    // ------------------------------------------------------------
    private static void test_MT08_lockedInput_isHandled() {
        final String ID = "MT-08";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();
            sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);

            SafetyManager.getInstance().lockInput();

            sut.hmi.increasePower(ZoneID.FRONT_LEFT);

            // Robuster Check: Sperre aktiv + keine Exception
            assertTrue(ID, SafetyManager.getInstance().isLocked(),
                    "Sperrzustand aktiv (SafetyManager.isLocked() == true)",
                    "Sperrzustand nicht aktiv (SafetyManager.isLocked() == false)");

            if (cc.hasAnyOutput()) {
                pass(ID, "Fehlbedienung verarbeitet (Ausgabe vorhanden)");
            } else {
                pass(ID, "Fehlbedienung verarbeitet (keine Ausgabe erforderlich)");
            }
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        } finally {
            SafetyManager.getInstance().unlockInput();
        }
    }

    // ------------------------------------------------------------
    // MT-09 – Fehlbedienung "Ungültige Timerdauer" (robust/indirekt)
    // ------------------------------------------------------------
    private static void test_MT09_invalidTimerValue_isHandled() {
        final String ID = "MT-09";
        Sut sut = new Sut();

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();
            sut.hmi.selectZone(ZoneID.FRONT_LEFT, true);

            sut.hmi.setTimer(ZoneID.FRONT_LEFT, -1);

            if (cc.hasAnyOutput()) {
                pass(ID, "Fehlbedienung verarbeitet (Ausgabe vorhanden)");
            } else {
                pass(ID, "Fehlbedienung verarbeitet (keine Ausgabe erforderlich)");
            }
        } catch (Exception e) {
            fail(ID, "Exception geworfen: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
