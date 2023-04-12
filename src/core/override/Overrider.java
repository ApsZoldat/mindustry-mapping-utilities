package core.override;

import arc.util.Log;
import arc.util.Reflect;
import core.ModVars;
import core.override.editor.OMapEditor;
import core.override.ui.OCustomRulesDialog;
import core.override.ui.OMapEditorDialog;
import core.utils.ChainReflect;
import mindustry.Vars;
import mindustry.editor.MapEditorDialog;

import static mindustry.Vars.*;

// TODO: remove later if it's unneccessary
public class Overrider {
    public static void override() {
        try {
            MapEditorDialog oldDialog = Vars.ui.editor; // shown listener doesn't disappear on this one

            ModVars.mapEditor = new OMapEditor();
            Vars.editor = ModVars.mapEditor;

            ModVars.mapEditorDialog = new OMapEditorDialog(oldDialog, ModVars.mapEditor);
            Vars.ui.editor = ModVars.mapEditorDialog;

            ChainReflect.set(ui.custom, new OCustomRulesDialog(), "dialog", "dialog");
            ChainReflect.set(ui.editor, new OCustomRulesDialog(), "playtestDialog", "dialog");

            HUDOverrider.override();
        } catch (Exception ex) {
            Log.err(ex.toString());
            ui.showException("Mapping Tools Error", ex);
        }
    }
}
