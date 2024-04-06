package mars.util;

import mars.Application;

import java.awt.*;
import java.util.Arrays;
	
	/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Specialized Font class designed to be used by both the
 * settings menu methods and the Settings class.
 *
 * @author Pete Sanderson
 * @version July 2007
 */
public class EditorFont {
    /**
     * Enumeration of the available font styles in the editor.
     */
    public enum Style {
        PLAIN("Plain", Font.PLAIN),
        BOLD("Bold", Font.BOLD),
        ITALIC("Italic", Font.ITALIC),
        BOLD_ITALIC("Bold + Italic", Font.BOLD | Font.ITALIC);

        public static final Style DEFAULT = PLAIN;

        private final String name;
        private final int flags;

        Style(String name, int flags) {
            this.name = name;
            this.flags = flags;
        }

        public String getName() {
            return this.name;
        }

        public int getFlags() {
            return this.flags;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static final int MIN_SIZE = 6;
    public static final int MAX_SIZE = 72;
    public static final int DEFAULT_SIZE = 12;

    /**
     * Names of fonts in 3 categories that are common to major Java platforms: Win, Mac, Linux.
     * <ul>
     *     <li>Monospace: Courier New and Lucida Sans Typewriter</li>
     *     <li>Serif: Georgia, Times New Roman</li>
     *     <li>Sans Serif: Ariel, Verdana</li>
     * </ul>
     * This is according to lists published by www.codestyle.org.
     */
    private static final String[] ALL_COMMON_FAMILIES = {
        "Arial",
        "Courier New",
        "Georgia",
        "Lucida Sans Typewriter",
        "Times New Roman",
        "Verdana",
    };

    /**
     * Names of fonts from {@link #ALL_COMMON_FAMILIES} which are guaranteed to
     * be available at runtime, as they have been checked against the local
     * GraphicsEnvironment.
     */
    private static final String[] COMMON_FAMILIES = actualCommonFamilies();

    /**
     * Obtain an array of common font family names.  These are guaranteed to
     * be available at runtime, as they were checked against the local
     * GraphicsEnvironment.
     *
     * @return Array of strings, each is a common and available font family name.
     */
    public static String[] getCommonFamilies() {
        return COMMON_FAMILIES;
    }

    /**
     * Obtain an array of all available font family names.  These are guaranteed to
     * be available at runtime, as they come from the local GraphicsEnvironment.
     *
     * @return Array of strings, each is an available font family name.
     */
    public static String[] getAllFamilies() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }

    /**
     * Given the name of a font style, returns the corresponding {@link Style}.
     * The style name is not case-sensitive.
     *
     * @param name The name of the font style.
     * @return The style with the name given.  If the name does not correspond
     *     to a valid style, {@link Style#DEFAULT} is returned.
     */
    public static Style getStyle(String name) {
        for (Style style : Style.values()) {
            if (style.getName().equalsIgnoreCase(name)) {
                return style;
            }
        }
        return Style.DEFAULT;
    }

    /**
     * Given an int field of flags that represents a font style from the {@link Font} class,
     * returns the corresponding {@link Style}.
     *
     * @param flags A combination of style flags from the {@link Font} class.
     * @return The style corresponding to the flags given.  If the flags do not correspond
     *     to a valid style, {@link Style#DEFAULT} is returned.
     */
    public static Style getStyle(int flags) {
        for (Style style : Style.values()) {
            if (style.getFlags() == flags) {
                return style;
            }
        }
        return Style.DEFAULT;
    }

    /**
     * Given an int representing font size, returns corresponding string.
     *
     * @param size Int representing size.
     * @return String value of parameter, unless it is less than MIN_SIZE (returns MIN_SIZE
     *     as String) or greater than MAX_SIZE (returns MAX_SIZE as String).
     */
    public static String sizeIntToSizeString(int size) {
        return String.valueOf(Math.max(MIN_SIZE, Math.min(MAX_SIZE, size)));
    }

    /**
     * Given a String representing font size, returns corresponding int.
     *
     * @param size String representing size.
     * @return int value of parameter, unless it is less than MIN_SIZE (returns
     *     MIN_SIZE) or greater than MAX_SIZE (returns MAX_SIZE).  If the string
     *     cannot be parsed as a decimal integer, it returns DEFAULT_SIZE.
     */
    public static int sizeStringToSizeInt(String size) {
        try {
            return Math.max(MIN_SIZE, Math.min(MAX_SIZE, Integer.parseInt(size)));
        }
        catch (NumberFormatException ignored) {
            return DEFAULT_SIZE;
        }
    }

    /**
     * Creates a new Font object based on the given String specifications.  This
     * is different than Font's constructor, which requires ints for style and size.
     * It assures that defaults and size limits are applied when necessary.
     *
     * @param family String containing font family.
     * @param style  String containing font style.  A list of available styles can
     *               be obtained from getFontStyleStrings().  The default style
     *               is substituted if necessary.
     * @param size   String containing font size.  The defaults and limits of
     *               sizeStringToSizeInt() are substituted if necessary.
     */
    public static Font createFontFromStringValues(String family, String style, String size) {
        return new Font(family, getStyle(style).getFlags(), sizeStringToSizeInt(size));
    }

    /**
     * Handy utility to produce a string that substitutes spaces for all tab characters
     * in the given string.  The number of spaces generated is based on the position of
     * the tab character and the editor's current tab size setting.
     *
     * @param string The original string
     * @return New string in which spaces are substituted for tabs
     * @throws NullPointerException if string is null
     */
    public static String substituteSpacesForTabs(String string) {
        return substituteSpacesForTabs(string, Application.getSettings().editorTabSize.get());
    }

    /**
     * Handy utility to produce a string that substitutes spaces for all tab characters
     * in the given string.  The number of spaces generated is based on the position of
     * the tab character and the specified tab size.
     *
     * @param string  The original string
     * @param tabSize The number of spaces each tab character represents
     * @return New string in which spaces are substituted for tabs
     * @throws NullPointerException if string is null
     */
    public static String substituteSpacesForTabs(String string, int tabSize) {
        if (string.indexOf('\t') == -1) {
            return string;
        }
        StringBuilder result = new StringBuilder(string);
        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == '\t') {
                char[] substitute = new char[tabSize - (i % tabSize)];
                Arrays.fill(substitute, ' ');
                result.replace(i, i + 1, new String(substitute));
                i += substitute.length - 1;
            }
        }
        return result.toString();
    }

    private static String[] actualCommonFamilies() {
        String[] result = new String[ALL_COMMON_FAMILIES.length];
        String[] availableFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        Arrays.sort(availableFamilies); // not sure if necessary; is the list already alphabetical?
        int foundCount = 0;
        for (String family : ALL_COMMON_FAMILIES) {
            if (Arrays.binarySearch(availableFamilies, family) >= 0) {
                result[foundCount++] = family;
            }
        }
        // If not all are found, create a new array with only the ones that are.
        if (foundCount < result.length) {
            String[] trimmedResult = new String[foundCount];
            System.arraycopy(result, 0, trimmedResult, 0, foundCount);
            return trimmedResult;
        }
        else {
            return result;
        }
    }
}

