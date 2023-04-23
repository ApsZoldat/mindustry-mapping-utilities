package core;

import core.override.editor.OMapEditor;
import core.override.ui.OMapEditorDialog;
import mindustry.editor.MapView;

// The SIN zone (ye it's literally global variables)
public class ModVars {
    public static OMapEditor mapEditor;
    public static OMapEditorDialog mapEditorDialog;
    public static MapView oldMapView;
    public static boolean inGame = false;
}
