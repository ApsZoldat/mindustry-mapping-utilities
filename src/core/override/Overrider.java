package core.override;

import arc.util.Log;
import arc.util.Reflect;
import core.ModVars;
import core.override.editor.OMapEditor;
import core.override.ui.OCustomRulesDialog;
import core.override.ui.OMapEditorDialog;
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

            Reflect.set(Reflect.get(ui.custom, "dialog").getClass(), Reflect.get(ui.custom, "dialog"), "dialog", new OCustomRulesDialog());
            Reflect.set(Reflect.get(ui.editor, "playtestDialog").getClass(), Reflect.get(ui.editor, "playtestDialog"), "dialog", new OCustomRulesDialog());

            HUDOverrider.override();
        } catch (Exception ex) {
            Log.err(ex.toString());
            ui.showException("Mapping Tools Error", ex);
        }
    }
}
