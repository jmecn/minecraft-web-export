package io.github.jmecn.minecraftwebexport.export.emi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class RecipeBundleDigest {

    private RecipeBundleDigest() {
    }

    static int sequenceWidth(int fileCount) {
        if (fileCount <= 1) {
            return 3;
        }
        return Math.max(3, String.valueOf(fileCount - 1).length());
    }

    static String stem(int sequence, byte[] fileBytes, int width) {
        String seq = String.format("%0" + width + "d", sequence);
        return seq + "-" + hexPrefix(fileBytes, 6);
    }

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String hexPrefix(byte[] data, int hexChars) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(hash, 0, (hexChars + 1) / 2).substring(0, hexChars);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
