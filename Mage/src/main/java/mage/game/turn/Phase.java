/*
 *  Copyright 2010 BetaSteward_at_googlemail.com. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY BetaSteward_at_googlemail.com ``AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BetaSteward_at_googlemail.com OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and should not be interpreted as representing official policies, either expressed
 *  or implied, of BetaSteward_at_googlemail.com.
 */
package mage.game.turn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import mage.constants.PhaseStep;
import mage.constants.TurnPhase;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.GameEvent.EventType;

/**
 *
 * @author BetaSteward_at_googlemail.com
 */
public abstract class Phase implements Serializable {

    protected TurnPhase type;
    protected List<Step> steps = new ArrayList<>();
    protected EventType event;
    protected EventType preEvent;
    protected EventType postEvent;

    protected UUID activePlayerId;
    protected Step currentStep;
    protected int count;

    public abstract Phase copy();

    public Phase() {
    }

    public Phase(final Phase phase) {
        this.type = phase.type;
        this.event = phase.event;
        this.preEvent = phase.preEvent;
        this.postEvent = phase.postEvent;
        this.activePlayerId = phase.activePlayerId;
        if (phase.currentStep != null) {
            this.currentStep = phase.currentStep.copy();
        }
        this.count = phase.count;
        for (Step step : phase.steps) {
            this.steps.add(step.copy());
        }
    }

    public TurnPhase getType() {
        return type;
    }

    public Step getStep() {
        return currentStep;
    }

    public void setStep(Step step) {
        this.currentStep = step;
    }

    public void resetCount() {
        count = 0;
    }

    public int getCount() {
        return count;
    }

    public boolean play(Game game, UUID activePlayerId) {
        if (game.isPaused() || game.gameOver(null)) {
            return false;
        }

        this.activePlayerId = activePlayerId;

        if (beginPhase(game, activePlayerId)) {

            for (Step step : steps) {
                if (game.isPaused() || game.gameOver(null)) {
                    return false;
                }
                if (game.getTurn().isEndTurnRequested() && !step.getType().equals(PhaseStep.CLEANUP)) {
                    continue;
                }
                currentStep = step;
                if (!game.getState().getTurnMods().skipStep(activePlayerId, getStep().getType())) {
                    playStep(game);
                    if (game.executingRollback()) {
                        return true;
                    }
                }
                if (!game.isSimulation() && checkStopOnStepOption(game)) {
                    return false;
                }

            }
            if (game.isPaused() || game.gameOver(null)) {
                return false;
            }
            count++;
            endPhase(game, activePlayerId);
            return true;
        }
        return false;
    }

    private boolean checkStopOnStepOption(Game game) {
        if (game.getOptions().stopOnTurn != null
                && game.getOptions().stopOnTurn <= game.getState().getTurnNum()
                && game.getOptions().stopAtStep == getStep().getType()) {
            game.pause();
            return true;
        }
        return false;
    }

    public boolean resumePlay(Game game, PhaseStep stepType, boolean wasPaused) {
        if (game.isPaused() || game.gameOver(null)) {
            return false;
        }

        this.activePlayerId = game.getActivePlayerId();
        Iterator<Step> it = steps.iterator();
        Step step;
        do {
            step = it.next();
            currentStep = step;
        } while (step.getType() != stepType);
        resumeStep(game, wasPaused);
        while (it.hasNext()) {
            step = it.next();
            if (game.isPaused() || game.gameOver(null)) {
                return false;
            }
            currentStep = step;
            if (!game.getState().getTurnMods().skipStep(activePlayerId, currentStep.getType())) {
                playStep(game);
                if (game.executingRollback()) {
                    return true;
                }
            }
        }

        if (game.isPaused() || game.gameOver(null)) {
            return false;
        }
        count++;
        endPhase(game, activePlayerId);
        return true;
    }

    public boolean beginPhase(Game game, UUID activePlayerId) {
        if (!game.replaceEvent(new GameEvent(event, null, null, activePlayerId))) {
            game.fireEvent(new GameEvent(preEvent, null, null, activePlayerId));
            return true;
        }
        return false;
    }

    public void endPhase(Game game, UUID activePlayerId) {
        game.fireEvent(new GameEvent(postEvent, null, null, activePlayerId));
        game.getState().getTriggers().removeAbilitiesOfNonExistingSources(game); // e.g. tokens that left the battlefield
    }

    public void prePriority(Game game, UUID activePlayerId) {
        currentStep.beginStep(game, activePlayerId);
    }

    public void postPriority(Game game, UUID activePlayerId) {
        currentStep.endStep(game, activePlayerId);
        //20091005 - 500.4/703.4n
        game.emptyManaPools();
        //20091005 - 500.9
        playExtraSteps(game, currentStep.getType());
    }

    protected void playStep(Game game) {
        if (!currentStep.skipStep(game, activePlayerId)) {
            game.getState().increaseStepNum();
            prePriority(game, activePlayerId);
            if (!game.isPaused() && !game.gameOver(null) && !game.executingRollback()) {
                currentStep.priority(game, activePlayerId, false);
                if (game.executingRollback()) {
                    return;
                }
            }
            if (!game.isPaused() && !game.gameOver(null) && !game.executingRollback()) {
                postPriority(game, activePlayerId);
            }
        }
    }

    protected void resumeStep(Game game, boolean wasPaused) {
        boolean resuming = true;
        if (currentStep == null || currentStep.getStepPart() == null) {
            game.end();
            return;
        }
        switch (currentStep.getStepPart()) {
            case PRE:
                if (wasPaused) {
                    currentStep.resumeBeginStep(game, activePlayerId);
                    resuming = false;
                } else {
                    prePriority(game, activePlayerId);
                }
            case PRIORITY:
                if (!game.isPaused() && !game.gameOver(null)) {
                    currentStep.priority(game, activePlayerId, resuming);
                }
            case POST:
                if (!game.isPaused() && !game.gameOver(null)) {
                    postPriority(game, activePlayerId);
                }
        }
    }

    private void playExtraSteps(Game game, PhaseStep afterStep) {
        while (true) {
            Step extraStep = game.getState().getTurnMods().extraStep(activePlayerId, afterStep);
            if (extraStep == null) {
                return;
            }
            currentStep = extraStep;
            playStep(game);
        }
    }

}
