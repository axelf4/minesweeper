package minesweeper.desktop;

import minesweeper.GdxGame;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "JadenSweeper.exe";
		config.useHDPI = true;
		/*config.foregroundFPS = 0;
		config.vSyncEnabled = false;*/
		new LwjglApplication(new GdxGame(), config);
	}
}
