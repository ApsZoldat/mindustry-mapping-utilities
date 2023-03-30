package core.ui;

import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import core.utils.PlanetBackgroundDrawer;

public class PlanetBackgroundDialog extends BaseDialog {
    private Table main;
    
    public PlanetBackgroundDialog() {
        super("", new DialogStyle(){{
            stageBackground = Styles.none;
            titleFont = Fonts.def;
            titleFontColor = Pal.accent;
        }});

        shown(this::setup);
    }

    private void setup() {
        cont.clear();
        cont.pane(m -> main = m);

        main.add("test");

        addCloseButton();
    }

    @Override
    public void draw() {
        if (state.rules.planetBackground != null) {
            float drawSize = Math.max(Core.graphics.getWidth(), Core.graphics.getHeight());
            Draw.rect(Draw.wrap(PlanetBackgroundDrawer.draw()), Core.graphics.getWidth() / 2, Core.graphics.getHeight() / 2, drawSize, -drawSize);
            Draw.flush();
        }
        super.draw();
    }
}
