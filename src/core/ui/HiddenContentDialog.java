package core.ui;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ScrollPane;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;

public class HiddenContentDialog <T extends UnlockableContent> extends BaseDialog {
    private ContentType type;
    private ObjectSet<T> set;
    private Boolf<T> pred;
    private BaseDialog addDialog;

    public HiddenContentDialog(String title, ContentType type, Boolf<T> pred) {
        super(title);

        this.type = type;
        this.pred = pred;

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
        float previousScroll = cont.getChildren().isEmpty() ? 0f : ((ScrollPane)cont.getChildren().first()).getScrollY();

        cont.clear();
        cont.pane(t -> {
            t.margin(10f);

            if(set.isEmpty()){
                t.add("@empty");
            }

            Seq<T> array = set.toSeq();
            array.sort();

            int cols = mobile && Core.graphics.isPortrait() ? 1 : mobile ? 2 : 3;
            int i = 0;

            for(T con : array){
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
        }).get().setScrollYForce(previousScroll);
        cont.row();
        cont.button("@add", Icon.add, () -> {addDialog.show();}).size(300f, 64f);
    };

    private void rebuildAddDialog() {
        addDialog.cont.clear();
        addDialog.cont.pane(t -> {
            t.left().margin(14f);
            int[] i = {0};
            content.<T>getBy(type).each(b -> !set.contains(b) && pred.get(b), b -> {
                int cols = mobile && Core.graphics.isPortrait() ? 4 : 12;
                t.button(new TextureRegionDrawable(b.uiIcon), Styles.flati, iconMed, () -> {
                    set.add(b);
                    rebuild();
                    rebuildAddDialog();
                }).size(60f);

                if(++i[0] % cols == 0){
                    t.row();
                }
            });
        });
    }
}
