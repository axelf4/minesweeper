package minesweeper;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;

public class BoardRenderer extends Widget implements EventListener {
	private static final int TILE_SIZE = 32;
	private static final int WIDTH = 30, HEIGHT = 16, NUM_MINES = 99;
	// private static final int WIDTH = 10000, HEIGHT = 10000, NUM_MINES = 20000000;
	private Texture tileTexture;
	private ShaderProgram shader;
	private Texture sprites;
	private Board board;
	private ByteBuffer pixels;
	private Vector2 viewOffset = new Vector2();
	private float scale = 1.0f;
	private Vector2 lastSize = new Vector2(0, 0);
	private Stage stage;
	private Skin skin;

	private static final Vector2 tmpCoords = new Vector2();

	private enum TileSprite {
		HIDDEN(0, 0), REVEALED(1, 0), FLAG(2, 0),
		DIGIT_1(0, 1), DIGIT_2(1, 1), DIGIT_3(2, 1), DIGIT_4(3, 1), DIGIT_5(0, 2), DIGIT_6(1, 2),
		DIGIT_7(2, 2), DIGIT_8(3, 2);

		public final int x, y;

		TileSprite(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public BoardRenderer(Stage stage, Skin skin) {
		init();
		setTouchable(Touchable.enabled);
		addListener(this);
		addListener(new BoardGestureListener());
		this.stage = stage;
		this.skin = skin;
	}

	private void init() {
		sprites = new Texture("tilesheet.png");
		// board = Board.generate(WIDTH, HEIGHT, NUM_MINES);
		board = new FastBoardBuilder(WIDTH, HEIGHT, NUM_MINES).build();

		pixels = ByteBuffer.allocateDirect(3 * WIDTH * HEIGHT);
		tileTexture = new Texture(WIDTH, HEIGHT, Pixmap.Format.RGB888);
		tileTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		for (int i = 0; i < 3 * WIDTH * HEIGHT; i += 3) {
			final TileSprite sprite = TileSprite.HIDDEN;
			pixels.put(i, (byte) sprite.x);
			pixels.put(i + 1, (byte) sprite.y);
			pixels.put(i + 2, (byte) 0);
			i += 3;
		}
		Gdx.gl.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGB, WIDTH, HEIGHT, 0, GL20.GL_RGB, GL20.GL_UNSIGNED_BYTE, pixels);

		// Start with the board centered
		viewOffset.add(new Vector2(WIDTH, HEIGHT).scl(TILE_SIZE).scl(scale).scl(0.5f));

		final String vertexShader = "attribute vec4 a_position;\n"
				+ "attribute vec4 a_color;\n"
				+ "attribute vec2 a_texCoord0;\n"
				+ "uniform mat4 u_projTrans;\n"
				+ "varying vec2 v_texCoords;\n"
				+ "varying vec2 pixelCoord;\n"
				+ "varying vec2 texCoord;\n"
				+ "uniform vec2 viewOffset;\n"
				+ "uniform vec2 viewSize;\n"
				+ "uniform vec2 inverseTileSize;\n"
				+ "uniform vec2 inverseTileTextureSize;\n"
				+ "void main() {\n"
				+ "	pixelCoord = a_texCoord0 * viewSize + viewOffset;\n"
				+ "	texCoord = pixelCoord * inverseTileTextureSize * inverseTileSize;\n"
				+ " v_texCoords = a_texCoord0;\n"
				+ " gl_Position = u_projTrans * a_position;\n"
				+ "}\n",
				fragmentShader = "#ifdef GL_ES\n"
						+ "precision mediump float;\n"
						+ "#endif\n"
						+ "varying vec2 v_texCoords;\n"
						+ "varying vec2 pixelCoord;\n"
						+ "varying vec2 texCoord;\n"
						+ "uniform sampler2D u_texture;\n"
						+ "uniform sampler2D sprites;\n"
						+ "uniform vec2 inverseSpriteTextureSize;\n"
						+ "uniform float tileSize;\n"
						+ "void main() {\n"
						+ "	if (texCoord.x < .0 || texCoord.y < .0 || texCoord.x > 1. || texCoord.y > 1.) discard;\n"
						+ "	vec4 tile = texture2D(u_texture, texCoord);\n"
						+ "	vec2 spriteOffset = floor(tile.xy * 256.0) * tileSize;\n"
						+ "	vec2 spriteCoord = mod(pixelCoord, tileSize);\n"
						+ "	gl_FragColor = texture2D(sprites, (spriteOffset + spriteCoord) * inverseSpriteTextureSize);\n"
						+ "}\n";
		shader = new ShaderProgram(vertexShader, fragmentShader);
		if (!shader.isCompiled()) {
		}
		System.out.println(shader.getLog());
		shader.begin();
		shader.setUniformf("tileSize", TILE_SIZE);
		shader.setUniformf("inverseTileSize", 1f / TILE_SIZE, 1f / TILE_SIZE);
		shader.setUniformf("inverseTileTextureSize", 1f / WIDTH, 1f / HEIGHT);
		shader.setUniformf("inverseSpriteTextureSize", 1f / sprites.getWidth(), 1f / sprites.getHeight());
		shader.setUniformi("sprites", 1);
		shader.end();
	}

	@Override
	public void layout() {
		// Keep the center centered
		final Vector2 newSize = new Vector2(getWidth(), getHeight());
		viewOffset.add(lastSize.sub(newSize).scl(0.5f));
		lastSize = newSize;
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		validate();
		ShaderProgram oldShader = batch.getShader();

		sprites.bind(1);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		float width = getWidth(), height = getHeight();
		batch.setShader(shader);
		shader.setUniformf("viewSize", scale * width, scale * height);
		shader.setUniformf("viewOffset", viewOffset);
		batch.draw(tileTexture, getX(), getY(), width, height, 0f, 1f, 1f, 0f);

		batch.setShader(oldShader);
	}

	private static final TileSprite spriteByTile[] = {
			/* 0x0 */ TileSprite.HIDDEN,
			TileSprite.HIDDEN, TileSprite.HIDDEN, TileSprite.HIDDEN, TileSprite.HIDDEN,
			TileSprite.HIDDEN, TileSprite.HIDDEN, TileSprite.HIDDEN, TileSprite.HIDDEN,
			null, null, null, null, null, null, null,
			/* 16 */ TileSprite.HIDDEN,
			TileSprite.HIDDEN, TileSprite.HIDDEN, TileSprite.HIDDEN, TileSprite.HIDDEN, TileSprite.HIDDEN,
			TileSprite.HIDDEN, TileSprite.HIDDEN, TileSprite.HIDDEN,
			null, null,
			null, null, null, null, null,
			/* 32 */ TileSprite.REVEALED,
			TileSprite.DIGIT_1, TileSprite.DIGIT_2, TileSprite.DIGIT_3, TileSprite.DIGIT_4,
			TileSprite.DIGIT_5, TileSprite.DIGIT_6, TileSprite.DIGIT_7, TileSprite.DIGIT_8,
			/* 41 */ null, null, null, null, null, null, null, /* 48: revealed with mine */ null,
			/* 49 */ null, null, null, null, null, null, null, null,
			/* 57 */ null, null, null,
			/* 60 */ null, null, null, null,
			/* 64 FLAG */ TileSprite.FLAG,
			TileSprite.FLAG, TileSprite.FLAG, TileSprite.FLAG, TileSprite.FLAG,
			TileSprite.FLAG, TileSprite.FLAG, TileSprite.FLAG, TileSprite.FLAG,
			null, null, null, null, null, null, null,
			/* 80: flag with mine */ TileSprite.FLAG,
			TileSprite.FLAG, TileSprite.FLAG, TileSprite.FLAG, TileSprite.FLAG,
			TileSprite.FLAG, TileSprite.FLAG, TileSprite.FLAG, TileSprite.FLAG,
	};

	private void updateRegion(Board board, int x1, int y1, int x2, int y2) {
		System.out.println("minX: " + x1 + " minY: " + y1 + " maxX: " + x2 + " maxY: " + y2);
		// Early out if nothing to update
		if (x2 - x1 == 0 || y2 - y1 == 0) return;

		int i = 0;
		for (int y = y1; y < y2; ++y) {
			for (int x = x1; x < x2; ++x) {
				TileSprite sprite = spriteByTile[board.getTile(x, y)];
				assert sprite != null;

				pixels.put(i, (byte) sprite.x);
				pixels.put(i + 1, (byte) sprite.y);
				pixels.put(i + 2, (byte) 0);
				i += 3;
			}
		}

		tileTexture.bind(0);
		final int width = x2 - x1, height = y2 - y1;
		Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, x1, y1, width, height, GL20.GL_RGB, GL20.GL_UNSIGNED_BYTE, pixels);
	}

	/**
	 * Returns whether or not the player has placed the correct number of flags around a tile.
	 *
	 * @param board The current board state.
	 * @param x     The x-coordinate of the tile.
	 * @param y     The y-coordinate of the tile.
	 * @return Returns whether surrounding tiles should be cleared.
	 */
	private boolean shouldClearAround(Board board, int x, int y) {
		if ((board.getTile(x, y) & Board.REVEALED_BIT) == 0)
			throw new IllegalArgumentException();

		final int neighbouringMines = board.getNeighbouringMineCount(x, y);
		final long neighbouringFlags = board.getNeighbouringTiles(x, y)
				.filter(c -> (board.getTile(c) & Board.FLAG_BIT) != 0).count();

		return neighbouringFlags == neighbouringMines;
	}

	private void onWin() {
		Dialog dialog = new Dialog("Win", skin, "dialog") {
			public void result(Object obj) {
			}
		};
		dialog.text("You won!");
		dialog.button("OK"); // Sends "true" as the result
		dialog.show(stage);
	}

	private void onGameOver() {
		Dialog dialog = new Dialog("Game over", skin, "dialog") {
			public void result(Object obj) {
			}
		};
		dialog.text("You blew up.");
		dialog.button("OK"); // Sends "true" as the result
		dialog.show(stage);
	}

	/**
	 * Returns the coordinates of the tile under the cursor.
	 *
	 * @param x The cursor x-coordinate in the coordinate system of the BoardRenderer.
	 * @param y The cursor y-coordinate in the coordinate system of the BoardRenderer.
	 * @return The coordinates of the tile.
	 */
	private Coord getCoordUnderCursor(float x, float y) {
		return new Coord((int) ((x * scale + viewOffset.x) / TILE_SIZE),
				(int) ((y * scale + viewOffset.y) / TILE_SIZE));
	}

	enum MouseIntent {
		CLICK, SET_FLAG;

		static MouseIntent getIntentFromTapButton(int button) {
			if (Gdx.app.getType() == Application.ApplicationType.Android) return MouseIntent.SET_FLAG;
			switch (button) {
				default:
				case Input.Buttons.LEFT:
					return MouseIntent.CLICK;
				case Input.Buttons.RIGHT:
					return MouseIntent.SET_FLAG;
			}
		}
	}

	private void performMouseAction(float x, float y, MouseIntent intent) {
		y = getHeight() - y;
		final Coord c = getCoordUnderCursor(x, y);
		if (board.isOutOfBounds(c)) return;

		final int tileType = board.getTile(c);
		if ((tileType & Board.REVEALED_BIT) != 0) {
			// Chord
			if (shouldClearAround(board, c.x, c.y)) {
				Board.ClearTileResult clearResult = board.clearTiles(board.getNeighbouringTiles(c.x, c.y).toArray(Coord[]::new));
				board = clearResult.board;
				if (board.getRemainingTiles() == 0) {
					onWin();
				} else if (clearResult.wasMine) {
					onGameOver();
				} else {
					updateRegion(board, clearResult.minX, clearResult.minY, clearResult.maxX, clearResult.maxY);
				}
			}
		} else {
			if (intent == MouseIntent.CLICK) {
				Board.ClearTileResult clearResult = board.clearTiles(c);
				board = clearResult.board;
				if (board.getRemainingTiles() == 0) {
					onWin();
				} else if (clearResult.wasMine) {
					onGameOver();
				} else {
					updateRegion(board, clearResult.minX, clearResult.minY, clearResult.maxX, clearResult.maxY);
				}
			} else if (intent == MouseIntent.SET_FLAG) {
				board = board.toggleFlag(c.x, c.y);
				updateRegion(board, c.x, c.y, c.x + 1, c.y + 1);
			}
		}
	}


	@Override
	public boolean handle(Event e) {
		if (!(e instanceof InputEvent)) return false;
		InputEvent event = (InputEvent) e;
		event.toCoordinates(event.getListenerActor(), tmpCoords);

		switch (event.getType()) {
			case scrolled:
				return scrolled(tmpCoords.x, tmpCoords.y, event.getScrollAmount());
		}
		return false;
	}

	private boolean scrolled(float x, float y, int amount) {
		final float oldScale = scale;
		scale += amount * 0.3f;
		if (scale <= 0) scale = 1e-4f;

		// Keep the cursor in the same place
		viewOffset.add(new Vector2(x, getHeight() - y).scl(oldScale - scale));

		return true;
	}

	private class BoardGestureListener extends ActorGestureListener {
		private Vector2 curInitialPointer1 = new Vector2(), curInitialPointer2 = new Vector2();

		BoardGestureListener() {
			super(20, 0.4f, 0.5f, 0.15f);
		}

		@Override
		public void tap(InputEvent event, float x, float y, int count, int button) {
			performMouseAction(x, y, MouseIntent.getIntentFromTapButton(button));
		}

		@Override
		public boolean longPress(Actor actor, float x, float y) {
			performMouseAction(x, y, MouseIntent.CLICK);
			return true;
		}

		@Override
		public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
			viewOffset.sub(scale * deltaX, scale * -deltaY);
		}

		@Override
		public void pinch(InputEvent event, Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
			if (initialPointer1.epsilonEquals(pointer1) && initialPointer2.epsilonEquals(pointer2)) {
				curInitialPointer1.set(initialPointer1);
				curInitialPointer2.set(initialPointer2);
			}

			final float oldScale = scale;
			final float scaleFactor = curInitialPointer1.dst(curInitialPointer2) / pointer1.dst(pointer2);
			if (Math.abs(scaleFactor - 1) < 0.1) {
				scale *= scaleFactor;
				if (scale <= 0) scale = 1e-4f;

				Vector2 middle = curInitialPointer1.cpy().add(curInitialPointer2).scl(.5f);
				Vector2 middle2 = pointer1.cpy().add(pointer2).scl(.5f);
				Vector2 diff = middle2.cpy().sub(middle);

				// Keep the cursor in the same place
				viewOffset.add(new Vector2(middle2.x, getHeight() - middle2.y).scl(oldScale - scale));

				pan(event, pointer1.x, pointer1.y, diff.x, diff.y);
			}

			curInitialPointer1.set(pointer1);
			curInitialPointer2.set(pointer2);
		}
	}
}
