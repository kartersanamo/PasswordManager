package com.kartersanamo.passwordManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private static final SecureRandom random = new SecureRandom();

    public static class PasswordOptions {
        private int length = 16;
        private boolean useUppercase = true;
        private boolean useLowercase = true;
        private boolean useDigits = true;
        private boolean useSpecial = true;

        public PasswordOptions() {}

        public PasswordOptions setLength(int length) {
            this.length = Math.max(4, Math.min(128, length)); // Between 4 and 128
            return this;
        }

        public PasswordOptions setUseUppercase(boolean use) {
            this.useUppercase = use;
            return this;
        }

        public PasswordOptions setUseLowercase(boolean use) {
            this.useLowercase = use;
            return this;
        }

        public PasswordOptions setUseDigits(boolean use) {
            this.useDigits = use;
            return this;
        }

        public PasswordOptions setUseSpecial(boolean use) {
            this.useSpecial = use;
            return this;
        }
    }

    public static String generate(PasswordOptions options) {
        // Build character pool
        StringBuilder charPool = new StringBuilder();
        List<String> requiredChars = new ArrayList<>();

        if (options.useUppercase) {
            charPool.append(UPPERCASE);
            requiredChars.add(String.valueOf(UPPERCASE.charAt(random.nextInt(UPPERCASE.length()))));
        }
        if (options.useLowercase) {
            charPool.append(LOWERCASE);
            requiredChars.add(String.valueOf(LOWERCASE.charAt(random.nextInt(LOWERCASE.length()))));
        }
        if (options.useDigits) {
            charPool.append(DIGITS);
            requiredChars.add(String.valueOf(DIGITS.charAt(random.nextInt(DIGITS.length()))));
        }
        if (options.useSpecial) {
            charPool.append(SPECIAL);
            requiredChars.add(String.valueOf(SPECIAL.charAt(random.nextInt(SPECIAL.length()))));
        }

        // Must have at least one character type
        if (charPool.isEmpty()) {
            charPool.append(LOWERCASE);
            requiredChars.add(String.valueOf(LOWERCASE.charAt(random.nextInt(LOWERCASE.length()))));
        }

        // Generate password
        StringBuilder password = new StringBuilder();

        // Add required characters first (at least one from each selected type)
        for (String reqChar : requiredChars) {
            password.append(reqChar);
        }

        // Fill remaining length with random characters
        String pool = charPool.toString();
        for (int i = requiredChars.size(); i < options.length; i++) {
            password.append(pool.charAt(random.nextInt(pool.length())));
        }

        // Shuffle the password to randomize positions
        List<Character> chars = new ArrayList<>();
        for (char c : password.toString().toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars, random);

        StringBuilder result = new StringBuilder();
        for (char c : chars) {
            result.append(c);
        }

        return result.toString();
    }

    public static String generateDefault() {
        return generate(new PasswordOptions());
    }

    public static String generateStrong() {
        return generate(new PasswordOptions()
            .setLength(20)
            .setUseUppercase(true)
            .setUseLowercase(true)
            .setUseDigits(true)
            .setUseSpecial(true));
    }

    public static String generateSimple() {
        return generate(new PasswordOptions()
            .setLength(12)
            .setUseUppercase(true)
            .setUseLowercase(true)
            .setUseDigits(true)
            .setUseSpecial(false));
    }

    public static String generatePin(int length) {
        return generate(new PasswordOptions()
            .setLength(length)
            .setUseUppercase(false)
            .setUseLowercase(false)
            .setUseDigits(true)
            .setUseSpecial(false));
    }
}

