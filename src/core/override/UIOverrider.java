package core.override;

import arc.util.Log;
import core.override.ui.OMapEditorDialog;
import mindustry.Vars;
import mindustry.editor.MapEditorDialog;

import static mindustry.Vars.*;

// TODO: remove later if it's unneccessary
public class UIOverrider {
    public static void override() {
        try {
            MapEditorDialog oldEditor = Vars.ui.editor; // shown listener doesn't disappear on this one 
            Vars.ui.editor =  new OMapEditorDialog(oldEditor);
    
        } catch (Exception ex) {
            Log.err(ex.toString());
            ui.showException("Mapping TOols Error", ex);
        }
    }
}
