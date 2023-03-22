package core;

import arc.util.Log;
import mindustry.game.EventType.*;
import mindustry.ui.dialogs.*;
import mindustry.mod.Mod;


import arc.Core;
import arc.Events;
import core.override.UIOverrider;

import static mindustry.Vars.*;

public class MappingUtilitiesMod extends Mod {

    public MappingUtilitiesMod() {
        Events.on(ClientLoadEvent.class, e -> {
            try {
                // add max/min zoom sliders to settings
                ui.settings.graphics.sliderPref("maxzoom", 60, 60, 250, 1,
                i -> Float.toString(Math.round((i / 10f) * 100f) / 100f) + "x");
                ui.settings.graphics.sliderPref("minzoom", 15, 1, 15, 1,
                i -> Float.toString(Math.round((i / 10f) * 100f) / 100f) + "x");

                UIOverrider.override();
            } catch (Exception ex) {
                Log.err(ex.toString());
                ui.showException("Mapping Tools Error", ex);
            }
        });

        Events.run(Trigger.update, () -> {
            renderer.maxZoom = Core.settings.getInt("maxzoom", 60) / 10f;
            renderer.minZoom = Core.settings.getInt("minzoom", 15) / 10f;
        });
    }

    @Override
    public void loadContent() {
        assert true;
    }

}
