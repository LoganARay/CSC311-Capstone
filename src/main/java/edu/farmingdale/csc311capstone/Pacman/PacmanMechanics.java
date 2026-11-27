package edu.farmingdale.csc311capstone.Pacman;

import java.util.*;
import static edu.farmingdale.csc311capstone.Pacman.PacmanMode.*;

/** Maze, BFS and scoring foundation extended with continuous, buffered tile movement. */
public class PacmanMechanics {
    public static final char WALL='#', PELLET='.', EMPTY=' ', POWER='o';
    public enum Direction {
        NONE(0,0), UP(-1,0), DOWN(1,0), LEFT(0,-1), RIGHT(0,1);
        public final int row,col;
        Direction(int row,int col){this.row=row;this.col=col;}
    }
    public enum State { READY, PLAYING, PAUSED, LIFE_LOST, LEVEL_COMPLETE, FINISHED }
    public enum GhostState { CHASE, SCATTER, FRIGHTENED, RETURNING }
    public enum Personality { BLINKY, PINKY, INKY, CLYDE }
    public enum Event { PELLET, POWER, CAPTURE, FRUIT, LIFE_LOST, LEVEL, START, FINISH }
    public record Cell(int row,int col) { }
    private static final Direction[] DIRECTIONS={Direction.UP,Direction.LEFT,Direction.DOWN,Direction.RIGHT};
    // Original maze retained; row seven now has a horizontal wrap tunnel.
    private static final String[] MAZE_LAYOUT={
            "#################",
            "#P..............#",
            "#.###.###.###.#.#",
            "#...............#",
            "###.###.#.###.###",
            "#.....#...#.....#",
            "#.###.#.#.#.###.#",
            "......#...#......",
            "###.###.#.###.###",
            "#...............#",
            "#.###.###.###.#.#",
            "#...............#",
            "#################"};
    private static final Cell[] GHOST_STARTS={new Cell(5,7),new Cell(5,8),new Cell(5,9),new Cell(7,8)};
    private static final Cell[] POWERS={new Cell(1,5),new Cell(3,15),new Cell(11,1),new Cell(11,15)};
    private static final Cell[] CORNERS={new Cell(1,15),new Cell(1,1),new Cell(11,15),new Cell(11,1)};
    private static final Cell FRUIT_CELL=new Cell(9,8);
    private static final class Actor {
        int row,col,toRow,toCol;
        double progress;
        boolean moving;
        Direction direction=Direction.NONE;
        Actor(Cell cell){reset(cell);}
        void reset(Cell cell){row=toRow=cell.row;col=toCol=cell.col;progress=0;moving=false;direction=Direction.NONE;}
        Cell cell(){return new Cell(row,col);}
        void begin(Cell next,Direction d){toRow=next.row;toCol=next.col;progress=0;moving=true;direction=d;}
        void arrive(){row=toRow;col=toCol;progress=0;moving=false;}
        double y(){return row+(toRow-row)*progress;}
        double x(int columns){int dx=toCol-col;if(Math.abs(dx)>1)dx=dx>0?-1:1;return col+dx*progress;}
    }
    private static final class Ghost {
        final Actor actor;
        final Personality personality;
        GhostState state=GhostState.SCATTER;
        double release;
        Ghost(Cell cell,Personality personality){actor=new Actor(cell);this.personality=personality;}
    }
    private final char[][] maze=new char[MAZE_LAYOUT.length][MAZE_LAYOUT[0].length()];
    private final Actor player=new Actor(new Cell(1,1));
    private final Ghost[] ghosts=new Ghost[4];
    private final Random random;
    private final PacmanMode mode;
    private final List<Event> events=new ArrayList<>();
    private final double[] powerRecharge=new double[4];
    private Direction queued=Direction.NONE;
    private State state,beforePause;
    private int score,remainingPellets,initialPellets,lives,level,pelletsCollected,powerCollected,captures,captureChain,levelPellets,fruitStage;
    private double elapsed,playTime,frightened,transition,fruitTime,deathRow,deathCol;
    private PacmanResult result;
    private boolean resultConsumed;

    public PacmanMechanics(){this(PacmanMode.CLASSIC,311);}
    public PacmanMechanics(PacmanMode mode,long seed){
        this.mode=Objects.requireNonNull(mode);random=new Random(seed);
        for(int i=0;i<4;i++)ghosts[i]=new Ghost(GHOST_STARTS[i],Personality.values()[i]);
        for(String row:MAZE_LAYOUT)if(row.length()!=getColumns())throw new IllegalArgumentException("Invalid maze width");
        resetGame();validateMaze();
    }
    private void validateMaze(){
        int[][] distance=distancesTo(player.cell());
        for(int r=0;r<getRows();r++)for(int c=0;c<getColumns();c++)
            if(!isWall(r,c)&&distance[r][c]==Integer.MAX_VALUE)throw new IllegalStateException("Unreachable maze tile");
        for(Cell cell:GHOST_STARTS)if(isWall(cell.row,cell.col))throw new IllegalStateException("Invalid ghost spawn");
    }
    public void queueDirection(Direction direction){if(direction!=Direction.NONE)queued=Objects.requireNonNull(direction);}
    public void clearInput(){queued=Direction.NONE;}
    public void update(double seconds){
        if(!Double.isFinite(seconds)||seconds<0)throw new IllegalArgumentException("Invalid delta time");
        if(state==State.PAUSED||state==State.FINISHED)return;
        while(seconds>1e-9&&state!=State.FINISHED){
            double dt=Math.min(seconds,1.0/240);seconds-=dt;
            if(state==State.READY){transition=Math.max(0,transition-dt);if(transition<=1e-9){state=State.PLAYING;emit(Event.START);}continue;}
            double active=mode.duration>0?Math.min(dt,Math.max(0,mode.duration-elapsed)):dt;
            elapsed+=active;
            if(mode.duration>0&&elapsed>=mode.duration-1e-8){
                finish(mode==GHOST_CHALLENGE?PacmanResult.Outcome.CHALLENGE_FAILED:PacmanResult.Outcome.TIME_EXPIRED);break;
            }
            if(state==State.LIFE_LOST||state==State.LEVEL_COMPLETE){
                transition-=active;
                if(transition<=1e-9){
                    if(state==State.LEVEL_COMPLETE){level++;buildMaze();resetPositions();}
                    state=State.PLAYING;emit(Event.START);
                }
                continue;
            }
            playTime+=active;
            frightened=Math.max(0,frightened-active);
            fruitTime=Math.max(0,fruitTime-active);
            rechargePowers(active);updateGhostStates();
            advancePlayer(active);
            if(state!=State.PLAYING)continue;
            checkGhostCollision();if(state!=State.PLAYING)continue;
            for(int i=0;i<4;i++){
                advanceGhost(i,active);checkGhostCollision();if(state!=State.PLAYING)break;
            }
        }
    }
    private void advancePlayer(double dt){
        double travel=dt*getPlayerSpeed();
        while(travel>1e-10&&state==State.PLAYING){
            if(!player.moving){
                Direction next=queued!=Direction.NONE&&neighbor(player.cell(),queued)!=null?queued:player.direction;
                Cell cell=neighbor(player.cell(),next);
                if(cell==null){player.direction=Direction.NONE;return;}
                player.begin(cell,next);
            }
            double step=Math.min(1-player.progress,travel);player.progress+=step;travel-=step;
            if(player.progress>=1-1e-9){player.arrive();collectAtPlayer();}
        }
    }
    /** Discrete movement retained for direct rules callers; the live controller uses queueDirection/update. */
    public boolean movePlayer(int rowChange,int colChange){
        if(state!=State.PLAYING||player.moving)return false;
        Direction d=Arrays.stream(DIRECTIONS).filter(v->v.row==rowChange&&v.col==colChange).findFirst().orElse(Direction.NONE);
        Cell next=neighbor(player.cell(),d);if(next==null)return false;
        player.reset(next);player.direction=d;collectAtPlayer();return true;
    }
    private void collectAtPlayer(){
        char tile=maze[player.row][player.col];
        if(tile==PELLET||tile==POWER){
            maze[player.row][player.col]=EMPTY;remainingPellets--;levelPellets++;pelletsCollected++;
            if(tile==POWER){
                score+=POWER_POINTS;powerCollected++;captureChain=0;frightened=getFrightenedDuration();emit(Event.POWER);
                for(int i=0;i<POWERS.length;i++)if(POWERS[i].equals(player.cell()))powerRecharge[i]=CHALLENGE_RECHARGE_SECONDS;
                updateGhostStates();
            }else{score+=PELLET_POINTS;emit(Event.PELLET);}
            if(fruitStage<2&&levelPellets>=initialPellets*(fruitStage==0?.30:.70)){fruitStage++;fruitTime=FRUIT_SECONDS;}
        }
        if(fruitTime>0&&player.cell().equals(FRUIT_CELL)){score+=FRUIT_POINTS*level;fruitTime=0;emit(Event.FRUIT);}
        if(remainingPellets==0){
            state=State.LEVEL_COMPLETE;transition=2;frightened=0;clearInput();emit(Event.LEVEL);
        }
    }
    private void rechargePowers(double dt){
        if(mode!=GHOST_CHALLENGE)return;
        for(int i=0;i<POWERS.length;i++)if(powerRecharge[i]>0){
            powerRecharge[i]-=dt;
            if(powerRecharge[i]<=0){Cell p=POWERS[i];if(maze[p.row][p.col]==EMPTY){maze[p.row][p.col]=POWER;remainingPellets++;}}
        }
    }
    private void advanceGhost(int index,double dt){
        Ghost ghost=ghosts[index];Actor a=ghost.actor;
        if(ghost.release>0){ghost.release-=dt;return;}
        if(!a.moving){
            if(ghost.state==GhostState.RETURNING&&a.cell().equals(GHOST_STARTS[index])){
                ghost.state=normalGhostState();ghost.release=1.2;return;
            }
            Cell target=ghost.state==GhostState.RETURNING?GHOST_STARTS[index]:ghost.state==GhostState.SCATTER?CORNERS[index]:targetFor(index);
            int[][] distances=distancesTo(ghost.state==GhostState.FRIGHTENED?player.cell():target);
            List<Direction> options=new ArrayList<>();
            for(Direction d:DIRECTIONS)if(neighbor(a.cell(),d)!=null)options.add(d);
            if(options.size()>1)options.removeIf(d->d.row==-a.direction.row&&d.col==-a.direction.col);
            if(options.isEmpty())return;
            if(ghost.state==GhostState.FRIGHTENED)Collections.shuffle(options,random);
            Direction best=options.getFirst();
            for(Direction d:options){Cell n=neighbor(a.cell(),d),b=neighbor(a.cell(),best);
                if(ghost.state==GhostState.FRIGHTENED?distances[n.row][n.col]>distances[b.row][b.col]:distances[n.row][n.col]<distances[b.row][b.col])best=d;
            }
            a.begin(neighbor(a.cell(),best),best);
        }
        double speed=ghost.state==GhostState.RETURNING?9:ghost.state==GhostState.FRIGHTENED?getGhostSpeed()*.64:getGhostSpeed();
        a.progress+=dt*speed;if(a.progress>=1-1e-9)a.arrive();
    }
    public void moveGhosts(){if(state==State.PLAYING)for(int i=0;i<4;i++)advanceGhost(i,1/getGhostSpeed());}
    private GhostState normalGhostState(){
        // Hard spends less time scattering, while every personality still gets a retreat phase.
        double cycle=mode==HARD?24:27,scatter=mode==HARD?4:7;
        return playTime%cycle<scatter?GhostState.SCATTER:GhostState.CHASE;
    }
    private void updateGhostStates(){
        for(Ghost g:ghosts)if(g.state!=GhostState.RETURNING)g.state=frightened>0?GhostState.FRIGHTENED:normalGhostState();
    }
    /** Project arbitrary personality targets onto a reachable walkable tile before BFS. */
    public Cell targetFor(int index){
        Ghost ghost=ghosts[index];int row=player.row,col=player.col;
        switch(ghost.personality){
            case BLINKY -> { }
            case PINKY -> {row+=player.direction.row*4;col+=player.direction.col*4;}
            case INKY -> {row=2*(row+player.direction.row*2)-ghosts[0].actor.row;col=2*(col+player.direction.col*2)-ghosts[0].actor.col;}
            case CLYDE -> {if(Math.abs(ghost.actor.row-row)+Math.abs(ghost.actor.col-col)<7)return CORNERS[3];}
        }
        return nearestWalkable(row,col);
    }
    private Cell nearestWalkable(int row,int col){
        Cell best=player.cell();int distance=Integer.MAX_VALUE;
        for(int r=0;r<getRows();r++)for(int c=0;c<getColumns();c++)if(!isWall(r,c)){
            int d=Math.abs(r-row)+Math.abs(c-col);if(d<distance){distance=d;best=new Cell(r,c);}
        }
        return best;
    }
    public int[][] distancesTo(Cell target){
        target=nearestWalkable(target.row,target.col);
        int[][] distance=new int[getRows()][getColumns()];for(int[] row:distance)Arrays.fill(row,Integer.MAX_VALUE);
        Queue<Cell> queue=new ArrayDeque<>();distance[target.row][target.col]=0;queue.add(target);
        while(!queue.isEmpty()){
            Cell current=queue.remove();
            for(Direction d:DIRECTIONS){Cell n=neighbor(current,d);if(n!=null&&distance[n.row][n.col]==Integer.MAX_VALUE){distance[n.row][n.col]=distance[current.row][current.col]+1;queue.add(n);}}
        }
        return distance;
    }
    public Cell neighbor(Cell cell,Direction direction){
        if(direction==Direction.NONE)return null;
        int row=cell.row+direction.row,col=cell.col+direction.col;
        if(row==7&&(col<0||col>=getColumns()))col=Math.floorMod(col,getColumns());
        return isWall(row,col)?null:new Cell(row,col);
    }
    public boolean checkGhostCollision(){
        if(state!=State.PLAYING)return false;
        for(Ghost ghost:ghosts){
            if(ghost.state==GhostState.RETURNING||ghost.release>0)continue;
            double dx=Math.abs(player.x(getColumns())-ghost.actor.x(getColumns()));
            if(player.row==7&&ghost.actor.row==7)dx=Math.min(dx,getColumns()-dx);
            if(Math.hypot(dx,player.y()-ghost.actor.y())>.65)continue;
            if(ghost.state==GhostState.FRIGHTENED){
                ghost.state=GhostState.RETURNING;score+=200<<Math.min(captureChain++,3);captures++;emit(Event.CAPTURE);
                if(mode==GHOST_CHALLENGE&&captures>=CAPTURE_TARGET)finish(PacmanResult.Outcome.CHALLENGE_COMPLETED);
            }else{
                deathRow=player.y();deathCol=player.x(getColumns());
                lives--;frightened=0;emit(Event.LIFE_LOST);
                if(lives<=0)finish(mode==GHOST_CHALLENGE?PacmanResult.Outcome.CHALLENGE_FAILED:PacmanResult.Outcome.GAME_OVER);
                else{resetPositions();state=State.LIFE_LOST;transition=2;}
            }
            return true;
        }
        return false;
    }
    private void resetPositions(){
        player.reset(new Cell(1,1));queued=Direction.NONE;
        for(int i=0;i<4;i++){ghosts[i].actor.reset(GHOST_STARTS[i]);ghosts[i].state=GhostState.SCATTER;ghosts[i].release=i*.65;}
    }
    private void buildMaze(){
        remainingPellets=0;levelPellets=0;fruitStage=0;fruitTime=0;frightened=0;captureChain=0;Arrays.fill(powerRecharge,0);
        for(int r=0;r<getRows();r++)for(int c=0;c<getColumns();c++){
            char tile=MAZE_LAYOUT[r].charAt(c);maze[r][c]=tile=='P'?EMPTY:tile;if(tile==PELLET)remainingPellets++;
        }
        for(Cell p:POWERS)maze[p.row][p.col]=POWER;
        initialPellets=remainingPellets;
    }
    public void resetGame(){
        score=pelletsCollected=powerCollected=captures=captureChain=0;lives=mode.startingLives;level=1;elapsed=playTime=0;
        result=null;resultConsumed=false;events.clear();buildMaze();resetPositions();state=State.READY;transition=READY_SECONDS;
    }
    public void togglePause(){
        if(state==State.FINISHED)return;
        if(state==State.PAUSED)state=beforePause;else{beforePause=state;state=State.PAUSED;clearInput();}
    }
    private void finish(PacmanResult.Outcome outcome){
        if(result!=null)return;
        state=State.FINISHED;clearInput();
        result=new PacmanResult("Pac-Man",mode,score,level-1,level,captures,pelletsCollected,powerCollected,lives,elapsed,outcome,
                mode==GHOST_CHALLENGE?CAPTURE_TARGET:0,getRemainingSeconds());emit(Event.FINISH);
    }
    private void emit(Event event){if(events.size()<128)events.add(event);}
    public List<Event> drainEvents(){var copy=List.copyOf(events);events.clear();return copy;}
    public Optional<PacmanResult> getResult(){return Optional.ofNullable(result);}
    public Optional<PacmanResult> consumeResult(){if(result==null||resultConsumed)return Optional.empty();resultConsumed=true;return Optional.of(result);}
    public boolean isWall(int row,int col){return row<0||row>=getRows()||col<0||col>=getColumns()||maze[row][col]==WALL;}
    public int getRows(){return maze.length;}
    public int getColumns(){return maze[0].length;}
    public int getStartRow(){return 1;}
    public int getStartCol(){return 1;}
    public int getPlayerRow(){return player.row;}
    public int getPlayerCol(){return player.col;}
    public double getPlayerVisualRow(){return player.y();}
    public double getPlayerVisualCol(){return player.x(getColumns());}
    public Direction getDirection(){return player.direction;}
    public int getGhostCount(){return ghosts.length;}
    public int getGhostRow(int i){return ghosts[i].actor.row;}
    public int getGhostCol(int i){return ghosts[i].actor.col;}
    public double getGhostVisualRow(int i){return ghosts[i].actor.y();}
    public double getGhostVisualCol(int i){return ghosts[i].actor.x(getColumns());}
    public GhostState getGhostState(int i){return ghosts[i].state;}
    public Direction getGhostDirection(int i){return ghosts[i].actor.direction;}
    public double getGhostRelease(int i){return ghosts[i].release;}
    public char getTile(int row,int col){return maze[row][col];}
    public int getScore(){return score;}
    public int getRemainingPellets(){return remainingPellets;}
    public int getLives(){return lives;}
    public int getLevel(){return level;}
    public int getCaptures(){return captures;}
    public int getPelletsCollected(){return pelletsCollected;}
    public double getElapsedSeconds(){return elapsed;}
    public double getRemainingSeconds(){return Math.max(0,mode.duration-elapsed);}
    public double getFrightenedSeconds(){return frightened;}
    public double getFrightenedDuration(){return Math.max(2.8,mode.frightenedSeconds-(level-1)*.35);}
    public double getTransitionSeconds(){return transition;}
    public double getSpeedMultiplier(){return Math.min(1.55,1+(level-1)*.07+(mode==SPEED_UP?(pelletsCollected/20)*.08:0));}
    public double getPlayerSpeed(){return Math.min(MAX_PLAYER_SPEED,PLAYER_SPEED*getSpeedMultiplier());}
    public double getGhostSpeed(){return Math.min(MAX_GHOST_SPEED,GHOST_SPEED*mode.ghostSpeedFactor*getSpeedMultiplier());}
    public double getDeathRow(){return deathRow;}
    public double getDeathCol(){return deathCol;}
    public double getFruitSeconds(){return fruitTime;}
    public Cell getFruitCell(){return FRUIT_CELL;}
    public PacmanMode getMode(){return mode;}
    public State getState(){return state;}
    public boolean isGameWon(){return result!=null&&result.outcome()==PacmanResult.Outcome.CHALLENGE_COMPLETED;}
    public boolean isGameOver(){return state==State.FINISHED;}
    public boolean isFinished(){return state==State.FINISHED;}
}
