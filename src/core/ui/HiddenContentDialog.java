package core.ui;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.content.Blocks;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.Category;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;

import static mindustry.Vars.*;

public class HiddenContentDialog <T extends UnlockableContent> extends BaseDialog {
    private ContentType type;
    private ObjectSet<T> set;
    private Boolf<T> pred;
    private BaseDialog addDialog;
    private Table buttonTable;
    private Table buttonTableAdd;

    private boolean isRevealedBlocks;

    private String searchText = "";
    private Category selectedCategory = null;

    public HiddenContentDialog(String title, ContentType type, Boolf<T> pred) {
        super(title);

        this.type = type;
        this.pred = pred;

        if (type == ContentType.block) {
            // 400 iq gigachad move
            if (((Boolf<Block>)pred).get(Blocks.cliff)) isRevealedBlocks = true;
        }

        addDialog = new BaseDialog("@add");
        addDialog.addCloseButton();
        addDialog.shown(this::rebuildAddDialog);

        addCloseButton();

        buttons.button("@addall", Icon.add, () -> {
            set.addAll(content.<T>getBy(type).select(pred));
            rebuild();
        }).size(180, 64f);

        buttons.button("@clear", Icon.trash, () -> {
            set.clear();
            rebuild();
        }).size(180, 64f);
        
        shown(this::rebuild);
    }

    // Use only this show() method
    public void show(ObjectSet<T> set) {
        this.set = set;
        super.show();
    }

    private void rebuild() {
        cont.clear();
        cont.table(search -> {
            search.image(Icon.zoom).padRight(8);
            search.field(searchText, this::rebuildPane).maxTextLength(maxNameLength).get().setMessageText("@players.search");
        }).pad(-2).row();
        if (type == ContentType.block && !isRevealedBlocks) {
            cont.table(t -> {
                t.marginTop(8f);
                t.defaults().marginRight(4f);
                for (Category category : Category.values()) {
                    t.button(ui.getIcon(category.name()), Styles.emptyTogglei, () -> {
                        if (selectedCategory == category) {
                            selectedCategory = null;
                        } else {
                            selectedCategory = category;
                        }
                        rebuildPane(searchText);
                    }).update(i -> i.setChecked(selectedCategory == category)).padRight(6f);
                }
            });
            cont.row();
        }

        buttonTable = cont.table().get();
        
        cont.row();
        cont.button("@add", Icon.add, () -> {addDialog.show();}).size(300f, 64f);

        rebuildPane(searchText);
    };

    private void rebuildPane(String search) {
        searchText = search;

        float previousScroll = buttonTable.getChildren().isEmpty() ? 0f : ((ScrollPane)buttonTable.getChildren().first()).getScrollY();

        buttonTable.clear();

        buttonTable.pane(t -> {
            t.margin(10f);

            if(set.isEmpty()){
                t.add("@empty");
            }

            Seq<T> array = set.toSeq();
            array.sort();

            int cols = mobile && Core.graphics.isPortrait() ? 1 : mobile ? 2 : 3;
            int i = 0;

            for(T con : array){
                if (selectedCategory != null && type == ContentType.block) {
                    if (((Block)con).category != selectedCategory) continue;
                }

                if (search.isEmpty() || con.localizedName.toLowerCase().contains(search.toLowerCase())) {
                    t.table(Tex.underline, b -> {
                        b.left().margin(4f);
                        b.image(con.uiIcon).size(iconMed).padRight(3);
                        b.add(con.localizedName).color(Color.lightGray).padLeft(3).growX().left().wrap();
    
                        b.button(Icon.cancel, Styles.clearNonei, () -> {
                            set.remove(con);
                            rebuild();
                        }).size(70f).pad(-4f).padLeft(0f);
                    }).size(300f, 70f).padRight(5);
    
                    if(++i % cols == 0){
                        t.row();
                    }
                }
            }
        }).get().setScrollYForce(previousScroll);
    }

    private void rebuildAddDialog() {
        addDialog.cont.clear();
        addDialog.cont.table(search -> {
            search.image(Icon.zoom).padRight(8);
            search.field(searchText, this::rebuildAddDialogPane).maxTextLength(maxNameLength).get().setMessageText("@players.search");
        }).pad(-2).row();
        if (type == ContentType.block && !isRevealedBlocks) {
            addDialog.cont.table(t -> {
                t.marginTop(8f);
                t.defaults().marginRight(4f);
                for (Category category : Category.values()) {
                    t.button(ui.getIcon(category.name()), Styles.emptyTogglei, () -> {
                        if (selectedCategory == category) {
                            selectedCategory = null;
                        } else {
                            selectedCategory = category;
                        }
                        rebuildAddDialogPane(searchText);
                    }).update(i -> i.setChecked(selectedCategory == category)).padRight(6f);
                }
            });
            addDialog.cont.row();
        }
        buttonTableAdd = addDialog.cont.table().get();
        rebuildAddDialogPane(searchText);
    }

    private void rebuildAddDialogPane(String search) {
        searchText = search;
        buttonTableAdd.clear();
        buttonTableAdd.pane(t -> {
            t.left().margin(14f);
            int[] i = {0};
            content.<T>getBy(type).each(
                b -> !set.contains(b) && pred.get(b) && 
                (search.isEmpty() || b.localizedName.toLowerCase().contains(search.toLowerCase())),
                
                b -> {
                int cols = mobile && Core.graphics.isPortrait() ? 4 : 12;
                if (type == ContentType.block) {
                    if (selectedCategory != null && ((Block)b).category != selectedCategory) return;
                }
                t.button(new TextureRegionDrawable(b.uiIcon), Styles.flati, iconMed, () -> {
                    set.add(b);
                    rebuild();
                    rebuildAddDialog();
                }).tooltip(b.localizedName).size(60f);

                if(++i[0] % cols == 0){
                    t.row();
                }
            });
        });
    }
}
