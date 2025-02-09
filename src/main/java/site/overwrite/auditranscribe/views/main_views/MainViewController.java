/*
 * MainViewController.java
 *
 * Created on 2022-02-09
 * Updated on 2022-07-03
 *
 * Description: Contains the main view's controller class.
 */

package site.overwrite.auditranscribe.views.main_views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import site.overwrite.auditranscribe.io.IOMethods;
import site.overwrite.auditranscribe.io.PropertyFile;
import site.overwrite.auditranscribe.io.db.ProjectsDB;
import site.overwrite.auditranscribe.io.json_files.data_encapsulators.SettingsData;
import site.overwrite.auditranscribe.io.json_files.file_classes.SettingsFile;
import site.overwrite.auditranscribe.misc.MyLogger;
import site.overwrite.auditranscribe.misc.Theme;
import site.overwrite.auditranscribe.system.OSMethods;
import site.overwrite.auditranscribe.system.OSType;
import site.overwrite.auditranscribe.utils.MiscUtils;
import site.overwrite.auditranscribe.views.helpers.Popups;
import site.overwrite.auditranscribe.views.helpers.ProjectIOHandlers;
import site.overwrite.auditranscribe.views.scene_switching.SceneSwitchingState;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

// Main class
public class MainViewController implements Initializable {
    // Attributes
    private ProjectsDB projectsDB;

    private FilteredList<Quartet<Long, String, String, String>> filteredList;  // List of project records

    private SettingsFile settingsFile;

    private SceneSwitchingState sceneSwitchingState = SceneSwitchingState.CLOSE_SCENE;
    private File selectedFile = null;

    // FXML Elements
    // Menu bar items
    @FXML
    private MenuBar menuBar;

    @FXML
    private MenuItem newProjectMenuItem, openProjectMenuItem, preferencesMenuItem, aboutMenuItem;

    // Main elements
    @FXML
    private AnchorPane rootPane;

    @FXML
    private Label versionLabel;

    @FXML
    private Button newProjectButton, openProjectButton;

    @FXML
    private TextField searchTextField;

    @FXML
    private ImageView searchImage;

    @FXML
    private ListView<Quartet<Long, String, String, String>> projectsListView;

    // Initialization method
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Make macOS systems use the system menu bar
        if (OSMethods.getOS() == OSType.MAC) {
            menuBar.useSystemMenuBarProperty().set(true);
        }

        // Get the current version
        try {
            // Get the project properties file
            PropertyFile projectPropertiesFile = new PropertyFile("project.properties");

            // Update the version label with the version number
            versionLabel.setText("Version " + projectPropertiesFile.getProperty("version"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Add methods to buttons
        newProjectButton.setOnAction(this::handleNewProject);

        openProjectButton.setOnAction(this::handleOpenProject);

        // Set the search field method
        searchTextField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredList.setPredicate(projectRecord -> {
                    // If filter text is empty, display all projects
                    if (newValue == null || newValue.isEmpty()) return true;

                    // Attempt to find a match within the *file path*
                    String searchFilter = newValue.toLowerCase();
                    String lowercaseFilepath = projectRecord.getValue2().toLowerCase();

                    return lowercaseFilepath.contains(searchFilter);
                })
        );

        // Update the projects list view
        projectsListView.setOnMouseClicked(mouseEvent -> {
            // Get the selected item
            Quartet<Long, String, String, String> selectedItem =
                    projectsListView.getSelectionModel().getSelectedItem();

            // Check if an item was selected
            if (selectedItem != null) {
                // Get the file of the selected item
                String filepath = selectedItem.getValue2();
                File file = new File(filepath);

                // Set the scene switching status and the selected file
                sceneSwitchingState = SceneSwitchingState.OPEN_PROJECT;
                selectedFile = file;

                // Close this stage
                ((Stage) rootPane.getScene().getWindow()).close();
            }
        });

        refreshProjectsListView();

        // Add methods to menu items
        newProjectMenuItem.setOnAction(this::handleNewProject);

        openProjectMenuItem.setOnAction(this::handleOpenProject);

        preferencesMenuItem.setOnAction(actionEvent -> PreferencesViewController.showPreferencesWindow(settingsFile));

        aboutMenuItem.setOnAction(actionEvent -> AboutViewController.showAboutWindow(settingsFile));

        // Report that the main view is ready to be shown
        MyLogger.log(Level.INFO, "Main view ready to be shown", this.getClass().toString());
    }

    // Getter/Setter methods

    public void setSettingsFile(SettingsFile settingsFile) {
        this.settingsFile = settingsFile;
    }

    public SceneSwitchingState getSceneSwitchingState() {
        if (sceneSwitchingState == null) return SceneSwitchingState.CLOSE_SCENE;
        return sceneSwitchingState;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    // Public methods

    /**
     * Method that sets the theme for the scene.
     */
    public void setThemeOnScene() {
        // Get the theme
        Theme theme = Theme.values()[settingsFile.data.themeEnumOrdinal];

        // Set stylesheets
        rootPane.getStylesheets().clear();  // Reset the stylesheets first before adding new ones

        rootPane.getStylesheets().add(IOMethods.getFileURLAsString("views/css/base.css"));
        rootPane.getStylesheets().add(IOMethods.getFileURLAsString("views/css/" + theme.cssFile));

        // Set graphics
        searchImage.setImage(new Image(IOMethods.getFileURLAsString(
                "images/icons/PNGs/" + theme.shortName + "/search.png"
        )));
    }

    /**
     * Method to refresh the projects' list view.
     */
    public void refreshProjectsListView() {
        // Get all projects' records
        List<Quartet<Long, String, String, String>> projects = new ArrayList<>();

        try {
            // Get the projects database
            projectsDB = new ProjectsDB();

            // Get all projects' records
            Map<Integer, Pair<String, String>> projectRecords = projectsDB.getAllProjects();

            // Get all the keys
            Set<Integer> keys = projectRecords.keySet();

            // For each file, get their last accessed time and generate the shortened name
            for (int key : keys) {
                // Get both the filepath and the filename
                Pair<String, String> values = projectRecords.get(key);
                String filepath = values.getValue0();
                String filename = values.getValue1();

                // Add the project to the records
                BasicFileAttributes attributes;
                try {
                    // Get the last modified time of the file
                    attributes = Files.readAttributes(Paths.get(filepath), BasicFileAttributes.class);

                    FileTime lastModifiedTime = attributes.lastModifiedTime();
                    long lastModifiedTimestamp = lastModifiedTime.toMillis();

                    // Get the shortened name of the file name
                    filename = filename.substring(0, filename.length() - 5);  // Exclude the ".audt" at the end
                    String shortenedName = MiscUtils.getShortenedName(filename);

                    // Add to the list of projects
                    projects.add(new Quartet<>(lastModifiedTimestamp, filename, filepath, shortenedName));
                } catch (NoSuchFileException e) {
                    projectsDB.deleteProjectRecord(key);
                }
            }

            // Sort the list of projects by the last access timestamp
            projects.sort(new SortByTimestamp());

        } catch (SQLException | IOException e) {
            MyLogger.logException(e);
            throw new RuntimeException(e);
        }

        // Convert the `projects` list to an FXML `ObservableList` and a `FilteredList` to allow for searching
        ObservableList<Quartet<Long, String, String, String>> projectsList = FXCollections.observableList(projects);
        filteredList = new FilteredList<>(projectsList);

        if (projectsList.size() != 0) {
            projectsListView.setBackground(Background.fill(Color.WHITE));
            projectsListView.setItems(new SortedList<>(filteredList));  // Use a sorted list for searching
            projectsListView.setCellFactory(
                    customListCellListView -> new CustomListCell(
                            projectsDB, projectsListView, settingsFile.data
                    )
            );
        } else {
            MyLogger.log(Level.INFO, "No projects found", this.getClass().toString());
            projectsListView.opacityProperty().set(0);
        }
    }

    // Private methods

    /**
     * Helper method that helps open a new project.
     *
     * @param actionEvent Event that triggered this function.
     */
    private void handleNewProject(ActionEvent actionEvent) {
        // Get the current window
        Window window = rootPane.getScene().getWindow();

        // Get user to select an audio file
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Audio files (*.wav, *.mp3, *.flac, *.aif, *.aiff)",
                "*.wav", "*.mp3", "*.flac", "*.aif", "*.aiff"
        );
        File file = ProjectIOHandlers.getFileFromFileDialog(window, extFilter);

        // Verify that the user actually chose a file
        if (file == null) {
            Popups.showInformationAlert("Info", "No file selected.");
        } else {
            // Set the scene switching status and the selected file
            sceneSwitchingState = SceneSwitchingState.NEW_PROJECT;
            selectedFile = file;

            // Close this stage
            ((Stage) rootPane.getScene().getWindow()).close();
        }
    }

    /**
     * Helper method that helps open an existing project.
     *
     * @param actionEvent Event that triggered this function.
     */
    private void handleOpenProject(ActionEvent actionEvent) {
        // Get the current window
        Window window = rootPane.getScene().getWindow();

        // Get user to select an AUDT file
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "AudiTranscribe files (*.audt)", "*.audt"
        );
        File file = ProjectIOHandlers.getFileFromFileDialog(window, extFilter);

        // Verify that the user actually chose a file
        if (file == null) {
            Popups.showInformationAlert("Info", "No file selected.");
        } else {
            // Set the scene switching status and the selected file
            sceneSwitchingState = SceneSwitchingState.OPEN_PROJECT;
            selectedFile = file;

            // Close this stage
            ((Stage) window).close();
        }
    }

    // Helper classes
    static class CustomListCell extends ListCell<Quartet<Long, String, String, String>> {
        // Attributes
        ProjectsDB db;
        ListView<?> projectsListView;

        // FXML elements
        HBox content;

        StackPane shortNameDisplayArea;
        Rectangle shortNameRectangle;
        Text shortNameText;

        Label nameLabel;
        Label lastModifiedTimeLabel;
        Label filepathLabel;

        Button removeButton;

        public CustomListCell(ProjectsDB db, ListView<?> projectsListView, SettingsData settingsData) {
            // Call superclass initialization method
            super();

            // Update attributes
            this.db = db;
            this.projectsListView = projectsListView;

            // Create all labels and texts
            nameLabel = new Label();
            lastModifiedTimeLabel = new Label();
            filepathLabel = new Label();

            shortNameText = new Text();

            // Set CSS classes on text labels
            nameLabel.getStyleClass().add("project-name-label");
            lastModifiedTimeLabel.getStyleClass().add("last-opened-date-label");
            filepathLabel.getStyleClass().add("filepath-label");

            shortNameText.getStyleClass().add("short-name-text");

            // Create the short name display area
            shortNameRectangle = new Rectangle();
            shortNameRectangle.getStyleClass().add("short-name-rectangle");

            shortNameRectangle.setFill(Color.TRANSPARENT);
            shortNameRectangle.setWidth(50);
            shortNameRectangle.setHeight(50);

            shortNameDisplayArea = new StackPane();
            shortNameDisplayArea.getChildren().addAll(shortNameRectangle, shortNameText);

            // Set the removal button's style and method
            ImageView removeButtonGraphic = new ImageView(
                    new Image(IOMethods.getFileURLAsString(
                            "images/icons/PNGs/" +
                                    Theme.values()[settingsData.themeEnumOrdinal].shortName +
                                    "/close.png"
                    ))
            );
            removeButtonGraphic.setFitWidth(40);
            removeButtonGraphic.setFitHeight(40);

            removeButton = new Button();
            removeButton.setGraphic(removeButtonGraphic);
            removeButton.getStyleClass().add("image-button");
            removeButton.getStyleClass().add("remove-project-button");

            removeButton.setOnAction(actionEvent -> {
                // Get the filepath of the project
                String filepath = filepathLabel.getText();

                // Get the primary key from the database
                int pk;
                try {
                    pk = db.getIDOfProjectWithFilepath(filepath);
                } catch (SQLException e) {
                    MyLogger.logException(e);
                    throw new RuntimeException(e);
                }

                // Delete that project's record from the database
                try {
                    db.deleteProjectRecord(pk);
                } catch (SQLException e) {
                    MyLogger.logException(e);
                    throw new RuntimeException(e);
                }

                MyLogger.log(
                        Level.INFO,
                        "Removed " + nameLabel.getText() + " with primary key " + pk + " from projects' database",
                        this.getClass().toString());

                // Remove this list item from the list view
                SortedList<?> sortedList = (SortedList<?>) getListView().getItems();
                FilteredList<?> filteredList = (FilteredList<?>) sortedList.getSource();
                ObservableList<?> sourceList = filteredList.getSource();
                sourceList.remove(getItem());

                if (sourceList.size() == 0) projectsListView.setBackground(Background.fill(Color.TRANSPARENT));
            });

            // Set the content
            HBox nameAndDateBox = new HBox(nameLabel, lastModifiedTimeLabel);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            HBox.setHgrow(lastModifiedTimeLabel, Priority.ALWAYS);
            nameAndDateBox.setSpacing(10);

            VBox nameDateAndFilepathBox = new VBox(nameAndDateBox, filepathLabel);
            nameDateAndFilepathBox.setSpacing(5);

            content = new HBox(shortNameDisplayArea, nameDateAndFilepathBox, removeButton);
            content.setSpacing(10);
            content.setPadding(
                    // Although FXML file uses 30, use 25 because spacing is 10
                    new Insets(0, 25, 0, 25)
            );
            content.getStyleClass().add("project-list-cell");
        }

        @Override
        protected void updateItem(Quartet<Long, String, String, String> object, boolean empty) {
            // Call superclass method
            super.updateItem(object, empty);

            // Ensure that the object is not null and non-empty
            if (object != null & !empty) {
                // Convert the timestamp to a date string
                lastModifiedTimeLabel.setText(
                        "[Last modified on " +
                                MiscUtils.formatDate(new Date(object.getValue0()), "yyyy-MM-dd HH:mm") +
                                "]"
                );
                nameLabel.setText(object.getValue1());
                filepathLabel.setText(object.getValue2());
                shortNameText.setText(object.getValue3());

                // Set the graphic of the list item
                setGraphic(content);
            } else {
                setGraphic(null);
            }
        }
    }

    static class SortByTimestamp implements Comparator<Quartet<Long, String, String, String>> {
        @Override
        public int compare(
                Quartet<Long, String, String, String> o1,
                Quartet<Long, String, String, String> o2
        ) {
            return (int) -(o1.getValue0() - o2.getValue0());  // Sort in descending order
        }
    }
}
