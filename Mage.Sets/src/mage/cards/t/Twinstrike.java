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
package mage.cards.t;

import java.util.UUID;
import mage.abilities.condition.InvertCondition;
import mage.abilities.condition.common.HellbentCondition;
import mage.abilities.decorator.ConditionalOneShotEffect;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.abilities.effects.common.DestroyTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.target.common.TargetCreaturePermanent;

/**
 *
 * @author Wehk
 */
public class Twinstrike extends CardImpl {

    public Twinstrike(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId,setInfo,new CardType[]{CardType.INSTANT},"{3}{B}{R}");

        // Twinstrike deals 2 damage to each of two target creatures.
        this.getSpellAbility().addEffect(new ConditionalOneShotEffect(
                new DamageTargetEffect(2),
                new InvertCondition(HellbentCondition.getInstance()),
                "{this} deals 2 damage to each of two target creatures"));        
        // Hellbent - Cackling Flames deals 5 damage to that creature or player instead if you have no cards in hand.
        this.getSpellAbility().addEffect(new ConditionalOneShotEffect(
                new DestroyTargetEffect(),
                HellbentCondition.getInstance(),
                "<br/><br/><i>Hellbent</i> - Destroy those creatures instead if you have no cards in hand"));
        this.getSpellAbility().addTarget(new TargetCreaturePermanent(2,2));
     
    }

    public Twinstrike(final Twinstrike card) {
        super(card);
    }

    @Override
    public Twinstrike copy() {
        return new Twinstrike(this);
    }
}
