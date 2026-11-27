package edu.farmingdale.csc311capstone.Pacman;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.util.EnumSet;
import static edu.farmingdale.csc311capstone.Pacman.PacmanMechanics.*;

/** Input, screen composition, sound and lifecycle; all rules remain in PacmanMechanics. */
public class PacmanController {
    @FXML private BorderPane root;
    @FXML private StackPane board;
    @FXML private Canvas canvas;
    @FXML private ScrollPane menuScroll;
    @FXML private VBox overlay;
    @FXML private Label scoreLabel,livesLabel,levelLabel,modeLabel,objectiveLabel;
    @FXML private Button pauseButton,restartButton,muteButton;
    private final PacmanAudio audio=new PacmanAudio();
    private final EnumSet<KeyCode> held=EnumSet.noneOf(KeyCode.class);
    private PacmanMechanics mechanics=new PacmanMechanics();
    private PacmanRenderer renderer;
    private boolean menu=true,showingResult;
    private long last;
    private double time,scorePulse;
    private Scene scene;
    private Window window;
    private final EventHandler<KeyEvent> pressed=this::keyPressed;
    private final EventHandler<KeyEvent> released=e->held.remove(e.getCode());
    private final ChangeListener<Boolean> focus=(o,a,b)->{if(!b){held.clear();mechanics.clearInput();if(!menu&&mechanics.getState()!=State.PAUSED&&!mechanics.isFinished())togglePause();}};
    private final ChangeListener<Window> windowListener=(o,a,b)->attachWindow(b);
    private final ChangeListener<Scene> sceneListener=(o,a,b)->{if(b!=scene)detach();};
    private final ChangeListener<Boolean> showingListener=(o,a,b)->{if(!b)detach();};
    private final AnimationTimer loop=new AnimationTimer(){
        @Override public void handle(long now){
            double dt=last==0?0:Math.min(.1,(now-last)/1e9);last=now;time+=dt;
            if(!menu)mechanics.update(dt);
            for(Event event:mechanics.drainEvents()){
                renderer.effect(event,mechanics);
                if(event==Event.PELLET||event==Event.POWER||event==Event.CAPTURE||event==Event.FRUIT)scorePulse=.25;
                audio.play(switch(event){case PELLET->"pellet";case POWER->"power";case CAPTURE->"capture";case FRUIT->"fruit";case LIFE_LOST->"life";case LEVEL->"level";case START->"start";case FINISH->mechanics.isGameWon()?"win":"gameover";});
            }
            scorePulse=Math.max(0,scorePulse-dt);scoreLabel.setScaleX(1+scorePulse*.3);scoreLabel.setScaleY(1+scorePulse*.3);
            updateHud();if(menu)renderer.drawAttract(time);else renderer.draw(mechanics,time,dt,false);
            if(!menu&&mechanics.isFinished()&&!showingResult)showResults();
        }
    };
    @FXML private void initialize(){
        renderer=new PacmanRenderer(canvas);canvas.widthProperty().bind(board.widthProperty());canvas.heightProperty().bind(board.heightProperty());
        root.sceneProperty().addListener((o,a,b)->{detach();if(b!=null)attach(b);});showMenu();
    }
    private void attach(Scene next){scene=next;next.addEventFilter(KeyEvent.KEY_PRESSED,pressed);next.addEventFilter(KeyEvent.KEY_RELEASED,released);next.windowProperty().addListener(windowListener);attachWindow(next.getWindow());}
    private void attachWindow(Window next){
        if(window!=null){window.focusedProperty().removeListener(focus);window.sceneProperty().removeListener(sceneListener);window.showingProperty().removeListener(showingListener);}
        window=next;
        if(next==null){loop.stop();held.clear();audio.stop();return;}
        next.focusedProperty().addListener(focus);next.sceneProperty().addListener(sceneListener);next.showingProperty().addListener(showingListener);last=0;loop.start();
    }
    private void detach(){
        loop.stop();held.clear();audio.stop();mechanics.clearInput();
        if(scene!=null){scene.removeEventFilter(KeyEvent.KEY_PRESSED,pressed);scene.removeEventFilter(KeyEvent.KEY_RELEASED,released);scene.windowProperty().removeListener(windowListener);}
        attachWindow(null);scene=null;
    }
    private void keyPressed(KeyEvent event){
        KeyCode code=event.getCode();
        if(menu){if(code==KeyCode.ESCAPE){showMenu();event.consume();}return;}
        Direction direction=switch(code){case UP,W->Direction.UP;case DOWN,S->Direction.DOWN;case LEFT,A->Direction.LEFT;case RIGHT,D->Direction.RIGHT;default->Direction.NONE;};
        if(direction!=Direction.NONE){if(mechanics.getState()!=State.PAUSED)mechanics.queueDirection(direction);event.consume();return;}
        if(code==KeyCode.P||code==KeyCode.SPACE||code==KeyCode.ESCAPE){if(held.add(code))togglePause();event.consume();}
        else if(code==KeyCode.R){if(held.add(code))restartGame();event.consume();}
    }
    private Label text(String value,String style){Label label=new Label(value);label.getStyleClass().add(style);label.setWrapText(true);label.setMaxWidth(530);label.setAlignment(Pos.CENTER);return label;}
    private Button button(String value,Runnable action){Button button=new Button(value);button.setMaxWidth(Double.MAX_VALUE);button.setWrapText(true);button.setOnAction(e->{audio.play("menu");action.run();});return button;}
    private void panel(String title,String description,double height){menuScroll.setVisible(true);menuScroll.setMaxHeight(height);menuScroll.setVvalue(0);overlay.getChildren().setAll(text(title,"panel-title"),text(description,"description"));}
    @FXML private void showMenu(){
        menu=true;showingResult=false;held.clear();audio.stop();renderer.clear();
        panel("P A C - M A N","MAZE RUNNER / FIVE ARCADE CHALLENGES",600);
        for(PacmanMode mode:PacmanMode.values()){
            Button b=button(mode.title.toUpperCase()+"\n"+mode.description,()->start(mode));b.getStyleClass().add("mode-card");overlay.getChildren().add(b);
        }
        overlay.getChildren().addAll(button("How to Play",this::showHelp),text("Tab + Enter to choose. Sound and Arcade controls are below.","description"));
        pauseButton.setDisable(true);restartButton.setDisable(true);
    }
    private void showHelp(){
        panel("HOW TO PLAY","Arrow keys or WASD steer. Tap once to keep moving. Queue turns just before intersections.",500);
        overlay.getChildren().addAll(text("Small pellets: 10. Power pellets: 50, then capture ghosts for 200 / 400 / 800 / 1,600. Blue ghosts are edible; flashing means power is ending. Eyes return safely to spawn. Fruit: 300 × level.","description"),
                text("Blinky pursues. Pinky aims ahead. Inky uses a vector from Blinky. Clyde retreats when close. Ghosts alternate chase and scatter. Use the side tunnels to escape.","description"),
                text("Space / P / Esc pauses. R restarts. Focus loss pauses. Time Attack and Ghost Challenge clocks include life and level transitions; the opening ready countdown is free.","description"),button("Back to Pac-Man",this::showMenu));
    }
    private void start(PacmanMode mode){
        mechanics=new PacmanMechanics(mode,System.nanoTime());menu=false;showingResult=false;held.clear();renderer.clear();menuScroll.setVisible(false);pauseButton.setDisable(false);restartButton.setDisable(false);last=0;root.requestFocus();audio.play("start");
    }
    @FXML private void togglePause(){
        if(menu||mechanics.isFinished())return;
        mechanics.togglePause();audio.stop();
        if(mechanics.getState()==State.PAUSED){
            panel("PAUSED","All movement, power timers and match clocks are frozen.",350);
            overlay.getChildren().addAll(button("Resume",this::togglePause),button("Restart run",this::restartGame),button("Pac-Man menu",this::showMenu));
        }else{menuScroll.setVisible(false);last=0;root.requestFocus();}
    }
    @FXML private void restartGame(){if(menu)return;mechanics.resetGame();showingResult=false;held.clear();renderer.clear();menuScroll.setVisible(false);last=0;root.requestFocus();audio.stop();}
    private void showResults(){
        showingResult=true;held.clear();var result=mechanics.getResult().orElseThrow();
        String title=switch(result.outcome()){case CHALLENGE_COMPLETED->"CHALLENGE COMPLETE";case CHALLENGE_FAILED->"CHALLENGE OVER";case TIME_EXPIRED->"TIME'S UP";case GAME_OVER->"GAME OVER";};
        panel(title,mechanics.getMode().title.toUpperCase()+" / "+result.score()+" POINTS",450);
        overlay.getChildren().addAll(text("Level "+result.levelReached()+" · "+result.levelsCompleted()+" cleared · "+result.pelletsCollected()+" pellets · "+result.powerPelletsCollected()+" power pellets","description"),
                text(result.ghostsCaptured()+" ghosts captured"+(result.captureTarget()>0?" / target "+result.captureTarget():"")+" · "+Math.round(result.durationSeconds())+" seconds","description"),
                button("Play Again",this::restartGame),button("Pac-Man menu",this::showMenu),button("Back to Arcade",this::backToArcade));
    }
    private void updateHud(){
        scoreLabel.setText(String.format("%06d",mechanics.getScore()));livesLabel.setText("LIVES  "+"● ".repeat(Math.max(0,mechanics.getLives())));levelLabel.setText("LEVEL "+String.format("%02d",mechanics.getLevel()));
        modeLabel.setText(menu?"CHOOSE YOUR CHALLENGE":mechanics.getMode().title.toUpperCase());
        String objective=mechanics.getRemainingPellets()+" PELLETS";
        if(mechanics.getMode().duration>0)objective=String.format("%02ds",(int)Math.ceil(mechanics.getRemainingSeconds()))+"  /  "+objective;
        if(mechanics.getMode()==PacmanMode.GHOST_CHALLENGE)objective=String.format("%02ds",(int)Math.ceil(mechanics.getRemainingSeconds()))+" / GHOSTS "+mechanics.getCaptures()+" OF "+PacmanMode.CAPTURE_TARGET;
        if(mechanics.getMode()==PacmanMode.SPEED_UP)objective=String.format("%.2f× SPEED / ",mechanics.getSpeedMultiplier())+objective;
        if(mechanics.getFrightenedSeconds()>0)objective+=String.format(" / POWER %.1fs",mechanics.getFrightenedSeconds());
        objectiveLabel.setText(menu?"COLLECT / OUTSMART / REPEAT":objective);
        pauseButton.setText(mechanics.getState()==State.PAUSED?"Resume":"Pause");if(!menu)pauseButton.setDisable(mechanics.isFinished());
    }
    @FXML private void toggleMute(){audio.toggleMute();muteButton.setText(audio.isMuted()?"Sound: off":"Sound: on");}
    @FXML private void backToArcade(){detach();SceneSwitcher.showMenu(root);}
}
