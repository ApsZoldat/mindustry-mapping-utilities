package core.override.ui;

import mindustry.editor.MapGenerateDialog;
import mindustry.editor.MapInfoDialog;
import mindustry.editor.MapObjectivesDialog;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import arc.scene.ui.*;
import arc.struct.*;
import core.ModVars;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.filters.*;

import static mindustry.Vars.*;

public class OMapInfoDialog extends MapInfoDialog {
    private final OWaveInfoDialog waveInfo = new OWaveInfoDialog();
    private final MapGenerateDialog generate = new MapGenerateDialog(false);;
    private final OCustomRulesDialog ruleInfo = new OCustomRulesDialog();
    private final MapObjectivesDialog objectives = new MapObjectivesDialog();

    private boolean preventLandscape = false;

    public OMapInfoDialog() {
        super();

        shown(() -> {
            ModVars.mapEditorDialog.endLandscape();
        });

        hidden(() -> {
            if (preventLandscape) {preventLandscape = false; return;}
            ModVars.mapEditorDialog.beginLandscape();
        });

        shown(this::setup);
    }

    private void setup() {
        cont.clear();

        ObjectMap<String, String> tags = editor.tags;
        
        cont.pane(t -> {
            t.add("@editor.mapname").padRight(8).left();
            t.defaults().padTop(15);

            TextField name = t.field(tags.get("name", ""), text -> {
                tags.put("name", text);
            }).size(400, 55f).maxTextLength(50).get();
            name.setMessageText("@unknown");

            t.row();
            t.add("@editor.description").padRight(8).left();

            TextArea description = t.area(tags.get("description", ""), Styles.areaField, text -> {
                tags.put("description", text);
            }).size(400f, 140f).maxTextLength(1000).get();

            t.row();
            t.add("@editor.author").padRight(8).left();

            TextField author = t.field(tags.get("author", ""), text -> {
                tags.put("author", text);
            }).size(400, 55f).maxTextLength(50).get();
            author.setMessageText("@unknown");

            t.row();

            t.table(Tex.button, r -> {
                r.defaults().width(230f).height(60f);

                var style = Styles.flatt;

                r.button("@editor.rules", Icon.list, style, () -> {
                    ruleInfo.show(Vars.state.rules, () -> Vars.state.rules = new Rules());
                    preventLandscape = true;
                    hide();
                }).marginLeft(10f);

                r.button("@editor.waves", Icon.units, style, () -> {
                    waveInfo.show();
                    hide();
                }).marginLeft(10f);

                r.row();

                r.button("@editor.objectives", Icon.info, style, () -> {
                    objectives.show(state.rules.objectives.all, state.rules.objectives.all::set);
                    hide();
                }).marginLeft(10f);

                r.button("@editor.generation", Icon.terrain, style, () -> {
                    //randomize so they're not all the same seed
                    var res = maps.readFilters(editor.tags.get("genfilters", ""));
                    res.each(GenerateFilter::randomize);

                    generate.show(res,
                    filters -> {
                        //reset seed to 0 so it is not written
                        filters.each(f -> f.seed = 0);
                        editor.tags.put("genfilters", JsonIO.write(filters));
                    });
                    hide();
                }).marginLeft(10f);
            }).colspan(2).center();

            name.change();
            description.change();
            author.change();

            t.margin(16f);
        });
    }
}
