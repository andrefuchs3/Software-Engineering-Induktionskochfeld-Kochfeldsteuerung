import core.CooktopController;
import hmi.HmiInput;
import hmi.HmiOutput;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import power.PowerControl;
import safety.SafetyManager;
import util.Types.ZoneID;

public class Test_Sprint1 {

    // -------- Mini Test Framework (ohne Framework) --------
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

    private static void assertEquals(String id, int expected, int actual, String ok, String bad) {
        if (expected == actual) pass(id, ok + " (expected=" + expected + ", actual=" + actual + ")");
        else fail(id, bad + " (expected=" + expected + ", actual=" + actual + ")");
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

    public static void main(String[] args) {
        System.out.println("===== Sprint 1 – Tests (ohne Framework) =====");

        // Isolation: definierter Startzustand
        SafetyManager.getInstance().unlockInput();

        runMT01_PowerControl();
        runMT02_SafetyManager();
        runMT03_ZoneIdEnum();

        runIT01_HmiInput_CooktopController();
        runIT02_CooktopController_PowerControl();
        runIT03_CooktopController_SafetyManager();

        System.out.println("--------------------------------------------");
        System.out.printf("Ergebnis: %d PASS, %d FAIL%n", passed, failed);
        System.out.println("===== Testdurchlauf beendet =====");

        if (failed > 0) System.exit(1);
    }

    // ---------------- Modulebene ----------------

    // MT-01 – PowerControl: Leistungsstufe erhöhen
    private static void runMT01_PowerControl() {
        final String ID = "MT-01";

        PowerControl power = new PowerControl();
        ZoneID zone = ZoneID.FRONT_LEFT;

        for (int i = 0; i < 5; i++) power.increaseLevel(zone);
        int before = power.getLevel(zone);

        power.increaseLevel(zone);
        int after = power.getLevel(zone);

        assertEquals(ID, before + 1, after,
                "Leistungsstufe wurde korrekt erhöht",
                "Leistungsstufe wurde NICHT korrekt erhöht");
    }

    // MT-02 – SafetyManager: Sperren / Entsperren
    private static void runMT02_SafetyManager() {
        final String ID = "MT-02";

        SafetyManager sm = SafetyManager.getInstance();
        sm.unlockInput();

        sm.lockInput();
        boolean locked = sm.isLocked();

        assertTrue(ID, locked,
                "Kindersicherung ist nach lockInput() aktiv",
                "Kindersicherung ist nach lockInput() NICHT aktiv");

        sm.unlockInput();
    }

    // MT-03 – ZoneID Enum: alle Zonen vorhanden?
    private static void runMT03_ZoneIdEnum() {
        final String ID = "MT-03";

        ZoneID[] values = ZoneID.values();
        String all = java.util.Arrays.toString(values);

        boolean ok =
                all.contains("FRONT_LEFT") &&
                all.contains("FRONT_RIGHT") &&
                all.contains("BACK_LEFT") &&
                all.contains("BACK_RIGHT");

        assertTrue(ID, ok,
                "Alle ZoneID Werte vorhanden",
                "ZoneID Werte fehlen: " + all);
    }

    // ---------------- Integrationsebene ----------------

    // IT-01 – HmiInput ↔ CooktopController (Zone aktivieren)
    private static void runIT01_HmiInput_CooktopController() {
        final String ID = "IT-01";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();

            hmi.selectZone(ZoneID.FRONT_LEFT, true);

            String txt = cc.text();
            // orientiert an deiner Ausgabe: "[HMI] Zone FRONT_LEFT AKTIV"
            assertContains(ID, txt, "Zone FRONT_LEFT AKTIV",
                    "HMI meldet Zone aktiv",
                    "HMI meldet Zone NICHT aktiv");
        }
    }

    // IT-02 – CooktopController ↔ PowerControl (Leistungsstufe erhöhen)
    private static void runIT02_CooktopController_PowerControl() {
        final String ID = "IT-02";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();

            hmi.selectZone(ZoneID.FRONT_LEFT, true);
            hmi.increasePower(ZoneID.FRONT_LEFT);

            String txt = cc.text();
            // orientiert an deiner Ausgabe: "[HMI] Zone FRONT_LEFT -> Leistungsstufe 1"
            assertContains(ID, txt, "Leistungsstufe 1",
                    "Leistungsstufe wurde auf 1 erhöht (HMI-Ausgabe)",
                    "Keine HMI-Ausgabe für Leistungsstufe 1 gefunden");
        }
    }

    // IT-03 – CooktopController ↔ SafetyManager (Kindersicherung blockiert)
    private static void runIT03_CooktopController_SafetyManager() {
        final String ID = "IT-03";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        try (ConsoleCapture cc = new ConsoleCapture()) {
            SafetyManager.getInstance().unlockInput();

            hmi.selectZone(ZoneID.FRONT_LEFT, true);
            hmi.toggleChildLock(); // EIN
            hmi.increasePower(ZoneID.FRONT_LEFT); // soll blockiert werden

            String txt = cc.text();

            assertTrue(ID, SafetyManager.getInstance().isLocked(),
                    "SafetyManager ist gesperrt (isLocked()==true)",
                    "SafetyManager ist NICHT gesperrt");

            // orientiert an deiner Ausgabe: "[HMI] FEHLER: Bedienung gesperrt"
            assertContains(ID, txt, "Bedienung gesperrt",
                    "Fehlermeldung bei gesperrter Bedienung erscheint",
                    "Fehlermeldung 'Bedienung gesperrt' fehlt");
        } finally {
            SafetyManager.getInstance().unlockInput();
        }
    }
}
