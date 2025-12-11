import core.CooktopController;
import hmi.HmiInput;
import hmi.HmiOutput;
import safety.SafetyManager;
import util.Types.ZoneID;

public class Sprint3_App {

    public static void main(String[] args) {
        System.out.println("===== Sprint 3 – Demo Fehlbedienungserkennung =====");

        HmiOutput out = new HmiOutput();
        CooktopController ctl = new CooktopController(out);
        HmiInput hmi = new HmiInput(ctl);

        // Zone absichtlich NICHT aktivieren → Fehlbedienung
        System.out.println("\n--- Szenario 1: Leistungsänderung auf inaktiver Zone ---");
        hmi.increasePower(ZoneID.FRONT_LEFT);
        hmi.increasePower(ZoneID.FRONT_LEFT);
        hmi.increasePower(ZoneID.FRONT_LEFT); // ab hier sollte Warnung erscheinen

        // Kindersicherung aktiv → weitere Fehlbedienungen
        System.out.println("\n--- Szenario 2: Bedienung bei aktiver Kindersicherung ---");
        hmi.selectZone(ZoneID.FRONT_LEFT, true);
        hmi.toggleChildLock(); // Lock EIN
        hmi.increasePower(ZoneID.FRONT_LEFT);
        hmi.decreasePower(ZoneID.FRONT_LEFT);

        // Aufräumen
        SafetyManager.getInstance().unlockInput();

        System.out.println("\n===== Demo beendet =====");
    }
}
