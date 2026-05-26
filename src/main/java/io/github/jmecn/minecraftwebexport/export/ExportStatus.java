package io.github.jmecn.minecraftwebexport.export;

public enum ExportStatus {
    SUCCESS("success"),
    PARTIAL("partial"),
    FAILED("failed");

    private final String wireValue;

    ExportStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
