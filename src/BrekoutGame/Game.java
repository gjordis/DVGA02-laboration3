/* Skapad av Jonas Schymberg
 * Kurs: DVGA02
 * VT - 24
 * Uppgift: Laboration2 - Breakoutspel */

package BrekoutGame;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.*;

public class Game {

	private BrickCollection brickCollection;
	private GameBoard board;
	private GameStateManager gameStateManager;
	private Paddle paddle;
	private ArrayList<Ball> ball = new ArrayList<Ball>();
	private ScoreBoard scoreBoard;
	private int tickCount = 0;

	public Game(GameBoard board) {
		this.board = board;
		brickCollection = new BrickCollection(Const.BRICKCOLLECTION_START_X, Const.BRICKCOLLECTION_START_Y,
				Const.BRICKCOLLECTION_WIDTH, Const.BRICKCOLLECTION_HEIGHT, Const.BRICKCOLLECTION_SPACING,
				board.getPreferredSize().width);
		paddle = new Paddle(board.getPreferredSize().width / 2 - 75, board.getPreferredSize().height - 25,
				Const.PADDLE_WIDTH, Const.PADDLE_HEIGHT, Const.PADDLE_NAME);
		ball.add(new Ball(Const.DEFAULT_VALUE, Const.DEFAULT_VALUE, Const.BALL_DIAMETER, Const.BALL_SPEED_X,
				Const.BALL_SPEED_Y, true));
		scoreBoard = new ScoreBoard(1, 1, 1, 1, brickCollection, board, this);
		gameStateManager = new GameStateManager(this);
	}

	public void update(Keyboard keyboard) {

		tickCount++;

		/* hanterar paus */
		gameStateManager.managePause(keyboard);
		if (gameStateManager.getState() == State.PAUSED)
			return;

		paddle.move(keyboard);
		paddle.isInsideWindow(board.getWidth());

		gameStateManager.manageWinLoss(keyboard);

		/* Inga brickor kvar på planen */
		if (brickCollection.checkVictory()) {
			gameStateManager.setState(State.VICTORY);
			ball.clear();
		}

		/* Inga bollar kvar på planen */
		else if (ball.isEmpty()) {
			scoreBoard.reduceBallsLeft();
			if (scoreBoard.getBallsLeft() <= 0)
				gameStateManager.setState(State.GAMEOVER);
			else
				ball.add(new Ball(Const.DEFAULT_VALUE, Const.DEFAULT_VALUE, Const.BALL_DIAMETER, Const.BALL_SPEED_X,
						Const.BALL_SPEED_Y, true));

		}
		manageBallBrickLogics(keyboard);
	}

	public void draw(Graphics2D graphics) {
		scoreBoard.draw(graphics);
		brickCollection.draw(graphics);
		paddle.draw(graphics);
		for (Ball b : ball) {
			b.draw(graphics);
		}

		if (gameStateManager.getState() == State.PAUSED) {
			graphics.setColor(Color.CYAN.brighter());
			graphics.setFont(new Font("Arial", Font.ITALIC, Const.SCOREBOARD_FONTSIZE_LARGE));
			int textWidth = board.getWidth() / 2 - graphics.getFontMetrics().stringWidth("PAUSED") / 2;
			graphics.drawString("PAUSED", textWidth, board.getHeight() / 3 + 80);
		}
	}

	public void powerUpExtraBalls(ColoredBrick hitBrick, List<Ball> newBalls) {
		if (hitBrick.getPowerUp() == "multiBall") {
			Random random = new Random();
			for (int i = 0; i < 5; i++) {

				int speedX = random.nextInt(-10, 10);
				int speedY = 10;

				newBalls.add(new Ball(hitBrick.getX() + hitBrick.getWidth() / 2 * i,
						hitBrick.getY() + hitBrick.getHeight() * i, 20, speedX, speedY, false));
			}
		}
	}

	public void restartGame() {
		scoreBoard.resetBallsLeft();
		brickCollection.resetScoreCount();
		ball.clear();
		this.brickCollection = new BrickCollection(Const.BRICKCOLLECTION_START_X, Const.BRICKCOLLECTION_START_Y,
				Const.BRICKCOLLECTION_WIDTH, Const.BRICKCOLLECTION_HEIGHT, Const.BRICKCOLLECTION_SPACING,
				board.getPreferredSize().width);
		// skickar med den nya kollektionen till scoreboard för att kunna räkna poäng
		scoreBoard.countScoreOn(brickCollection);
		gameStateManager.setState(State.RUNNING);
	}

	public void manageBallBrickLogics(Keyboard keyboard) {
		/*
		 * Hanterar möjligheten till fler än en boll genom att ha en lista med alla
		 * bollar
		 */
		List<Ball> newBalls = new ArrayList<>();
		Iterator<Ball> iterator = ball.iterator();
		while (iterator.hasNext()) {

			Ball b = iterator.next();
			b.update(keyboard);
			b.initiationPosition(paddle);
			b.hitWall(board.getWidth(), board.getHeight());

			/* boll träffar paddel */
			if (b.getBounds().intersects(paddle.getBounds()))
				b.hitPaddle(board.getWidth(), board.getHeight(), paddle);

			/* Släpper bollen från paddel */
			if (keyboard.isKeyDown(Key.Space) && b.getStartPosition())
				b.setBallToPaddle(false);

			/* !!Skall tas bord!! Används för test */
			if (keyboard.isKeyDown(Key.Enter))
				b.resetBall();

			/*
			 * Kontrollerar om en bricka är träffad genom att använda logiken i hitByBall
			 * metoden i brickCollection. Vid träff returneras den träffade brickan.
			 */
			ColoredBrick hitBrick = brickCollection.hitByBall(b);
			if (hitBrick != null) {
				hitBrick.hit();
				hitBrick.updateColor(hitBrick.getHp());
				if (hitBrick.getHp() <= 0) {
					powerUpExtraBalls(hitBrick, newBalls);
					brickCollection.removeBrick(hitBrick);

				}
				b.reverseY();
			}

			/* Rensar bort döda bollar */
			if (!b.getAliveStatus()) {
				iterator.remove();
			}

		}
		/* Lägger till objekt som adderats från powerups */
		ball.addAll(newBalls);
	}

	public int getTickCount() {
		return tickCount;
	}

	public void resetTickCount() {
		tickCount = 0;
	}
	
	public State getState() {
		return gameStateManager.getState();
	}

}
