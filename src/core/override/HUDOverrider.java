package core.override;

import arc.func.Intc;
import arc.scene.Element;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Strings;
import core.ModVars;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

public class HUDOverrider {
    public static void override() {
        try {
            // Numbered team changer
            Intc teamChanger = (s) -> {if (s != Integer.MIN_VALUE) Call.setPlayerTeamEditor(player, Team.get(s));};

            Table editorElem = ui.hudGroup.find("editor");
            editorElem.clear();
            editorElem.table(Tex.buttonEdge4, t -> {
                t.name = "teams";
                t.add("@editor.teams").growX().left();
                t.row();
                t.table(teams -> {
                    teams.left();
                    int i = 0;
                    for(Team team : Team.baseTeams){
                        ImageButton button = teams.button(Tex.whiteui, Styles.clearNoneTogglei, 40f, () -> Call.setPlayerTeamEditor(player, team))
                        .size(50f).margin(6f).get();
                        button.getImageCell().grow();
                        button.getStyle().imageUpColor = team.color;
                        button.update(() -> button.setChecked(player.team() == team));

                        if(++i % 3 == 0){
                            teams.row();
                        }
                    }
                }).left();
                t.table(t2 -> {
                    t2.left();
                    t2.add("@editor.editteam").padLeft(mobile ? 0f : 5f).left().update(a -> a.setColor(player.team().color));
                    t2.row();
                    t2.field(Integer.toString(editor.drawTeam.id), s -> teamChanger.get(Strings.parseInt(s)))
                        .padRight(60f).update(a -> {if (a.getText() != "") a.setText(Integer.toString(player.team().id));})
                        .valid(f -> Strings.parseInt(f) >= 0 && Strings.parseInt(f) <= 255).width(70f).left();
                }).row();

            }).width(65f * 5 + 4f);

        } catch (Exception ex) {
            Log.err(ex.toString());
            ui.showException("Mapping Tools Error", ex);
        }
    }
}
