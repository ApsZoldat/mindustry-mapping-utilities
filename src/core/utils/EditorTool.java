package core.utils;

import arc.func.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static core.ModVars.*;

public enum EditorTool {
    zoom(KeyCode.v),
    pick(KeyCode.i){
        public void touched(int x, int y){
            if(!Structs.inBounds(x, y, editor.width(), editor.height())) return;

            Tile tile = editor.tile(x, y);
            mapEditor.setDrawBlock(tile.block() == Blocks.air || !tile.block().inEditor ? tile.overlay() == Blocks.air ? tile.floor() : tile.overlay() : tile.block());
        }
    },
    line(KeyCode.l, "orthogonal", "erase", "eraseorthogonal"){

        @Override
        public void touchedLine(int x1, int y1, int x2, int y2){
            //straight
            if(mode == 0 || mode == 2){
                if(Math.abs(x2 - x1) > Math.abs(y2 - y1)){
                    y2 = y1;
                }else{
                    x2 = x1;
                }
            }

            Bresenham2.line(x1, y1, x2, y2, (x, y) -> {
                if (mode == 1 || mode == 2) {
                    mapEditor.ODrawBlocks(x, y, tile -> true, tile -> tile.remove());
                } else {
                    mapEditor.ODrawBlocks(x, y);
                }
            });
        }
    },
    pencil(KeyCode.b){
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(int x, int y){
            mapEditor.ODrawBlocks(x, y);
        }
    },
    eraser(KeyCode.e, "eraseores"){
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(int x, int y){
            if (mode == -1) {  // default mode
                mapEditor.ODrawBlocks(x, y, tile -> true, tile -> tile.remove());
            } else if (mode == 0) {
                mapEditor.ODrawBlocks(x, y, tile -> true, tile -> tile.clearOverlay());
            }
        }
    },
    fill(KeyCode.g, "replaceall", "fillteams"){
        {
            edit = true;
        }

        IntSeq stack = new IntSeq();

        @Override
        public void touched(int x, int y){
            if(!Structs.inBounds(x, y, editor.width(), editor.height())) return;
            Tile tile = editor.tile(x, y);

            if (mapEditor.cliffMode) {
                fill(x, y, false, t -> !mapEditor.cliffMatrix.get(t.x, t.y), t -> mapEditor.cliffMatrix.set(t.x, t.y, true)); // EZ
            } else {
                //mode 0 or 1, fill everything with the floor/tile or replace it
                if(mode == 0 || mode == -1){
                    if(editor.drawBlock.isMultiblock()){
                        // don't fill multiblocks, thanks
                        // but fill teams when multiblock is selected, thanks, Anuke
                        pencil.touched(x, y);
                        return;
                    }

                    //can't fill parts or multiblocks
                    if(tile.block().isMultiblock()){
                        return;
                    }

                    Boolf<Tile> tester;
                    Cons<Tile> setter;

                    if(editor.drawBlock.isOverlay()){
                        Block dest = tile.overlay();
                        if(dest == editor.drawBlock) return;
                        tester = t -> t.overlay() == dest && (t.floor().hasSurface() || !t.floor().needsSurface);
                        setter = t -> t.setOverlay(editor.drawBlock);
                    }else if(editor.drawBlock.isFloor()){
                        Block dest = tile.floor();
                        if(dest == editor.drawBlock) return;
                        tester = t -> t.floor() == dest;
                        setter = t -> t.setFloorUnder(editor.drawBlock.asFloor());
                    }else{
                        Block dest = tile.block();
                        if(dest == editor.drawBlock) return;
                        tester = t -> t.block() == dest;
                        setter = t -> t.setBlock(editor.drawBlock, editor.drawTeam);
                    }

                    //replace only when the mode is 0 using the specified functions
                    fill(x, y, mode == 0, tester, setter);
                }else if(mode == 1){ //mode 1 is team fill

                    //only fill synthetic blocks, it's meaningless otherwise
                    if(tile.synthetic()){
                        Team dest = tile.team();
                        if(dest == editor.drawTeam) return;
                        fill(x, y, true, t -> t.getTeamID() == dest.id && t.synthetic(), t -> t.setTeam(editor.drawTeam));
                    }
                }
            }
        }

        void fill(int x, int y, boolean replace, Boolf<Tile> tester, Cons<Tile> filler){
            int width = editor.width(), height = editor.height();

            if(replace){
                //just do it on everything
                for(int cx = 0; cx < width; cx++){
                    for(int cy = 0; cy < height; cy++){
                        Tile tile = editor.tile(cx, cy);
                        if(tester.get(tile)){
                            filler.get(tile);
                        }
                    }
                }

            }else{
                //perform flood fill
                int x1;

                stack.clear();
                stack.add(Point2.pack(x, y));

                try{
                    while(stack.size > 0 && stack.size < width*height){
                        int popped = stack.pop();
                        x = Point2.x(popped);
                        y = Point2.y(popped);

                        x1 = x;
                        while(x1 >= 0 && tester.get(editor.tile(x1, y))) x1--;
                        x1++;
                        boolean spanAbove = false, spanBelow = false;
                        while(x1 < width && tester.get(editor.tile(x1, y))){
                            filler.get(editor.tile(x1, y));

                            if(!spanAbove && y > 0 && tester.get(editor.tile(x1, y - 1))){
                                stack.add(Point2.pack(x1, y - 1));
                                spanAbove = true;
                            }else if(spanAbove && !tester.get(editor.tile(x1, y - 1))){
                                spanAbove = false;
                            }

                            if(!spanBelow && y < height - 1 && tester.get(editor.tile(x1, y + 1))){
                                stack.add(Point2.pack(x1, y + 1));
                                spanBelow = true;
                            }else if(spanBelow && y < height - 1 && !tester.get(editor.tile(x1, y + 1))){
                                spanBelow = false;
                            }
                            x1++;
                        }
                    }
                    stack.clear();
                }catch(OutOfMemoryError e){
                    //hack
                    stack = null;
                    System.gc();
                    e.printStackTrace();
                    stack = new IntSeq();
                }
            }
        }
    },
    spray(KeyCode.q, "erase"){
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(int x, int y){
            if (mode == -1) {
                mapEditor.ODrawBlocks(x, y, tile -> Mathf.chance(mapEditor.sprayChance), null);
            } else if (mode == 0) {
                mapEditor.ODrawBlocks(x, y, tile -> true, tile -> {if (Mathf.chance(mapEditor.sprayChance)) tile.remove();});
            }
        }
    };

    public static final EditorTool[] all = values();

    /** All the internal alternate placement modes of this tool. */
    public final String[] altModes;
    /** Key to activate this tool. */
    public KeyCode key = KeyCode.unset;
    /** The current alternate placement mode. -1 is the standard mode, no changes.*/
    public int mode = -1;
    /** Whether this tool causes canvas changes when touched.*/
    public boolean edit;
    /** Whether this tool should be dragged across the canvas when the mouse moves.*/
    public boolean draggable;

    EditorTool(){
        this(new String[]{});
    }

    EditorTool(KeyCode code){
        this(new String[]{});
        this.key = code;
    }

    EditorTool(String... altModes){
        this.altModes = altModes;
    }

    EditorTool(KeyCode code, String... altModes){
        this.altModes = altModes;
        this.key = code;
    }

    public void touched(int x, int y){}

    public void touchedLine(int x1, int y1, int x2, int y2){}
}
