package core.ui;

import mindustry.graphics.Pal;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.Planet;
import mindustry.content.Planets;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import core.utils.PlanetBackgroundDrawer;

public class PlanetBackgroundDialog extends BaseDialog {
    private Table main;
    private Table planetTable;
    private Table paramsTable;
    private PlanetParams params;
    private boolean inputMode = false;
    private boolean tableRow = Core.graphics.getWidth() < 680; // When planet selection and params tables should be splitted with row()
    
    public PlanetBackgroundDialog() {
        super("", new DialogStyle(){{
            stageBackground = Styles.none;
            titleFont = Fonts.def;
            titleFontColor = Pal.accent;
        }});

        Events.run(Trigger.update, () -> {
            resetup();
        });

        addCloseButton();
        shown(this::setup);
    }

    // Resetups the dialog when window rescaled
    private void resetup() {
        boolean newTableRow = Core.graphics.getWidth() < 680;
        
        if (tableRow != newTableRow) {
            tableRow = newTableRow;
            setup();
        }
    }

    private void addPlanet(Planet planet) {
        planetTable.button(planet.localizedName, Icon.planet, Styles.togglet, () -> {
            params.planet = planet;
            PlanetBackgroundDrawer.update();
        }).marginLeft(14f).padBottom(5f).width(220f).height(55f).checked(params.planet == planet).get().getChildren().get(1).setColor(planet.iconColor);
        planetTable.row();
    }

    private void addPlanets() {
        addPlanet(Planets.sun);
        addPlanet(Planets.serpulo);
        addPlanet(Planets.erekir);
        addPlanet(Planets.gier);
        addPlanet(Planets.notva);
        addPlanet(Planets.verilus);
    }

    private void setup() {
        params = state.rules.planetBackground;

        cont.clear();
        cont.pane(m -> main = m);

        main.table(t -> planetTable = t).padRight(20f);
        addPlanets();

        if (tableRow) main.row();

        main.table(t -> paramsTable = t);

        paramsTable.add("@rules.background.zoom").padBottom(4f).row();
        paramsTable.slider(0.1f, 40f, 0.1f, params.zoom, f -> {params.zoom = f; PlanetBackgroundDrawer.update();}).width(400f);
    }

    @Override
    public void draw() {
        if (params != null) {
            float drawSize = Math.max(Core.graphics.getWidth(), Core.graphics.getHeight());
            Draw.rect(Draw.wrap(PlanetBackgroundDrawer.draw()), Core.graphics.getWidth() / 2, Core.graphics.getHeight() / 2, drawSize, -drawSize);
            Draw.flush();
        }
        super.draw();
    }
}
