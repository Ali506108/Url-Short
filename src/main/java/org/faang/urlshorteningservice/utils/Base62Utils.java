package org.faang.urlshorteningservice.utils;

import java.util.UUID;

public class Base62Utils {


    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String encode(long value) {
        StringBuilder builder = new StringBuilder();

        while(value > 0) {
            int index = (int) (value % 62);
            builder.append(BASE62.charAt(index));
            value /= 62;
        }
        return builder.toString();
    }


    public static String uuidToBase62() {
        UUID uuid = UUID.randomUUID();
        long hash = Math.abs(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        return encode(hash);

    }
}
