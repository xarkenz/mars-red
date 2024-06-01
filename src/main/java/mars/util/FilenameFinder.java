package mars.util;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Utility class to perform necessary file-related search
 * operations.  One is to find file names in JAR file,
 * another is to find names of files in given directory
 * of normal file system.
 *
 * @author Pete Sanderson
 * @version October 2006
 */
public class FilenameFinder {
    private static final String JAR_EXTENSION = ".jar";
    private static final String FILE_URL = "file:";
    private static final boolean ALLOW_DIRECTORIES_IN_SEARCH = false;
    public static String MATCH_ALL_EXTENSIONS = "*";

    /**
     * Locate files and return list of file names.  Given a known relative directory path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension. If the "known file path" doesn't work
     * because MARS is running from an executable JAR file, it will locate the
     * directory in the JAR file and proceed from there.  NOTE: since this uses
     * the class loader to get the resource, the directory path needs to be
     * relative to classpath, not absolute.  To work with an arbitrary file system,
     * use the other version of this overloaded method.  Will NOT match directories
     * that happen to have the desired extension.
     *
     * @param classLoader   class loader to use
     * @param directoryPath Search will be confined to this directory.  Use "/" as
     *                      separator but do NOT include starting or ending "/"  (e.g. mars/tools)
     * @param fileExtension Only files with this extension will be added
     *                      to the list.  Do NOT include the "." in extension.
     * @return array list of matching file names as Strings.  If none, list is empty.
     */
    public static List<String> findFilenames(ClassLoader classLoader, String directoryPath, String fileExtension) {
        // Modified by DPS 10-July-2008 to better handle path containing space
        // character (%20) and to hopefully handle path containing non-ASCII
        // characters.  The "toURI()" approach was suggested by MARS user
        // Felipe Lessa and worked for him when running 'java Mars' but it did
        // not work when executing from a jar file 'java -jar Mars.jar'.  I
        // took it from there and discovered that in the latter situation,
        // "toURI()" created a URI prefixed with "jar:" and the "getPath()" in
        // that case returns null! If you strip the "jar:" prefix and create a
        // new URI from the resulting string, it works!  Thanks Felipe!
        //
        // Modified by Ingo Kofler 24-Sept-2009 to handle multiple JAR files.
        // This requires use of ClassLoader.getResources() instead of
        // getResource().  The former will look in all JAR files listed in
        // in the java command.
        //
        // Note: I couldn't see the need to strip the "jar:" schema off of the URI
        // when the File constructor can take the URI directly... so I removed that
        // logic. It doesn't seem to change the behavior...? - Sean Clarke 04/2024

        fileExtension = checkFileExtension(fileExtension);
        List<String> filenameList = new ArrayList<>();

        try {
            Enumeration<URL> urls = classLoader.getResources(directoryPath);

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url.getProtocol().equals("jar")) {
                    // Must be running from a JAR file. Use ZipFile to find files and create list.
                    // Modified 12/28/09 by DPS to add results to existing filenameList instead of overwriting it.
                    filenameList.addAll(getListFromJar(extractJarFilename(url.getPath()), directoryPath, fileExtension));
                }
                else if (url.getProtocol().equals("file")) {
                    File directory = new File(url.getPath());
                    File[] files = directory.listFiles();
                    if (files != null) {
                        // Have array of File objects; convert to names and add to list
                        FileFilter filter = getFileFilter(fileExtension, "", ALLOW_DIRECTORIES_IN_SEARCH);
                        filenameList.addAll(Arrays.stream(files)
                            .filter(filter::accept)
                            .map(File::getName)
                            .toList());
                    }
                }
            }
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }

        return filenameList;
    }


    /**
     * Locate files and return list of file names.  Given a known relative directory path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension. If the "known file path" doesn't work
     * because MARS is running from an executable JAR file, it will locate the
     * directory in the JAR file and proceed from there.  NOTE: since this uses
     * the class loader to get the resource, the directory path needs to be
     * relative to classpath, not absolute.  To work with an arbitrary file system,
     * use the other version of this overloaded method.
     *
     * @param classLoader    class loader to use
     * @param directoryPath  Search will be confined to this directory.  Use "/" as
     *                       separator but do NOT include starting or ending "/"  (e.g. mars/tools)
     * @param fileExtensions ArrayList of Strings containing file extensions.
     *                       Only files with an extension in this list will be added to the list.
     *                       Do NOT include the ".", e.g. "class" not ".class".  If Arraylist or
     *                       extension null or empty, all files are added.
     * @return array list of matching file names as Strings.  If none, list is empty.
     */
    public static List<String> findFilenames(ClassLoader classLoader, String directoryPath, List<String> fileExtensions) {
        List<String> filenameList;

        if (fileExtensions == null || fileExtensions.isEmpty()) {
            filenameList = findFilenames(classLoader, directoryPath, "");
        }
        else {
            filenameList = new ArrayList<>();
            for (String fileExtension : fileExtensions) {
                fileExtension = checkFileExtension(fileExtension);
                filenameList.addAll(findFilenames(classLoader, directoryPath, fileExtension));
            }
        }

        return filenameList;
    }


    /**
     * Locate files and return list of file names.  Given a known directory path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension.  If file extension is null or empty, all
     * filenames are returned. Returned list contains absolute filename paths.
     *
     * @param directoryPath Search will be confined to this directory.
     * @param fileExtension Only files with this extension will be added to the list.
     *                      Do NOT include "." in extension.
     *                      If null or empty string, all files are added.
     * @return ArrayList of matching file names (absolute path).  If none, list is empty.
     */
    public static List<String> findFilenames(String directoryPath, String fileExtension) {
        fileExtension = checkFileExtension(fileExtension);
        List<String> filenameList = new ArrayList<>();
        File directory = new File(directoryPath);
        File[] allFiles = directory.listFiles();
        if (allFiles != null) {
            FileFilter filter = getFileFilter(fileExtension, "", ALLOW_DIRECTORIES_IN_SEARCH);
            for (File file : allFiles) {
                if (filter.accept(file)) {
                    filenameList.add(file.getAbsolutePath());
                }
            }
        }
        return filenameList;
    }


    /**
     * Locate files and return list of file names.  Given a known directory path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension.  If file extension is null or empty, all
     * filenames are returned. Returned list contains absolute filename paths.
     *
     * @param directoryPath  Search will be confined to this directory.
     * @param fileExtensions ArrayList of Strings containing file extensions.
     *                       Only files with an extension in this list will be added
     *                       to the list.  Do NOT include the "." in extensions.  If Arraylist or
     *                       extension null or empty, all files are added.
     * @return ArrayList of matching file names (absolute path).  If none, list is empty.
     */
    public static List<String> findFilenames(String directoryPath, List<String> fileExtensions) {
        List<String> filenameList = new ArrayList<>();
        String fileExtension;
        if (fileExtensions == null || fileExtensions.isEmpty()) {
            filenameList = findFilenames(directoryPath, "");
        }
        else {
            for (String extension : fileExtensions) {
                fileExtension = checkFileExtension(extension);
                filenameList.addAll(findFilenames(directoryPath, fileExtension));
            }
        }
        return filenameList;
    }


    /**
     * Return list of file names.  Given a list of file names, it will return the list
     * of all having the given file extension.  If file extension is null or empty, all
     * filenames are returned.  Returned list contains absolute filename paths.
     *
     * @param nameList      ArrayList of String containing file names.
     * @param fileExtension Only files with this extension will be added to the list.
     *                      If null or empty string, all files are added.  Do NOT include "." in extension.
     * @return ArrayList of matching file names (absolute path).  If none, list is empty.
     */
    public static List<String> findFilenames(List<String> nameList, String fileExtension) {
        fileExtension = checkFileExtension(fileExtension);
        List<String> filenameList = new ArrayList<>();
        FileFilter filter = getFileFilter(fileExtension, "", ALLOW_DIRECTORIES_IN_SEARCH);
        for (String name : nameList) {
            File file = new File(name);
            if (filter.accept(file)) {
                filenameList.add(file.getAbsolutePath());
            }
        }
        return filenameList;
    }


    /**
     * Return list of file names.  Given a list of file names, it will return the list
     * of all having the given file extension.  If file extension is null or empty, all
     * filenames are returned.  Returned list contains absolute filename paths.
     *
     * @param nameList       ArrayList of String containing file names.
     * @param fileExtensions ArrayList of Strings containing file extensions.
     *                       Only files with an extension in this list will be added
     *                       to the list.  Do NOT include the "." in extensions.  If Arraylist or
     *                       extension null or empty, all files are added.
     * @return ArrayList of matching file names (absolute path).  If none, list is empty.
     */
    public static List<String> findFilenames(List<String> nameList, List<String> fileExtensions) {
        List<String> filenameList;

        if (fileExtensions == null || fileExtensions.isEmpty()) {
            filenameList = findFilenames(nameList, "");
        }
        else {
            filenameList = new ArrayList<>();
            for (String extension : fileExtensions) {
                String fileExtension = checkFileExtension(extension);
                filenameList.addAll(findFilenames(nameList, fileExtension));
            }
        }

        return filenameList;
    }

    /**
     * Get the filename extension of the specified File.
     *
     * @param file the File object representing the file of interest.
     * @return The filename extension (everything that follows last '.' in filename) or null if none.
     */
    public static String getExtension(File file) {
        String filename = file.getName();
        int dotIndex = filename.lastIndexOf('.');
        // The dot cannot be the first or last character of the filename
        if (0 < dotIndex && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        else {
            return null;
        }
    }

    /**
     * Get a FileFilter that will filter files based on the given list of filename extensions.
     *
     * @param extensions        ArrayList of Strings, each string is acceptable filename extension.
     * @param description       String containing description to be added in parentheses after list of extensions.
     * @param acceptDirectories boolean value true if directories are accepted by the filter, false otherwise.
     * @return a FileFilter object that accepts files with given extensions, and directories if so indicated.
     */
    public static FileFilter getFileFilter(List<String> extensions, String description, boolean acceptDirectories) {
        return new MarsFileFilter(extensions, description, acceptDirectories);
    }

    /**
     * Get a FileFilter that will filter files based on the given list of filename extensions.
     * All directories are accepted by the filter.
     *
     * @param extensions  ArrayList of Strings, each string is acceptable filename extension
     * @param description String containing description to be added in parentheses after list of extensions.
     * @return a FileFilter object that accepts files with given extensions, and directories if so indicated.
     */
    public static FileFilter getFileFilter(List<String> extensions, String description) {
        return getFileFilter(extensions, description, true);
    }

    /**
     * Get a FileFilter that will filter files based on the given filename extension.
     *
     * @param extension         String containing acceptable filename extension.
     * @param description       String containing description to be added in parentheses after list of extensions.
     * @param acceptDirectories boolean value true if directories are accepted by the filter, false otherwise.
     * @return a FileFilter object that accepts files with given extensions, and directories if so indicated.
     */
    public static FileFilter getFileFilter(String extension, String description, boolean acceptDirectories) {
        return new MarsFileFilter(List.of(extension), description, acceptDirectories);
    }

    /**
     * Get a FileFilter that will filter files based on the given filename extension.
     * All directories are accepted by the filter.
     *
     * @param extension   String containing acceptable filename extension
     * @param description String containing description to be added in parentheses after list of extensions.
     * @return a FileFilter object that accepts files with given extensions, and directories if so indicated.
     */
    public static FileFilter getFileFilter(String extension, String description) {
        return getFileFilter(extension, description, true);
    }

    /**
     * Determine if given filename ends with given extension.
     *
     * @param name      A String containing the file name
     * @param extension A String containing the file extension.  Leading period is optional.
     * @return Returns true if filename ends with given extension, false otherwise.
     */
    public static boolean fileExtensionMatch(String name, String extension) {
        // For assured results, make sure extension starts with "."	(will add it if not there)
        return (extension == null || extension.isEmpty() || name.endsWith(((extension.startsWith(".")) ? "" : ".") + extension));
    }

    /**
     * Return list of file names in specified folder inside JAR.
     */
    private static List<String> getListFromJar(String jarName, String directoryPath, String fileExtension) {
        fileExtension = checkFileExtension(fileExtension);
        List<String> nameList = new ArrayList<>();
        if (jarName == null) {
            return nameList;
        }

        try (ZipFile zipFile = new ZipFile(new File(jarName))) {
            Enumeration<? extends ZipEntry> list = zipFile.entries();
            while (list.hasMoreElements()) {
                ZipEntry entry = list.nextElement();
                int lastSlashIndex = entry.getName().lastIndexOf('/');
                if (entry.getName().substring(0, Math.max(0, lastSlashIndex)).equals(directoryPath) && fileExtensionMatch(entry.getName(), fileExtension)) {
                    nameList.add(entry.getName().substring(lastSlashIndex + 1));
                }
            }
        }
        catch (Exception exception) {
            System.err.println("Exception occurred reading files from JAR:");
            exception.printStackTrace(System.err);
        }

        return nameList;
    }

    /**
     * Given pathname, extract and return JAR file name (must be only element containing ".jar").
     */
    // Modified to return file path of JAR file, not just its name.  This was
    // by request of Zachary Kurmas of Grant Valley State, who got errors trying
    // to run the Mars.jar file from a different working directory.  He helpfully
    // pointed out what the error is and where it occurs.  Originally, it would
    // work only if the JAR file was in the current working directory (as would
    // be the case if executed from a GUI by double-clicking the jar icon).
    // DPS 5 Dec 2007
    private static String extractJarFilename(String path) {
        if (path.toLowerCase().startsWith(FILE_URL)) {
            path = path.substring(FILE_URL.length());
        }
        int jarPosition = path.toLowerCase().indexOf(JAR_EXTENSION);
        return (jarPosition >= 0) ? path.substring(0, jarPosition + JAR_EXTENSION.length()) : path;
    }

    /**
     * Make sure file extension, if it is real, does not start with '.' -- if it does, remove it.
     */
    private static String checkFileExtension(String fileExtension) {
        return (fileExtension == null || !fileExtension.startsWith(".")) ? fileExtension : fileExtension.substring(1);
    }

    /**
     * FileFilter subclass to be instantiated by the getFileFilter method above.
     */
    private static class MarsFileFilter extends FileFilter {
        private final List<String> extensions;
        private final String fullDescription;
        private final boolean acceptDirectories;

        private MarsFileFilter(List<String> extensions, String description, boolean acceptDirectories) {
            this.extensions = extensions.stream()
                .map(FilenameFinder::checkFileExtension)
                .toList();
            this.fullDescription = buildFullDescription(description);
            this.acceptDirectories = acceptDirectories;
        }

        /**
         * User provides descriptive phrase to be parenthesized.
         * We will attach it to description of the extensions.  For example, if the extensions
         * given are "s" and "asm" and the description is "Assembler Programs" the full description
         * generated here will be "Assembler Programs (*.s; *.asm)"
         */
        private String buildFullDescription(String description) {
            StringBuilder result = new StringBuilder((description == null) ? "" : description);
            if (!extensions.isEmpty()) {
                result.append(" (")
                    .append(String.join("; ", extensions.stream().map(extension -> "*." + extension).toList()))
                    .append(")");
            }
            return result.toString();
        }

        @Override
        public String getDescription() {
            return fullDescription;
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return acceptDirectories;
            }
            String fileExtension = getExtension(file);
            if (fileExtension != null) {
                for (String extension : extensions) {
                    if (extension.equals(MATCH_ALL_EXTENSIONS) || fileExtension.equals(extension)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}

