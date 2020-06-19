package de.gesundkrank.jskills.trueskill;

import de.gesundkrank.jskills.factorgraphs.Factor;
import de.gesundkrank.jskills.factorgraphs.FactorGraph;
import de.gesundkrank.jskills.factorgraphs.FactorGraphLayerBase;
import de.gesundkrank.jskills.factorgraphs.FactorList;
import de.gesundkrank.jskills.factorgraphs.KeyedVariable;
import de.gesundkrank.jskills.factorgraphs.Schedule;
import de.gesundkrank.jskills.factorgraphs.ScheduleSequence;
import de.gesundkrank.jskills.trueskill.layers.IteratedTeamDifferencesInnerLayer;
import de.gesundkrank.jskills.trueskill.layers.PlayerPerformancesToTeamPerformancesLayer;
import de.gesundkrank.jskills.GameInfo;
import de.gesundkrank.jskills.IPlayer;
import de.gesundkrank.jskills.ITeam;
import de.gesundkrank.jskills.Rating;
import de.gesundkrank.jskills.numerics.GaussianDistribution;
import de.gesundkrank.jskills.trueskill.layers.PlayerPriorValuesToSkillsLayer;
import de.gesundkrank.jskills.trueskill.layers.PlayerSkillsToPerformancesLayer;
import de.gesundkrank.jskills.trueskill.layers.TeamDifferencesComparisonLayer;
import de.gesundkrank.jskills.trueskill.layers.TeamPerformancesToTeamPerformanceDifferencesLayer;

import java.util.*;

public class TrueSkillFactorGraph extends FactorGraph<TrueSkillFactorGraph> {

    private final List<FactorGraphLayerBase<GaussianDistribution>> layers;
    private final PlayerPriorValuesToSkillsLayer priorLayer;
    private GameInfo gameInfo;

    public TrueSkillFactorGraph(GameInfo gameInfo, Collection<ITeam> teams, int[] teamRanks) {
        this.priorLayer = new PlayerPriorValuesToSkillsLayer(this, teams);
        setGameInfo(gameInfo);

        this.layers = new ArrayList<>();
        this.layers.add(priorLayer);
        this.layers.add(new PlayerSkillsToPerformancesLayer(this));
        this.layers.add(new PlayerPerformancesToTeamPerformancesLayer(this));
        this.layers.add(new IteratedTeamDifferencesInnerLayer(
                this,
                new TeamPerformancesToTeamPerformanceDifferencesLayer(this),
                new TeamDifferencesComparisonLayer(this, teamRanks)));
    }

    public GameInfo getGameInfo() {
        return gameInfo;
    }

    private void setGameInfo(GameInfo info) {
        gameInfo = info;
    }

    public void buildGraph() {
        Object lastOutput = null;

        for (FactorGraphLayerBase<GaussianDistribution> currentLayer : layers) {
            if (lastOutput != null) {
                currentLayer.setRawInputVariablesGroups(lastOutput);
            }

            currentLayer.buildLayer();
            lastOutput = currentLayer.getRawOutputVariablesGroups();
        }
    }

    public void runSchedule() {
        Schedule<GaussianDistribution> fullSchedule = createFullSchedule();
        // TODO: Maybe something can be done w/ this?
        double fullScheduleDelta = fullSchedule.visit();
    }

    public double getProbabilityOfRanking() {
        FactorList<GaussianDistribution> factorList = new FactorList<>();

        for (FactorGraphLayerBase<GaussianDistribution> currentLayer : layers) {
            for (Factor<GaussianDistribution> currentFactor : currentLayer.getUntypedFactors()) {
                factorList.addFactor(currentFactor);
            }
        }

        double logZ = factorList.getLogNormalization();
        return Math.exp(logZ);
    }

    private Schedule<GaussianDistribution> createFullSchedule() {
        List<Schedule<GaussianDistribution>> fullSchedule = new ArrayList<>();

        for (FactorGraphLayerBase<GaussianDistribution> currentLayer : layers) {
            Schedule<GaussianDistribution>
                    currentPriorSchedule =
                    currentLayer.createPriorSchedule();
            if (currentPriorSchedule != null) {
                fullSchedule.add(currentPriorSchedule);
            }
        }

        // Getting as a list to use reverse()
        List<FactorGraphLayerBase<GaussianDistribution>> allLayers = new ArrayList<>(layers);
        Collections.reverse(allLayers);

        for (FactorGraphLayerBase<GaussianDistribution> currentLayer : allLayers) {
            Schedule<GaussianDistribution>
                    currentPosteriorSchedule =
                    currentLayer.createPosteriorSchedule();
            if (currentPosteriorSchedule != null) {
                fullSchedule.add(currentPosteriorSchedule);
            }
        }

        return new ScheduleSequence<>("Full schedule", fullSchedule);
    }

    public Map<IPlayer, Rating> getUpdatedRatings() {
        Map<IPlayer, Rating> result = new HashMap<>();
        for (List<KeyedVariable<IPlayer, GaussianDistribution>> currentTeam : priorLayer
                .getOutputVariablesGroups()) {
            for (KeyedVariable<IPlayer, GaussianDistribution> currentPlayer : currentTeam) {
                final Rating rating = new Rating(currentPlayer.getValue().getMean(),
                                                 currentPlayer.getValue().getStandardDeviation());
                result.put(currentPlayer.getKey(), rating);
            }
        }

        return result;
    }
}