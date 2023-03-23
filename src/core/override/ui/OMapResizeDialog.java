package core.override.ui;


import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.editor.MapResizeDialog;
import mindustry.graphics.Pal;

import static mindustry.Vars.*;

public class OMapResizeDialog extends MapResizeDialog {
    public static int minSize = 1, maxSize = 2048, increment = 64;

    int width, height, shiftX, shiftY;

    public OMapResizeDialog(ResizeListener cons){
        super(cons);

        closeOnBack();
        shown(() -> {
            renderer.minimap.reset();
            renderer.minimap.updateAll();

            cont.clear();
            width = editor.width();
            height = editor.height();

            Table table = new Table();

            for(boolean w : Mathf.booleans){
                table.add(w ? "@width" : "@height").padRight(8f);
                table.defaults().height(60f).padTop(8);

                table.field((w ? width : height) + "", TextFieldFilter.digitsOnly, value -> {
                    int val = Integer.parseInt(value);
                    if(w) width = val; else height = val;
                }).valid(value -> Strings.canParsePositiveInt(value) && Integer.parseInt(value) <= maxSize && Integer.parseInt(value) >= minSize).maxTextLength(4);

                table.row();
            }

            for(boolean x : Mathf.booleans){
                table.add(x ? "@editor.shiftx" : "@editor.shifty").padRight(8f);
                table.defaults().height(60f).padTop(8);

                table.field((x ? shiftX : shiftY) + "", value -> {
                    int val = Integer.parseInt(value);
                    if(x) shiftX = val; else shiftY = val;
                }).valid(Strings::canParseInt).maxTextLength(4);

                table.row();
            }

            cont.row();
            cont.add(table);


            TextureRegion map = Draw.wrap(renderer.minimap.getTexture());
            cont.image(map).maxSize(400f).minSize(400f);

            cont.row();
            cont.add("@editor.maxsize").color(Pal.accent).padTop(-40).align(Align.center);
        
            buttons.clear();
            buttons.button("@cancel", this::hide);
            buttons.button("@ok", () -> {
                cons.get(width, height, shiftX, shiftY);
                hide();
            });
        });
    }
}

