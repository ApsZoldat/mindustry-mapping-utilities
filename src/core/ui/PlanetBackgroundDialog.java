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
import arc.func.Floatc;
import arc.func.Floatp;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.math.geom.Vec3;
import arc.scene.style.Drawable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Strings;
import core.utils.PlanetBackgroundDrawer;

public class PlanetBackgroundDialog extends BaseDialog {
    private Table main;
    private Table planetTable;
    private Table paramsTable;
    private PlanetParams params;

    private boolean inputMode = false;
    private boolean tableRow = Core.graphics.getWidth() < 680; // When planet selection and params tables should be splitted with row()

    private Cell<TextButton> UIButton;
    private boolean UIHidden = false;

    private float rotX = 0f;
    private float rotY = 0f;
    
    public PlanetBackgroundDialog() {
        super("", new DialogStyle(){{
            stageBackground = Styles.none;
            titleFont = Fonts.def;
            titleFontColor = Pal.accent;
            // Don't specify background so it won't dark the planet view
        }});

        Events.run(Trigger.update, () -> {
            resetup();
        });

        addCloseButton();
        UIButton = buttons.button("@rules.background.hideui", Icon.eyeOff, this::switchUI).size(210f, 64f);
        shown(this::setup);
    }

    private void addBackground() {
        state.rules.planetBackground = new PlanetParams();
        setup();
    }

    private void removeBackground() {
        state.rules.planetBackground = null;
        setup();
    }

    private void switchUI() {
        if (params != null) {
            UIHidden = !UIHidden;

            UIButton.clearElement();
            TextButton button = new TextButton("");
            if (!UIHidden) {
                button.setText("@rules.background.hideui");
                button.add(new Image(Icon.eyeOff)).size(Icon.eye.imageSize());
            } else {
                button.setText("@rules.background.showui");
                button.add(new Image(Icon.eye)).size(Icon.eye.imageSize());
            }
    
            button.getCells().reverse();
            button.clicked(this::switchUI);
            UIButton.setElement(button);
    
            setup();
        }
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
        }).marginLeft(14f).padBottom(5f).width(220f).height(55f).checked(params.planet == planet).update(b -> b.setChecked(params.planet == planet))
        .get().getChildren().get(1).setColor(planet.iconColor);
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

    private void setupSliders() {
        paramsTable.add("@rules.background.zoom").padBottom(4f).row();
        paramsTable.slider(0.1f, 40f, 0.1f, params.zoom, f -> {params.zoom = f; PlanetBackgroundDrawer.update();}).padBottom(4f).width(400f).row();

        paramsTable.add("@rules.background.camerarot").padTop(10f).padBottom(4f).row();
        paramsTable.table(t2 -> {
            t2.add("X").padRight(8f);
            t2.slider(0f, 360f, 0.5f, rotX, f -> {rotX = f; updateRotation();}).width(380).row();
            t2.add("Y").padRight(8f);
            t2.slider(0f, 180, 0.5f, rotY, f -> {rotY = f; updateRotation();}).width(380).row();
        });
        paramsTable.row();
    }

    private void number(Table tb, String text, Floatc cons, Floatp prov, float min, float max) {
        tb.table(t -> {
            t.left();
            t.add(text).left().padRight(5);
            t.field((prov.get()) + "", s -> cons.get(Strings.parseFloat(s)))
            .padRight(100f)
            .valid(f -> Strings.parseFloat(f) >= min && Strings.parseFloat(f) <= max).width(120f).left();
        }).padTop(0);
        tb.row();
    }

    private void setupInputs() {
        number(paramsTable, "@rules.background.zoom", f -> {params.zoom = f; PlanetBackgroundDrawer.update();}, () -> params.zoom, 0.1f, 40f);

        paramsTable.add("@rules.background.camerarot").padTop(10f).padBottom(4f).row();
        paramsTable.table(t2 -> {
            number(t2, "X", f -> {rotX = f; updateRotation();}, () -> rotX, 0f, 360f);
            number(t2, "Y", f -> {rotY = f; updateRotation();}, () -> rotY, 0f, 180);
        });
    }

    private void setup() {
        params = state.rules.planetBackground;

        if (params != null) {
            rotX = new Vec2(params.camPos.x, params.camPos.z).angle();
            rotY = new Vec2(0, params.camPos.y).angle();
        }

        cont.clear();
        cont.pane(m -> main = m);

        if (!UIHidden && params != null) {
            main.button("@rules.background.removebackground", () -> {ui.showConfirm("@rules.background.removalwarning", this::removeBackground);})
            .marginLeft(14f).padBottom(5f).width(220f).height(55f).row();
            main.add("@rules.background.description").color(new Color(1f, 1f, 1f, 1f)).padBottom(20f);
            main.row();

            main.table(t -> {
                t.table(t2 -> planetTable = t2).padRight(20f);
                addPlanets();
        
                if (tableRow) t.row();
        
                t.table(t2 -> paramsTable = t2);
        
                if (!inputMode) setupSliders();
                else setupInputs();
            });

            main.row();

            main.button("@rules.background.switchinputmode", Styles.togglet, () -> {
                inputMode = !inputMode;
                setup();
            }).marginLeft(14f).padBottom(5f).width(220f).height(55f);
            planetTable.row();
        } else if (params == null) {
            main.add("@rules.background.nobackground").color(new Color(1f, 1f, 1f, 1f)).padBottom(20f).row();
            main.button("@rules.background.addbackground", Styles.togglet, () -> {
                addBackground();
            }).marginLeft(14f).width(220f).height(55f);
        }

        PlanetBackgroundDrawer.update();
    }

    private void updateRotation() {
        params.camPos = new Vec3(Mathf.cosDeg(rotX), Mathf.cosDeg(rotY), Mathf.sinDeg(rotX));
        PlanetBackgroundDrawer.update();
    }

    @Override
    public void draw() {
        if (params != null) {
            float drawSize = Math.max(Core.graphics.getWidth(), Core.graphics.getHeight());
            Draw.rect(Draw.wrap(PlanetBackgroundDrawer.draw()), Core.graphics.getWidth() / 2, Core.graphics.getHeight() / 2, drawSize, -drawSize);
            Draw.flush();
        } else {
            Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
            Styles.black9.draw(x, y, width, height);
        }

        super.draw();
    }
}
