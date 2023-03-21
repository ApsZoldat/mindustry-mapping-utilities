package core;

import arc.util.Log;
import mindustry.game.EventType.*;
import mindustry.ui.dialogs.*;
import mindustry.mod.Mod;


import arc.Core;
import arc.Events;
import core.override.UIOverrider;
import mindustry.Vars;


public class MappingUtilitiesMod extends Mod {

    public MappingUtilitiesMod() {
        Events.on(ClientLoadEvent.class, e -> {
            try {
                // add max/min zoom sliders to settings
                Vars.ui.settings.graphics.sliderPref("maxzoom", 60, 60, 250, 1,
                i -> Float.toString(Math.round((i / 10f) * 100f) / 100f) + "x");
                Vars.ui.settings.graphics.sliderPref("minzoom", 15, 1, 15, 1,
                i -> Float.toString(Math.round((i / 10f) * 100f) / 100f) + "x");

                UIOverrider.override();
            } catch (Exception ex) {
                Log.err(ex.toString());
                BaseDialog dialog = new BaseDialog("oh no");
                dialog.cont.add(ex.toString()).row();
                dialog.cont.button("OK", dialog::hide).size(100f, 50f);
                dialog.show();
            }
        });

        Events.run(Trigger.update, () -> {
            Vars.renderer.maxZoom = Core.settings.getInt("maxzoom", 60) / 10f;
            Vars.renderer.minZoom = Core.settings.getInt("minzoom", 15) / 10f;
        });
    }

    @Override
    public void loadContent() {
        assert true;
    }

}
