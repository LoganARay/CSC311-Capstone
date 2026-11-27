package edu.farmingdale.csc311capstone.Pong;

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
import static edu.farmingdale.csc311capstone.Pong.PongConfig.*;

/** Presentation and lifecycle only; the model owns every match rule. */
public class PongController {
    @FXML private BorderPane root;
    @FXML private StackPane court;
    @FXML private Canvas canvas;
    @FXML private VBox overlay;
    @FXML private Label leftScore, rightScore, modeLabel, detailLabel, rightLabel;
    @FXML private Button pauseButton, restartButton, muteButton;
    private final EnumSet<KeyCode> keys = EnumSet.noneOf(KeyCode.class);
    private final PongAudio audio = new PongAudio();
    private PongMechanics mechanics = new PongMechanics();
    private PongRenderer renderer;
    private boolean menu = true, showingResult;
    private long last;
    private double animationTime, scorePulse;
    private Window window;
    private Scene attachedScene;
    private final EventHandler<KeyEvent> pressed = this::keyPressed;
    private final EventHandler<KeyEvent> released = e -> keys.remove(e.getCode());
    private final ChangeListener<Boolean> focus = (o,a,b) -> { if (!b) { keys.clear(); if (!menu && mechanics.getState()!=PongMechanics.State.PAUSED && !mechanics.isGameOver()) togglePause(); } };
    private final ChangeListener<Window> windowListener = (o,a,b) -> attachWindow(b);
    private final ChangeListener<Scene> stageSceneListener = (o,a,b) -> { if (b != attachedScene) detach(); };
    private final ChangeListener<Boolean> showingListener = (o,a,b) -> { if (!b) detach(); };
    private final AnimationTimer loop = new AnimationTimer() {
        public void handle(long now) {
            double dt=last==0?0:Math.min(.10,(now-last)/1e9); last=now; animationTime+=dt;
            if(!menu) mechanics.update(dt,(keys.contains(KeyCode.S)?1:0)-(keys.contains(KeyCode.W)?1:0),
                    (keys.contains(KeyCode.DOWN)?1:0)-(keys.contains(KeyCode.UP)?1:0));
            for(var e:mechanics.drainEvents()) {
                renderer.effect(e,mechanics);
                if(e==PongMechanics.Event.SCORE)scorePulse=.35;
                audio.play(switch(e){case PADDLE->"paddle";case WALL->"wall";case SCORE->"score";case START->"start";case COUNTDOWN->"countdown";case FINISH->mechanics.getWinner()==2?"defeat":"victory";});
            }
            scorePulse=Math.max(0,scorePulse-dt);
            leftScore.setScaleX(1+scorePulse*.5);leftScore.setScaleY(1+scorePulse*.5);
            rightScore.setScaleX(1+scorePulse*.5);rightScore.setScaleY(1+scorePulse*.5);
            updateHud(); renderer.draw(mechanics,animationTime,dt,menu);
            if(!menu && mechanics.isGameOver() && !showingResult) showResults();
        }
    };
    @FXML private void initialize() {
        renderer=new PongRenderer(canvas);
        canvas.widthProperty().bind(court.widthProperty()); canvas.heightProperty().bind(court.heightProperty());
        root.sceneProperty().addListener((o,oldScene,newScene)->{detach(); if(newScene!=null)attach(newScene);});
        showMenu();
    }
    private void attach(Scene scene) {
        attachedScene=scene; scene.addEventFilter(KeyEvent.KEY_PRESSED,pressed);scene.addEventFilter(KeyEvent.KEY_RELEASED,released);
        scene.windowProperty().addListener(windowListener);attachWindow(scene.getWindow());
    }
    private void attachWindow(Window next) {
        if(window!=null){window.focusedProperty().removeListener(focus);window.sceneProperty().removeListener(stageSceneListener);window.showingProperty().removeListener(showingListener);}
        window=next;
        if(next==null){loop.stop();keys.clear();audio.stop();return;}
        next.focusedProperty().addListener(focus);next.sceneProperty().addListener(stageSceneListener);next.showingProperty().addListener(showingListener);
        last=0;loop.start();
    }
    private void detach() {
        loop.stop();keys.clear();audio.stop();
        if(attachedScene!=null){attachedScene.removeEventFilter(KeyEvent.KEY_PRESSED,pressed);attachedScene.removeEventFilter(KeyEvent.KEY_RELEASED,released);attachedScene.windowProperty().removeListener(windowListener);}
        attachWindow(null);attachedScene=null;
    }
    private void keyPressed(KeyEvent e) {
        if(menu) { if(e.getCode()==KeyCode.ESCAPE){showMenu();e.consume();} return; }
        switch(e.getCode()) {
            case W,S,UP,DOWN -> { if(mechanics.getState()!=PongMechanics.State.PAUSED)keys.add(e.getCode());e.consume(); }
            case SPACE,P,ESCAPE -> { if(keys.add(e.getCode()))togglePause();e.consume(); }
            case R -> {if(keys.add(KeyCode.R))restartGame();e.consume();}
            default -> { }
        }
    }
    private Button button(String text,Runnable action) {
        Button b=new Button(text);b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(e->{audio.play("menu");action.run();});return b;
    }
    private Label text(String text,String style) { Label l=new Label(text);l.getStyleClass().add(style);l.setWrapText(true);l.setAlignment(Pos.CENTER);l.setMaxWidth(430);return l; }
    private void panel(String title,String subtitle) {
        overlay.setVisible(true);overlay.getChildren().setAll(text(title,"panel-title"),text(subtitle,"description"));
    }
    @FXML private void showMenu() {
        menu=true;keys.clear();audio.stop();renderer.clear();showingResult=false;
        panel("P O N G","NEON CIRCUIT  /  THREE WAYS TO RALLY");
        overlay.getChildren().addAll(button("Player vs AI",()->showSetup(Mode.PLAYER_VS_AI)),button("Local Multiplayer",()->start(PongConfig.local())),button("Blitz · 60 seconds",()->showSetup(Mode.BLITZ)),button("How to Play",this::showHelp));
        overlay.getChildren().add(text("First to 7 in standard play. Blitz: highest score wins; ties are a draw.","description"));
        pauseButton.setDisable(true);restartButton.setDisable(true);
    }
    private void showSetup(Mode mode) {
        panel(mode==Mode.BLITZ?"BLITZ":"CHOOSE YOUR RIVAL",mode==Mode.BLITZ?"60 seconds. Every point counts. Tied scores end in a draw.":"First to seven. Aim with the edges of your paddle.");
        ComboBox<Opponent> opponent=new ComboBox<>();opponent.getItems().setAll(Opponent.values());opponent.setValue(Opponent.CPU);opponent.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Difficulty> difficulty=new ComboBox<>();difficulty.getItems().setAll(Difficulty.values());difficulty.setValue(Difficulty.MEDIUM);difficulty.setMaxWidth(Double.MAX_VALUE);
        if(mode==Mode.BLITZ){overlay.getChildren().addAll(text("Opponent: HUMAN = local two-player","description"),opponent);opponent.valueProperty().addListener((o,a,b)->difficulty.setDisable(b==Opponent.HUMAN));}
        overlay.getChildren().addAll(text("AI difficulty · Easy / Medium / Hard","description"),difficulty,text("Easy: forgiving tracking. Medium: balanced. Hard: predicts wall bounces.","description"),button("Start match",()->start(new PongConfig(mode,opponent.getValue(),difficulty.getValue()))),button("Back",this::showMenu));
    }
    private void showHelp() {
        panel("HOW TO PLAY","W / S moves Player One. Up / Down moves Player Two. In AI modes, you control the left paddle.");
        overlay.getChildren().addAll(text("Hit near a paddle's edge to angle your shot. Rallies get faster. Space / P / Esc pauses; R restarts. Losing focus pauses safely. Tab and Enter navigate menus.","description"),button("Back to Pong",this::showMenu));
    }
    private void start(PongConfig config) {
        mechanics=new PongMechanics(config,System.nanoTime());menu=false;showingResult=false;keys.clear();renderer.clear();overlay.setVisible(false);
        pauseButton.setDisable(false);restartButton.setDisable(false);last=0;root.requestFocus();audio.play("start");
    }
    @FXML private void togglePause() {
        if(menu||mechanics.isGameOver())return;
        mechanics.togglePause();keys.removeIf(k->k==KeyCode.W||k==KeyCode.S||k==KeyCode.UP||k==KeyCode.DOWN);audio.stop();
        if(mechanics.getState()==PongMechanics.State.PAUSED){
            panel("PAUSED","The court will be here when you are ready.");
            overlay.getChildren().addAll(button("Resume",this::togglePause),button("Restart match",this::restartGame),button("Pong menu",this::showMenu));
        }else{overlay.setVisible(false);root.requestFocus();last=0;}
    }
    @FXML private void restartGame() { if(menu)return;mechanics.resetGame();showingResult=false;keys.clear();renderer.clear();overlay.setVisible(false);last=0;root.requestFocus();audio.stop(); }
    private void showResults() {
        showingResult=true;keys.clear();
        var result=mechanics.getResult().orElseThrow();
        String winner=mechanics.getWinner()==0?"DRAW":mechanics.getWinner()==1?"PLAYER ONE WINS":mechanics.getConfig().opponent()==Opponent.CPU?"CPU WINS":"PLAYER TWO WINS";
        panel(winner,result.playerScore()+"  :  "+result.opponentScore()+"  ·  "+mechanics.getConfig().title());
        overlay.getChildren().addAll(text(mechanics.getConfig().opponentLabel()+" · "+result.paddleHits()+" hits · best rally "+result.longestRally()+" · "+Math.round(result.durationSeconds())+"s","description"),button("Play Again",this::restartGame),button("Pong menu",this::showMenu),button("Back to Arcade",this::backToArcade));
        pauseButton.setDisable(true);
    }
    private void updateHud() {
        leftScore.setText(String.format("%02d",mechanics.getPlayer1Score()));rightScore.setText(String.format("%02d",mechanics.getPlayer2Score()));
        rightLabel.setText(mechanics.getConfig().opponentLabel());modeLabel.setText(menu?"SELECT YOUR CIRCUIT":mechanics.getConfig().title());
        detailLabel.setText(menu?"PRECISION / PACE / PLAY":mechanics.getConfig().mode()==Mode.BLITZ?String.format("%02d SECONDS",(int)Math.ceil(mechanics.getRemainingSeconds())):"FIRST TO 7");
        pauseButton.setText(mechanics.getState()==PongMechanics.State.PAUSED?"Resume":"Pause");
        if(!menu)pauseButton.setDisable(mechanics.isGameOver());
    }
    @FXML private void toggleMute(){audio.toggleMute();muteButton.setText(audio.isMuted()?"Sound: off":"Sound: on");}
    @FXML private void backToArcade(){detach();SceneSwitcher.showMenu(root);}
}
