package com.pipeline.model.join;

import com.pipeline.model.Player;
import com.pipeline.model.ScoreEvent;

public class ScoreWithPlayer {
    private ScoreEvent scoreEvent;
    private Player player;

    public ScoreWithPlayer(ScoreEvent scoreEvent, Player player) {
        this.scoreEvent = scoreEvent;
        this.player = player;
    }

    public ScoreEvent getScoreEvent() {
        return this.scoreEvent;
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public String toString() {
        return "{" + " scoreEvent='" + getScoreEvent() + "'" + ", player='" + getPlayer() + "'" + "}";
    }
}
