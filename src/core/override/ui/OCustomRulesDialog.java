package core.override.ui;


import arc.Core;
import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import core.ModVars;
import core.ui.HiddenContentDialog;
import core.ui.PlanetBackgroundDialog;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.Rules.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.CustomRulesDialog;
import mindustry.ui.dialogs.LoadoutDialog;
import mindustry.world.*;
import mindustry.world.meta.Env;

import static arc.util.Time.*;
import static mindustry.Vars.*;

public class OCustomRulesDialog extends CustomRulesDialog {
    Rules rules;
    private Table main;
    private Prov<Rules> resetter;
    private LoadoutDialog loadoutDialog;
    private Boolean customMode = false;
    private BaseDialog backgroundDialog;
    private HiddenContentDialog <Block> bannedBlocks;
    private HiddenContentDialog <Block> revealedBlocks;
    private HiddenContentDialog <UnitType> bannedUnits;
    private int currentNumberedTeam = 0;

    public OCustomRulesDialog() {
        super();

        backgroundDialog = new PlanetBackgroundDialog();
        loadoutDialog = new LoadoutDialog();
        bannedBlocks = new HiddenContentDialog<Block>("@bannedblocks", ContentType.block, Block::canBeBuilt);
        revealedBlocks = new HiddenContentDialog<Block>("@rules.revealedblocks", ContentType.block, b -> true, true);
        bannedUnits = new HiddenContentDialog<UnitType>("@bannedunits", ContentType.unit, u -> !u.isHidden());

        hidden(() -> {
            ModVars.mapEditorDialog.beginLandscape();
        });

        addCloseListener();

        shown(this::setup);
    }

    public void show(Rules rules, Prov<Rules> resetter){
        this.rules = rules;
        this.resetter = resetter;
        super.show(rules, resetter);
    }

    void setup() {
        cont.clear();
        cont.pane(m -> main = m).scrollX(false);
        main.margin(10f);
        main.button("@settings.reset", () -> {
            rules = resetter.get();
            setup();
            requestKeyboard();
            requestScroll();
        }).size(300f, 50f);
        main.left().defaults().fillX().left().pad(5);
        main.row();

        title("@rules.title.waves");
        check("@rules.waves", b -> rules.waves = b, () -> rules.waves);
        check("@rules.wavetimer", b -> rules.waveTimer = b, () -> rules.waveTimer);
        check("@rules.wavesending", b -> rules.waveSending = b, () -> rules.waveSending, () -> rules.waves);
        check("@rules.wavetimer", b -> rules.waveTimer = b, () -> rules.waveTimer, () -> rules.waves);
        check("@rules.waitForWaveToEnd", b -> rules.waitEnemies = b, () -> rules.waitEnemies, () -> rules.waves && rules.waveTimer);
        numberi("@rules.wavelimit", f -> rules.winWave = f, () -> rules.winWave, () -> rules.waves, 0, Integer.MAX_VALUE);
        number("@rules.wavespacing", false, f -> rules.waveSpacing = f * 60f, () -> rules.waveSpacing / 60f, () -> rules.waves && rules.waveTimer, 1, Float.MAX_VALUE);

        number("@rules.initialwavespacing", false, f -> rules.initialWaveSpacing = f * 60f, () -> rules.initialWaveSpacing / 60f, () -> rules.waves && rules.waveTimer, 0, Float.MAX_VALUE);
        
        number("@rules.dropzoneradius", false, f -> rules.dropZoneRadius = f * tilesize, () -> rules.dropZoneRadius / tilesize, () -> rules.waves);

        title("@rules.title.resourcesbuilding");
        check("@rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources);

        check("@rules.onlydepositcore", b -> rules.onlyDepositCore = b, () -> rules.onlyDepositCore);
        check("@rules.reactorexplosions", b -> rules.reactorExplosions = b, () -> rules.reactorExplosions);
        check("@rules.schematic", b -> rules.schematicsAllowed = b, () -> rules.schematicsAllowed);
        check("@rules.coreincinerates", b -> rules.coreIncinerates = b, () -> rules.coreIncinerates);
        check("@rules.cleanupdeadteams", b -> rules.cleanupDeadTeams = b, () -> rules.cleanupDeadTeams, () -> rules.pvp);
        check("@rules.ghostblocks", b -> rules.ghostBlocks = b, () -> rules.ghostBlocks);
        check("@rules.disableworldprocessors", b -> rules.disableWorldProcessors = b, () -> rules.disableWorldProcessors);
        number("@rules.buildcostmultiplier", false, f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, () -> !rules.infiniteResources);
        number("@rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier, 0.001f, 50f);
        number("@rules.deconstructrefundmultiplier", false, f -> rules.deconstructRefundMultiplier = f, () -> rules.deconstructRefundMultiplier, () -> !rules.infiniteResources);
        number("@rules.blockhealthmultiplier", f -> rules.blockHealthMultiplier = f, () -> rules.blockHealthMultiplier);
        number("@rules.blockdamagemultiplier", f -> rules.blockDamageMultiplier = f, () -> rules.blockDamageMultiplier);

        main.button("@configure",
            () -> loadoutDialog.show(999999, rules.loadout,
                i -> true,
                () -> rules.loadout.clear().add(new ItemStack(Items.copper, 100)),
                () -> {}, () -> {}
        )).left().width(300f).row();

        //main.button("@bannedblocks", () -> showBanned("@bannedblocks", ContentType.block, rules.bannedBlocks, Block::canBeBuilt)).left().width(300f).row();
        main.button("@bannedblocks", () -> bannedBlocks.show(rules.bannedBlocks)).left().width(300f).row();
        main.button("@rules.revealedblocks", () -> revealedBlocks.show(rules.revealedBlocks)).left().width(300f).row();

        check("@rules.hidebannedblocks", b -> rules.hideBannedBlocks = b, () -> rules.hideBannedBlocks);
        check("@bannedblocks.whitelist", b -> rules.blockWhitelist = b, () -> rules.blockWhitelist);

        title("@rules.title.unit");

        try {
            String ammotext = Core.bundle.get("rules.unitammo"); // use default bundle text but remove the confusing "(may be removed)"
            if (ammotext.indexOf("[") != -1) ammotext = ammotext.substring(0, ammotext.indexOf("["));
            check(ammotext, b -> rules.unitAmmo = b, () -> rules.unitAmmo);
        } catch (Exception Ex) {
            check("@rules.unitammo", b -> rules.unitAmmo = b, () -> rules.unitAmmo); // just in case someone using minecraft enchanting table language
        }
        
        check("@rules.unitcapvariable", b -> rules.unitCapVariable = b, () -> rules.unitCapVariable);
        check("@rules.posessionallowed", b -> rules.possessionAllowed = b, () -> rules.possessionAllowed);
        check("@rules.logicbuild", b -> rules.logicUnitBuild = b, () -> rules.logicUnitBuild);
        numberi("@rules.unitcap", f -> rules.unitCap = f, () -> rules.unitCap, -999, 999);
        number("@rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier);
        number("@rules.unitcrashdamagemultiplier", f -> rules.unitCrashDamageMultiplier = f, () -> rules.unitCrashDamageMultiplier);
        number("@rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier, 0f, 50f);
        number("@rules.unitcostmultiplier", f -> rules.unitCostMultiplier = f, () -> rules.unitCostMultiplier);
        number("@rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier);

        //main.button("@bannedunits", () -> showBanned("@bannedunits", ContentType.unit, rules.bannedUnits, u -> !u.isHidden())).left().width(300f).row();
        main.button("@bannedunits", () -> bannedUnits.show(rules.bannedUnits)).left().width(300f).row();

        check("@bannedunits.whitelist", b -> rules.unitWhitelist = b, () -> rules.unitWhitelist);

        title("@rules.title.enemy");
        check("@rules.attack", b -> rules.attackMode = b, () -> rules.attackMode);
        check("@rules.corecapture", b -> rules.coreCapture = b, () -> rules.coreCapture);
        check("@rules.coredestroyclear", b -> rules.coreDestroyClear = b, () -> rules.coreDestroyClear);
        main.add("@rules.coredestroyclearinfo").color(Pal.accent).padTop(4f).padRight(100f).padBottom(10f).row();
        check("@rules.placerangecheck", b -> rules.placeRangeCheck = b, () -> rules.placeRangeCheck);
        check("@rules.polygoncoreprotection", b -> rules.polygonCoreProtection = b, () -> rules.polygonCoreProtection);
        number("@rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200), () -> !rules.polygonCoreProtection);

        title("@rules.title.environment");
        check("@rules.explosions", b -> rules.damageExplosions = b, () -> rules.damageExplosions);
        check("@rules.fire", b -> rules.fire = b, () -> rules.fire);
        check("@rules.fog", b -> rules.fog = b, () -> rules.fog);
        check("@rules.staticfog", b -> rules.staticFog = b, () -> rules.staticFog, () -> rules.fog);

        main.button(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui) {{
                    update(() -> setColor(rules.staticColor));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@rules.staticcolor");
        }, () -> ui.picker.show(rules.staticColor, rules.staticColor::set)).left().width(250f).row();

        main.button(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui) {{
                    update(() -> setColor(rules.dynamicColor));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@rules.dynamiccolor");
        }, () -> ui.picker.show(rules.dynamicColor, rules.dynamicColor::set)).left().width(250f).row();

        check("@rules.lighting", b -> rules.lighting = b, () -> rules.lighting);
        check("@rules.disableoutside", b -> rules.disableOutsideArea = b, () -> rules.disableOutsideArea);
        check("@rules.borderdarkness", b -> rules.borderDarkness = b, () -> rules.borderDarkness);

        check("@rules.limitarea", b -> rules.limitMapArea = b, () -> rules.limitMapArea);
        numberi("x", x -> rules.limitX = x, () -> rules.limitX, () -> rules.limitMapArea, 0, 10000);
        numberi("y", y -> rules.limitY = y, () -> rules.limitY, () -> rules.limitMapArea, 0, 10000);
        numberi("w", w -> rules.limitWidth = w, () -> rules.limitWidth, () -> rules.limitMapArea, 0, 10000);
        numberi("h", h -> rules.limitHeight = h, () -> rules.limitHeight, () -> rules.limitMapArea, 0, 10000);

        number("@rules.solarmultiplier", f -> rules.solarMultiplier = f, () -> rules.solarMultiplier);

        main.button(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui) {{
                    update(() -> setColor(rules.ambientLight));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@rules.ambientlight");
        }, () -> ui.picker.show(rules.ambientLight, rules.ambientLight::set)).left().width(250f).row();

        main.button("@rules.weather", this::weatherDialog).width(250f).left().row();

        main.button("@rules.planetbackground", this::backgroundDialog).width(250f).left().row();

        main.button("@rules.envbutton", this::environmentDialog).width(250f).left().row();

        title("@rules.title.planet");

        main.table(Tex.button, t -> {
            t.margin(10f);
            var group = new ButtonGroup<>();
            var style = Styles.flatTogglet;

            t.defaults().size(140f, 50f);

            for(Planet planet : content.planets().select(p -> p.accessible && p.visible && p.isLandable())) {
                t.button(planet.localizedName, style, () -> {
                    rules.env = planet.defaultEnv;
                    rules.attributes.clear();
                    rules.attributes.add(planet.defaultAttributes);
                    rules.hiddenBuildItems.clear();
                    rules.hiddenBuildItems.addAll(planet.hiddenItems);

                    group.uncheckAll();
                }).group(group);

                if(t.getChildren().size % 3 == 0){
                    t.row();
                }
            }

            group.uncheckAll();

            /* that doesn't work uhh
            group.getButtons().asSet().each(b -> {
                if (currentPlanet() != null) {
                    group.setChecked(currentPlanet().localizedName);
                } else {
                    group.uncheckAll();
                }
                
            });*/

            t.button("@rules.anyenv", style, () -> {
                rules.env = Vars.defaultEnv;
                rules.hiddenBuildItems.clear();
            }).group(group).checked(b -> rules.hiddenBuildItems.size == 0);
        }).left().fill(false).expand(false, false).row();

        main.add("@rules.techinfo").color(Pal.accent).padTop(5).padRight(100f).padBottom(-5);
        main.row();

        title("@rules.title.teams");

        team("@rules.playerteam", t -> rules.defaultTeam = t, () -> rules.defaultTeam);
        team("@rules.enemyteam", t -> rules.waveTeam = t, () -> rules.waveTeam);

        for(Team team : Team.baseTeams){
            boolean[] shown = {false};
            Table wasMain = main;

            main.button("[#" + team.color +  "]" + team.localized() + (team.emoji.isEmpty() ? "" : "[] " + team.emoji), Icon.downOpen, Styles.togglet, () -> {
                shown[0] = !shown[0];
            }).marginLeft(14f).width(260f).height(55f).checked(a -> shown[0]).row();

            main.collapser(t -> {
                t.left().defaults().fillX().left().pad(5);
                main = t;
                TeamRule teams = rules.teams.get(team);

                number("@rules.blockhealthmultiplier", f -> teams.blockHealthMultiplier = f, () -> teams.blockHealthMultiplier);
                number("@rules.blockdamagemultiplier", f -> teams.blockDamageMultiplier = f, () -> teams.blockDamageMultiplier);

                check("@rules.cheat", b -> teams.cheat = b, () -> teams.cheat);
                check("@rules.rtsai", b -> teams.rtsAi = b, () -> teams.rtsAi, () -> team != rules.defaultTeam);
                check("@rules.corespawn", b -> teams.aiCoreSpawn = b, () -> teams.aiCoreSpawn);

                numberi("@rules.rtsminsquadsize", f -> teams.rtsMinSquad = f, () -> teams.rtsMinSquad, () -> teams.rtsAi, 0, 100);
                numberi("@rules.rtsmaxsquadsize", f -> teams.rtsMaxSquad = f, () -> teams.rtsMaxSquad, () -> teams.rtsAi, 1, 1000);
                number("@rules.rtsminattackweight", f -> teams.rtsMinWeight = f, () -> teams.rtsMinWeight, () -> teams.rtsAi);

                check("@rules.buildai", b -> teams.buildAi = b, () -> teams.buildAi, () -> team != rules.defaultTeam && rules.env != Planets.erekir.defaultEnv && !rules.pvp);
                number("@rules.buildaitier", false, f -> teams.buildAiTier = f, () -> teams.buildAiTier, () -> teams.buildAi && rules.env != Planets.erekir.defaultEnv && !rules.pvp, 0, 1);

                check("@rules.infiniteresources", b -> teams.infiniteResources = b, () -> teams.infiniteResources);
                number("@rules.buildspeedmultiplier", f -> teams.buildSpeedMultiplier = f, () -> teams.buildSpeedMultiplier, 0.001f, 50f);

                number("@rules.unitdamagemultiplier", f -> teams.unitDamageMultiplier = f, () -> teams.unitDamageMultiplier);
                number("@rules.unitcrashdamagemultiplier", f -> teams.unitCrashDamageMultiplier = f, () -> teams.unitCrashDamageMultiplier);
                number("@rules.unitbuildspeedmultiplier", f -> teams.unitBuildSpeedMultiplier = f, () -> teams.unitBuildSpeedMultiplier, 0.001f, 50f);
                number("@rules.unitcostmultiplier", f -> teams.unitCostMultiplier = f, () -> teams.unitCostMultiplier);
                number("@rules.unithealthmultiplier", f -> teams.unitHealthMultiplier = f, () -> teams.unitHealthMultiplier);

                main = wasMain;
            }, () -> shown[0]).growX().row();
        }

        Table numberedTeamEdit = new Table();
        boolean[] shown = {false};

        numberedTeamEdit.clear();
        numberedTeamEdit.left().defaults().fillX().left().pad(5);

        Cons<Integer> resetNumberedTeam = teamID -> {
            Table wasMain = main;
            main = numberedTeamEdit;

            TeamRule teams = rules.teams.get(Team.get(teamID));

            numberedTeamEdit.clear();
            number("@rules.blockhealthmultiplier", f -> teams.blockHealthMultiplier = f, () -> teams.blockHealthMultiplier);
            number("@rules.blockdamagemultiplier", f -> teams.blockDamageMultiplier = f, () -> teams.blockDamageMultiplier);

            check("@rules.cheat", b -> teams.cheat = b, () -> teams.cheat);
            check("@rules.rtsai", b -> teams.rtsAi = b, () -> teams.rtsAi, () -> Team.get(teamID) != rules.defaultTeam);
            check("@rules.corespawn", b -> teams.aiCoreSpawn = b, () -> teams.aiCoreSpawn);

            numberi("@rules.rtsminsquadsize", f -> teams.rtsMinSquad = f, () -> teams.rtsMinSquad, () -> teams.rtsAi, 0, 100);
            numberi("@rules.rtsmaxsquadsize", f -> teams.rtsMaxSquad = f, () -> teams.rtsMaxSquad, () -> teams.rtsAi, 1, 1000);
            number("@rules.rtsminattackweight", f -> teams.rtsMinWeight = f, () -> teams.rtsMinWeight, () -> teams.rtsAi);

            check("@rules.infiniteresources", b -> teams.infiniteResources = b, () -> teams.infiniteResources);
            number("@rules.buildspeedmultiplier", f -> teams.buildSpeedMultiplier = f, () -> teams.buildSpeedMultiplier, 0.001f, 50f);

            number("@rules.unitdamagemultiplier", f -> teams.unitDamageMultiplier = f, () -> teams.unitDamageMultiplier);
            number("@rules.unitcrashdamagemultiplier", f -> teams.unitCrashDamageMultiplier = f, () -> teams.unitCrashDamageMultiplier);
            number("@rules.unitbuildspeedmultiplier", f -> teams.unitBuildSpeedMultiplier = f, () -> teams.unitBuildSpeedMultiplier, 0.001f, 50f);
            number("@rules.unitcostmultiplier", f -> teams.unitCostMultiplier = f, () -> teams.unitCostMultiplier);
            number("@rules.unithealthmultiplier", f -> teams.unitHealthMultiplier = f, () -> teams.unitHealthMultiplier);


            main = wasMain;
        };

        numberi("@rules.numberedteam", f -> {
            currentNumberedTeam = f;
            resetNumberedTeam.get(currentNumberedTeam);
        }, () -> currentNumberedTeam, 0, 255);

        main.row();
        
        main.button("[#" + Team.get(currentNumberedTeam).color +  "]" + Integer.toString(currentNumberedTeam), Icon.downOpen, Styles.togglet, () -> {
            shown[0] = !shown[0];
        }).marginLeft(14f).width(260f).height(55f).checked(a -> shown[0]).update(b -> b.setText("[#" + Team.get(currentNumberedTeam).color +  "]" + Integer.toString(currentNumberedTeam))).row();
        main.collapser(numberedTeamEdit, () -> shown[0]);
        main.row();

        title("@rules.misc");
        main.add("@rules.misc.warning").color(Pal.accent).padTop(-10).padRight(100f).padBottom(1);
        main.row();

        check("@rules.misc.pvppause", b -> rules.pvpAutoPause = b, () -> rules.pvpAutoPause);
        check("@rules.misc.cangameover", b -> rules.canGameOver = b, () -> rules.canGameOver);
        check("@rules.misc.unitpayupdate", b -> rules.unitPayloadUpdate = b, () -> rules.unitPayloadUpdate);

        number("@rules.misc.drag", f -> rules.dragMultiplier = f, () -> rules.dragMultiplier);
        main.row();

        customMode = (rules.modeName != null);
        check("@rules.misc.custommode", b -> {
            if (b) {
                customMode = true;
            } else {
                rules.modeName = null;
                customMode = false;
            }
        }, () -> rules.modeName != null);
        text("@rules.misc.modename", s -> rules.modeName = s, () -> (rules.modeName == null ? "Gamemode" : rules.modeName), () -> customMode);
    }

    void team(String text, Cons<Team> cons, Prov<Team> prov) {
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5);

            for(Team team : Team.baseTeams){
                t.button(Tex.whiteui, Styles.squareTogglei, 38f, () -> {
                    cons.get(team);
                }).pad(1f).checked(b -> prov.get() == team).size(60f).tooltip(team.localized()).with(i -> i.getStyle().imageUpColor = team.color);
            }
        }).padTop(0).row();
    }

    void text(String text, Cons<String> cons, Prov<String> prov, Boolp condition) {
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
                .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            t.field((prov.get()) + "", s -> cons.get(s))
                .update(a -> a.setDisabled(!condition.get()))
                .padRight(100f)
                .valid(s -> s.length() <= 256).left();
        }).padTop(0).row();
    }

    void number(String text, Floatc cons, Floatp prov) {
        number(text, false, cons, prov, () -> true, 0, Float.MAX_VALUE);
    }

    void number(String text, Floatc cons, Floatp prov, float min, float max) {
        number(text, false, cons, prov, () -> true, min, max);
    }

    void number(String text, boolean integer, Floatc cons, Floatp prov, Boolp condition) {
        number(text, integer, cons, prov, condition, 0, Float.MAX_VALUE);
    }

    void number(String text, Floatc cons, Floatp prov, Boolp condition) {
        number(text, false, cons, prov, condition, 0, Float.MAX_VALUE);
    }

    void numberi(String text, Intc cons, Intp prov, int min, int max) {
        numberi(text, cons, prov, () -> true, min, max);
    }

    void numberi(String text, Intc cons, Intp prov, Boolp condition, int min, int max) {
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
                .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            t.field((prov.get()) + "", s -> cons.get(Strings.parseInt(s)))
                .update(a -> a.setDisabled(!condition.get()))
                .padRight(100f)
                .valid(f -> Strings.parseInt(f) >= min && Strings.parseInt(f) <= max).width(120f).left();
        }).padTop(0).row();
    }

    void number(String text, boolean integer, Floatc cons, Floatp prov, Boolp condition, float min, float max) {
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
            .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            t.field((integer ? (int)prov.get() : prov.get()) + "", s -> cons.get(Strings.parseFloat(s)))
            .padRight(100f)
            .update(a -> a.setDisabled(!condition.get()))
            .valid(f -> Strings.canParsePositiveFloat(f) && Strings.parseFloat(f) >= min && Strings.parseFloat(f) <= max).width(120f).left();
        }).padTop(0);
        main.row();
    }

    void check(String text, Boolc cons, Boolp prov) {
        check(text, cons, prov, () -> true);
    }

    void check(String text, Boolc cons, Boolp prov, Boolp condition) {
        main.check(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get())).padRight(100f).get().left();
        main.row();
    }

    void title(String text) {
        main.add(text).color(Pal.accent).padTop(20).padRight(100f).padBottom(-3);
        main.row();
        main.image().color(Pal.accent).height(3f).padRight(100f).padBottom(20);
        main.row();
    }

    Cell<TextField> field(Table table, float value, Floatc setter) {
        return table.field(Strings.autoFixed(value, 2), v -> setter.get(Strings.parseFloat(v)))
            .valid(Strings::canParsePositiveFloat)
            .size(90f, 40f).pad(2f);
    }

    void weatherDialog() {
        BaseDialog dialog = new BaseDialog("@rules.weather");
        Runnable[] rebuild = {null};

        dialog.cont.pane(base -> {

            rebuild[0] = () -> {
                base.clearChildren();
                int cols = Math.max(1, (int)(Core.graphics.getWidth() / Scl.scl(450)));
                int idx = 0;

                for(WeatherEntry entry : rules.weather){
                    base.top();
                    //main container
                    base.table(Tex.pane, c -> {
                        c.margin(0);

                        //icons to perform actions
                        c.table(Tex.whiteui, t -> {
                            t.setColor(Pal.gray);

                            t.top().left();
                            t.add(entry.weather.localizedName).left().padLeft(6);

                            t.add().growX();

                            ImageButtonStyle style = Styles.geni;
                            t.defaults().size(42f);

                            t.button(Icon.cancel, style, () -> {
                                rules.weather.remove(entry);
                                rebuild[0].run();
                            });
                        }).growX();

                        c.row();

                        //all the options
                        c.table(f -> {
                            f.marginLeft(4);
                            f.left().top();

                            f.defaults().padRight(4).left();

                            f.add("@rules.weather.duration");
                            field(f, entry.minDuration / toMinutes, v -> entry.minDuration = v * toMinutes).disabled(v -> entry.always);
                            f.add("@waves.to");
                            field(f, entry.maxDuration / toMinutes, v -> entry.maxDuration = v * toMinutes).disabled(v -> entry.always);
                            f.add("@unit.minutes");

                            f.row();

                            f.add("@rules.weather.frequency");
                            field(f, entry.minFrequency / toMinutes, v -> entry.minFrequency = v * toMinutes).disabled(v -> entry.always);
                            f.add("@waves.to");
                            field(f, entry.maxFrequency / toMinutes, v -> entry.maxFrequency = v * toMinutes).disabled(v -> entry.always);
                            f.add("@unit.minutes");

                            f.row();

                            f.check("@rules.weather.always", val -> entry.always = val).checked(cc -> entry.always).padBottom(4);

                            //intensity can't currently be customized

                        }).grow().left().pad(6).top();
                    }).width(410f).pad(3).top().left().fillY();

                    if(++idx % cols == 0){
                        base.row();
                    }
                }
            };

            rebuild[0].run();
        }).grow();

        dialog.addCloseButton();

        dialog.buttons.button("@add", Icon.add, () -> {
            BaseDialog add = new BaseDialog("@add");
            add.cont.pane(t -> {
                t.background(Tex.button);
                int i = 0;
                for(Weather weather : content.<Weather>getBy(ContentType.weather)){
                    if(weather.hidden) continue;

                    t.button(weather.localizedName, Styles.flatt, () -> {
                        rules.weather.add(new WeatherEntry(weather));
                        rebuild[0].run();

                        add.hide();
                    }).size(140f, 50f);
                    if(++i % 2 == 0) t.row();
                }
            });
            add.addCloseButton();
            add.show();
        }).width(170f);

        dialog.show();
    }

    void changeEnv(CheckBox check, int envVar) {
        if (check.isChecked()) {
            Vars.state.rules.env = Vars.state.rules.env | envVar;
        } else {
            Vars.state.rules.env = Vars.state.rules.env & ~envVar;
        }
    }

    void envCheck(Table tb, String text, int envVar, String description) {
        CheckBox check = new CheckBox(text);
        check.changed(() -> {changeEnv(check, envVar);});
        check.setChecked((Vars.state.rules.env & envVar) != 0);
        check.left();
        tb.add(check);
        tb.row();

        Cell<Label> desc = tb.add(description);
        desc.get().setWidth(600f);
        desc.get().setWrap(true);
        tb.row();
    }

    void backgroundDialog() {
        backgroundDialog.show();
    }

    void environmentDialog() {
        BaseDialog dialog = new BaseDialog("@rules.envdialog");
        dialog.cont.pane(m -> main = m);

        main.left().defaults().fillX().left().pad(5);

        main.add("@rules.env.warning").color(Pal.accent).padTop(20).padRight(100f).padBottom(3);
        main.row();

        envCheck(main, "@rules.env.terrestrial", Env.terrestrial, "@rules.env.terrestrial.description");
        envCheck(main, "@rules.env.space", Env.space, "@rules.env.space.description");
        envCheck(main, "@rules.env.underwater", Env.underwater, "@rules.env.underwater.description");
        envCheck(main, "@rules.env.spores", Env.spores, "@rules.env.spores.description");
        envCheck(main, "@rules.env.scorching", Env.scorching, "@rules.env.scorching.description");
        envCheck(main, "@rules.env.groundOil", Env.groundOil, "@rules.env.groundOil.description");
        envCheck(main, "@rules.env.groundWater", Env.groundWater, "@rules.env.groundWater.description");
        envCheck(main, "@rules.env.oxygen", Env.oxygen, "@rules.env.oxygen.description");

        dialog.addCloseButton();

        dialog.show();
    }

    /*
    Planet currentPlanet() {
        ObjectSet<Item> planetEnabledItems = new ObjectSet<Item>();
        ObjectSet<Item> currentEnabledItems = new ObjectSet<Item>();

        for (Planet planet : content.planets()) {
            currentEnabledItems.clear();
            planetEnabledItems.clear();
    
            for (Item item : content.items()) {
                if (!rules.hiddenBuildItems.contains(item)) currentEnabledItems.add(item);
                if (!planet.hiddenItems.contains(item)) planetEnabledItems.add(item);
            }
            if (currentEnabledItems.equals(planetEnabledItems)) return planet;
        }

        return null;
    }*/
    // TODO: make planet buttons checking
}
