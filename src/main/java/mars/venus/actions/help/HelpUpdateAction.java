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
import java.util.concurrent.CompletableFuture;

/**
 * Action for the Help -> Check for Updates menu item.
 *
 * @author Colin Wong
 */
public class HelpUpdateAction extends VenusAction {
    private static final String RELEASES_URL = "https://api.github.com/repos/xarkenz/mars-red/releases";
    private static final Duration TIMEOUT_DURATION = Duration.of(5, ChronoUnit.SECONDS);

    public HelpUpdateAction(VenusUI gui, Integer mnemonic, KeyStroke accel) {
        super(gui, "Check for Updates...", VenusUI.getSVGActionIcon("update.svg"), "Check if a newer version is available", mnemonic, accel);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        fetchReleases()
            .thenAccept(this::showUpdateDialog)
            .exceptionally(exception -> {
                if (exception instanceof InterruptedException) {
                    System.err.println(this.getClass().getSimpleName() + ": failed to fetch releases (timed out)");
                    JOptionPane.showMessageDialog(
                        this.gui,
                        "The connection timed out when attempting to fetch release information from GitHub.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
                else {
                    System.err.println(this.getClass().getSimpleName() + ": failed to fetch releases:");
                    exception.getCause().printStackTrace(System.err);
                    JOptionPane.showMessageDialog(
                        this.gui,
                        "Failed to fetch release information from GitHub. (Are you connected to the internet?)",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
                return null;
            });
    }

    /**
     * Asynchronously fetch the list of released versions using the GitHub API.
     *
     * @return A mapping from version identifiers to their corresponding user-facing release page URLs,
     *         wrapped in a {@link CompletableFuture} to enable asynchronous behavior. Note that this future
     *         completes exceptionally with an {@link IOException} if the HTTP <code>GET</code> request fails.
     */
    public static CompletableFuture<Map<Version, String>> fetchReleases() {
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

        return HttpClient.newHttpClient()
            // Send the request asynchronously
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            // Process the response once it is received
            .thenApply((response) -> {
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
            });
    }

    private void showUpdateDialog(Map<Version, String> versionsToUrls) {
        // Run on the GUI thread
        SwingUtilities.invokeLater(() -> {
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
                        exception.printStackTrace(System.err);
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
        });
    }
}
