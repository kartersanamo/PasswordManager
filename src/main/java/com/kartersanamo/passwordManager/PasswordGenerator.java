package com.kartersanamo.passwordManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    // API configuration
    private static final String WORDS_API_URL = "https://wordsapiv1.p.rapidapi.com/words/?random=true";
    private static final String API_HOST = "wordsapiv1.p.rapidapi.com";
    private static final String API_KEY = "a4ce2c21aemsh88f73c145ad8d30p198ca0jsne194d342c655";

    // Fallback word list (used if API fails)
    private static final String[] FALLBACK_WORD_LIST = {
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

    /**
     * Fetches a random word from WordsAPI.
     * Falls back to local word list if API call fails.
     */
    private static String getRandomWord() {
        try {
            URL url = new URL(WORDS_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-rapidapi-host", API_HOST);
            conn.setRequestProperty("x-rapidapi-key", API_KEY);
            conn.setConnectTimeout(2000); // 2 second timeout
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                String word = jsonResponse.getString("word");

                // Validate word (only letters, reasonable length)
                if (word != null && word.matches("[a-zA-Z]{3,10}")) {
                    return word.toLowerCase();
                }
            }
        } catch (Exception e) {
            // API failed, will use fallback
            System.err.println("WordsAPI call failed, using fallback: " + e.getMessage());
        }

        // Fallback to local word list
        return FALLBACK_WORD_LIST[random.nextInt(FALLBACK_WORD_LIST.length)];
    }

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

        public void setUseSpecial(boolean use) {
            this.useSpecial = use;
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

            // Get random word from API (or fallback)
            String word = getRandomWord();

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
}