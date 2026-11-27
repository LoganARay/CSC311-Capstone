package edu.farmingdale.csc311capstone.Pong;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static edu.farmingdale.csc311capstone.Pong.PongConfig.*;

/** Logical coordinates stay fixed while the court scales uniformly to fit the window. */
final class PongRenderer {
    private static final Color CYAN = Color.web("#55f4ed"), PINK = Color.web("#ff70ba");
    private record Dot(double x, double y) { }
    private static class Particle {
        double x, y, vx, vy, life = .4;
        Particle(double x, double y, Random r) { this.x=x; this.y=y; vx=(r.nextDouble()-.5)*230; vy=(r.nextDouble()-.5)*230; }
    }
    private final Canvas canvas;
    private final ArrayDeque<Dot> trail = new ArrayDeque<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random(311);
    private double flash;
    PongRenderer(Canvas canvas) { this.canvas = canvas; }
    void clear() { trail.clear(); particles.clear(); flash=0; }
    void effect(PongMechanics.Event event, PongMechanics model) {
        if (event == PongMechanics.Event.SCORE) { flash = .4; trail.clear(); }
        if (event == PongMechanics.Event.PADDLE || event == PongMechanics.Event.WALL)
            for (int i=0;i<10 && particles.size()<100;i++) particles.add(new Particle(model.getBallX(), model.getBallY(), random));
    }
    void draw(PongMechanics model, double time, double dt, boolean menu) {
        GraphicsContext g=canvas.getGraphicsContext2D();
        double w=canvas.getWidth(), h=canvas.getHeight();
        g.setFill(Color.web("#080e20")); g.fillRect(0,0,w,h);
        double scale=Math.min(w/WIDTH,h/HEIGHT);
        g.save(); g.translate((w-WIDTH*scale)/2,(h-HEIGHT*scale)/2); g.scale(scale,scale);
        g.setStroke(Color.web("#101e34")); g.setLineWidth(1);
        for(int x=0;x<WIDTH;x+=40) g.strokeLine(x,0,x,HEIGHT);
        for(int y=0;y<HEIGHT;y+=40) g.strokeLine(0,y,WIDTH,y);
        g.setStroke(Color.web("#31435d")); g.setLineDashes(8,12); g.strokeLine(WIDTH/2,16,WIDTH/2,HEIGHT-16); g.setLineDashes();
        g.setStroke(Color.web("#3c6d8f")); g.strokeRoundRect(1,1,WIDTH-2,HEIGHT-2,14,14);
        double bx=menu?WIDTH/2+Math.sin(time*.9)*300:model.getBallX();
        double by=menu?HEIGHT/2+Math.sin(time*1.7)*145:model.getBallY();
        if (menu || model.getState()==PongMechanics.State.PLAYING) {
            trail.addLast(new Dot(bx,by)); if(trail.size()>18)trail.removeFirst();
        }
        int i=0;
        for(Dot p:trail) { g.setFill(Color.rgb(85,244,237,.02+(++i)*.014)); g.fillOval(p.x-6,p.y-6,12,12); }
        paddle(g,LEFT_X,menu?HEIGHT/2+Math.sin(time)*120-PADDLE_HEIGHT/2:model.getLeftY(),CYAN);
        paddle(g,RIGHT_X,menu?HEIGHT/2+Math.sin(time+.8)*120-PADDLE_HEIGHT/2:model.getRightY(),PINK);
        g.setFill(Color.rgb(255,255,255,.10)); g.fillOval(bx-14,by-14,28,28);
        g.setFill(Color.web("#f6ffed")); g.fillOval(bx-BALL_RADIUS,by-BALL_RADIUS,16,16);
        particles.removeIf(p->p.life<=0);
        for(Particle p:particles){p.life-=dt;p.x+=p.vx*dt;p.y+=p.vy*dt;g.setFill(Color.rgb(125,241,255,Math.max(0,p.life/.4)));g.fillRect(p.x,p.y,3,3);}
        if(flash>0){g.setFill(Color.rgb(85,244,237,flash*.18));g.fillRect(0,0,WIDTH,HEIGHT);flash=Math.max(0,flash-dt);}
        if (!menu && model.getState()==PongMechanics.State.READY) {
            g.setFill(Color.rgb(8,14,32,.70));g.fillRoundRect(WIDTH/2-80,HEIGHT/2-70,160,140,20,20);
            g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);g.setFill(CYAN);g.setFont(Font.font("Monospaced",52));
            g.fillText(String.valueOf((int)Math.ceil(model.getReadySeconds())),WIDTH/2,HEIGHT/2+12);
            g.setFont(Font.font("Monospaced",13));g.fillText("GET READY",WIDTH/2,HEIGHT/2+42);
        }
        g.restore();
    }
    private void paddle(GraphicsContext g,double x,double y,Color color){
        g.setFill(color.deriveColor(0,1,1,.12));g.fillRoundRect(x-6,y-6,PADDLE_WIDTH+12,PADDLE_HEIGHT+12,14,14);
        g.setFill(color);g.fillRoundRect(x,y,PADDLE_WIDTH,PADDLE_HEIGHT,7,7);
        g.setFill(Color.rgb(255,255,255,.75));g.fillRoundRect(x+4,y+5,3,PADDLE_HEIGHT-10,3,3);
    }
}
