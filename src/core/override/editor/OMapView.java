package core.override.editor;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.ScissorStack;
import arc.input.GestureDetector;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Bresenham2;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Scl;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import core.ModVars;
import mindustry.content.Blocks;
import core.utils.EditorTool;
import mindustry.editor.MapEditor;
import mindustry.editor.MapView;
import mindustry.graphics.Pal;
import mindustry.input.Binding;
import mindustry.ui.GridImage;
import mindustry.world.Tile;

import static mindustry.Vars.*;
import static core.ModVars.*;

public class OMapView extends MapView {
    EditorTool tool = EditorTool.pencil;
    private float offsetx, offsety;
    private float zoom = 1f;
    private boolean grid = false;
    private GridImage image = new GridImage(0, 0);
    private Vec2 vec = new Vec2();
    private Rect rect = new Rect();
    private Vec2[] brushPolygons;

    public boolean showCliffs;

    boolean drawing;
    int lastx, lasty;
    int startx, starty;
    float mousex, mousey;
    EditorTool lastTool;

    public OMapView(){
        oldMapView.setTool(mindustry.editor.EditorTool.zoom);

        float size = ModVars.mapEditor.brushSize;
        brushPolygons = Geometry.pixelCircle(size, (index, x, y) -> Mathf.dst(x, y, index - size % 1f, index - size % 1f) <= size - 0.5f);

        Core.input.getInputProcessors().insert(0, new GestureDetector(20, 0.5f, 2, 0.15f, this));
        this.touchable = Touchable.enabled;

        Point2 firstTouch = new Point2();

        addListener(new InputListener(){
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                mousex = x;
                mousey = y;
                requestScroll();

                return false;
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                requestScroll();
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(pointer != 0){
                    return false;
                }

                if(!mobile && button != KeyCode.mouseLeft && button != KeyCode.mouseMiddle && button != KeyCode.mouseRight){
                    return true;
                }
                
                if(button == KeyCode.mouseRight){
                    lastTool = tool;
                    tool = EditorTool.eraser;
                    mapEditor.currentTool = EditorTool.eraser;
                }

                if(button == KeyCode.mouseMiddle){
                    lastTool = tool;
                    tool = EditorTool.zoom;
                    mapEditor.currentTool = EditorTool.zoom;
                }

                mousex = x;
                mousey = y;

                Point2 p = project(x, y);
                lastx = p.x;
                lasty = p.y;
                startx = p.x;
                starty = p.y;
                tool.touched(p.x, p.y);
                firstTouch.set(p);

                if(tool.edit){
                    ui.editor.resetSaved();
                }

                drawing = true;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(!mobile && button != KeyCode.mouseLeft && button != KeyCode.mouseMiddle && button != KeyCode.mouseRight){
                    return;
                }

                drawing = false;

                Point2 p = project(x, y);

                if(tool == EditorTool.line){
                    ui.editor.resetSaved();
                    tool.touchedLine(startx, starty, p.x, p.y);
                }

                mapEditor.flushOp();

                if((button == KeyCode.mouseMiddle || button == KeyCode.mouseRight) && lastTool != null){
                    tool = lastTool;
                    mapEditor.currentTool = lastTool;
                    lastTool = null;
                }

            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                mousex = x;
                mousey = y;

                Point2 p = project(x, y);

                if(drawing && tool.draggable && !(p.x == lastx && p.y == lasty)){
                    ui.editor.resetSaved();
                    Bresenham2.line(lastx, lasty, p.x, p.y, (cx, cy) -> tool.touched(cx, cy));
                }

                if(tool == EditorTool.line && (tool.mode == 0 || tool.mode == 2)){
                    if(Math.abs(p.x - firstTouch.x) > Math.abs(p.y - firstTouch.y)){
                        lastx = p.x;
                        lasty = firstTouch.y;
                    }else{
                        lastx = firstTouch.x;
                        lasty = p.y;
                    }
                }else{
                    lastx = p.x;
                    lasty = p.y;
                }
            }
        });
    }

    public void recalculateBrushPoly() {
        float size = ModVars.mapEditor.brushSize;
        brushPolygons = Geometry.pixelCircle(size, (index, x, y) -> Mathf.dst(x, y, index - size % 1f, index - size % 1f) <= size - 0.5f);
    }

    public mindustry.editor.EditorTool getTool() { // one more "fake" method
        return null;
    }

    public EditorTool OGetTool() {
        return tool;
    }

    public void setTool(EditorTool tool){
        this.tool = tool;
        mapEditor.currentTool = tool;
    }

    public boolean isGrid(){
        return grid;
    }

    public void setGrid(boolean grid){
        this.grid = grid;
    }

    public void center(){
        offsetx = offsety = 0;
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(Core.scene.getKeyboardFocus() == null || !Core.scene.hasField() && !Core.input.keyDown(KeyCode.controlLeft)){
            float ax = Core.input.axis(Binding.move_x);
            float ay = Core.input.axis(Binding.move_y);
            offsetx -= ax * 15 * Time.delta / zoom;
            offsety -= ay * 15 * Time.delta / zoom;
        }

        if(Core.input.keyTap(KeyCode.shiftLeft)){
            lastTool = tool;
            tool = EditorTool.pick;
            mapEditor.currentTool = EditorTool.pick;
        }

        if(Core.input.keyRelease(KeyCode.shiftLeft) && lastTool != null){
            tool = lastTool;
            mapEditor.currentTool = lastTool;
            lastTool = null;
        }

        if(Core.scene.getScrollFocus() != this) return;

        zoom += Core.input.axis(Binding.zoom) / 10f * zoom;
        clampZoom();
    }

    private void clampZoom(){
        zoom = Mathf.clamp(zoom, 0.2f, 20f);
    }

    Point2 project(float x, float y){
        float ratio = 1f / ((float)mapEditor.width() / mapEditor.height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        x = (x - getWidth() / 2 + sclwidth / 2 - offsetx * zoom) / sclwidth * mapEditor.width();
        y = (y - getHeight() / 2 + sclheight / 2 - offsety * zoom) / sclheight * mapEditor.height();

        if(mapEditor.drawBlock.size % 2 == 0 && tool != EditorTool.eraser){
            return Tmp.p1.set((int)(x - 0.5f), (int)(y - 0.5f));
        }else{
            return Tmp.p1.set((int)x, (int)y);
        }
    }

    private Vec2 unproject(int x, int y){
        float ratio = 1f / ((float)mapEditor.width() / mapEditor.height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        float px = ((float)x / mapEditor.width()) * sclwidth + offsetx * zoom - sclwidth / 2 + getWidth() / 2;
        float py = ((float)(y) / mapEditor.height()) * sclheight
        + offsety * zoom - sclheight / 2 + getHeight() / 2;
        return vec.set(px, py);
    }

    @Override
    public void draw(){
        float ratio = 1f / ((float)mapEditor.width() / mapEditor.height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        float centerx = x + width / 2 + offsetx * zoom;
        float centery = y + height / 2 + offsety * zoom;

        image.setImageSize(mapEditor.width(), mapEditor.height());

        if(!ScissorStack.push(rect.set(x + Core.scene.marginLeft, y + Core.scene.marginBottom, width, height))){
            return;
        }

        Draw.color(Pal.remove);
        Lines.stroke(2f);
        Lines.rect(centerx - sclwidth / 2 - 1, centery - sclheight / 2 - 1, sclwidth + 2, sclheight + 2);
        mapEditor.renderer.draw(centerx - sclwidth / 2 + Core.scene.marginLeft, centery - sclheight / 2 + Core.scene.marginBottom, sclwidth, sclheight);
        Draw.reset();

        if(grid){
            Draw.color(Color.gray);
            image.setBounds(centerx - sclwidth / 2, centery - sclheight / 2, sclwidth, sclheight);
            image.draw();

            Lines.stroke(2f);
            Draw.color(Pal.bulletYellowBack);
            Lines.line(centerx - sclwidth/2f, centery - sclheight/4f, centerx + sclwidth/2f, centery - sclheight/4f);
            Lines.line(centerx - sclwidth/4f, centery - sclheight/2f, centerx - sclwidth/4f, centery + sclheight/2f);
            Lines.line(centerx - sclwidth/2f, centery + sclheight/4f, centerx + sclwidth/2f, centery + sclheight/4f);
            Lines.line(centerx + sclwidth/4f, centery - sclheight/2f, centerx + sclwidth/4f, centery + sclheight/2f);

            Lines.stroke(3f);
            Draw.color(Pal.accent);
            Lines.line(centerx - sclwidth/2f, centery, centerx + sclwidth/2f, centery);
            Lines.line(centerx, centery - sclheight/2f, centerx, centery + sclheight/2f);

            Draw.reset();
        }

        int index = 0;
        for(int i = 0; i < MapEditor.brushSizes.length; i++){
            if(mapEditor.brushSize == MapEditor.brushSizes[i]){
                index = i;
                break;
            }
        }

        float scaling = zoom * Math.min(width, height) / mapEditor.width();

        if (mapEditor.cliffMode) {
            Draw.color(Pal.reactorPurple);
        } else {
            Draw.color(Pal.accent);
        }

        Lines.stroke(Scl.scl(2f));

        if((!mapEditor.drawBlock.isMultiblock() || mapEditor.cliffMode || mapEditor.drawTeamsMode || tool == EditorTool.eraser
        || (tool == EditorTool.spray && tool.mode == 0) || (tool == EditorTool.line && (tool.mode == 1 || tool.mode == 2))) && tool != EditorTool.fill){
            if(tool == EditorTool.line && drawing){
                Vec2 v1 = unproject(startx, starty).add(x, y);
                float sx = v1.x, sy = v1.y;
                Vec2 v2 = unproject(lastx, lasty).add(x, y);

                // straight
                if(tool.mode == 0 || tool.mode == 2){
                    if(Math.abs(lastx - startx) > Math.abs(lasty - starty)){
                        v2 = unproject(lastx, starty).add(x, y);
                    } else {
                        v2 = unproject(startx, lasty).add(x, y);
                    }
                }

                if(mapEditor.squareMode){
                    Lines.square(sx + scaling/2f, sy + scaling/2f, scaling * ((mapEditor.brushSize == 1.5f ? 1f : mapEditor.brushSize) + 0.5f));
                    Lines.square(v2.x + scaling/2f, v2.y + scaling/2f, scaling * ((mapEditor.brushSize == 1.5f ? 1f : mapEditor.brushSize) + 0.5f));
                }else{
                    Lines.poly(brushPolygons, sx, sy, scaling);
                    Lines.poly(brushPolygons, v2.x, v2.y, scaling);
                }

                Lines.line(sx + scaling/2f, sy + scaling/2f, v2.x + scaling/2f, v2.y + scaling/2f);
            }

            if((tool.edit || (tool == EditorTool.line && !drawing)) && (!mobile || drawing)){
                Point2 p = project(mousex, mousey);
                Vec2 v = unproject(p.x, p.y).add(x, y);

                if(mapEditor.squareMode){
                    Lines.square(v.x + scaling/2f, v.y + scaling/2f, scaling * ((mapEditor.brushSize == 1.5f ? 1f : mapEditor.brushSize) + 0.5f));
                }else{
                    Lines.poly(brushPolygons, v.x, v.y, scaling);
                }
            }
        }else{
            if((tool.edit || tool == EditorTool.line) && (!mobile || drawing)){
                Point2 p = project(mousex, mousey);
                Vec2 v = unproject(p.x, p.y).add(x, y);
                float offset = (mapEditor.drawBlock.size % 2 == 0 ? scaling / 2f : 0f);

                if((tool == EditorTool.line) && (tool.mode == 0 || tool.mode == 2) && drawing) {
                    if(Math.abs(lastx - startx) > Math.abs(lasty - starty)){
                        v = unproject(lastx, starty).add(x, y);
                    } else {
                        v = unproject(startx, lasty).add(x, y);
                    }
                }

                Lines.square(
                v.x + scaling / 2f + offset,
                v.y + scaling / 2f + offset,
                scaling * mapEditor.drawBlock.size / 2f);

                if(tool == EditorTool.line && drawing){
                    Vec2 v1 = unproject(startx, starty).add(x, y);
                    float sx = v1.x, sy = v1.y;
                    Vec2 v2 = unproject(lastx, lasty).add(x, y);

                    Lines.square(
                    sx + scaling / 2f + offset,
                    sy + scaling / 2f + offset,
                    scaling * mapEditor.drawBlock.size / 2f);

                    Lines.line(sx + scaling/2f, sy + scaling/2f, v2.x + scaling/2f, v2.y + scaling/2f);
                }
            }
        }

        Draw.color(Pal.accent);
        Lines.stroke(Scl.scl(3f));
        Lines.rect(x, y, width, height);

        if (mapEditor.cliffMode) {
            Draw.color(Color.valueOf("8a73c688"));

            for (Tile tile : world.tiles) {
                Boolean cliff_marked = mapEditor.cliffMatrix.get(tile.x, tile.y);

                if (cliff_marked != null && cliff_marked) {
                    Vec2 v = unproject(tile.x, tile.y).add(x, y);

                    Draw.rect(Core.atlas.white(), v.x + scaling/2f, v.y + scaling/2f, scaling, scaling);
                }
            }
        }

        Draw.color(new Color(255f, 255f, 255f, 255f));

        if (showCliffs) {
            for (Tile tile : world.tiles) {
                if (tile.block() == Blocks.cliff) {
                    Vec2 v = unproject(tile.x, tile.y).add(x, y);

                    Draw.rect(Core.atlas.find("cliff"), v.x + scaling/2f, v.y + scaling/2f, scaling, scaling);
                }
            }
        }

        Draw.reset();

        ScissorStack.pop();
    }

    private boolean active(){
        return Core.scene != null && Core.scene.getKeyboardFocus() != null
        && Core.scene.getKeyboardFocus().isDescendantOf(ui.editor)
        && ui.editor.isShown() && tool == EditorTool.zoom &&
        Core.scene.hit(Core.input.mouse().x, Core.input.mouse().y, true) == this;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY){
        if(!active()) return false;
        offsetx += deltaX / zoom / 2;  // old editor pan works too
        offsety += deltaY / zoom / 2;
        
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance){
        if(!active()) return false;
        float nzoom = distance - initialDistance;
        zoom += nzoom / 10000f / Scl.scl(1f) * zoom / 2;
        clampZoom();
        return false;
    }

    @Override
    public boolean pinch(Vec2 initialPointer1, Vec2 initialPointer2, Vec2 pointer1, Vec2 pointer2){
        return false;
    }

    @Override
    public void pinchStop(){

    }
}
