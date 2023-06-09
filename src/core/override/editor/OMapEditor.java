package core.override.editor;

import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.struct.GridBits;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import core.ModVars;
import mindustry.content.Blocks;
import mindustry.editor.EditorTile;
import core.utils.EditorTool;
import mindustry.editor.MapEditor;
import mindustry.editor.DrawOperation.OpType;
import mindustry.gen.Building;
import mindustry.gen.TileOp;
import mindustry.io.MapIO;
import mindustry.maps.Map;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.WorldContext;

import static mindustry.Vars.*;

public class OMapEditor extends MapEditor {
    public boolean cliffMode = false; 
    public boolean squareMode = false;
    public boolean replaceMode = false;
    public boolean drawTeamsMode = false;

    public double sprayChance = 0.012;

    public GridBits cliffMatrix;
    private boolean loading;

    public EditorTool currentTool = EditorTool.pencil;

    private final Context context = new Context();

    public void setDrawBlock(Block block) {
        drawBlock = block;
    }

    public void clearCliffMatrix(int width, int height) {
        cliffMatrix = new GridBits(width, height);
    }

    public void cliffMatrixApply(boolean down) {
        for(Tile tile : world.tiles){
            if(!cliffMatrix.get(tile.x, tile.y)) continue;

            int rotation = 0;
            if (!down) {
                for(int i = 0; i < 8; i++){
                    Tile other = world.tiles.get((tile.x + Geometry.d8[i].x), (tile.y + Geometry.d8[i].y));
                    if (other != null) {
                        Boolean otherBit = cliffMatrix.get(other.x, other.y);
    
                        if(other != null && !otherBit){
                            rotation |= (1 << i);
                        }
                    }
                }
            } else {
                boolean inMiddle = true;

                for (int j = 0; j < 4; j++) {
                    if (inMiddle) {
                        inMiddle = cliffMatrix.get((tile.x + Geometry.d4[j].x), (tile.y + Geometry.d4[j].y));
                    }
                }

                if (!inMiddle) {
                    for(int i = 0; i < 8; i++) {
                        Tile other = world.tiles.get((tile.x + Geometry.d8[i].x), (tile.y + Geometry.d8[i].y));
    
                        if (other == null) break;
    
                        Boolean otherBit = cliffMatrix.get(other.x, other.y);
                        if (otherBit) {
                            inMiddle = true;
    
                            for (int j = 0; j < 4; j++) {
                                if (inMiddle) {
                                    inMiddle = cliffMatrix.get((other.x + Geometry.d4[j].x), (other.y + Geometry.d4[j].y));
                                }
                            }
            
                            if (inMiddle) {
                                rotation |= (1 << i);
                            }
                        }
                    }
                }
            }

            if (rotation != 0) {
                tile.setBlock(Blocks.cliff);
                tile.data = (byte)rotation;
            } else {
                if (tile.block() == Blocks.cliff) {
                    tile.setBlock(Blocks.air);
                }
            }
        }

        clearCliffMatrix(width(), height());
    }

    public void cliffMatrixClear() {
        clearCliffMatrix(width(), height());
    }

    // these methods are being used by old map editor dialog class that has some annoying listeners
    // so they do nothing
    @Override
    public void beginEdit(int width, int height) {return;}

    @Override
    public void beginEdit(Map map) {return;}

    @Override
    public void beginEdit(Pixmap pixmap) {return;}

    public void OBeginEdit(int width, int height){
        reset();

        loading = true;
        createTiles(width, height);
        renderer.resize(width, height);
        clearCliffMatrix(width, height);
        loading = false;
    }

    public void OBeginEdit(Map map){
        reset();

        loading = true;
        tags.putAll(map.tags);
        if(map.file.parent().parent().name().equals("1127400") && steam){
            tags.put("steamid",  map.file.parent().name());
        }
        load(() -> MapIO.loadMap(map, context));
        renderer.resize(width(), height());
        clearCliffMatrix(width(), height());
        loading = false;
    }

    public void OBeginEdit(Pixmap pixmap){
        reset();

        createTiles(pixmap.width, pixmap.height);
        load(() -> MapIO.readImage(pixmap, tiles()));
        renderer.resize(width(), height());
        clearCliffMatrix(width(), height());
    }

    private void reset(){
        clearOp();
        brushSize = 1;
        drawBlock = Blocks.stone;
        tags = new StringMap();
    }

    private void createTiles(int width, int height){
        Tiles tiles = world.resize(width, height);

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                tiles.set(x, y, new EditorTile(x, y, Blocks.stone.id, (short)0, (short)0));
            }
        }
    }

    public void drawBlocksReplace(int x, int y){
        drawBlocks(x, y, tile -> tile.block() != Blocks.air || drawBlock.isFloor());
    }

    public void drawBlocks(int x, int y){
        drawBlocks(x, y, false, false, tile -> true);
    }

    public void drawBlocks(int x, int y, Boolf<Tile> tester){
        drawBlocks(x, y, false, false, tester);
    }

    // these methods are being used by old map editor class that has some annoying listeners
    // so they do nothing
    public void drawBlocks(int x, int y, boolean square, boolean forceOverlay, Boolf<Tile> tester){ }
    public void drawCircle(int x, int y, Cons<Tile> drawer){}
    public void drawSquare(int x, int y, Cons<Tile> drawer){}

    public void ODrawBlocks(int x, int y){
        ODrawBlocks(x, y, tile -> true, null);
    }

    // leave customDrawer null to use default drawers
    public void ODrawBlocks(int x, int y, Boolf<Tile> tester, Cons<Tile> customDrawer) {
        if(drawBlock.isMultiblock() && !cliffMode && !drawTeamsMode && customDrawer == null){
            x = Mathf.clamp(x, (drawBlock.size - 1) / 2, width() - drawBlock.size / 2 - 1);
            y = Mathf.clamp(y, (drawBlock.size - 1) / 2, height() - drawBlock.size / 2 - 1);
            if(!hasOverlap(x, y)){
                tile(x, y).setBlock(drawBlock, drawTeam, rotation);
            }
        }else{
            Cons<Tile> drawer;
            if (customDrawer != null) {  // set custom drawer
                drawer = customDrawer;
            }
            else {
                if (drawTeamsMode) {  // set team changer drawer
                    drawer = tile -> tile.setTeam(editor.drawTeam);
                } else {  // set default drawer
                    drawer = tile -> {
                        boolean isFloor = drawBlock.isFloor() && drawBlock != Blocks.air;
                        
                        if (replaceMode && !(tile.block() != Blocks.air || drawBlock.isFloor())) return;
                        if (!tester.get(tile)) return;

                        if(isFloor){
                            if (tile.floor().isLiquid && drawBlock.isOverlay()) {  // underliquid drawing
                                tile.setOverlayQuiet(drawBlock);
                                renderer.updatePoint(tile.x, tile.y);
                            } else if(!(drawBlock.asFloor().wallOre && !tile.block().solid)){
                                tile.setFloor(drawBlock.asFloor());
                            }
                        }else if(!(tile.block().isMultiblock() && !drawBlock.isMultiblock())){
                            if(drawBlock.rotate && tile.build != null && tile.build.rotation != rotation){
                                addTileOp(TileOp.get(tile.x, tile.y, (byte)OpType.rotation.ordinal(), (byte)rotation));
                            }

                            tile.setBlock(drawBlock, drawTeam, rotation);
                        }
                    };
                }
            }

            if(squareMode){
                ODrawSquare(x, y, drawer);
            }else{
                ODrawCircle(x, y, drawer);
            }
        }
    }

    public void ODrawCircle (int x, int y, Cons<Tile> drawer) {
        int clamped = (int)brushSize;
        for(int rx = -clamped; rx <= clamped; rx++){
            for(int ry = -clamped; ry <= clamped; ry++){
                if(Mathf.within(rx, ry, brushSize - 0.5f + 0.0001f)){
                    int wx = x + rx, wy = y + ry;

                    if(wx < 0 || wy < 0 || wx >= width() || wy >= height()){
                        continue;
                    }

                    if (cliffMode && currentTool != EditorTool.zoom) {
                        cliffMatrix.set(wx, wy, currentTool != EditorTool.eraser);
                    } else {
                        drawer.get(tile(wx, wy));
                    }
                }
            }
        }
    }

    public void ODrawSquare (int x, int y, Cons<Tile> drawer) {
        int clamped = (int)brushSize;
        for(int rx = -clamped; rx <= clamped; rx++){
            for(int ry = -clamped; ry <= clamped; ry++){
                int wx = x + rx, wy = y + ry;

                if(wx < 0 || wy < 0 || wx >= width() || wy >= height()){
                    continue;
                }

                if (cliffMode && currentTool != EditorTool.zoom) {
                    cliffMatrix.set(wx, wy, currentTool != EditorTool.eraser);
                } else {
                    drawer.get(tile(wx, wy));
                }
            }
        }
    }

    boolean hasOverlap(int x, int y){
        Tile tile = world.tile(x, y);
        //allow direct replacement of blocks of the same size
        if(tile != null && tile.isCenter() && tile.block() != drawBlock && tile.block().size == drawBlock.size && tile.x == x && tile.y == y){
            return false;
        }

        //else, check for overlap
        int offsetx = -(drawBlock.size - 1) / 2;
        int offsety = -(drawBlock.size - 1) / 2;
        for(int dx = 0; dx < drawBlock.size; dx++){
            for(int dy = 0; dy < drawBlock.size; dy++){
                int worldx = dx + offsetx + x;
                int worldy = dy + offsety + y;
                Tile other = world.tile(worldx, worldy);

                if(other != null && other.block().isMultiblock()){
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isLoading(){
        return loading;
    }

    public void updateRenderer(){
        Tiles tiles = world.tiles;
        Seq<Building> builds = new Seq<>();

        for(int i = 0; i < tiles.width * tiles.height; i++){
            Tile tile = tiles.geti(i);
            var build = tile.build;
            if(build != null){
                builds.add(build);
            }
            tiles.seti(i, new EditorTile(tile.x, tile.y, tile.floorID(), tile.overlayID(), build == null ? tile.blockID() : 0));
        }

        for(var build : builds){
            tiles.get(build.tileX(), build.tileY()).setBlock(build.block, build.team, build.rotation, () -> build);
        }

        renderer.resize(width(), height());
    }

    public void load(Runnable r){
        loading = true;
        r.run();
        loading = false;
    }

    public Tiles tiles(){
        return world.tiles;
    }

    public Tile tile(int x, int y){
        return world.rawTile(x, y);
    }

    public int width(){
        return world.width();
    }

    public int height(){
        return world.height();
    }
    
    class Context implements WorldContext{
        @Override
        public Tile tile(int index){
            return world.tiles.geti(index);
        }

        @Override
        public void resize(int width, int height){
            world.resize(width, height);
        }

        @Override
        public Tile create(int x, int y, int floorID, int overlayID, int wallID){
            Tile tile = new EditorTile(x, y, floorID, overlayID, wallID);
            tiles().set(x, y, tile);
            return tile;
        }

        @Override
        public boolean isGenerating(){
            return world.isGenerating();
        }

        @Override
        public void begin(){
            world.beginMapLoad();
        }

        @Override
        public void end(){
            world.endMapLoad();
        }
    }
}
