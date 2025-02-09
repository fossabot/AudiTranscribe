/*
 * SettingsData.java
 *
 * Created on 2022-05-22
 * Updated on 2022-06-19
 *
 * Description: Class that contains the settings data.
 */

package site.overwrite.auditranscribe.io.json_files.data_encapsulators;

import site.overwrite.auditranscribe.audio.WindowFunction;
import site.overwrite.auditranscribe.misc.Theme;
import site.overwrite.auditranscribe.spectrogram.ColourScale;

/**
 * Class that contains the settings data.
 */
public class SettingsData {
    // Overarching data
    public boolean isSetupCompleted = false;

    // Appearance data
    public int themeEnumOrdinal = Theme.LIGHT_MODE.ordinal();

    // Spectrogram data
    public int colourScaleEnumOrdinal  = ColourScale.VIRIDIS.ordinal();
    public int windowFunctionEnumOrdinal = WindowFunction.HANN_WINDOW.ordinal();

    // I/O data
    public int autosaveInterval = 5;  // In minutes

    // Audio data
    public double notePlayingDelayOffset = 0.2;  // In seconds; to account for note playing delay
    public String ffmpegInstallationPath = null;  // Path to the ffmpeg installation
}
