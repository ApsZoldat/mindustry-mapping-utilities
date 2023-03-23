package core.override;

import arc.util.Log;
import core.ModVars;
import core.override.editor.OMapEditor;
import core.override.ui.OMapEditorDialog;
import mindustry.Vars;
import mindustry.editor.MapEditorDialog;

import static mindustry.Vars.*;

// TODO: remove later if it's unneccessary
public class Overrider {
    public static void override() {
        try {
            MapEditorDialog oldDialog = Vars.ui.editor; // shown listener doesn't disappear on this one

            OMapEditor newEditor = new OMapEditor();
            Vars.editor = newEditor;
            Vars.ui.editor =  new OMapEditorDialog(oldDialog, newEditor);
            ModVars.mapEditor = newEditor;
        } catch (Exception ex) {
            Log.err(ex.toString());
            ui.showException("Mapping Tools Error", ex);
        }
    }
}
