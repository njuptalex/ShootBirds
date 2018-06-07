package com.mygdx.game;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.StretchViewport;

import com.mygdx.game.ActorGroup.BirdsGroup;
import com.mygdx.game.ActorGroup.BulletsFactory;
import com.mygdx.game.ActorGroup.DeadGroup;
import com.mygdx.game.actor.BaseActor;


public class MainGame extends InputAdapter implements ApplicationListener{

    private static final String TAG = MainGame.class.getSimpleName();

    private Stage stage;

    private Texture pao;
    private BaseActor paoActor;

    private static final int PLAY = 1;
    private static final int OVER = -1;

    private static final float WORLD_WIDTH = 640;
    private static final float WORLD_HEIGHT = 480;
    private static final String[] birdsArray = {"鸵鸟.png","爱情鸟.png","红鸟.png","黑鸟.png", "小小鸟.png"};
    private static final int MAX_BIRD_NUM  = 50;
    private static final int MAX_BULLET_NUM  = 100;
    private int GAME_STATE;
    private int BIRDS_COUNT;
    private int BULLET_COUNT;
    private int HITS;
    private int SCORE;

    private InputProcessorEvent processorEvent;
    private Array<String> messages;
    private BitmapFont font;
    private BitmapFont gameStateWords;
    private SpriteBatch batch;
    private static final int MESSAGE_MAX = 10;

    // 鸟类演员组
    private BirdsGroup birdsGroup;

    // 子弹演员组
    private Group bulletsGroup;

    // 死去的鸟的演员组
    private DeadGroup deadGroup;

    private BaseActor GameOver;

    private BaseActor tryAgain;


    // 背景音乐
    private Music backgroundMusic;

    // 音效
    private Sound shootSound;
    private Sound hitSound;
    private Sound overSound;
    private Sound restartSound;


    @Override
    public void create() {
        this.GAME_STATE = PLAY;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.gameStateWords = new BitmapFont();
        this.messages = new Array<String>();

//        // 加载背景音乐, 创建 Music 实例
//         backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("backgroundMusic.ogg"));
//        // 背景音乐设置循环播放
//        backgroundMusic.setLooping(true);
//
//        // 设置音量, 值范围 0.0 ~ 1.0
//        // music.setVolume(float volume);
//
//        // 手动暂停播放, 暂停播放后调用 play() 将从暂停位置开始继续播放
//        // music.pause();
//
//        // 手动停止播放, 停止播放后调用 play() 将从头开始播放
//        // music.stop();
//
//        // 手动播放音乐, 这里游戏启动时开始播放背景音乐
//        backgroundMusic.play();

        // 加载各音效
        shootSound = Gdx.audio.newSound(Gdx.files.internal("audio/touch.ogg"));
        hitSound = Gdx.audio.newSound(Gdx.files.internal("audio/hit.ogg"));
        overSound = Gdx.audio.newSound(Gdx.files.internal("audio/die.ogg"));
        restartSound = Gdx.audio.newSound(Gdx.files.internal("audio/restart.ogg"));

        // 使用伸展视口（StretchViewport）创建舞台
        this.stage = new Stage(new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT));

        // 设置炮台和小鸟儿
        this.BIRDS_COUNT = 0;
        this.HITS = 0;
        this.BULLET_COUNT = MAX_BULLET_NUM;
        setActors(birdsArray);

        // 设置鼠标监听器(被动监听)
        this.processorEvent = new InputProcessorEvent();
        Gdx.input.setInputProcessor(processorEvent);
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Crosshair); // 设置准星形状，哈哈好像没用呢！

//        // 首先必须注册输入处理器（stage）, 将输入的处理设置给 舞台（Stage 实现了 InputProcessor 接口）（手动监听）
//        // 这样舞台才能接收到输入事件, 分发给相应的演员 或 自己处理。
//        Gdx.input.setInputProcessor(stage);
//
//        // 给舞台添加输入监听器（包括触屏, 鼠标点击, 键盘按键 的输入）
//        stage.addListener(new InputListener());

        // 绘制文字
        font = new BitmapFont();
        Thread thread = new Thread();
        thread.start();
    }

    @Override
    public void render() {
        // 白色清屏
        Gdx.gl.glClearColor(1,1,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 绘制舞台
        stage.act();
        stage.draw();

        // 绘制message
        batch.begin();
        for (int i = 0; i < messages.size; ++i) {
            font.draw(batch, messages.get(i), 20.0f, 480 - 40.0f * (i + 1));
        }

        String words = "Current birds: " + BIRDS_COUNT + "\nLeft bullets: " + BULLET_COUNT + "\nScore: " + SCORE + "\nHits: " + HITS;
        gameStateWords.draw(batch,
                words,
                20.0f,
                80.0f);
        batch.end();

        Actor[] bullet = bulletsGroup.getChildren().begin();
        Actor[] bird = birdsGroup.getChildren().begin();


        // 子弹和鸟的碰撞检测
        for (int i = 0; i < bulletsGroup.getChildren().size; i++) {
            Actor actor = bullet[i];
            for (int j = 0; j < birdsGroup.getChildren().size; j++) {
                Actor target = bird[j];
                if (BulletsFactory.attackAlive((BaseActor) target, (BaseActor) actor)) {

                    hitSound.play();
                    SCORE += 10;
                    HITS ++;
                    // 删除子弹
                    bulletsGroup.removeActor(actor);

                    // 在鸟的原位置生成星星落下
                    deadGroup.addDeadBird((BaseActor) target);

                    // 设置鸟掉落
                    ((BaseActor) target).setBirdState(BaseActor.FALL);

                    // 生成一只相同的掉落鸟
                    BaseActor fallBird = new BaseActor(((BaseActor) target).getRegion());
                    fallBird.setPosition(target.getX()+target.getWidth()/2, target.getY()+target.getHeight()/2);
                    fallBird.setOrigin(target.getWidth()/2, target.getHeight()/2);
                    float fallBirdSpeed = ((BaseActor) target).getFlySpeed();
                    // 掉落鸟保持原来的水平飞行速度
                    BirdsGroup.fly(fallBird, fallBirdSpeed);
                    // 设置其状态为掉落
                    fallBird.setBirdState(BaseActor.FALL);
                    Action rotateAction = Actions.rotateBy(180, 1);
                    fallBird.addAction(rotateAction);
                    stage.addActor(fallBird);

                    // 删除原鸟
                    birdsGroup.removeActor(target);
                    // 再添加一只鸟飞入
                    birdsGroup.addOneBird(birdsArray);
                    this.BIRDS_COUNT ++;
                    SCORE+=10;
                    break;
                }
            }
        }


        Actor[] dead = deadGroup.getChildren().begin();
        // 子弹和星星的碰撞检测
        for (int i = 0; i < bulletsGroup.getChildren().size; i++) {
            Actor actor = bullet[i];
            for (int j = 0; j < deadGroup.getChildren().size; j++) {
                Actor target = dead[j];
                if (BulletsFactory.attackAlive((BaseActor) target, (BaseActor) actor)) {
                    hitSound.play();
                    bulletsGroup.removeActor(actor);
                    deadGroup.removeActor(target);
                    // 增加十个子弹
                    BULLET_COUNT += 10;
                    // 增加100分
                    SCORE += 100;
                }
            }
        }

        // 子弹到达目标后消失
        bullet = bulletsGroup.getChildren().begin();
        for (int j = 0; j < bulletsGroup.getChildren().size; j++) {
            Actor actor = bullet[j];
            if (!BulletsFactory.checkAlive((BaseActor) actor)) {
                bulletsGroup.removeActor(actor);
            }
        }

        // 游戏结束判断
        if (BIRDS_COUNT == MAX_BIRD_NUM) {
            this.GAME_STATE = OVER;
            overSound.play();
            ShowGameOverTips();
        }
        else if (BULLET_COUNT == 0) {
            this.GAME_STATE = OVER;
            overSound.play();
            ShowGameOverTips();
        }
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void dispose() {
        // 释放纹理资源
        if (pao != null) {
            pao.dispose();
        }
        // 释放音效
        if (shootSound != null) {
            shootSound.dispose();
        }
        // 释放音效
        if (hitSound != null) {
            hitSound.dispose();
        }
        // 释放音效
        if (overSound != null) {
            overSound.dispose();
        }
        // 释放舞台资源
        if (stage != null) {
            stage.dispose();
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    public class InputProcessorEvent implements InputProcessor {

        @Override
        public boolean keyDown(int keycode) {
            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            return false;
        }

        @Override
        public boolean keyTyped(char character) {
            return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            // 限制子弹的数量为5个
            // addMessage("touchDown: screenX(" + screenX + ") screenY(" + screenY + ")");

            Vector3 vector3 = new Vector3(screenX, screenY, 0);
            stage.getCamera().unproject(vector3); // 坐标转化

            if (GAME_STATE == OVER) {
                if (vector3.y > stage.getHeight() - tryAgain.getHeight()) {
                    if (vector3.x < tryAgain.getWidth()) {
                        // Gdx.app.log(TAG, "X:" + vector3.x + ", y:"+vector3.y);
                        RestartGame();
                        return true;
                    }
                }
            }

            if (bulletsGroup.getChildren().size >=5) {
                return false;
            }

            if (BULLET_COUNT <= 0) {
                return false;
            }
            else {
                // 点击区域过低则不响应
                if (vector3.y < paoActor.getY() + stage.getHeight()/2) {
                    return false;
                }
                else {
                    BULLET_COUNT--;
                    shootSound.play();
                    // 添加新飞镖到飞镖组
                    bulletsGroup.addActor(BulletsFactory.createBullet(paoActor, vector3));
                    return true;
                }
            }

        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            // 设置炮台指向鼠标
            int rotateDegree = (int) (Math.toDegrees(Math.atan2((stage.getHeight() - screenY) ,(screenX - paoActor.getX())))-90);
            paoActor.setRotation(rotateDegree);
            // addMessage("mouseMoved: screenX(" + screenX + ") screenY(" + screenY + ")" + "degree" + rotateDegree+90);
            return true;
        }

        @Override
        public boolean scrolled(int amount) {
            return false;
        }
    }

    private void addMessage(String message) {
        messages.add(message + "time: " + System.currentTimeMillis());

        if (messages.size > MESSAGE_MAX) {
            messages.removeIndex(0);
        }
    }

    private void setActors(String[] birdsArray) {
        // 放置炮台
        this.pao = new Texture("弓箭.png");
        paoActor = new BaseActor(new TextureRegion(pao));
        paoActor.setPosition((stage.getWidth() - paoActor.getWidth())/2, 0);
        paoActor.setOrigin(paoActor.getWidth()/2, 0);
        stage.addActor(paoActor);

        // 添加5只鸟，每击落一只鸟，就添加一只新的鸟
        this.birdsGroup = new BirdsGroup(birdsArray, 5, WORLD_WIDTH, WORLD_HEIGHT);
        this.BIRDS_COUNT = 5;
        stage.addActor(birdsGroup);

        // 添加子弹群
        this.bulletsGroup = new Group();
        stage.addActor(bulletsGroup);

        // 添加星星群
        this.deadGroup = new DeadGroup();
        stage.addActor(deadGroup);

        // 添加游戏结束的提示信息
        this.GameOver = new BaseActor(new TextureRegion(new Texture("已结束.png")));
        GameOver.setPosition((stage.getWidth()-GameOver.getWidth())/2, - GameOver.getHeight());
        GameOver.setOrigin(GameOver.getWidth()/2, GameOver.getHeight()/2);
        stage.addActor(GameOver);

        this.tryAgain = new BaseActor(new TextureRegion(new Texture("again_1f.png")));
        tryAgain.setPosition( -tryAgain.getWidth(), stage.getHeight() - tryAgain.getHeight());
        tryAgain.setOrigin(tryAgain.getWidth()/2, tryAgain.getHeight()/2);
        stage.addActor(tryAgain);
    }

    private void ShowGameOverTips() {
        Action Popup = Actions.moveTo((stage.getWidth() - GameOver.getWidth())/2,(stage.getHeight() - GameOver.getHeight())/2, 1);
        deadGroup.clearChildren();
        birdsGroup.clearChildren();
        bulletsGroup.clearChildren();
        GameOver.addAction(Popup);
        tryAgain.setPosition(0, stage.getHeight() - tryAgain.getHeight());
        tryAgain.addListener(new tryAgainListener());

    }

    private void RestartGame() {
        if(this.GAME_STATE == OVER) {
            SequenceAction PopOff = Actions.sequence(Actions.rotateBy(360,0.5F), Actions.moveTo(-tryAgain.getWidth(), stage.getHeight() - tryAgain.getHeight(),0.5F));
            tryAgain.addAction(PopOff);
            GameOver.addAction(Actions.moveTo((stage.getWidth()-GameOver.getWidth())/2, - GameOver.getHeight(), 1));

            this.GAME_STATE = PLAY;
            restartSound.play();
            this.HITS = 0;
            this.birdsGroup = new BirdsGroup(birdsArray, 5, WORLD_WIDTH, WORLD_HEIGHT);
            this.BIRDS_COUNT = 5;
            stage.addActor(birdsGroup);
            this.BULLET_COUNT = MAX_BULLET_NUM;
            this.SCORE = 0;
        }
    }

    private class tryAgainListener extends ClickListener {

        @Override
        public void clicked(InputEvent event, float x, float y) {
            // 获取响应这个点击事件的演员
            Actor actor = event.getListenerActor();
            Gdx.app.log(TAG, "被点击: " + x + ", " + y + "; Actor: " + actor.getClass().getSimpleName());
        }
    }
}