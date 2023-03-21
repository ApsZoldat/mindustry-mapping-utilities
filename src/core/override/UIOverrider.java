package core.override;

import arc.util.Log;
import arc.util.Reflect;
import core.override.ui.OMapInfoDialog;
import core.override.ui.OMapResizeDialog;
import mindustry.Vars;
import mindustry.editor.MapEditorDialog;
import mindustry.ui.dialogs.BaseDialog;

public class UIOverrider {
    public static void override() {
        try {
            Reflect.set(MapEditorDialog.class, Vars.ui.editor, "infoDialog", new OMapInfoDialog());
            Reflect.set(MapEditorDialog.class, Vars.ui.editor, "resizeDialog", new OMapResizeDialog((width, height, shiftX, shiftY) -> {
                if(!(Vars.editor.width() == width && Vars.editor.height() == height && shiftX == 0 && shiftY == 0)){
                    Vars.ui.loadAnd(() -> {
                        Vars.editor.resize(width, height, shiftX, shiftY);
                    });
                }                   
            }));
    
        } catch (Exception ex) {
            Log.err(ex.toString());
            BaseDialog dialog = new BaseDialog("oh no");
            dialog.cont.add(ex.toString()).row();
            dialog.cont.button("OK", dialog::hide).size(100f, 50f);
            dialog.show();
        }
    }
}
