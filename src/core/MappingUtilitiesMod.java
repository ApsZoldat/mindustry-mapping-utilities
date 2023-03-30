package core;

import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.mod.Mod;


import arc.Core;
import arc.Events;
import core.override.Overrider;

import static mindustry.Vars.*;

// I won't apologize if you're reading this code as example for your mod
// This is the worst example possible (override folder moment)
public class MappingUtilitiesMod extends Mod {

    public MappingUtilitiesMod() {
        Events.on(ClientLoadEvent.class, e -> {
            try {
                Vars.maxSchematicSize = 512; // nobody will mind

                // add max/min zoom sliders to settings
                ui.settings.graphics.sliderPref("maxzoom", 60, 60, 250, 1,
                i -> Float.toString(Math.round((i / 10f) * 100f) / 100f) + "x");
                ui.settings.graphics.sliderPref("minzoom", 15, 1, 15, 1,
                i -> Float.toString(Math.round((i / 10f) * 100f) / 100f) + "x");

                Overrider.override();
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
