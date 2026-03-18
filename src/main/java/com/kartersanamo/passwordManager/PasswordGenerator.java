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
    
    // Word list for memorable passwords
    private static final String[] WORD_LIST = {
        "apple", "banana", "cherry", "dragon", "eagle", "falcon", "grape", "horse",
        "island", "jungle", "kitten", "lemon", "mango", "night", "ocean", "panda",
        "queen", "river", "storm", "tiger", "uncle", "valley", "water", "xray",
        "yellow", "zebra", "arrow", "beach", "cloud", "dance", "earth", "fire",
        "green", "happy", "ice", "jazz", "king", "light", "moon", "nature",
        "orange", "peace", "quiet", "rain", "snow", "tree", "urban", "voice",
        "wind", "youth", "anchor", "brave", "craft", "dream", "energy", "flame",
        "glory", "heart", "image", "joker", "knife", "laser", "magic", "noble",
        "orbit", "power", "quest", "rocket", "space", "tower", "unity", "video",
        "wave", "xenon", "yacht", "zero", "alpha", "beta", "gamma", "delta",
        "echo", "foxtrot", "golf", "hotel", "india", "juliet", "kilo", "lima",
        "metro", "nova", "omega", "piano", "quartz", "radio", "sigma", "tango",
        "ultra", "venus", "whiskey", "zulu", "amber", "beaver", "copper", "diver",
        "ember", "frost", "glow", "honor", "ivory", "jewel", "karma", "lotus"
    };

    private static final SecureRandom random = new SecureRandom();

    public static class PasswordOptions {
        private int length = 16;
        private boolean useUppercase = true;
        private boolean useLowercase = true;
        private boolean useDigits = true;
        private boolean useSpecial = true;
        private boolean useWords = false;
        private int wordCount = 4;
        private String wordSeparator = "-";

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

        public PasswordOptions setUseWords(boolean use) {
            this.useWords = use;
            return this;
        }

        public PasswordOptions setWordCount(int count) {
            this.wordCount = Math.max(2, Math.min(8, count));
            return this;
        }

        public PasswordOptions setWordSeparator(String separator) {
            this.wordSeparator = separator;
            return this;
        }
        
        public boolean isUseWords() { return useWords; }
        public int getWordCount() { return wordCount; }
        public String getWordSeparator() { return wordSeparator; }
    }

    public static String generate(PasswordOptions options) {
        if (options.useWords) {
            return generateWordBased(options);
        } else {
            return generateCharacterBased(options);
        }
    }
    
    private static String generateWordBased(PasswordOptions options) {
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < options.wordCount; i++) {
            if (i > 0) {
                password.append(options.wordSeparator);
            }
            
            String word = WORD_LIST[random.nextInt(WORD_LIST.length)];
            
            // Optionally capitalize first letter
            if (options.useUppercase && random.nextBoolean()) {
                word = word.substring(0, 1).toUpperCase() + word.substring(1);
            }
            
            password.append(word);
            
            // Optionally add a digit
            if (options.useDigits && random.nextBoolean()) {
                password.append(random.nextInt(10));
            }
        }
        
        return password.toString();
    }
    
    private static String generateCharacterBased(PasswordOptions options) {
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
    
    public static String generateWordBased() {
        return generate(new PasswordOptions()
            .setUseWords(true)
            .setWordCount(4)
            .setWordSeparator("-")
            .setUseUppercase(true)
            .setUseDigits(true));
    }
}



