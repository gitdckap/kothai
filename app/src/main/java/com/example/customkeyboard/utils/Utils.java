package com.example.customkeyboard.utils;


import static android.content.Context.WINDOW_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;

import com.example.customkeyboard.model.Emoji;
import com.example.customkeyboard.model.KeyboardKeyState;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class Utils {

    public static List<String> cachedEnglishCsv;
    public static List<String> cachedTamilCsv;
    public static List<Emoji> cachedEmojiCsv;
    public static boolean isLongPress = false;
    public static boolean isSelectAll = false;
    public static String[] browserList = {
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.UCMobile.intl",
            "com.cloudmosa.puffinFree",
            "com.kiwibrowser.browser",
            "com.yandex.browser",
            "org.torproject.torbrowser",
            "com.microsoft.bing"
    };

    public static String[] giphyCategories = {
            "Trending",
            "Good Morning",
            "Good Night",
            "Love You",
            "Hugs",
            "Angry",
            "Emoji",
            "Sad",
            "Happy",
            "Wow",
            "Sorry",
            "Thanks",
            "Bye",
            "Hi",
            "Wink",
            "Yes",
    };

    public static List<Character> specialCharacters = Arrays.asList('ை', 'ே', 'ெ');  // 'ை', 'ா', 'ே', 'ெ', 'ூ', 'ு', 'ீ', 'ி', 'ௌ'
    public static List<String> specialLetter = Arrays.asList("க்ஷெ", "க்ஷே", "க்ஷை"); //"க்ஷி", "க்ஷீ", "க்ஷு", "க்ஷூ", "க்ஷா", "க்ஷெ", "க்ஷே", "க்ஷை", "க்ஷ்", "க்ஷொ", "க்ஷோ"

    public static String getWordAtCursor(InputConnection inputConnection) {
        if (inputConnection == null) {
            return null;
        }
        CharSequence beforeCursor = inputConnection.getTextBeforeCursor(50, 0);
        CharSequence afterCursor = inputConnection.getTextAfterCursor(50, 0);

        if (beforeCursor == null || afterCursor == null) {
            return null;
        }

        String before = beforeCursor.toString();
        String after = afterCursor.toString();

        // Find the word boundaries
        int start = before.lastIndexOf(' ') + 1; // or use regex to handle punctuation
        int end = after.indexOf(' '); // or use regex to handle punctuation
        if (end == -1) {
            end = after.length();
        }
        // Extract the word
        return before.substring(start) + after.substring(0, end);
    }

    private static final String EMOJI_PATTERN = "[\\p{So}\\p{Sc}\\p{Sk}\\p{Sm}\\p{Cs}]|" + // Match most symbols
            "(?:\\uD83C[\\uDDE6-\\uDDFF]){2}|" +    // Flags (regional indicator symbols)
            "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]|" +   // Surrogate pair emojis
            "[\\u2600-\\u26FF\\u2700-\\u27BF]|" +   // Miscellaneous symbols (stars, circles, zodiac)
            "[\\u2300-\\u23FF\\u2B50]|" +           // Stars and shapes
            "[\\uFE00-\\uFE0F]|" +                  // Variation Selectors
            "[\\u200D]";                            // Zero Width Joiner (used in emojis with skin tones or sequences)

    public static boolean containsEmoji(String text) {
        Pattern pattern = Pattern.compile(EMOJI_PATTERN);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    public static List<String> initializeCSV(Context context) {
        if (cachedTamilCsv != null) {
            return cachedTamilCsv;
        }
        List<String> tamilCSV = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("tamil_suggestions.csv")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                tamilCSV.add(line.trim().split(",")[0]);
            }
        } catch (IOException e) {
            Timber.e(e, "Error reading CSV file");
        }
        cachedTamilCsv = tamilCSV;
        return cachedTamilCsv;
    }

    public static List<String> initializeEnglishCSV(Context context) {
        if (cachedEnglishCsv != null) {
            return cachedEnglishCsv;
        }
        List<String> englishCsv = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("english_suggestions.csv")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                englishCsv.add(line.trim());
            }
        } catch (IOException e) {
            Timber.e(e, "Error reading CSV file");
        }
        cachedEnglishCsv = englishCsv;
        return cachedEnglishCsv;
    }

    public static List<Emoji> initializeEmojiCSV(Context context) {
        if (cachedEmojiCsv != null) {
            return cachedEmojiCsv;
        }
        try {
            InputStreamReader english_is = new InputStreamReader(context.getAssets().open("emojis.csv"));
            BufferedReader reader = new BufferedReader(english_is);
            String line;
            List<Emoji> emojiList = new ArrayList<>();
            List<String> smile_people = new ArrayList<>();
            List<String> animals_nature = new ArrayList<>();
            List<String> food_drink = new ArrayList<>();
            List<String> activity = new ArrayList<>();
            List<String> travel_places = new ArrayList<>();
            List<String> objects = new ArrayList<>();
            List<String> symbols = new ArrayList<>();
            List<String> flags = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] splitText = line.split(",");
                switch (splitText[0]) {
                    case "Activities":
                        activity.add(splitText[4]);
                        break;
                    case "Animals-Nature":
                        animals_nature.add(splitText[4]);
                        break;
                    case "Flags":
                        flags.add(splitText[4]);
                        break;
                    case "Food-Drink":
                        food_drink.add(splitText[4]);
                        break;
                    case "Objects":
                        objects.add(splitText[4]);
                        break;
                    case "Smileys-Emotion":
                    case "People-Body":
                        smile_people.add(splitText[4]);
                        break;
                    case "Symbols":
                        symbols.add(splitText[4]);
                        break;
                    case "Travel-Places":
                        travel_places.add(splitText[4]);
                        break;
                    default:
                        break;
                }
            }
            emojiList.add(new Emoji("Recent", new ArrayList<>()));
            emojiList.add(new Emoji("Smileys & People", smile_people));
            emojiList.add(new Emoji("Animals & Nature", animals_nature));
            emojiList.add(new Emoji("Food & Drink", food_drink));
            emojiList.add(new Emoji("Activity", activity));
            emojiList.add(new Emoji("Travel & Places", travel_places));
            emojiList.add(new Emoji("Objects", objects));
            emojiList.add(new Emoji("Symbols", symbols));
            emojiList.add(new Emoji("Flags", flags));
            cachedEmojiCsv = emojiList;
            return cachedEmojiCsv;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearGIF(Context context) {
        try {
            File gifFileFolder = new File(context.getFilesDir(), "gifs");
            if (gifFileFolder.exists()) {
                for (File gifFile : Objects.requireNonNull(gifFileFolder.listFiles())) {
                    gifFile.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum VoiceStatus {
        IDLE,
        LISTENING,
        WAIT,
        DEFAULT
    }

    public static Integer[] tamilKeys_1() {
        return new Integer[]{2949, 2950, 2951, 2952, 2953, 2954, 2958, 2959, 2960, 2962, 2963, 44, 2972, 2999, 3000, 3001, -101, -100, 46};
    }

    public static Integer[] tamilKeys_2() {
        return new Integer[]{3014, 3015, 3016, 3021, 3008, 3010, 3009, 3007, 3006};
    }

    public static Integer[] tamilKeys_3() {
        return new Integer[]{2990, 2965, 2984, 2992, 2980, 2986, 2997, 2994, 2979, 2985, 2970, 2969, 2974, 2975, 2996, 2995, 2993, 2991};
    }

    public static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static int spToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().scaledDensity;
        return Math.round(dp * density);
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }


    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
}

/* Emoji delete */
//            int range = 11;
//            for (int i = range; i > 0; i--) {
//                CharSequence sequence = ic.getTextBeforeCursor(i, 0);
//                if (sequence != null) {
//                    s = tempEmoji.stream().filter(string -> string.equals(sequence.toString())).findFirst().orElse(null);
//                    if (s != null) {
//                        ic.deleteSurroundingText(s.length(), 0);
//                        tempEmoji.remove(s);
//                        return;
//                    }
//                }
//            }