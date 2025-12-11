package safety;

import hmi.HmiOutput;

/**
 * Einfache Fehlbedienungserkennung (F-14).
 * Zählt ungültige Bedienaktionen und zeigt ab einem Schwellwert eine Warnung an.
 */
public class MisuseDetector {

    private static final MisuseDetector INSTANCE = new MisuseDetector();

    private int invalidOperationCount = 0;
    private final int threshold = 3;

    private MisuseDetector() {
        // Singleton
    }

    public static MisuseDetector getInstance() {
        return INSTANCE;
    }

    /**
     * Registrierung einer ungültigen Bedienaktion.
     * Bei Erreichen des Schwellwerts wird eine Warnung über HmiOutput ausgegeben.
     */
    public void registerInvalidOperation(String reason, HmiOutput out) {
        invalidOperationCount++;

        // Optional: Grund in der Konsole protokollieren
        out.showWarning("Fehlbedienung registriert: " + reason
                + " (Zähler=" + invalidOperationCount + ")");

        if (invalidOperationCount >= threshold) {
            out.showWarning("Fehlbedienung erkannt: Bitte Bedienung prüfen.");
            invalidOperationCount = 0; // Zähler zurücksetzen
        }
    }
}
