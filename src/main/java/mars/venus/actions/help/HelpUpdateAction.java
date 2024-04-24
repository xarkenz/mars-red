package mars.venus.actions.help;

import mars.Application;
import mars.venus.VenusUI;
import mars.venus.actions.VenusAction;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Action to check for updates (Help -> Update menu item)
 *
 * @author Colin Wong
 */
public class HelpUpdateAction extends VenusAction {
    private static final String RELEASES_URL = "https://api.github.com/repos/xarkenz/mars-red/releases";
    private static final Duration TIMEOUT_DURATION = Duration.of(5, ChronoUnit.SECONDS);

    public HelpUpdateAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Map<Version, String> versionsToUrls;
        try {
            versionsToUrls = fetchReleases();
        }
        catch (IOException exception) {
            System.err.println(this.getClass().getSimpleName() + ": failed to fetch releases:");
            exception.printStackTrace();
            JOptionPane.showMessageDialog(
                this.gui,
                "Failed to fetch release information from GitHub. (Are you connected to the internet?)",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        catch (InterruptedException exception) {
            System.err.println(this.getClass().getSimpleName() + ": failed to fetch releases (timed out)");
            JOptionPane.showMessageDialog(
                this.gui,
                "The connection timed out when attempting to fetch release information from GitHub.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Get the entry for the latest version (i.e. maximum), or null if the map is empty
        Map.Entry<Version, String> latestVersion = versionsToUrls.entrySet().stream()
            .max(Map.Entry.comparingByKey())
            .orElse(null);

        // Get the current application version
        Version currentVersion = Version.parse(Application.VERSION);

        // Check whether the latest version is "greater than" the current version
        if (latestVersion != null && latestVersion.getKey().compareTo(currentVersion) > 0) {
            int choice = JOptionPane.showConfirmDialog(
                this.gui,
                "<html>A newer version of " + Application.NAME + " is available for download on GitHub.<ul>"
                    + "<li>Current version: <b>" + currentVersion + "</b></li>"
                    + "<li>Latest version: <b>" + latestVersion.getKey() + "</b></li>"
                    + "</ul>Go to release page?</html>",
                "Check for Updates",
                JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                // The user has requested to go to the release page, so open it with the default browser
                try {
                    Desktop.getDesktop().browse(new URI(latestVersion.getValue()));
                }
                catch (Exception exception) {
                    System.err.println(this.getClass().getSimpleName() + ": failed to browse '" + latestVersion.getValue() + "':");
                    exception.printStackTrace();
                    JOptionPane.showMessageDialog(
                        this.gui,
                        "<html>Failed to access the following URL:<br><br><a href='"
                            + latestVersion.getValue() + "'>" + latestVersion.getValue() + "</a></html>",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
        else {
            JOptionPane.showMessageDialog(
                this.gui,
                "You are using the latest version of " + Application.NAME + " (" + currentVersion + ").",
                "Check for Updates",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    public static Map<Version, String> fetchReleases() throws IOException, InterruptedException {
        // Create a GitHub API request
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                .uri(new URI(RELEASES_URL))
                .timeout(TIMEOUT_DURATION)
                .GET()
                .build();
        }
        catch (URISyntaxException exception) {
            // This should not happen unless RELEASES_URL is invalid
            throw new IllegalStateException(HelpUpdateAction.class.getSimpleName() + ": invalid RELEASES_URL: " + RELEASES_URL);
        }

        // Send the request
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        // Parse the response as JSON
        JSONArray releases = new JSONArray(response.body());

        // Obtain all versions and corresponding URLs from the JSON array
        Map<Version, String> versionsToUrls = new HashMap<>();
        for (int index = 0; index < releases.length(); index++) {
            JSONObject release = releases.getJSONObject(index);
            String releaseVersion = release.getString("tag_name");
            // Remove leading 'v' before tag name, if present
            if (releaseVersion.startsWith("v")) {
                releaseVersion = releaseVersion.substring(1);
            }
            versionsToUrls.put(Version.parse(releaseVersion), release.getString("html_url"));
        }

        return versionsToUrls;
    }
}
