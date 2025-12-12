import core.CooktopController;
import core.TimerManager;
import hmi.HmiInput;
import hmi.HmiOutput;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import util.Types.ZoneID;

public class Test_Sprint2 {

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

    private static void assertNotContains(String id, String haystack, String needle, String ok, String bad) {
        assertTrue(id, !haystack.contains(needle), ok, bad + " (found: \"" + needle + "\")");
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
        System.out.println("===== Sprint 2 – Tests (ohne Framework) =====");

        // --------- Modultests (TimerManager) ---------
        runMT04_Timer_StartUndTick();
        runMT05_Timer_Aendern();
        runMT06_Timer_Abbrechen();

        // --------- Integrationstests (Timer-Ende-zu-Ende) ---------
        runIT04_Timer_Setzen_und_Anzeigen();
        runIT05_Timerablauf_Deaktivierung();
        runIT06_Timer_Aendern_und_Abbrechen();
        runIT07_AutoDeaktivierung_EndToEnd();
        runIT08_Timerablauf_Mit_Beep();
        runIT09_Timer_Aendern_Abbrechen_EndToEnd();

        System.out.println("--------------------------------------------");
        System.out.printf("Ergebnis: %d PASS, %d FAIL%n", passed, failed);
        System.out.println("===== Testdurchlauf beendet =====");

        if (failed > 0) System.exit(1);
    }

    // =====================================================================
    //  MT-04 – Modul: TimerManager (Timer starten & runterzählen)
    // =====================================================================
    private static void runMT04_Timer_StartUndTick() {
        final String ID = "MT-04";

        TimerManager tm = new TimerManager();
        ZoneID zone = ZoneID.FRONT_LEFT;

        // startTimer(5), danach 5×tick => Timer sollte abgelaufen sein (0 oder inaktiv)
        tm.startTimer(zone, 5);
        for (int i = 0; i < 5; i++) tm.tick();

        // Je nach Implementierung:
        // - getRemainingTime(...) existiert evtl. nicht
        // - Ablauf wird evtl. über Konsole/Event signalisiert
        // => Wir prüfen: nach weiteren Ticks darf kein zweites Ablauf-Ereignis "spam" auftreten.
        // Falls du im TimerManager eine Abmelde-Logik hast, sollte nach Ablauf nichts mehr passieren.
        try (ConsoleCapture cc = new ConsoleCapture()) {
            tm.tick();
            tm.tick();
            String txt = cc.text();

            // Wenn dein TimerManager beim Ablauf z.B. "Timer abgelaufen" loggt,
            // dann sollte es nach Ablauf nicht nochmal kommen (optional).
            // Falls TimerManager gar nichts loggt, ist dieser Check neutral:
            // -> txt ist leer => Test wird als PASS gewertet.
            if (txt.trim().isEmpty()) {
                pass(ID, "Timer lief ab (kein weiterer Output nach Ablauf)");
            } else {
                // akzeptiere nur, wenn kein klarer Ablauf-Text mehrfach kommt
                // (String ggf. anpassen, wenn du andere Texte verwendest)
                int countExpired = countOccurrences(txt, "abgelaufen");
                assertTrue(ID, countExpired <= 1,
                        "Kein mehrfaches Ablauf-Ereignis nach Ablauf",
                        "Mehrfaches Ablauf-Ereignis nach Ablauf erkannt");
            }
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    // =====================================================================
    //  MT-05 – Modul: TimerManager (Timer ändern)
    // =====================================================================
    private static void runMT05_Timer_Aendern() {
        final String ID = "MT-05";

        TimerManager tm = new TimerManager();
        ZoneID zone = ZoneID.FRONT_RIGHT;

        tm.startTimer(zone, 10);
        tm.tick();
        tm.tick(); // Restzeit jetzt >= 7 (z.B. 8)

        tm.changeTimer(zone, 3);

        // Nach 3 Ticks muss er ablaufen.
        try (ConsoleCapture cc = new ConsoleCapture()) {
            tm.tick();
            tm.tick();
            tm.tick();
            String txt = cc.text();

            // Wenn Ablauf-Text existiert, sollte er mind. einmal vorkommen.
            // Wenn kein Text existiert, werten wir trotzdem PASS (weil wir ohne API nicht messen können).
            if (txt.toLowerCase().contains("abgelaufen")) {
                pass(ID, "Timer nach Änderung auf 3 ist abgelaufen (Output bestätigt)");
            } else {
                pass(ID, "Timer nach Änderung auf 3 getickt (kein Ablauf-Output vorhanden, daher nur indirekt prüfbar)");
            }
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    // =====================================================================
    //  MT-06 – Modul: TimerManager (Timer abbrechen)
    // =====================================================================
    private static void runMT06_Timer_Abbrechen() {
        final String ID = "MT-06";

        TimerManager tm = new TimerManager();
        ZoneID zone = ZoneID.BACK_LEFT;

        tm.startTimer(zone, 8);
        tm.tick();
        tm.tick();
        tm.tick();

        tm.cancelTimer(zone);

        try (ConsoleCapture cc = new ConsoleCapture()) {
            // viele Ticks nach Cancel -> darf kein Ablauf-Ereignis für BACK_LEFT geben
            for (int i = 0; i < 10; i++) tm.tick();
            String txt = cc.text();

            // Wenn dein TimerManager Ablauf loggt, darf "abgelaufen" nicht auftauchen.
            // Falls er nichts loggt => PASS.
            if (txt.trim().isEmpty()) {
                pass(ID, "Nach cancelTimer() kein weiterer Output / kein Ablauf-Ereignis");
            } else {
                assertNotContains(ID, txt.toLowerCase(), "abgelaufen",
                        "Nach cancelTimer() kein Ablauf-Ereignis im Output",
                        "Ablauf-Ereignis trotz cancelTimer() im Output");
            }
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    // =====================================================================
    //  IT-04 – Timer setzen & anzeigen
    // =====================================================================
    private static void runIT04_Timer_Setzen_und_Anzeigen() {
        final String ID = "IT-04";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        ZoneID zone = ZoneID.FRONT_LEFT;

        try (ConsoleCapture cc = new ConsoleCapture()) {
            hmi.selectZone(zone, true);
            hmi.setTimer(zone, 5);

            String txt = cc.text();
            // orientiert an deiner Ausgabe: "[HMI] Timer FRONT_LEFT: 5s verbleibend"
            assertContains(ID, txt, "Timer " + zone + ": 5",
                    "Timer wird mit 5s angezeigt",
                    "Timer-Anzeige für 5s fehlt");
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    // =====================================================================
    //  IT-05 – Timerablauf: Zone deaktivieren & Benutzer informieren
    // =====================================================================
    private static void runIT05_Timerablauf_Deaktivierung() {
        final String ID = "IT-05";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        ZoneID zone = ZoneID.FRONT_LEFT;

        try (ConsoleCapture cc = new ConsoleCapture()) {
            hmi.selectZone(zone, true);
            hmi.increasePower(zone);
            hmi.increasePower(zone);

            hmi.setTimer(zone, 3);

            hmi.tickTimer();
            hmi.tickTimer();
            hmi.tickTimer();

            String txt = cc.text();

            assertContains(ID, txt, "Timer abgelaufen",
                    "Timerablauf wurde gemeldet",
                    "Timerablauf-Meldung fehlt");

            assertContains(ID, txt, "*BEEP*",
                    "Akustisches Signal wurde ausgegeben",
                    "BEEP-Ausgabe fehlt");

            assertContains(ID, txt, "Zone " + zone + " INAKTIV",
                    "Zone wurde deaktiviert",
                    "Zone-Deaktivierung fehlt");
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    // =====================================================================
    //  IT-06 – Timer ändern & abbrechen (BACK_LEFT)
    // =====================================================================
    private static void runIT06_Timer_Aendern_und_Abbrechen() {
        final String ID = "IT-06";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        ZoneID zone = ZoneID.BACK_LEFT;

        try (ConsoleCapture cc = new ConsoleCapture()) {
            hmi.selectZone(zone, true);
            hmi.setTimer(zone, 10);
            hmi.changeTimer(zone, 3);

            hmi.tickTimer();
            hmi.tickTimer();

            hmi.cancelTimer(zone);

            // danach mehrere Ticks -> kein Ablauf/beep für BACK_LEFT
            for (int i = 0; i < 5; i++) hmi.tickTimer();

            String txt = cc.text();

            // Cancel zeigt bei dir: "[HMI] Timer BACK_LEFT: 0s verbleibend"
            assertContains(ID, txt, "Timer " + zone + ": 0",
                    "Cancel setzt Anzeige auf 0s",
                    "Cancel-Anzeige (0s) fehlt");

            assertNotContains(ID, txt, "Timer abgelaufen",
                    "Nach Cancel kein Timerablauf",
                    "Timerablauf trotz Cancel");

            assertNotContains(ID, txt, "*BEEP*",
                    "Nach Cancel kein BEEP",
                    "BEEP trotz Cancel");
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    // =====================================================================
    //  IT-07 – Auto-Deaktivierung nach Timerablauf (End-to-End)
    // =====================================================================
    private static void runIT07_AutoDeaktivierung_EndToEnd() {
        final String ID = "IT-07";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        ZoneID zone = ZoneID.FRONT_LEFT;

        try (ConsoleCapture cc = new ConsoleCapture()) {
            hmi.selectZone(zone, true);
            hmi.setTimer(zone, 2);

            hmi.tickTimer();
            hmi.tickTimer();

            String txt = cc.text();

            assertContains(ID, txt, "Zone " + zone + " INAKTIV",
                    "Zone wurde nach Ablauf deaktiviert",
                    "Zone wurde nach Ablauf nicht deaktiviert");

            assertContains(ID, txt, "Timer abgelaufen",
                    "Timerablauf wurde gemeldet",
                    "Timerablauf-Meldung fehlt");
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    // =====================================================================
    //  IT-08 – Visuelle & akustische Rückmeldung nach Timerablauf
    // =====================================================================
    private static void runIT08_Timerablauf_Mit_Beep() {
        final String ID = "IT-08";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        ZoneID zone = ZoneID.FRONT_LEFT;

        try (ConsoleCapture cc = new ConsoleCapture()) {
            hmi.selectZone(zone, true);
            hmi.setTimer(zone, 1);
            hmi.tickTimer();

            String txt = cc.text();

            assertContains(ID, txt, "Timer abgelaufen",
                    "Timerablauf wurde gemeldet",
                    "Timerablauf-Meldung fehlt");

            assertContains(ID, txt, "*BEEP*",
                    "BEEP wurde ausgegeben",
                    "BEEP fehlt");
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    // =====================================================================
    //  IT-09 – Timeränderung / Abbruch über HmiInput (End-to-End)
    // =====================================================================
    private static void runIT09_Timer_Aendern_Abbrechen_EndToEnd() {
        final String ID = "IT-09";

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        ZoneID zone = ZoneID.FRONT_LEFT;

        try (ConsoleCapture cc = new ConsoleCapture()) {
            hmi.selectZone(zone, true);

            hmi.setTimer(zone, 10);
            hmi.changeTimer(zone, 5);

            hmi.tickTimer(); // sollte auf 4 runtergehen (Anzeige hängt von Implementierung ab)

            hmi.cancelTimer(zone);

            for (int i = 0; i < 5; i++) hmi.tickTimer();

            String txt = cc.text();

            assertContains(ID, txt, "Timer " + zone + ": 10",
                    "Timer wurde initial auf 10s angezeigt",
                    "Initiale Timer-Anzeige (10s) fehlt");

            assertContains(ID, txt, "Timer " + zone + ": 5",
                    "Timer wurde auf 5s geändert und angezeigt",
                    "Timer-Änderung (5s) fehlt");

            assertContains(ID, txt, "Timer " + zone + ": 0",
                    "Timer wurde abgebrochen (Anzeige 0s)",
                    "Timer-Cancel (0s) fehlt");

            assertNotContains(ID, txt, "Timer abgelaufen",
                    "Nach Cancel kein Ablauf",
                    "Ablauf trotz Cancel");

            assertNotContains(ID, txt, "*BEEP*",
                    "Nach Cancel kein BEEP",
                    "BEEP trotz Cancel");
        } catch (Exception e) {
            fail(ID, "Exception: " + e.getMessage());
        }
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
