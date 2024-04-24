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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action to check for updates (Help -> Update menu item)
 *
 * @author Colin Wong
 */
public class HelpUpdateAction extends VenusAction {
    private static final String RELEASES_URL = "https://api.github.com/repos/xarkenz/mars-red/releases";

    public HelpUpdateAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(RELEASES_URL))
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .GET()
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray releaseArray = new JSONArray(response.body());

            // Parse releases into versions and URLs from json response
            Map<Version, String> versionsToUrls = new HashMap<>();
            for (int index = 0; index < releaseArray.length(); index++) {
                JSONObject release = releaseArray.getJSONObject(index);
                String releaseVersion = release.getString("tag_name");
                // Remove leading 'v' before tag name
                if (releaseVersion.startsWith("v")) {
                    releaseVersion = releaseVersion.replaceFirst("v", "");
                }
                versionsToUrls.put(Version.parse(releaseVersion), release.getString("html_url"));
            }
            // Sort version entries in decreasing order
            List<Map.Entry<Version, String>> sortedVersions = versionsToUrls.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .toList();

            // Check if largest fetched version is "greater than" current version
            Version currentVersion = Version.parse(Application.VERSION);
            if (!sortedVersions.isEmpty() && sortedVersions.get(0).getKey().compareTo(currentVersion) > 0) {
                Map.Entry<Version, String> newestVersion = sortedVersions.get(0);
                int choice = JOptionPane.showConfirmDialog(
                    this.gui,
                    "A newer version of " + Application.NAME + " is available.\n"
                        + "Current version: " + currentVersion + "\n"
                        + "Latest version: " + newestVersion.getKey() + "\n"
                        + "Go to release page?",
                    "Check for Updates",
                    JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    Desktop.getDesktop().browse(new URI(newestVersion.getValue()));
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
        catch (IOException exception) {
            System.err.println(this.getClass().getSimpleName() + ": error 504: could not open releases page");
        }
        catch (URISyntaxException exception) {
            System.err.println(this.getClass().getSimpleName() + ": error 503: could not open releases page");
        }
        catch (InterruptedException exception) {
            System.err.println(this.getClass().getSimpleName() + ": error: update request timeout exceeded");
        }
    }
}
