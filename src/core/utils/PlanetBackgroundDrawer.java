package core.utils;

import arc.graphics.*;
import arc.graphics.gl.FrameBuffer;
import arc.util.Nullable;
import mindustry.graphics.g3d.PlanetRenderer;

import static mindustry.Vars.*;
import static arc.Core.*;

public class PlanetBackgroundDrawer {
    static private @Nullable FrameBuffer backgroundBuffer;
    static private PlanetRenderer planets = new PlanetRenderer();

    static public int size = Math.max(graphics.getWidth(), graphics.getHeight());
    static public float drawSize = Math.max(camera.width, camera.height);

    static public Texture draw() {
        if (state.rules.planetBackground == null) {
            return new Texture(0, 0);
        }

        size = Math.max(graphics.getWidth(), graphics.getHeight());

        boolean resized = false;
        if(backgroundBuffer == null){
            resized = true;
            backgroundBuffer = new FrameBuffer(size, size);
        }

        if(resized || backgroundBuffer.resizeCheck(size, size)){
            backgroundBuffer.begin(Color.clear);

            var params = state.rules.planetBackground;

            //override some values
            params.viewW = size;
            params.viewH = size;
            params.alwaysDrawAtmosphere = true;
            params.drawUi = false;


            planets.render(params);

            backgroundBuffer.end();
        }

        drawSize = Math.max(camera.width, camera.height);
        return backgroundBuffer.getTexture();
    }
}
