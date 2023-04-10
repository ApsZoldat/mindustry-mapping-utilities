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

                Overrider.override();
            } catch (Exception ex) {
                Log.err(ex.toString());
                ui.showException("Mapping Tools Error", ex);
            }
        });
    }

    @Override
    public void loadContent() {
        assert true;
    }

}
