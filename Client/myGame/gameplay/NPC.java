package myGame.gameplay;

import myGame.core.MyGame;
import org.joml.*;
import tage.*;
import tage.ai.behaviortrees.*;

import java.lang.Math;

public class NPC {
    private static final float NPC_Y_SPEED = 2.5f;

    private final MyGame game;
    private final GameObject npcPaddle;
    private final BehaviorTree npcBehavior;
    private boolean isNPCActive = true;

    public NPC(MyGame g) {
        game = g;
        npcPaddle = createNPCPaddle();
        npcBehavior = createNPCBehaviorTree();
    }

    private GameObject createNPCPaddle() {
        var paddle = new GameObject(GameObject.root(), game.getPaddleS_2(), game.getGhostTexture());
        paddle.setLocalScale(new Matrix4f().scaling(0.1f));
        paddle.setLocalRotation(new Matrix4f().rotateY((float) Math.toRadians(-90)).rotateX((float) Math.toRadians(90)));
        Vector3f playerPos = game.getPlayerPosition();
        paddle.setLocalTranslation(new Matrix4f().translation(-playerPos.x, playerPos.y, -3f));
        return paddle;
    }

    private BehaviorTree createNPCBehaviorTree() {
        BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
        BTSelector root = new BTSelector(0);
        root.addChild(new MirrorPlayerFollowBallYAction());
        bt.insertAtRoot(root);
        return bt;
    }

    public void update(float elapsedTime, GameObject ball) {
        if (isNPCActive) {
            npcBehavior.update(elapsedTime / 1000f);
        }
    }

    public void setIsNPCActive(boolean active) {
        isNPCActive = active;
    }

    public GameObject getNPCPaddle() {
        return npcPaddle;
    }

    private class MirrorPlayerFollowBallYAction extends BTAction {
        @Override
        protected BTStatus update(float deltaTime) {
            Vector3f playerPos = game.getPlayerPosition();
            Vector3f ballPos = game.getBall().getBall().getLocalTranslation().getTranslation(new Vector3f());
            Vector3f npcPos = npcPaddle.getLocalTranslation().getTranslation(new Vector3f());

            npcPos.x = -playerPos.x;

            float dy = ballPos.y - npcPos.y;
            float move = NPC_Y_SPEED * deltaTime;

            if (Math.abs(dy) > move) {
                npcPos.y += move * Math.signum(dy);
            } else {
                npcPos.y = ballPos.y;
            }

            npcPaddle.setLocalTranslation(new Matrix4f().translation(npcPos.x, npcPos.y, -3f));
            return BTStatus.BH_RUNNING;
        }
    }
}
